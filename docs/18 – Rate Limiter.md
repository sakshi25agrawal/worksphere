# Document 18

# Feature 18 – Rate Limiter

---

# Phase 18.1 – Introduction to Rate Limiter

## Objective

In this feature, we will implement **Rate Limiting** using **Resilience4j** to protect the Employee Service from receiving too many requests within a short period of time.

The Rate Limiter controls how many requests are allowed during a configurable time window and rejects any additional requests once the configured limit has been reached.

---

## What is Rate Limiter?

A **Rate Limiter** is a resilience pattern that restricts the number of requests a client can make to an API within a specified period.

Instead of allowing unlimited requests, the application grants only a fixed number of permissions during every refresh interval.

Once all permissions are consumed, additional requests are rejected until the permissions are refreshed.

---

## Why do we need Rate Limiter?

Even if our application is highly optimized, an unexpected spike in traffic can overload the application.

Without Rate Limiting, the following problems may occur:

- API abuse
- Bot attacks
- DDoS attacks
- High CPU utilization
- Database overload
- Increased response time
- Thread starvation
- Poor user experience

Rate Limiter helps protect the application by controlling incoming traffic.

---

## Real World Example

Imagine an ATM.

You can perform only a limited number of transactions within a certain period.

If the limit is exceeded, the bank temporarily blocks additional transactions.

The same concept is applied to APIs.

```
Request 1 ✅

Request 2 ✅

Request 3 ✅

Request 4 ❌

Wait for configured interval...

Request 5 ✅
```

---

## How does Rate Limiter work?

A Rate Limiter maintains a fixed number of permissions.

Each incoming request consumes one permission.

When all permissions have been consumed, any further request is rejected.

After the configured refresh interval, the permissions are restored automatically.

Example

```
Available Permissions = 3

↓

Request 1
Permissions = 2

↓

Request 2
Permissions = 1

↓

Request 3
Permissions = 0

↓

Request 4
Rejected

↓

After Refresh Interval

Permissions = 3
```

---

## Rate Limiter in WorkSphere

Current Employee Service Flow

```
Client
   │
   ▼
Employee Service
   │
   ▼
Department Service
```

If a client sends hundreds or thousands of requests continuously, the Employee Service may become overloaded.

With Rate Limiter

```
Incoming Requests
        │
        ▼
Rate Limiter
        │
 ┌──────┴────────┐
 │               │
 ▼               ▼
Allowed      Rejected
 │
 ▼
Employee Service
```

Only the configured number of requests are allowed to reach the service.

---

## Circuit Breaker vs Rate Limiter

| Circuit Breaker | Rate Limiter |
|-----------------|--------------|
| Protects against failing downstream services | Protects against excessive incoming traffic |
| Opens after repeated failures | Rejects requests after reaching the configured limit |
| Improves fault tolerance | Improves traffic control |
| Failure based | Request count based |

---

## Resilience4j Modules

```
Resilience4j

├── Circuit Breaker ✅ Completed
├── Retry
├── Rate Limiter ← Current Feature
├── Bulkhead
├── Time Limiter
└── Cache
```

---

## What we will implement

In this feature we will:

- Configure Rate Limiter
- Apply Rate Limiter to Employee APIs
- Create a fallback method
- Handle Rate Limiter exceptions globally
- Test using Postman
- Verify Rate Limiter behaviour
- Document the implementation

---

## Expected Behaviour

Suppose we configure

```
Limit = 3 Requests

Refresh Interval = 10 Seconds
```

The behaviour will be

```
Request 1 ✅

Request 2 ✅

Request 3 ✅

Request 4 ❌

Request 5 ❌

Wait 10 Seconds

Request 6 ✅

Request 7 ✅
```

---

## Outcome

After completing this feature, the Employee Service will be protected against excessive traffic by allowing only a configured number of requests within a given time window.

---

**Status:** ✅ Phase 18.1 Completed

---

# Phase 18.2 – Configuring Rate Limiter

# Phase 18.2 – Configuring Rate Limiter

## Objective

In this phase, we will configure a Rate Limiter for the Employee Service.

The Rate Limiter will allow only a limited number of requests within a specified time window.

---

## Why configuration is required?

By default, Resilience4j does not know:

- How many requests should be allowed
- When the counter should reset
- How long a request should wait
- Which Rate Limiter instance to use

Therefore, we configure these properties inside **application.yml**.

