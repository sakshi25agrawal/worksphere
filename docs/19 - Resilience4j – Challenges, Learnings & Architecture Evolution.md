# Document 19

# Resilience4j – Challenges, Learnings & Architecture Evolution

---

# Phase 19.1 – Purpose of this Document

## Objective

This document captures the complete engineering journey while implementing **Resilience4j** in the WorkSphere Employee Service.

Instead of documenting only the final implementation, this document explains:

- Initial implementation approach
- Challenges encountered during development
- Root cause analysis
- Different approaches explored
- Architecture evolution
- Production best practices
- Key learnings

The objective is to explain not only **how** Resilience4j was implemented, but also **why** certain implementation decisions were taken and how the architecture evolved after understanding the framework's internal behaviour.

---

## Why was this document created?

During the implementation of Retry, Circuit Breaker and Rate Limiter, several unexpected behaviours were observed.

Initially, it appeared that the implementation was incorrect.

However, after debugging, analysing the logs and understanding how Resilience4j internally applies multiple decorators using Spring AOP proxies, it became clear that most of the observed behaviour was expected.

Instead of simply fixing the code, every issue was analysed to understand:

- Why the issue occurred.
- What caused the unexpected behaviour.
- Why the previous implementation was not ideal.
- What changes were made.
- Why the new implementation is better.
- How similar problems are handled in production systems.

This document serves as an engineering diary and captures the complete learning process.

---

## Scope

This document covers the following topics:

- Retry
- Circuit Breaker
- Rate Limiter
- Fallback Mechanism
- Exception Propagation
- Business Exceptions vs Infrastructure Exceptions
- Spring AOP Proxy Behaviour
- Production Architecture Considerations
- Best Practices followed during implementation

---

## Expected Outcome

After reading this document, the reader should be able to understand:

- Why multiple Resilience4j annotations behave differently when placed on the same method.
- How fallback methods are executed.
- Why business exceptions should not be treated as infrastructure failures.
- Why Circuit Breaker prevents requests from reaching downstream services when the circuit is open.
- Why Rate Limiter, Retry and Circuit Breaker solve completely different problems.
- Why production applications generally separate resilience concerns into dedicated client services instead of placing all resilience annotations directly inside business service classes.

---

# Phase 19.2 – Initial Implementation Approach

## Objective

The primary objective was to make the communication between **Employee Service** and **Department Service** resilient by implementing the following Resilience4j patterns:

- Retry
- Circuit Breaker
- Rate Limiter

Initially, the idea was to apply all three resilience patterns directly on a single service method responsible for validating the department before creating an employee.

---

## Initial Architecture

The initial implementation looked as follows:

```text
Client

        │

        ▼

Employee Controller

        │

        ▼

Employee Service

        │

        ├── Business Validation

        ├── Retry

        ├── Circuit Breaker

        ├── Rate Limiter

        └── Department Feign Client

                     │

                     ▼

             Department Service
```

In this approach, the **Employee Service** was responsible for:

- Business validations.
- Calling Department Service using Feign Client.
- Handling Retry.
- Handling Circuit Breaker.
- Handling Rate Limiter.
- Handling Feign Exceptions.
- Returning business exceptions to the client.

---

## Initial Resilience Configuration

All three Resilience4j annotations were applied on the same method.

```java
@Retry(
        name = "departmentService",
        fallbackMethod = "departmentFallback"
)
@CircuitBreaker(
        name = "departmentService",
        fallbackMethod = "departmentFallback"
)
@RateLimiter(
        name = "departmentServiceRateLimiter",
        fallbackMethod = "departmentFallback"
)
```

The assumption was that each annotation would independently perform its responsibility:

- Retry would retry failed requests.
- Circuit Breaker would monitor service failures.
- Rate Limiter would control request traffic.
- The fallback method would be executed only once whenever any resilience pattern failed.

At this stage, the implementation appeared clean and straightforward because all resilience logic was centralized in a single method.

---

## Why this approach was chosen?

Initially, keeping all Resilience4j annotations on a single method seemed beneficial because:

