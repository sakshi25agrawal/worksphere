# Feature 17 – Resilience4j

## Overview

Resilience4j is a lightweight fault-tolerance library designed for Java applications. It helps build resilient microservices by providing mechanisms to gracefully handle failures occurring during communication between distributed services.

In the WorkSphere project, the Employee Service communicates with the Department Service using a Feign Client. If the Department Service becomes unavailable, repeated requests from the Employee Service can lead to cascading failures, poor application performance, and a degraded user experience.

To address this challenge, Resilience4j provides several resilience patterns such as Circuit Breaker, Retry, Fallback, Time Limiter, Rate Limiter, and Bulkhead. These patterns enable applications to recover gracefully from failures while maintaining high availability and system stability.

This feature focuses on implementing enterprise-grade fault tolerance in the Employee Service using Resilience4j.

---
# Phase 17.2 – Adding Resilience4j Dependencies

---

## Objective

The objective of this phase is to configure the Employee Service with the required Resilience4j libraries. These dependencies enable Spring Boot to implement fault tolerance patterns such as Circuit Breaker, Retry, and Fallback for inter-service communication.

At the end of this phase, the project will be ready to implement resilience features in the upcoming phases.

---

## Why Do We Need Additional Dependencies?

Spring Boot provides many built-in starters for web applications, data access, validation, security, and REST APIs. However, fault tolerance is not included as part of the default Spring Boot framework.

Since the Employee Service communicates with the Department Service using a Feign Client, there is always a possibility that the Department Service may become unavailable due to:

- Service downtime
- Network issues
- Slow response
- Temporary server failure
- Unexpected exceptions

Without any resilience mechanism, these failures directly affect the Employee Service.

Therefore, we integrate Resilience4j into our project.

---

## Dependencies Added

The following dependencies are added to the **employee-service** module.

### 1. Spring Cloud Circuit Breaker

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Purpose

This dependency integrates Resilience4j with Spring Cloud Circuit Breaker.

It provides:

- Spring Boot auto-configuration
- Circuit Breaker annotations
- Integration with Feign Client
- Automatic bean configuration
- Spring Cloud compatibility

Instead of manually creating Circuit Breaker instances, Spring Boot manages everything automatically.

---

### 2. Resilience4j Spring Boot Integration

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

### Purpose

This dependency contains the core Resilience4j implementation for Spring Boot 3.

It provides support for:

- Circuit Breaker
- Retry
- Rate Limiter
- Bulkhead
- Time Limiter
- Fallback Method
- Monitoring
- Configuration using `application.yml`

Without this dependency, the Resilience4j annotations cannot be used.

---

## Why Are Two Dependencies Required?

At first glance, both dependencies appear similar, but they serve different responsibilities.

| Dependency | Responsibility |
|------------|----------------|
| `spring-cloud-starter-circuitbreaker-resilience4j` | Integrates Spring Cloud Circuit Breaker with Resilience4j |
| `resilience4j-spring-boot3` | Provides the actual Resilience4j implementation and Spring Boot support |

The first dependency enables Spring Cloud to work with Circuit Breakers, while the second provides the underlying resilience features.

Both are required for proper integration.

---

## Maven Configuration

The dependencies are added inside the `<dependencies>` section of the **employee-service/pom.xml** file.

After saving the file, Maven automatically downloads all required libraries from Maven Central.

---

## Verifying the Dependencies

To verify that the dependencies are configured correctly:

1. Save the `pom.xml` file.
2. Reload the Maven project in IntelliJ IDEA.
3. Execute the following command:

```bash
mvn clean install
```

If the build completes successfully, the dependencies have been installed correctly.

---

## Expected Result

After successful configuration:

- Resilience4j libraries are available in the project.
- Spring Boot automatically detects the dependencies.
- Circuit Breaker annotations become available.
- Retry annotations become available.
- Fallback methods can be implemented.
- The application is ready for resilience configuration.

---

## Phase Summary

In this phase, we configured the required Resilience4j dependencies for the Employee Service.