---

## Location

Employee Service

```
src/main/resources/application.yml
```

---

## Rate Limiter Configuration

```yaml
resilience4j:
  ratelimiter:
    instances:
      departmentServiceRateLimiter:
        limitForPeriod: 3
        limitRefreshPeriod: 10s
        timeoutDuration: 0
```

---

## Understanding each property

### 1. limitForPeriod

```yaml
limitForPeriod: 3
```

This defines the maximum number of requests allowed during one refresh period.

Example

```
Allowed

Request 1 ✅

Request 2 ✅

Request 3 ✅

Blocked

Request 4 ❌

Request 5 ❌
```

---

### 2. limitRefreshPeriod

```yaml
limitRefreshPeriod: 10s
```

After every 10 seconds, the request count resets.

Example

```
Time 0 sec

3 permissions available

↓

Request 1

Request 2

Request 3

↓

Permissions exhausted

↓

Wait 10 seconds

↓

Permissions become 3 again
```

---

### 3. timeoutDuration

```yaml
timeoutDuration: 0
```

This defines how long a request waits for permission.

Setting

```yaml
timeoutDuration: 0
```

means

> If permission is unavailable, fail immediately.

No waiting.

---

If configured as

```yaml
timeoutDuration: 5s
```

then

```
Request

↓

Permission unavailable

↓

Wait for 5 seconds

↓

Permission received?

YES → Execute

NO → Fail
```

---

## Why are we using timeoutDuration = 0?

For REST APIs, waiting usually provides a poor user experience.

Instead of making users wait,

we immediately return

```
HTTP 429

Too Many Requests
```

or a custom fallback response.

This is the recommended approach for stateless APIs.

---

## Current configuration summary

| Property | Value | Meaning |
|----------|-------|---------|
| limitForPeriod | 3 | Allow only 3 requests |
| limitRefreshPeriod | 10 seconds | Reset counter every 10 seconds |
| timeoutDuration | 0 | Fail immediately if limit is exceeded |

---

## Request Flow

```
Incoming Request
        │
        ▼
Check Available Permission
        │
        ├── Available
        │        ▼
        │   Execute API
        │
        └── Not Available
                 ▼
        Reject Request
```

---

## Why are we creating a named instance?

```
departmentServiceRateLimiter
```

Later we will use

```java
@RateLimiter(name = "departmentServiceRateLimiter")
```

This annotation tells Resilience4j which configuration to use.

---

## Expected Behaviour

```
Request 1 ✅

Request 2 ✅

Request 3 ✅

Request 4 ❌

Request 5 ❌

(wait 10 seconds)

Request 6 ✅
```

---

## Outcome

After completing this phase,

our Employee Service will have a fully configured Rate Limiter instance that can be applied to any API using the `@RateLimiter` annotation.

---

# Phase 18.3 – Applying Rate Limiter to Employee Service

## Objective

In this phase, we will apply the configured Rate Limiter to the Employee Service using the `@RateLimiter` annotation.

This annotation intercepts every incoming request and checks whether permission is available before allowing the method to execute.

If the request limit has already been reached, the request will not execute. Instead, a fallback method will be invoked.

---

## Location

```
employee-service

└── service

    └── impl

        └── EmployeeServiceImpl.java
```

---

## Existing Method

Current implementation

```java
@Override
public EmployeeResponse createEmployee(EmployeeRequest request) {

    // Business Logic

}
```

---

## Applying the Annotation

Add the following annotation above the method.

```java
@RateLimiter(
        name = "departmentServiceRateLimiter",
        fallbackMethod = "departmentRateLimiterFallback"
)
```

The method becomes

```java
@Override
@RateLimiter(
        name = "departmentServiceRateLimiter",
        fallbackMethod = "departmentRateLimiterFallback"
)
public EmployeeResponse createEmployee(EmployeeRequest request) {

    // Existing Business Logic

}
```

---

## Understanding the Annotation

### name

```java
name = "departmentServiceRateLimiter"
```

This refers to the Rate Limiter instance configured inside

```
application.yml
```

```yaml
resilience4j:
  ratelimiter:
    instances:
      departmentServiceRateLimiter:
```

Both names **must be exactly the same**.

---

### fallbackMethod

```java
fallbackMethod = "departmentRateLimiterFallback"
```

If the request exceeds the configured limit,

instead of throwing an exception directly,