- The implementation was simple.
- All resilience logic was available at one place.
- Less code duplication.
- Easier to understand while learning Resilience4j.
- Faster implementation.

However, during testing, several unexpected behaviours started appearing which required deeper investigation.

---

# Phase 19.3 – Issue 1: Multiple Fallbacks Were Executing

## Problem Statement

After implementing **Retry**, **Circuit Breaker**, and **Rate Limiter** on the same method, an unexpected behaviour was observed during testing.

Whenever the Rate Limiter rejected a request, the logs showed that multiple fallback executions were taking place.

Example:

```text
Rate limit exceeded :
RateLimiter 'departmentServiceRateLimiter' does not permit further calls.

↓

Department Service unavailable :
Too many requests.

↓

Department Service unavailable :
Department Service is temporarily unavailable.
```

Instead of executing only the Rate Limiter fallback, the Circuit Breaker fallback was also getting executed.

This behaviour was unexpected because the assumption was that only one resilience pattern would handle the request.

---

## Initial Understanding

Initially it was assumed that each annotation would execute independently.

Example:

```text
Retry
    ↓
Retry Fallback

Circuit Breaker
    ↓
Circuit Breaker Fallback

Rate Limiter
    ↓
Rate Limiter Fallback
```

The expectation was that whichever resilience pattern failed first would execute its own fallback and terminate the request.

However, this was not what actually happened.

---

## Actual Behaviour

During execution, the following sequence was observed.

```text
Request

↓

Rate Limiter

↓

Rate Limiter Fallback

↓

Throws RateLimitExceededException

↓

Circuit Breaker receives the exception

↓

Circuit Breaker Fallback

↓

Throws DepartmentServiceUnavailableException
```

As a result, multiple fallback methods were executed for the same request.

This also resulted in misleading logs because the original Rate Limiter exception was being converted into a different exception by another fallback.

---

## Root Cause

The root cause was the way Spring Boot applies Resilience4j annotations.

Retry, Circuit Breaker and Rate Limiter are implemented using **Spring AOP Proxies**.

When multiple annotations are placed on the same method, they do not execute independently.

Instead, they wrap each other in the form of nested decorators.

The execution chain becomes:

```text
Retry

↓

Circuit Breaker

↓

Rate Limiter

↓

Actual Business Method
```

Therefore, if an inner resilience pattern throws an exception from its fallback, the outer resilience pattern receives that exception and may execute its own fallback.

This behaviour is completely expected because every annotation acts as another wrapper around the business method.

---

## Key Learning

When multiple Resilience4j annotations are placed on the same method:

- They execute as nested decorators.
- They do not behave independently.
- Exceptions thrown by one fallback can be processed by another resilience pattern.
- This behaviour can lead to fallback chaining if the exception propagation is not handled carefully.

Understanding this behaviour was the first major learning during the implementation of Resilience4j.

---

# Phase 19.4 – Issue 2: Business Exceptions Were Being Converted into System Failures

## Problem Statement

After solving the fallback chaining issue, another unexpected behaviour was observed.

When an invalid Department Id was passed, the Department Service correctly returned **404 – Department Not Found**.

However, instead of returning the expected business exception, the application started throwing an **Internal Server Error (500)**.

The logs showed something similar to:

```text
Department fallback: ResourceNotFoundException

↓

Department fallback: RuntimeException

↓

Retry executed again

↓

Circuit Breaker counted it as a failure
```

Instead of returning:

```text
404
Department Not Found
```

the application returned:

```text
500
Internal Server Error
```

---

## Initial Fallback Implementation

The common fallback method initially ended with:

```java
throw new RuntimeException(ex);
```

The intention was to ensure that any unhandled exception was propagated further.

However, this introduced an unexpected issue.

---

## Why did this happen?

When the Department Service returned **404**, the application threw:

```java
ResourceNotFoundException
```

Inside the fallback method, none of the previous conditions matched.

Therefore, execution reached:

```java
throw new RuntimeException(ex);
```

As a result,