The Spring Cloud starter enables Circuit Breaker integration, while the Resilience4j Spring Boot dependency provides the actual implementation of resilience patterns.

With the dependencies successfully added, the project is now ready for configuring the Circuit Breaker in the next phase.

# Phase 17.3 – Configuring Resilience4j

---

## Objective

The objective of this phase is to configure Circuit Breaker properties using the `application.yml` file.

Instead of relying on default values, we configure Resilience4j according to our application's requirements. Proper configuration allows the Circuit Breaker to detect service failures, prevent repeated failed requests, and automatically recover when the downstream service becomes available.

---

## Why Do We Need Configuration?

Simply adding the Resilience4j dependencies is not sufficient.

The Circuit Breaker needs configuration to determine:

- When should it open?
- How many failures are acceptable?
- How long should it wait before retrying?
- When should it close again?
- How many requests should be tested after recovery?

Without these configurations, Resilience4j uses default values, which may not be suitable for production applications.

---

## Circuit Breaker States

A Circuit Breaker operates in three different states.

```text
                Failure Threshold Reached
      ┌──────────────────────────────────────────┐
      │                                          │
      ▼                                          │
+-------------+      Failures      +-------------+
|   CLOSED    | -----------------> |    OPEN     |
+-------------+                    +-------------+
      ▲                                  │
      │                                  │
      │ Successful Calls                 │ Wait Duration
      │                                  ▼
      │                           +------------------+
      └-------------------------- |   HALF OPEN      |
                                  +------------------+
```

### CLOSED State

This is the normal operating state.

- Every request is forwarded to the Department Service.
- Resilience4j monitors every request.
- Success and failure counts are maintained.

---

### OPEN State

When the configured failure rate exceeds the threshold:

- Circuit Breaker moves to the OPEN state.
- No requests are sent to the Department Service.
- The client immediately receives a fallback response.

This prevents unnecessary network calls.

---

### HALF_OPEN State

After waiting for a configured duration:

- A limited number of requests are allowed.
- If these requests succeed, the Circuit Breaker closes.
- If they fail, it immediately opens again.

This allows automatic recovery.

---

# Circuit Breaker Configuration

The following configuration will be added inside the `application.yml` file.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      departmentService:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

---

# Configuration Properties

## 1. slidingWindowSize

```yaml
slidingWindowSize: 10
```

### Purpose

Defines the number of recent requests considered while calculating the failure rate.

Example:

If the last **10** requests are:

```
Success
Success
Failure
Failure
Success
Failure
Failure
Failure
Success
Failure
```

Only these 10 requests are evaluated.

---

## 2. minimumNumberOfCalls

```yaml
minimumNumberOfCalls: 5
```

### Purpose

The Circuit Breaker starts evaluating failures only after at least **5 requests** have been processed.

Example:

```
Only 3 requests completed
↓

No failure calculation
```

After the fifth request:

```
Failure Rate Calculation Starts
```

This prevents the Circuit Breaker from opening too early.

---

## 3. failureRateThreshold

```yaml
failureRateThreshold: 50
```

### Purpose

Defines the percentage of failed requests required to open the Circuit Breaker.

Example:

```
10 Requests

6 Failed

Failure Rate = 60%

Circuit Opens
```

Another example:

```
10 Requests

4 Failed

Failure Rate = 40%

Circuit Remains Closed
```

---

## 4. waitDurationInOpenState

```yaml
waitDurationInOpenState: 10s
```

### Purpose

Specifies how long the Circuit Breaker remains OPEN before allowing test requests.

Example:

```
OPEN

↓

Wait 10 Seconds

↓

HALF OPEN
```

---

## 5. permittedNumberOfCallsInHalfOpenState

```yaml
permittedNumberOfCallsInHalfOpenState: 3
```

### Purpose

Defines how many requests are allowed during the HALF_OPEN state.

Example:

```
Request 1 → Success

Request 2 → Success

Request 3 → Success

↓

Circuit Closes
```

If any request fails:

```
Circuit Opens Again
```

---

## 6. automaticTransitionFromOpenToHalfOpenEnabled

```yaml
automaticTransitionFromOpenToHalfOpenEnabled: true
```

### Purpose

Automatically changes the Circuit Breaker from OPEN to HALF_OPEN after the configured waiting period.

Without this property:

```
Circuit Remains OPEN
```

With this property enabled:

```
OPEN

↓

Wait Duration

↓

HALF OPEN

↓

Recovery Check
```

---

# Why These Values?

The chosen values are suitable for a development environment.

| Property | Value | Reason |
|----------|------|--------|
| slidingWindowSize | 10 | Monitor recent requests |
| minimumNumberOfCalls | 5 | Avoid premature failure calculation |
| failureRateThreshold | 50% | Open only after significant failures |
| waitDurationInOpenState | 10 seconds | Allow quick recovery testing |
| permittedNumberOfCallsInHalfOpenState | 3 | Validate service health |
| automaticTransition | true | Automatic recovery |

In production, these values may vary depending on traffic volume and business requirements.

---

# Expected Result

After applying this configuration:

- Circuit Breaker becomes available.
- Failure monitoring starts automatically.
- Service failures are tracked.
- Circuit transitions between CLOSED, OPEN, and HALF_OPEN states.
- The application is ready for Circuit Breaker implementation.

---

# Phase Summary

In this phase, we configured the Circuit Breaker for the Employee Service.

We learned how each configuration property influences the behavior of the Circuit Breaker and how Resilience4j manages service failures through its three operating states.

The next phase will implement the Circuit Breaker annotation in the Employee Service and connect it with a fallback method.

# Phase 17.4 – Implementing Circuit Breaker

---

## Objective

The objective of this phase is to implement the Circuit Breaker in the Employee Service while communicating with the Department Service.

Instead of directly calling the Department Service through the Feign Client, the request will now pass through a Circuit Breaker. The Circuit Breaker continuously monitors the health of the Department Service and prevents unnecessary requests when the service becomes unavailable.

This implementation improves application reliability and prevents cascading failures.

---

## Why Do We Need a Circuit Breaker?

Currently, the Employee Service validates the department by calling the Department Service using the Feign Client.

```java
departmentFeignClient.getDepartment(request.departmentId());
```

The above call works perfectly when the Department Service is available.

However, if the Department Service is:

- Down
- Restarting
- Experiencing network issues
- Responding very slowly

Every request from the Employee Service continues attempting to communicate with it.

This results in:

- Increased response time
- Repeated exceptions
- High CPU usage
- Blocked application threads
- Poor user experience

To solve this problem, we wrap the Feign Client call inside a Circuit Breaker.

---

# Current Flow

Before implementing the Circuit Breaker, the communication flow is:

```text
Client
   │
   ▼
Employee Service
   │
   ▼
Feign Client
   │
   ▼
Department Service
```

Every request reaches the Department Service even when it is unavailable.

---

# New Flow

After implementing the Circuit Breaker:

```text
Client
   │
   ▼
Employee Service
   │
   ▼
Circuit Breaker
   │
   ├──────────────► Department Service
   │
   └──────────────► Fallback Method
```

Now every request first passes through the Circuit Breaker.

Depending on the health of the Department Service, the Circuit Breaker decides whether the request should proceed.

---

# Circuit Breaker Annotation

Resilience4j provides the `@CircuitBreaker` annotation.

It is placed on the method that communicates with an external service.

Syntax:

```java
@CircuitBreaker(
        name = "departmentService",
        fallbackMethod = "departmentServiceFallback"
)
```

---

## Understanding the Annotation

### name

```java
name = "departmentService"
```

This specifies which Circuit Breaker configuration should be used.

The value must exactly match the configuration inside `application.yml`.