Resilience4j automatically invokes the fallback method.

This allows us to return a meaningful response to the client.

---

## Execution Flow

```
Incoming Request

        │

        ▼

Rate Limiter

        │

Permission Available?

        │

 ┌──────┴─────────┐

 │                │

 ▼                ▼

YES              NO

 │                │

 ▼                ▼

Execute      Call Fallback

Business        Method

Logic
```

---

## Why do we use a Fallback?

Without a fallback

```
Client

↓

Exception

↓

HTTP 500
```

With a fallback

```
Client

↓

Fallback Method

↓

Meaningful Response

↓

HTTP 429 or Custom Response
```

The application behaves more gracefully.

---

## Benefits

Using `@RateLimiter`

- No manual request counting.
- No custom synchronization logic.
- Cleaner business code.
- Automatic integration with Resilience4j.
- Easy configuration through YAML.

---

## Current Flow

```
Client

↓

createEmployee()

↓

Rate Limiter

↓

Permission Available?

↓

YES

↓

Business Logic
```

If permission is unavailable

```
Client

↓

createEmployee()

↓

Rate Limiter

↓

Permission Not Available

↓

Fallback Method
```

---

## Outcome

After completing this phase,

the Employee Service is protected by the configured Rate Limiter.

Every incoming request now passes through the Rate Limiter before the business logic executes.

---
# Phase 18.4 – Creating the Fallback Method

## Objective

In this phase, we will create the fallback method that will be executed whenever the Rate Limiter rejects an incoming request.

Instead of exposing internal exceptions to the client, the fallback method allows us to return a controlled and meaningful response.

---

## Why do we need a Fallback Method?

Suppose our Rate Limiter allows only **3 requests every 10 seconds**.

The first three requests execute normally.

```
Request 1 ✅

Request 2 ✅

Request 3 ✅
```

The fourth request exceeds the configured limit.

Instead of executing the business logic, Resilience4j immediately invokes the fallback method.

```
Request 4

↓

Rate Limiter

↓

Permission Not Available

↓

Fallback Method
```

---

## Creating the Fallback Method

Add the following method inside **EmployeeServiceImpl.java**

```java
public EmployeeResponse departmentRateLimiterFallback(
        EmployeeRequest request,
        RequestNotPermitted ex) {

    throw new DepartmentServiceUnavailableException(
            "Too many requests. Please try again after some time."
    );
}
```

---

## Method Signature

The fallback method **must** satisfy the following rules.

### Rule 1

The method name must match the annotation.

```java
@RateLimiter(
        name = "departmentServiceRateLimiter",
        fallbackMethod = "departmentRateLimiterFallback"
)
```

Therefore,

```java
departmentRateLimiterFallback(...)
```

must exist.

---

### Rule 2

The fallback method must contain **all original method parameters**.

Original method

```java
public EmployeeResponse createEmployee(
        EmployeeRequest request)
```

Fallback

```java
public EmployeeResponse departmentRateLimiterFallback(
        EmployeeRequest request,
        RequestNotPermitted ex)
```

Notice that

```
EmployeeRequest request
```

is exactly the same.

---

### Rule 3

The last parameter must be the exception generated by Resilience4j.

```java
RequestNotPermitted ex
```

This exception indicates that the Rate Limiter has rejected the request because no permissions are currently available.

---

## Why is RequestNotPermitted Required?

Whenever the request limit is exceeded,

Resilience4j throws

```java
RequestNotPermitted
```

Instead of propagating it directly,

it passes the exception into the fallback method.

```
Incoming Request

↓

Rate Limiter

↓

Request Limit Reached

↓

RequestNotPermitted

↓

Fallback Method
```

---

## Why are we throwing DepartmentServiceUnavailableException?

Inside the fallback we throw

```java
DepartmentServiceUnavailableException
```

This exception is already handled in our **GlobalExceptionHandler**.

As a result,

our API returns a consistent JSON response.

Instead of

```
Internal Server Error
```

the client receives

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Too many requests. Please try again after some time."
}
```

---

## Complete Flow

```
Client

↓

createEmployee()

↓

Rate Limiter

↓

Permission Available?

        │

 ┌──────┴─────────┐

 │                │

 ▼                ▼

YES              NO

 │                │

 ▼                ▼

Business     RequestNotPermitted

Logic               │

                    ▼

          departmentRateLimiterFallback()

                    │

                    ▼