```text
ResourceNotFoundException

↓

Wrapped inside RuntimeException

↓

Retry considered it as a service failure

↓

Circuit Breaker considered it as a failed call

↓

Internal Server Error
```

Although the Department Service was healthy, the application started treating a normal business validation failure as an infrastructure failure.

---

## Root Cause

The fallback method was wrapping business exceptions inside a generic RuntimeException.

Retry and Circuit Breaker do not know the business meaning of an exception.

They only see:

```text
RuntimeException
```

Therefore they assume:

- Service failure
- Retry required
- Failure should be counted by Circuit Breaker

which is completely incorrect for a business validation.

---

## Solution

Instead of wrapping business exceptions, they should be propagated directly.

Example:

```java
if (ex instanceof ResourceNotFoundException resourceNotFoundException) {
    throw resourceNotFoundException;
}
```

Similarly,

```java
if (ex instanceof RateLimitExceededException rateLimitExceededException) {
    throw rateLimitExceededException;
}

if (ex instanceof DepartmentServiceUnavailableException departmentServiceUnavailableException) {
    throw departmentServiceUnavailableException;
}
```

Only unknown exceptions should reach the final fallback logic.

---

## Business Exception vs Infrastructure Exception

One of the biggest learnings from this issue was understanding the difference between business exceptions and infrastructure failures.

### Business Exception

Examples:

- Department does not exist.
- Employee email already exists.
- Invalid request data.

These indicate that:

- The application is working correctly.
- The downstream service is healthy.
- The request itself is invalid.

These exceptions should **never** trigger Retry or Circuit Breaker.

---

### Infrastructure Exception

Examples:

- Connection Refused
- Socket Timeout
- HTTP 500
- Service Down

These indicate that:

- The downstream service is unavailable.
- Retry may recover the request.
- Circuit Breaker should monitor repeated failures.

These exceptions should participate in resilience mechanisms.

---

## Key Learning

One of the most important production principles learned during this implementation was:

> **Never convert business exceptions into generic RuntimeExceptions.**

Business exceptions represent expected application behaviour.

Infrastructure exceptions represent unexpected system failures.

Resilience patterns such as Retry and Circuit Breaker should react only to infrastructure failures and should ignore business validation errors.

Failing to distinguish between these two categories results in unnecessary retries, incorrect Circuit Breaker statistics, and misleading application behaviour.

---

# Phase 19.5 – Issue 3: Why "Department Not Found" Was Not Returned When the Circuit Breaker Was Open

## Problem Statement

After fixing the exception propagation issue, another unexpected behaviour was observed during testing.

The following scenario was tested:

- Department Service was intentionally made unavailable.
- Circuit Breaker entered the **OPEN** state.
- A request was made using an invalid Department Id.

Example:

```text
Department Id = 100
```

The expected response was:

```text
404

Department Not Found
```

However, the application returned:

```text
503

Department Service is temporarily unavailable.
```

This initially appeared to be incorrect because the Department Id itself was invalid.

---

## Observed Logs

The application logs looked similar to the following:

```text
Before calling Department Service

↓

Department fallback:
CallNotPermittedException

↓

Department Service temporarily unavailable
```

Notice that no request reached the Department Service.

The application never received the opportunity to validate whether Department 100 existed or not.

---

## Initial Assumption

Initially it was assumed that the application would always validate the Department Id before returning any resilience-related exception.

The expected execution flow was:

```text
Employee Service

↓

Department Service

↓

404

↓

Department Not Found
```

However, this assumption was incorrect.

---

## Actual Execution Flow

When the Circuit Breaker is in the OPEN state, the request flow becomes:

```text
Employee Service

↓

Circuit Breaker

↓

CallNotPermittedException

↓

Fallback

↓

503 Service Unavailable
```

The request never reaches the Department Service.

Therefore,

Department Service never gets the opportunity to return:

```text
404

Department Not Found
```

---

## Why Does Circuit Breaker Behave Like This?

The primary responsibility of a Circuit Breaker is to protect the system from repeatedly calling an unhealthy downstream service.