Example:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      departmentService:
```

If the names do not match, Spring Boot cannot locate the correct configuration.

---

### fallbackMethod

```java
fallbackMethod = "departmentServiceFallback"
```

Whenever the Circuit Breaker blocks the request or the Department Service throws an exception, this method is executed.

Instead of returning a 500 Internal Server Error, the application executes the fallback logic.

The fallback method will be implemented in the next phase.

---

# Where Should the Annotation Be Applied?

The annotation should be placed on the method responsible for calling the Department Service.

In the WorkSphere project, this is the `createEmployee()` method.

Example:

```java
@CircuitBreaker(
        name = "departmentService",
        fallbackMethod = "departmentServiceFallback"
)
@Override
public EmployeeResponse createEmployee(EmployeeRequest request) {

    // Existing business logic

}
```

This ensures that every Department Service validation is protected by the Circuit Breaker.

---

# Request Execution Flow

After implementing the annotation, every request follows this sequence:

```text
Create Employee Request

        │

        ▼

EmployeeService#createEmployee()

        │

        ▼

Circuit Breaker

        │

        ├────────────► Department Service Available
        │                     │
        │                     ▼
        │             Continue Processing
        │
        │
        └────────────► Department Service Unavailable
                              │
                              ▼
                       Execute Fallback Method
```

---

# Expected Behaviour

### Scenario 1

Department Service is available.

```text
Client

↓

Employee Service

↓

Circuit Breaker

↓

Department Service

↓

Employee Created Successfully
```

---

### Scenario 2

Department Service is temporarily unavailable.

```text
Client

↓

Employee Service

↓

Circuit Breaker

↓

Department Service Failure

↓

Fallback Method
```

No repeated requests are sent to the unavailable service.

---

### Scenario 3

Failure threshold exceeds configured limit.

```text
Failure Rate > 50%

↓

Circuit Breaker Opens

↓

No Request Sent

↓

Fallback Executed Immediately
```

This prevents unnecessary network calls.

---

# Code Changes

In this phase, only the `@CircuitBreaker` annotation is added.

No business logic is modified.

The fallback method will be implemented in the next phase.

---

# Expected Result

After implementing the annotation:

- Employee Service becomes protected by the Circuit Breaker.
- All Department Service calls pass through the Circuit Breaker.
- Service failures are monitored automatically.
- The application is ready for fallback implementation.

---

# Phase Summary

In this phase, we integrated the Circuit Breaker with the Employee Service by applying the `@CircuitBreaker` annotation.

The annotation ensures that all external calls to the Department Service are monitored by Resilience4j. Whenever repeated failures occur, the Circuit Breaker prevents unnecessary requests and prepares the application to execute a fallback method, which will be implemented in the next phase.

# Phase 17.5 – Implementing Fallback Method

---

## Objective

The objective of this phase is to implement a fallback method that will be executed whenever the Circuit Breaker prevents communication with the Department Service.

Instead of exposing internal server errors to the client, the fallback method provides a controlled response whenever the downstream service becomes unavailable.

This ensures graceful degradation of the application and improves user experience.

---

# What is a Fallback Method?

A fallback method is an alternative method that is automatically executed by Resilience4j whenever the protected method fails.

Failures may occur because of:

- Department Service is down
- Network timeout
- Connection refused
- Circuit Breaker is OPEN
- Unexpected runtime exception

Instead of propagating these exceptions to the client, Resilience4j invokes the fallback method.

---

# Why Do We Need a Fallback?

Consider the current Employee Service.

Whenever an employee is created, the following call is executed:

```java
departmentFeignClient.getDepartment(request.departmentId());
```

If the Department Service is unavailable:

```text
FeignException

↓

RuntimeException

↓

HTTP 500

↓

Client Receives Internal Server Error
```

This is not a good user experience.

Instead, we want:

```text
FeignException

↓

Circuit Breaker

↓

Fallback Method

↓