DepartmentServiceUnavailableException

                    │

                    ▼

GlobalExceptionHandler

                    │

                    ▼

HTTP 503 Response
```

---

## Why not return EmployeeResponse?

We could return a dummy `EmployeeResponse`, but that would incorrectly indicate that the employee was created successfully.

Throwing a custom exception is better because:

- It clearly communicates that the request failed.
- The Global Exception Handler generates a consistent API response.
- The client knows the operation was not completed.

---

## Outcome

After completing this phase,

- The Rate Limiter has a fallback method.
- Excess requests are handled gracefully.
- Internal exceptions are hidden from clients.
- API responses remain consistent across the application.

---
# Phase 18.5 – Testing the Rate Limiter

## Objective

In this phase, we will verify that the configured Rate Limiter is working correctly.

The objective is to ensure that:

- Only the configured number of requests are allowed.
- Additional requests are rejected.
- The fallback method is invoked.
- The Global Exception Handler returns the expected response.
- Requests are allowed again after the refresh interval.

---

## Test Configuration

Our current configuration is:

```yaml
resilience4j:
  ratelimiter:
    instances:
      departmentServiceRateLimiter:
        limitForPeriod: 3
        limitRefreshPeriod: 10s
        timeoutDuration: 0
```

Meaning:

- Maximum Requests = **3**
- Refresh Interval = **10 Seconds**
- Waiting Time = **0 Seconds**

---

## API Used

```
POST /api/v1/employees
```

Example Request

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@test.com",
  "salary": 50000,
  "departmentId": 100
}
```

---

## Test 1 – First Request

Expected Result

```
HTTP 201 Created
```

Reason

The Rate Limiter still has available permissions.

```
Permissions

3

↓

2
```

---

## Test 2 – Second Request

Expected Result

```
HTTP 201 Created
```

Permissions

```
2

↓

1
```

---

## Test 3 – Third Request

Expected Result

```
HTTP 201 Created
```

Permissions

```
1

↓

0
```

Now all permissions have been consumed.

---

## Test 4 – Fourth Request

Expected Result

```
HTTP 503 Service Unavailable
```

Response

```json
{
    "timestamp": "...",
    "status": 503,
    "error": "Service Unavailable",
    "message": "Too many requests. Please try again after some time.",
    "path": "/api/v1/employees"
}
```

Reason

No permissions are available.

Instead of executing the business logic,

Resilience4j invokes the fallback method.

---

## Verify Application Logs

The logs should contain a message similar to:

```
Department Service is unavailable :
CircuitBreaker 'departmentService' is OPEN
```

or for the Rate Limiter,

```
RequestNotPermitted

RateLimiter 'departmentServiceRateLimiter'
```

This confirms that the request never entered the business logic.

---

## Wait for Refresh Interval

Wait

```
10 Seconds
```

After the refresh interval,

Resilience4j restores all permissions automatically.

```
Permissions

0

↓

3
```

---

## Test Again

Send another request.

Expected Result

```
HTTP 201 Created
```

This confirms that the permissions have been refreshed.

---

## Complete Execution Flow

```
Request 1

↓

Permission Available

↓

Business Logic

↓

Employee Created

──────────────────────────

Request 2

↓

Permission Available

↓

Business Logic

↓

Employee Created

──────────────────────────

Request 3

↓

Permission Available

↓

Business Logic

↓

Employee Created

──────────────────────────

Request 4

↓

No Permission Available

↓

Rate Limiter

↓

Fallback Method

↓

DepartmentServiceUnavailableException

↓

Global Exception Handler

↓

HTTP 503 Response

──────────────────────────

Wait 10 Seconds

↓

Permissions Refreshed

↓

Request 5

↓

Business Logic

↓

Employee Created
```

---

## Expected Behaviour Summary

| Request | Result |
|----------|--------|
| Request 1 | Allowed |
| Request 2 | Allowed |
| Request 3 | Allowed |
| Request 4 | Rejected |
| Wait 10 Seconds | Permissions Reset |
| Request 5 | Allowed Again |

---

## Outcome

After completing this phase, we successfully verified that:

- The Rate Limiter restricts excessive requests.
- Requests beyond the configured limit are rejected.
- The fallback method is executed automatically.
- The Global Exception Handler returns a consistent response.
- Permissions are refreshed automatically after the configured interval.

---