Once the Circuit Breaker determines that the downstream service is unhealthy, it intentionally blocks further requests.

Instead of allowing the application to wait for:

- Connection Timeout
- Socket Timeout
- Connection Refused

it immediately rejects the request.

This behaviour prevents:

- Cascading failures
- Resource exhaustion
- Unnecessary network calls
- Increased response time

---

## Understanding the OPEN State

When the Circuit Breaker is OPEN:

```text
Employee Service

        │

        ▼

Circuit Breaker

        │

        ▼

Request Rejected

        │

        X

Department Service
```

The Department Service is never contacted.

Therefore:

- No HTTP 200
- No HTTP 404
- No HTTP 500

Only:

```text
CallNotPermittedException
```

is generated.

---

## Is This Behaviour Correct?

Yes.

This is exactly how Circuit Breakers are designed to work in production systems.

Companies such as:

- Netflix
- Amazon
- Microsoft
- Uber

follow the same principle.

If a downstream service is already known to be unhealthy, repeatedly asking that service for data only increases the load and delays recovery.

Instead, the Circuit Breaker immediately fails fast.

---

## Production Perspective

Suppose Department Service crashes.

Without Circuit Breaker:

```text
Employee Service

↓

Department Service

↓

Connection Timeout

↓

Retry

↓

Timeout

↓

Retry

↓

Timeout
```

Every request waits for network timeouts.

Application threads remain blocked.

Overall system performance degrades.

---

With Circuit Breaker:

```text
Employee Service

↓

Circuit Breaker

↓

503 Service Unavailable
```

The response is returned immediately without making any network call.

This significantly improves application stability.

---

## Important Learning

One of the biggest learnings from this issue was:

> A business validation can only happen if the downstream service is actually reachable.

If the Circuit Breaker blocks the request before reaching the downstream service, business validations such as:

- Department Exists
- Employee Exists
- Resource Not Found

cannot be performed.

The resilience layer takes higher priority because protecting system availability is more important than validating business data when the downstream service is already unavailable.

---

## Key Takeaway

This behaviour initially appeared to be incorrect.

However, after understanding the purpose of Circuit Breaker, it became clear that this is the expected production behaviour.

A Circuit Breaker does not validate business rules.

Its responsibility is to decide whether the downstream service should be called at all.

If the service is unavailable, the request is rejected immediately to protect the overall system.

---

# Phase 19.6 – Understanding Business Exceptions vs Infrastructure Exceptions

## Why is this distinction important?

One of the biggest learnings during the implementation of Resilience4j was understanding that **not every exception represents a service failure**.

Initially, every exception was treated as a failure.

As a result:

- Retry retried unnecessary requests.
- Circuit Breaker counted business validation failures as service failures.
- Incorrect application behaviour was observed.

After analysing the implementation, it became clear that exceptions can be broadly classified into two categories:

- Business Exceptions
- Infrastructure Exceptions

Understanding this distinction is essential for implementing resilience patterns correctly.

---

# Business Exceptions

Business exceptions indicate that the application is working correctly, but the request itself violates a business rule.

Examples include:

- Department does not exist.
- Employee email already exists.
- Invalid employee data.
- Invalid department id.
- Validation failures.

Example:

```text
Department Service

↓

Department Id = 100

↓

404 Not Found

↓

ResourceNotFoundException
```

In this scenario:

- Department Service is healthy.
- Database is reachable.
- Network communication is successful.
- The application behaves exactly as expected.

The only issue is that the requested department does not exist.

Therefore,

this is **not a system failure**.

---

# Infrastructure Exceptions

Infrastructure exceptions indicate that the application cannot communicate with the downstream service.

Examples include:

- Connection Refused
- Socket Timeout
- Service Down
- HTTP 500
- HTTP 503
- DNS Failure
- Network Failure

Example:

```text
Employee Service

↓

Department Service

↓

Connection Refused
```

In this case:

- The downstream service is unavailable.
- Retry may recover the request.
- Circuit Breaker should monitor repeated failures.

This represents an actual infrastructure problem.

---

# Comparison