Meaningful Response
```

---

# How Does Resilience4j Find the Fallback Method?

The fallback method name is specified inside the annotation.

```java
@CircuitBreaker(
        name = "departmentService",
        fallbackMethod = "departmentServiceFallback"
)
```

Whenever an exception occurs, Resilience4j searches for a method named:

```java
departmentServiceFallback(...)
```

If the method is not found, the application fails during startup.

---

# Rules for Creating a Fallback Method

A fallback method must follow these rules:

- It must be in the same class.
- It must have the same return type as the original method.
- It must accept all original parameters.
- The last parameter must be an `Exception`.

---

## Original Method

```java
public EmployeeResponse createEmployee(EmployeeRequest request)
```

---

## Fallback Method

```java
public EmployeeResponse departmentServiceFallback(
        EmployeeRequest request,
        Exception ex
)
```

Notice that:

- `EmployeeRequest` is retained.
- `Exception` is added as the last parameter.
- Return type remains `EmployeeResponse`.

---

# Why Is the Exception Parameter Required?

The exception parameter contains the actual cause of failure.

Examples:

```text
FeignException
```

```text
ConnectException
```

```text
SocketTimeoutException
```

```text
CallNotPermittedException
```

Inside the fallback method, we can log the actual reason for the failure.

Example:

```java
log.error("Department Service failed : {}", ex.getMessage());
```

This greatly simplifies debugging in production environments.

---

# Implementation

Add the following method inside `EmployeeServiceImpl`.

```java
private EmployeeResponse departmentServiceFallback(
        EmployeeRequest request,
        Exception ex) {

    log.error("Department Service is unavailable : {}", ex.getMessage());

    throw new RuntimeException(
            "Department Service is temporarily unavailable. Please try again later."
    );
}
```

---

# Request Flow

Normal scenario:

```text
Client

↓

Employee Service

↓

Circuit Breaker

↓

Department Service

↓

Employee Created
```

Failure scenario:

```text
Client

↓

Employee Service

↓

Circuit Breaker

↓

Department Service Failure

↓

Fallback Method

↓

Controlled Exception
```

---

# Benefits of a Fallback Method

Using a fallback method provides several advantages:

- Prevents Internal Server Errors
- Provides meaningful error messages
- Improves application reliability
- Enables graceful degradation
- Simplifies production debugging
- Prevents cascading failures

---

# Expected Result

After implementing the fallback method:

- The application starts successfully.
- Circuit Breaker can locate the fallback method.
- Department Service failures are handled gracefully.
- The client receives a meaningful response instead of an unexpected server error.

---

# Phase Summary

In this phase, we implemented the fallback method for the Employee Service.

The fallback method acts as a safety mechanism whenever communication with the Department Service fails. Instead of exposing technical exceptions, it returns a controlled response, making the application more resilient and user-friendly.

# Phase 17.6 – Retry Mechanism using Resilience4j

---

## Objective

The objective of this phase is to automatically retry failed requests before declaring the Department Service unavailable.

Retry helps recover from temporary failures such as:

- Temporary network issues
- Short service downtime
- Connection timeout
- Slow startup of dependent services

Instead of failing immediately, the Employee Service will attempt the request multiple times before invoking the fallback method.

---

# Why Do We Need Retry?

Suppose the Department Service is restarting.

Without Retry:

```text
Employee Service
        │
        ▼
Department Service (temporarily unavailable)
        │
        ▼
Failure
```

Even though the service might become available after one second, the request has already failed.

Retry gives the downstream service another opportunity to respond.

---

# Request Flow Without Retry

```text
Client

↓

Employee Service

↓

Feign Client

↓

Department Service

↓

Failure

↓

Fallback
```

Only one attempt is made.

---

# Request Flow With Retry

```text
Client

↓

Employee Service

↓

Attempt 1

↓

Failure

↓

Attempt 2

↓

Failure

↓

Attempt 3

↓

Success
```

If all retry attempts fail:

```text
Attempt 1

↓

Failure

↓

Attempt 2

↓

Failure

↓

Attempt 3

↓

Failure

↓

Circuit Breaker

↓