| Business Exception | Infrastructure Exception |
|--------------------|--------------------------|
| Department Not Found | Service Down |
| Employee Already Exists | Connection Refused |
| Invalid Request | Socket Timeout |
| Validation Failure | HTTP 500 |
| Duplicate Email | DNS Failure |

---

# How Resilience Patterns Should Behave

## Retry

Retry should only retry temporary failures.

Retry should **NOT** retry:

- ResourceNotFoundException
- DuplicateResourceException
- ValidationException

Retry **SHOULD** retry:

- Connection Refused
- Timeout
- HTTP 500
- HTTP 503

---

## Circuit Breaker

Circuit Breaker should monitor infrastructure failures only.

It should **NOT** open because:

- Department does not exist.
- Employee already exists.
- Invalid request.

It should open only when:

- Service is unavailable.
- Network communication fails.
- Repeated infrastructure failures occur.

---

## Rate Limiter

Rate Limiter is completely different.

It does not care whether:

- Department exists.
- Department does not exist.
- Service is healthy.

Its only responsibility is controlling traffic.

Example:

```text
1000 Requests

↓

Rate Limiter

↓

Only configured requests allowed

↓

Remaining requests rejected
```

Rate Limiter protects the downstream service from excessive traffic.

---

# Why did we configure ignoreExceptions?

To prevent Retry and Circuit Breaker from treating business exceptions as failures, the following configuration was introduced.

Example:

```yaml
ignoreExceptions:
  - com.worksphere.common.exception.ResourceNotFoundException
  - com.worksphere.common.exception.RateLimitExceededException
```

This tells Resilience4j:

> These exceptions are expected application behaviour.

Do not:

- Retry them.
- Count them as failures.
- Open the Circuit Breaker because of them.

---

# Production Perspective

In production systems:

Business Exceptions are handled by business logic.

Infrastructure Exceptions are handled by resilience patterns.

Keeping these responsibilities separate results in:

- Cleaner architecture.
- Accurate Circuit Breaker statistics.
- Fewer unnecessary retries.
- Better application performance.
- Easier debugging.

---

# Key Learning

One of the most important principles learned during this implementation is:

> **Business validation failures should never be treated as infrastructure failures.**

Resilience patterns exist to improve system availability and fault tolerance.

They should react only to failures that affect communication with downstream services, not to valid business responses returned by healthy services.

---

# Phase 19.7 – Evolution of the Fallback Strategy

## Background

Initially, a separate fallback method was created for every Resilience4j annotation.

The implementation looked like this:

```text
Retry
    │
    ▼
retryFallback()

Circuit Breaker
    │
    ▼
circuitBreakerFallback()

Rate Limiter
    │
    ▼
rateLimiterFallback()
```

The expectation was that every resilience pattern would execute its own fallback independently.

This appeared to be a clean design because every annotation had its own dedicated fallback.

---

# Problem with Multiple Fallback Methods

During testing, unexpected behaviour was observed.

For example,

when the Rate Limiter rejected a request,

its fallback method threw a custom exception.

Instead of stopping there,

the Circuit Breaker received that exception and executed its own fallback.

The execution flow became:

```text
Rate Limiter

↓

RateLimiterFallback()

↓

RateLimitExceededException

↓

Circuit Breaker

↓

CircuitBreakerFallback()

↓

DepartmentServiceUnavailableException
```

As a result,

multiple fallback methods were executed for a single request.

This produced:

- Duplicate logs.
- Incorrect exception propagation.
- Confusing debugging behaviour.
- Wrong API responses.

---

# First Attempt to Solve the Problem

To avoid multiple fallback executions,

all Resilience4j annotations were configured to use the same fallback method.

Example:

```java
@Retry(
        name = "departmentService",
        fallbackMethod = "departmentFallback"
)

@CircuitBreaker(
        name = "departmentService",
        fallbackMethod = "departmentFallback"
)

@RateLimiter(
        name = "departmentServiceRateLimiter",
        fallbackMethod = "departmentFallback"
)
```

Now,

every resilience pattern delegated exception handling to a single method.

---

# Unified Fallback Design

Instead of maintaining multiple fallback methods,

the exception type was inspected inside one common fallback.

Example:

```java
if (ex instanceof RequestNotPermitted) {
    ...
}

if (ex instanceof CallNotPermittedException) {
    ...
}

if (ex instanceof FeignException.NotFound) {
    ...
}

if (ex instanceof FeignException) {
    ...
}
```

The fallback became responsible for converting framework exceptions into meaningful business exceptions.

---

# Advantages of a Common Fallback

Using a common fallback provided several benefits.

### Centralized Exception Handling

All resilience-related exceptions are translated in one place.

---

### Easier Maintenance

Adding a new exception requires changes in only one method instead of multiple fallback methods.

---

### Better Readability

The resilience configuration becomes simpler because every annotation points to the same fallback.

---

### Consistent API Responses

All resilience patterns return exceptions using a consistent format.

---

### Easier Debugging

Since every resilience exception passes through a single method,

logs become easier to understand and maintain.

---

# Limitation of the Common Fallback

Although the common fallback simplified exception handling,

another issue was discovered.

When multiple Resilience4j annotations are applied to the same method,

the fallback itself can still be executed multiple times because of Spring AOP proxy chaining.

For example,

a Rate Limiter exception could still propagate to the Circuit Breaker,

causing the same fallback to execute again.

This behaviour is expected because Retry, Circuit Breaker and Rate Limiter are nested decorators.

Therefore,

a common fallback improves maintainability,

but it does not completely eliminate exception propagation between resilience patterns.

---

# Final Learning

Using a common fallback method is generally a better approach than maintaining separate fallback methods.

However,

when multiple Resilience4j annotations are applied on the same method,

developers must understand that:

- Exceptions may still travel through multiple decorators.
- Exception propagation must be handled carefully.
- Business exceptions should be rethrown directly.
- Infrastructure exceptions should be translated appropriately.

A common fallback simplifies the implementation,

but understanding the execution order of Resilience4j decorators remains essential.

---

# Phase 19.8 – Architecture Evolution: Moving Resilience Logic to a Dedicated Client Service

## Background

During the initial implementation, all Resilience4j annotations were directly applied inside the `EmployeeServiceImpl`.

The service was responsible for:

- Employee business validation.
- Calling Department Service.
- Retry logic.
- Circuit Breaker.
- Rate Limiter.
- Feign exception handling.
- Fallback implementation.

As more resilience patterns were added, the service became difficult to maintain.

---

# Initial Design

The initial implementation looked like this:

```text
                    Employee Controller
                            │
                            ▼
                  EmployeeServiceImpl
                            │
        ┌───────────────────┼────────────────────┐
        │                   │                    │
        ▼                   ▼                    ▼
  Business Logic        Resilience4j        Feign Client
   Validation        (Retry/CB/RateLimiter)
                            │
                            ▼
                  Department Service
```

### Responsibilities of EmployeeServiceImpl

The EmployeeServiceImpl was responsible for:

- Validating employee request.
- Validating department.
- Calling Department Service.
- Handling Retry.
- Handling Circuit Breaker.
- Handling Rate Limiter.
- Handling fallback methods.
- Handling Feign exceptions.
- Creating employee.
- Returning response.

This violated one of the most important design principles.

---

# Problem

The EmployeeServiceImpl started handling multiple responsibilities.

It now contained:

- Business logic.
- Infrastructure logic.
- Network communication.
- Resilience configuration.
- Exception translation.

As a result:

- The class became larger.
- Testing became more difficult.
- Future maintenance became harder.
- Adding new resilience patterns increased complexity.

---

# Single Responsibility Principle (SRP)

According to the **Single Responsibility Principle**, a class should have only one reason to change.

In our implementation:

EmployeeServiceImpl would change when:

- Business logic changes.
- Retry configuration changes.
- Circuit Breaker configuration changes.
- Rate Limiter configuration changes.
- Feign client changes.

Clearly, it had multiple reasons to change.

---

# Improved Architecture