Fallback
```

---

# Retry vs Circuit Breaker

Although both improve resilience, they solve different problems.

## Retry

Retry attempts the same request again when a temporary failure occurs.

It assumes the downstream service may recover quickly.

---

## Circuit Breaker

Circuit Breaker protects the application from repeatedly calling an unhealthy service.

Once the failure threshold is reached, it stops all outgoing calls and directly executes the fallback method.

---

# Working Together

The recommended execution order is:

```text
Retry

↓

Circuit Breaker

↓

Fallback
```

Meaning:

- Retry first attempts to recover.
- If all retry attempts fail, Circuit Breaker records the failure.
- Once enough failures occur, Circuit Breaker opens.
- Future requests immediately execute the fallback.

---

# Benefits of Retry

Using Retry provides several advantages:

- Recovers from transient failures
- Reduces unnecessary client errors
- Improves application reliability
- Prevents users from manually retrying requests
- Works seamlessly with Circuit Breaker

---

# Implementation Overview

In this phase we will:

- Add the `@Retry` annotation.
- Configure retry properties.
- Test multiple retry attempts.
- Observe retry logs.
- Integrate Retry with the existing Circuit Breaker.

---

# Phase Summary

Retry automatically attempts failed requests before considering them unsuccessful.

When combined with Circuit Breaker, Retry helps recover from short-lived failures while Circuit Breaker protects the system from repeatedly calling unhealthy services.

Together they form one of the most common resilience patterns used in enterprise microservices.


# Phase 17.7 – Testing Retry and Circuit Breaker

---

## Objective

The objective of this phase is to verify that both Retry and Circuit Breaker are working correctly under different scenarios.

Testing resilience is as important as implementing it because it confirms that the application behaves correctly during service failures and recovery.

---

# Test Scenarios

We will verify the following scenarios:

### Scenario 1 – Department Service is Running

Expected Result:

- Employee is created successfully.
- Department validation succeeds.
- No Retry is executed.
- Circuit Breaker remains CLOSED.

---

### Scenario 2 – Department Service is Down

Expected Result:

- Employee Service retries the configured number of times.
- Retry attempts fail.
- Fallback method executes.
- Client receives HTTP 503 (Service Unavailable).

---

### Scenario 3 – Continuous Failures

Expected Result:

- Circuit Breaker records failed requests.
- Failure threshold is reached.
- Circuit Breaker changes from CLOSED to OPEN.

After opening:

- No request is sent to Department Service.
- Fallback method executes immediately.

---

### Scenario 4 – Recovery

Expected Result:

After the configured wait duration:

OPEN

↓

HALF_OPEN

↓

Test Request

↓

If Success

↓

CLOSED

If Failure

↓

OPEN

---

# Circuit Breaker States

## CLOSED

All requests are allowed.

```
Employee Service
        │
        ▼
Department Service
```

---

## OPEN

Requests are blocked.

```
Employee Service

↓

Circuit Breaker

↓

Fallback
```

---

## HALF_OPEN

A limited number of requests are allowed to determine whether the downstream service has recovered.

```
Employee Service

↓

Test Request

↓

Department Service

↓

Success → CLOSED

Failure → OPEN
```

---

# Expected Logs

Typical logs observed during testing include:

```text
CircuitBreaker 'departmentService' changed state from CLOSED to OPEN
```

```text
CircuitBreaker 'departmentService' changed state from OPEN to HALF_OPEN
```

```text
CircuitBreaker 'departmentService' changed state from HALF_OPEN to CLOSED
```

---

# API Responses

## Department Available

HTTP Status

```
201 Created
```

---

## Department Not Found

HTTP Status

```
404 Not Found
```

---

## Department Service Down

HTTP Status

```
503 Service Unavailable
```

---

# Learning Outcome

After completing this phase, we understand:

- Retry attempts temporary recovery.
- Circuit Breaker protects the application from repeated failures.
- Fallback provides a graceful response to clients.
- Different HTTP responses represent different failure scenarios.

---

# Phase Summary

Testing confirms that the Employee Service can continue operating gracefully even when the Department Service is unavailable.

This completes the implementation and validation of Resilience4j-based fault tolerance.