To improve the design, the resilience logic was moved into a dedicated client service.

```text
                    Employee Controller
                            │
                            ▼
                  EmployeeServiceImpl
                            │
                            ▼
              DepartmentClientService
                            │
          ┌─────────────────┼──────────────────┐
          │                 │                  │
          ▼                 ▼                  ▼
      Retry          Circuit Breaker     Rate Limiter
                            │
                            ▼
                    DepartmentFeignClient
                            │
                            ▼
                   Department Service
```

---

# Responsibilities After Refactoring

## EmployeeServiceImpl

Responsible only for:

- Employee business validation.
- Calling DepartmentClientService.
- Saving employee.
- Returning response.

It no longer knows anything about:

- Retry
- Circuit Breaker
- Rate Limiter
- Feign exceptions

---

## DepartmentClientService

Responsible for:

- Calling Department Service.
- Applying Retry.
- Applying Circuit Breaker.
- Applying Rate Limiter.
- Translating Feign exceptions.
- Executing fallback methods.

All resilience-related concerns are centralized in one place.

---

# Benefits of This Design

### 1. Better Separation of Concerns

Business logic and infrastructure logic are completely separated.

---

### 2. Easier Maintenance

Changes in resilience configuration do not affect business logic.

---

### 3. Improved Readability

EmployeeServiceImpl becomes much smaller and easier to understand.

---

### 4. Easier Unit Testing

EmployeeServiceImpl can be tested by mocking only DepartmentClientService.

DepartmentClientService can be tested independently for resilience behaviour.

---

### 5. Reusability

Any future service that needs department information can reuse the same DepartmentClientService without duplicating resilience logic.

---

# Production Perspective

Large enterprise applications generally avoid placing Resilience4j annotations directly inside business services.

Instead, a dedicated client layer is introduced.

Example:

```text
EmployeeService

↓

DepartmentClientService

↓

Feign Client

↓

Department Service
```

This architecture is commonly used because it:

- Improves maintainability.
- Promotes code reuse.
- Simplifies testing.
- Keeps business logic independent of infrastructure concerns.

---

# Key Learning

Moving resilience logic into a dedicated client service is not mandatory for small projects.

However, as applications grow and interact with multiple external services, this design significantly improves code quality, maintainability, and scalability.

It also aligns with enterprise architecture principles such as:

- Single Responsibility Principle (SRP)
- Separation of Concerns (SoC)
- Layered Architecture
- Reusability

---

# Phase 19.9 – Production Standard Approach

## Is the Current Implementation Production Ready?

The current implementation successfully demonstrates all Resilience4j patterns in a single method.

This approach is excellent for learning because it helps understand:

- Retry
- Circuit Breaker
- Rate Limiter
- Fallback mechanism
- Exception propagation
- Spring AOP execution flow

However, in large-scale enterprise applications, this is generally not the preferred architecture.

---

## Why?

Applying multiple resilience annotations directly on a business service increases complexity.

As more resilience patterns are added, the service starts handling:

- Business logic
- Infrastructure communication
- Exception translation
- Retry
- Circuit Breaker
- Rate Limiter
- Fallback implementation

This makes the service harder to maintain.

---

## Production Architecture

Most enterprise applications introduce a dedicated client layer.

Example:

```text
Controller

        │

        ▼

Employee Service

        │

        ▼

Department Client Service

        │

        ▼

Feign Client

        │

        ▼

Department Service
```

In this architecture,

Employee Service is completely unaware of:

- Retry
- Circuit Breaker
- Rate Limiter
- Feign exceptions

It simply requests department information from DepartmentClientService.

The client service becomes responsible for all resilience-related concerns.

---

## Advantages

This architecture provides:

- Better Separation of Concerns
- Easier Maintenance
- Better Unit Testing
- Improved Code Reusability
- Cleaner Business Logic
- Easier Debugging

This approach closely follows enterprise development practices.

---

## Why did we keep everything together?

For the WorkSphere learning project, all resilience patterns were intentionally implemented together.

The objective was to:

- Understand each Resilience4j component.
- Observe their interaction.
- Study exception propagation.
- Understand fallback execution.
- Learn Spring AOP behaviour.

Once the concepts became clear, the architecture was evolved toward a cleaner client-based design.


---

# Phase 19.10 – Future Improvements in WorkSphere

The current implementation serves as a strong learning foundation.

However, future microservices in WorkSphere will demonstrate each resilience pattern independently using real-world scenarios.

---

## Planned Architecture

### Employee Service

Purpose:

Validate Department before creating Employee.

Patterns:

- Retry
- Circuit Breaker
- Rate Limiter

Reason:

Employee Service depends on Department Service.

---

### Leave Service

Purpose:

Apply Leave Request.

Pattern:

Bulkhead

Reason:

Prevent excessive leave requests from exhausting application threads.

---

### Payroll Service

Purpose:

Generate Salary.

Pattern:

Time Limiter

Reason:

Salary calculation or external integrations may take longer than expected.

---

### Notification Service

Purpose:

Send Email / SMS.

Pattern:

Retry

Reason:

Notification providers often experience temporary failures.

---

### Attendance Service

Purpose:

Punch In / Punch Out.

Pattern:

Rate Limiter

Reason:

Prevent users from continuously hitting attendance APIs.

---

### Project Service

Purpose:

Assign Employees to Projects.

Pattern:

Circuit Breaker

Reason:

Avoid cascading failures if Employee Service becomes unavailable.

---

## Goal

Instead of applying every resilience pattern everywhere,

each microservice will demonstrate the resilience pattern that naturally fits its business use case.

This approach better represents enterprise architecture and improves maintainability.


---

# Phase 19.11 – Final Learnings & Best Practices

During the implementation of Resilience4j, several important engineering lessons were learned.

---

## Key Learnings

### 1. Retry is for temporary failures.

Retry should only be used when the failure may recover automatically.

Examples:

- Connection Timeout
- HTTP 500
- Service Unavailable

Retry should never be used for business validation failures.

---

### 2. Circuit Breaker protects downstream services.

Its objective is not to validate business rules.

Its objective is to stop unnecessary network calls when a dependency is already known to be unhealthy.

---

### 3. Rate Limiter protects application capacity.

It controls request traffic.

It is completely independent of business validation.

---

### 4. Business Exceptions are not system failures.

Examples:

- Department Not Found
- Employee Already Exists
- Validation Failure

These exceptions should never trigger Retry or open the Circuit Breaker.

---

### 5. Infrastructure Exceptions should participate in resilience.

Examples:

- Connection Refused
- Timeout
- HTTP 500
- Service Down

These exceptions should be monitored by Retry and Circuit Breaker.

---

### 6. Multiple Resilience4j annotations execute as nested decorators.

Retry,

Circuit Breaker,

and Rate Limiter

do not execute independently.

They wrap each other using Spring AOP proxies.

Understanding this execution flow is essential for designing correct fallback strategies.

---

### 7. Fallback methods should never hide business exceptions.

Business exceptions should be propagated directly.

Only infrastructure failures should be translated into resilience-related responses.

---

### 8. Separate Business Logic from Infrastructure Logic.

Business services should focus on business operations.

Infrastructure concerns such as:

- Retry
- Circuit Breaker
- Rate Limiter
- Feign communication

should ideally be isolated inside dedicated client services.

---

## Overall Learning

Implementing Resilience4j was not just about adding annotations.

It required understanding:

- Distributed systems
- Fault tolerance
- Service communication
- Exception categorization
- Spring AOP
- Enterprise architecture principles

The implementation evolved through multiple iterations, debugging sessions, and architectural improvements.

This journey provided a much deeper understanding of how resilience patterns behave in real production environments.

---

## Conclusion

The Resilience4j implementation in WorkSphere successfully demonstrates:

- Retry
- Circuit Breaker
- Rate Limiter
- Exception Handling
- Fallback Strategy
- Production Concepts
- Architecture Evolution

More importantly, it documents the engineering decisions taken during implementation, making the project a practical learning reference rather than just a code sample.