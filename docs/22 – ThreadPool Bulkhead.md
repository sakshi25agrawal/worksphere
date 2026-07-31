# 22 – ThreadPool Bulkhead

## 22.1 Introduction

### Overview

In the previous chapter, WorkSphere implemented **Semaphore Bulkhead** to limit the number of concurrent requests accessing the Department Service.

Semaphore Bulkhead successfully protects downstream services by restricting the number of requests that are allowed to execute simultaneously. However, it does not solve another important production problem: **thread starvation**.

In enterprise microservice architectures, downstream services may become slow due to heavy load, database latency, network issues, or third-party integrations. When a synchronous request waits for a slow downstream service, the caller thread remains blocked until the response is received.

If many requests become blocked simultaneously, the application may exhaust all available server threads, causing the entire service to become unresponsive even though only one downstream dependency is slow.

ThreadPool Bulkhead addresses this problem by isolating slow operations into a dedicated thread pool.

---

## Why ThreadPool Bulkhead is Needed

Consider the following architecture.

```text
Employee Service
        │
        ▼
Department Service
```

Assume the Department Service normally responds within:

```text
50 milliseconds
```

Under normal conditions, the request flow is fast and efficient.

However, suppose the Department Service becomes slow and starts responding in:

```text
10 seconds
```

Now imagine 100 users send requests simultaneously.

Without any protection, every request occupies one Tomcat request thread while waiting for the Department Service.

The execution flow becomes:

```text
Tomcat Thread

↓

Waiting for Department Service

↓

10 Seconds

↓

Response Returned
```

During this waiting period, the Tomcat thread cannot process any other request.

---

## Thread Starvation

If all Tomcat request threads become occupied waiting for slow downstream services, no threads remain available to serve new incoming requests.

The application eventually reaches the following state:

```text
Tomcat Thread Pool

100 Threads

↓

100 Threads Busy

↓

No Available Threads

↓

Employee Service Stops Accepting Requests
```

Although the Employee Service itself is functioning correctly, it becomes unavailable because every thread is blocked waiting for another service.

This situation is commonly known as **Thread Starvation**.

...

## 22.2 Internal Working of ThreadPool Bulkhead

---

# Introduction

Understanding how ThreadPool Bulkhead works internally is essential before implementing it in WorkSphere.

Unlike Semaphore Bulkhead, which simply limits the number of concurrent requests, ThreadPool Bulkhead creates a dedicated pool of worker threads. Incoming requests are submitted to this pool, allowing the original request thread to be released immediately.

This mechanism provides thread isolation and prevents slow downstream services from exhausting the application's request threads.

---

# Internal Architecture

The execution flow of ThreadPool Bulkhead is shown below.

```text
                    Client Request
                          │
                          ▼
                 Tomcat Request Thread
                          │
                          ▼
              ThreadPool Bulkhead Queue
                          │
                          ▼
                 Worker Thread Pool
                          │
                          ▼
                 Department Service
                          │
                          ▼
                 Department Response
                          │
                          ▼
                     CompletableFuture
                          │
                          ▼
                    HTTP Response
```

Unlike Semaphore Bulkhead, the Tomcat request thread does not execute the remote call directly.

---

# Execution Flow

The complete lifecycle of a request is as follows.

### Step 1

Client sends a request.

```text
POST /api/v1/employees
```

---

### Step 2

The request reaches

```text
EmployeeController
```

---

### Step 3

The request is delegated to

```text
EmployeeServiceImpl
```

---

### Step 4

Business logic calls

```java
departmentGateway.getDepartment(id);
```

---

### Step 5

Gateway delegates the request to

```text
DepartmentResilienceService
```

---

### Step 6

Instead of directly invoking Feign Client,

ThreadPool Bulkhead submits the task to its worker thread pool.

```text
Tomcat Thread

↓

Submit Task

↓

Worker Queue
```

Immediately after submission, the Tomcat thread becomes available to process another incoming request.

---

### Step 7

One worker thread picks the task from the queue.

```text
Worker Thread

↓

DepartmentFeignClient

↓

Department Service
```

The worker thread now waits for the Department Service response.

The Tomcat request thread is completely free.

---

### Step 8

Once the Department Service responds,

the worker thread completes the

```text
CompletableFuture
```

and the response is returned to the client.

---

# Thread Lifecycle

The lifecycle of a request thread changes significantly.

## Semaphore Bulkhead

```text
Tomcat Thread

↓

Department Service

↓

Waiting...

↓

Waiting...

↓

Waiting...

↓

Response
```

The request thread remains occupied.

---

## ThreadPool Bulkhead

```text
Tomcat Thread

↓

Submit Task

↓

Released Immediately
```

Worker thread:

```text
Worker Thread

↓

Department Service

↓

Waiting...

↓

Waiting...

↓

Response
```

The waiting happens inside the worker thread instead of the request thread.

---

# Thread Isolation

ThreadPool Bulkhead introduces thread isolation.

Instead of using one common thread pool,

the application now has two independent thread pools.

```text
                 Application

        ┌──────────────────────────┐
        │                          │
        │     Tomcat Thread Pool   │
        │                          │
        └─────────────┬────────────┘
                      │
                      ▼

             ThreadPool Bulkhead

                      │

        ┌─────────────▼────────────┐
        │                          │
        │ Worker Thread Pool       │
        │                          │
        └─────────────┬────────────┘
                      │
                      ▼

             Department Service
```

If the Department Service becomes slow,

only the Worker Thread Pool is affected.

The Tomcat thread pool remains healthy.

---

# Request Queue

ThreadPool Bulkhead introduces another important concept:

Queueing.

Suppose

Worker Threads

```text
5
```

Queue Capacity

```text
10
```

Execution becomes

```text
Request 1

↓

Worker Thread 1


Request 2

↓

Worker Thread 2


...

Request 5

↓

Worker Thread 5


Request 6

↓

Queue


Request 7

↓

Queue


...

Request 15

↓

Queue
```

All worker threads are busy,

so new requests wait in the queue.

---

# Queue Full Scenario

Suppose

```text
Worker Threads = 5

Queue Capacity = 10
```

Maximum requests accepted

```text
5 Running

+

10 Waiting

=

15
```

Request number

```text
16
```

is immediately rejected.

This prevents unlimited memory usage.

---

# Core Components

ThreadPool Bulkhead consists of four important components.

## 1. Worker Threads

These execute remote service calls.

Example

```text
Department Service

Notification Service

Payment Gateway
```

---

## 2. Queue

Stores waiting requests.

If all worker threads are busy,

new tasks wait inside the queue.

---

## 3. Task Submission

Instead of executing immediately,

requests are submitted as tasks.

Most implementations internally use

```java
CompletableFuture
```

or

```java
ExecutorService
```

---

## 4. Future

The caller receives

```java
CompletableFuture<T>
```

instead of the actual object.

The result becomes available once the worker thread finishes execution.

---

# Advantages

ThreadPool Bulkhead provides several advantages.

- Thread isolation
- Better scalability
- Prevents request thread starvation
- Supports queueing
- Suitable for blocking operations
- Better resource utilization
- Improved system responsiveness

---

# Limitations

Despite its advantages,

ThreadPool Bulkhead also introduces additional complexity.

- More memory usage
- Thread management overhead
- Context switching
- Queue configuration required
- Slightly higher latency compared to Semaphore Bulkhead

Therefore, it should only be used when thread isolation is actually required.

---

# Real Production Examples

ThreadPool Bulkhead is commonly used for:

- Payment Gateway
- Email Service
- SMS Service
- Third-party REST APIs
- File Upload
- PDF Generation
- Report Generation
- Image Processing

These operations are generally slow and should not occupy request threads.

---

# Summary

ThreadPool Bulkhead isolates slow operations by introducing a dedicated worker thread pool.

Instead of allowing request threads to wait for downstream services, the request is converted into a task, executed by a worker thread, and completed asynchronously.

This architecture significantly improves resilience, prevents thread starvation, and allows the application to remain responsive even when external dependencies become slow.

---

# 22 – ThreadPool Bulkhead

## 22.3 ThreadPool Bulkhead Configuration & Important Properties

---

# Introduction

ThreadPool Bulkhead works by creating a dedicated thread pool that executes slow or blocking operations independently of the application's main request threads.

Unlike Semaphore Bulkhead, which only limits concurrent execution, ThreadPool Bulkhead introduces several configuration properties that control thread creation, request queuing, and resource utilization.

Choosing appropriate values for these properties is critical because incorrect configuration may lead to poor performance, excessive memory usage, or unnecessary request rejections.

This section explains every important configuration property used by ThreadPool Bulkhead.

---

# ThreadPool Bulkhead Architecture

```text
                   Client Request
                         │
                         ▼
                Tomcat Request Thread
                         │
                         ▼
               Submit Task to Queue
                         │
          ┌──────────────┴──────────────┐
          │                             │
          ▼                             ▼
     Queue Available              Queue Full
          │                             │
          ▼                             ▼
 Worker Thread Executes          Reject Request
          │
          ▼
 Department Service
          │
          ▼
 CompletableFuture
          │
          ▼
 HTTP Response
```

The behavior of this flow is controlled by several configuration properties.

---

# Configuration Properties

---

## 1. coreThreadPoolSize

### Definition

The minimum number of worker threads that ThreadPool Bulkhead keeps available for executing requests.

These threads remain alive even when there are no incoming requests.

---

### Example

```java
coreThreadPoolSize = 2
```

Execution

```text
Worker Thread 1

Worker Thread 2
```

These two threads are always available.

Even if there are no requests, they remain active.

---

### Why is it important?

Creating threads is an expensive operation.

Keeping a small number of worker threads alive improves performance because new requests do not need to wait for thread creation.

---

### Production Recommendation

Choose a small value.

Typical values:

```text
2

4

8
```

depending on CPU capacity.

---

# 2. maxThreadPoolSize

### Definition

The maximum number of worker threads that can exist simultaneously.

When incoming requests exceed the available core threads, additional worker threads may be created until this limit is reached.

---

### Example

```java
coreThreadPoolSize = 2

maxThreadPoolSize = 5
```

Execution

```text
Request 1

↓

Worker 1


Request 2

↓

Worker 2


Request 3

↓

Worker 3


Request 4

↓

Worker 4


Request 5

↓

Worker 5
```

After reaching five worker threads,

no additional worker threads are created.

---

### Why is it important?

It prevents unlimited thread creation.

Unlimited threads can cause:

- High CPU usage
- Memory exhaustion
- Context switching overhead

---

### Production Recommendation

Choose based on:

- CPU cores
- Downstream latency
- Expected traffic

Never use unnecessarily large values.

---

# 3. queueCapacity

### Definition

Maximum number of waiting tasks allowed inside the queue.

If all worker threads are busy,

new requests wait inside this queue.

---

### Example

```java
queueCapacity = 10
```

Execution

```text
Worker Threads Busy

↓

Request 6

↓

Queue Position 1


Request 7

↓

Queue Position 2
```

The queue stores requests until a worker thread becomes available.

---

### Queue Full

Suppose

```text
Workers = 5

Queue = 10
```

Maximum accepted requests

```text
5 Running

+

10 Waiting

=

15 Requests
```

Request number

```text
16
```

is rejected immediately.

---

### Why is queueCapacity important?

Without a queue,

requests would fail immediately after all worker threads become busy.

A queue absorbs temporary traffic spikes.

---

### Production Recommendation

Keep a reasonable queue size.

Large queues consume memory.

Very small queues increase request rejection.

---

# 4. keepAliveDuration

### Definition

Time an extra worker thread remains alive when it becomes idle.

Applicable only to threads created above the core thread count.

---

### Example

Configuration

```java
keepAliveDuration = 20 seconds
```

Execution

```text
Core Threads

↓

Always Alive


Extra Worker Thread

↓

Idle

↓

20 Seconds

↓

Destroyed
```

---

### Why is it important?

It releases unnecessary worker threads after traffic decreases.

This improves memory utilization.

---

### Production Recommendation

Usually between

```text
20 Seconds

30 Seconds

60 Seconds
```

depending on application workload.

---

# Property Relationship

Example

```java
coreThreadPoolSize = 2

maxThreadPoolSize = 5

queueCapacity = 10
```

Execution

```text
Request 1

↓

Worker 1


Request 2

↓

Worker 2


Request 3

↓

Worker 3


Request 4

↓

Worker 4


Request 5

↓

Worker 5


Request 6

↓

Queue


...

Request 15

↓

Queue


Request 16

↓

Rejected
```

This demonstrates how all configuration properties work together.

---

# How Requests are Processed

Step 1

```text
Request Arrives
```

↓

Step 2

```text
Worker Available?
```

Yes

↓

Assign Worker

---

No

↓

Queue Available?

Yes

↓

Store in Queue

---

No

↓

Reject Request

---

# Resource Utilization

Worker threads consume:

- CPU
- Memory
- Context switching

Queue consumes:

- Heap Memory

Therefore,

ThreadPool Bulkhead must always be configured carefully.

---

# Recommended Configuration for WorkSphere

For our learning project,

the following configuration is sufficient.

```text
Core Threads        : 2

Maximum Threads     : 4

Queue Capacity      : 10

Keep Alive          : 20 Seconds
```

This configuration allows us to easily observe thread creation, queueing, and rejection during testing.

---

# Enterprise Considerations

Configuration values should always be selected based on:

- Number of CPU cores
- Average response time
- Peak traffic
- Downstream latency
- Available memory
- SLA requirements

There is no universal configuration suitable for every application.

---

# Summary

ThreadPool Bulkhead introduces configurable worker threads and request queues that provide stronger isolation than Semaphore Bulkhead.

Understanding each configuration property is essential because these values directly affect application performance, resource utilization, scalability, and resilience.

A well-configured ThreadPool Bulkhead protects the application from slow downstream services while maintaining high responsiveness under heavy load.

---

## 22.4 Implementing ThreadPool Bulkhead in WorkSphere

---

# Introduction

After understanding the internal architecture and configuration properties of ThreadPool Bulkhead, the next step is to integrate it into WorkSphere.

At this stage, WorkSphere already supports the following Resilience4j patterns:

- Retry
- Circuit Breaker
- Rate Limiter
- Semaphore Bulkhead

Rather than replacing the existing implementation, ThreadPool Bulkhead will be introduced as an additional resilience strategy.

This allows the project to demonstrate both types of Bulkhead patterns and provides a practical comparison between them.

---

# Existing Architecture

The current communication flow is shown below.

```text
EmployeeController
        │
        ▼
EmployeeServiceImpl
        │
        ▼
DepartmentGateway
        │
        ▼
DepartmentGatewayImpl
        │
        ▼
DepartmentResilienceService
        │
        ▼
DepartmentFeignClient
        │
        ▼
Department Service
```

Currently, the resilience layer contains:

- Retry
- Circuit Breaker
- Rate Limiter
- Semaphore Bulkhead

ThreadPool Bulkhead will also be implemented inside the same resilience layer.

---

# Why Not Create Another Service?

A common question is:

> Should we create a separate `DepartmentThreadPoolBulkheadService`?

The answer is **No**.

The purpose of `DepartmentResilienceService` is to centralize all resilience-related concerns for the Department Service.

This includes:

- Retry
- Circuit Breaker
- Rate Limiter
- Semaphore Bulkhead
- ThreadPool Bulkhead
- TimeLimiter (future)

Keeping these patterns together makes the architecture cleaner and easier to maintain.

---

# Current Implementation

Current method:

```java
getDepartment(Long departmentId)
```

This method already contains:

- Retry
- Circuit Breaker
- Rate Limiter
- Semaphore Bulkhead

This implementation will remain unchanged.

It demonstrates the Semaphore Bulkhead pattern.

---

# New Implementation Strategy

Instead of modifying the existing method, a new method will be introduced.

Example:

```java
getDepartmentUsingThreadPool(Long departmentId)
```

This approach allows WorkSphere to demonstrate both Bulkhead implementations independently.

Advantages:

- Existing implementation continues to work.
- Easy comparison between Semaphore and ThreadPool Bulkhead.
- Easier testing.
- Cleaner documentation.
- Better interview demonstration.

---

# Why Keep Both Implementations?

In real enterprise projects, different downstream services may require different resilience strategies.

Example:

```text
Employee Service

│

├── Department Service
│      │
│      └── Semaphore Bulkhead
│
├── Payment Gateway
│      │
│      └── ThreadPool Bulkhead
│
├── Notification Service
│      │
│      └── ThreadPool Bulkhead
│
└── Inventory Service
       │
       └── Semaphore Bulkhead
```

Both implementations are valid depending on the characteristics of the downstream service.

---

# Implementation Steps

The implementation will be completed in the following sequence.

## Step 1

Configure ThreadPool Bulkhead in

```text
application.yml
```

---

## Step 2

Create ThreadPool Bulkhead configuration.

---

## Step 3

Create a new service method using ThreadPool Bulkhead.

---

## Step 4

Implement fallback handling.

---

## Step 5

Test using Postman.

---

## Step 6

Perform concurrent testing using Apache JMeter.

---

# Expected Request Flow

Once implemented, the execution flow will become:

```text
Employee Request

↓

EmployeeServiceImpl

↓

DepartmentGateway

↓

DepartmentGatewayImpl

↓

DepartmentResilienceService

↓

ThreadPool Bulkhead

↓

Worker Thread

↓

DepartmentFeignClient

↓

Department Service
```

Unlike Semaphore Bulkhead, the Department Service call will execute using a worker thread instead of the Tomcat request thread.

---

# Benefits Achieved

After implementing ThreadPool Bulkhead, WorkSphere will demonstrate:

- Retry
- Circuit Breaker
- Rate Limiter
- Semaphore Bulkhead
- ThreadPool Bulkhead

using the same enterprise architecture.

This provides a complete example of modern Resilience4j integration within a layered Spring Boot microservices application.

---

# Summary

Rather than replacing the existing Semaphore Bulkhead implementation, ThreadPool Bulkhead will be added as an additional implementation inside the existing `DepartmentResilienceService`.

This approach keeps the architecture clean, supports side-by-side comparison of both Bulkhead patterns, and prepares the project for future implementation of TimeLimiter and advanced resilience strategies.

---

## 22.5 Configuring ThreadPool Bulkhead in application.yml

---

# Introduction

Before implementing ThreadPool Bulkhead in the code, it is necessary to configure its behavior.

Resilience4j allows ThreadPool Bulkhead properties to be defined inside the application's configuration file. Keeping these values in `application.yml` provides flexibility because the configuration can be changed without modifying the source code.

Externalized configuration also follows Spring Boot best practices and makes the application easier to maintain across different environments such as Development, QA, Staging, and Production.

---

# Current WorkSphere Resilience Configuration

At this stage, WorkSphere already contains configuration for:

- Retry
- Circuit Breaker
- Rate Limiter
- Semaphore Bulkhead

The next step is to add the ThreadPool Bulkhead configuration while keeping the existing configuration unchanged.

---

# ThreadPool Bulkhead Configuration

The following configuration is added inside `application.yml`.

```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      departmentServiceThreadPool:
        core-thread-pool-size: 2
        max-thread-pool-size: 4
        queue-capacity: 10
        keep-alive-duration: 20s
```

---

# Configuration Explanation

## Instance Name

```yaml
departmentServiceThreadPool
```

This is the unique name of the ThreadPool Bulkhead instance.

The same name will later be referenced in the `@ThreadPoolBulkhead` annotation.

Example:

```java
@ThreadPoolBulkhead(name = "departmentServiceThreadPool")
```

---

## core-thread-pool-size

```yaml
core-thread-pool-size: 2
```

This defines the minimum number of worker threads maintained by the ThreadPool Bulkhead.

Characteristics:

- Always available
- Created during startup
- Reused for incoming requests

Example:

```text
Worker Thread 1

Worker Thread 2
```

These threads remain alive even when there are no requests.

---

## max-thread-pool-size

```yaml
max-thread-pool-size: 4
```

This defines the maximum number of worker threads that can exist simultaneously.

Example

```text
Core Threads

↓

2

↓

Additional Threads Created

↓

Thread 3

Thread 4
```

Once four worker threads are busy, no more worker threads are created.

---

## queue-capacity

```yaml
queue-capacity: 10
```

If every worker thread is busy, incoming requests are stored inside the queue.

Example

```text
Worker Threads

↓

Busy

↓

Queue

↓

10 Waiting Requests
```

When the queue becomes full, additional requests are rejected.

---

## keep-alive-duration

```yaml
keep-alive-duration: 20s
```

Extra worker threads created beyond the core thread count remain alive for 20 seconds after becoming idle.

Example

```text
Extra Worker Thread

↓

Idle

↓

20 Seconds

↓

Destroyed
```

This prevents unnecessary resource consumption.

---

# Complete Request Capacity

Current configuration

```yaml
core-thread-pool-size: 2

max-thread-pool-size: 4

queue-capacity: 10
```

Execution

```text
Request 1

↓

Worker 1


Request 2

↓

Worker 2


Request 3

↓

Worker 3


Request 4

↓

Worker 4


Request 5

↓

Queue


...

Request 14

↓

Queue


Request 15

↓

Rejected
```

The ThreadPool Bulkhead can therefore handle:

- 4 running requests
- 10 waiting requests

Total accepted requests:

```text
14
```

The fifteenth request is rejected immediately.

---

# Why These Values?

These values have been intentionally selected for WorkSphere.

They provide:

- Simple testing
- Easy visualization
- Observable thread creation
- Queue behavior during concurrent requests

In production, these values should always be determined using:

- CPU cores
- Memory availability
- Expected traffic
- Downstream latency
- Performance testing results

---

# Best Practices

When configuring ThreadPool Bulkhead:

- Avoid very large thread pools.
- Avoid unlimited queue sizes.
- Configure values based on load testing.
- Keep different services in separate ThreadPool Bulkheads.
- Never use the same ThreadPool Bulkhead for unrelated downstream services.

Example

```text
Department Service

↓

departmentServiceThreadPool


Notification Service

↓

notificationServiceThreadPool


Payroll Service

↓

payrollServiceThreadPool
```

This provides better isolation between downstream dependencies.

---

# WorkSphere Architecture After Configuration

```text
Employee Service

↓

DepartmentGateway

↓

DepartmentGatewayImpl

↓

DepartmentResilienceService

↓

ThreadPool Bulkhead

↓

Worker Thread Pool

↓

DepartmentFeignClient

↓

Department Service
```

---

# Summary

ThreadPool Bulkhead configuration defines how worker threads are created, reused, queued, and destroyed.

Externalizing these properties into `application.yml` follows Spring Boot best practices and provides the flexibility required for enterprise applications.

With the configuration now complete, the next step is to modify `DepartmentResilienceService` and implement asynchronous execution using the configured ThreadPool Bulkhead.

---
# 22 – ThreadPool Bulkhead

## 22.6 Implementing ThreadPool Bulkhead in DepartmentResilienceService

---

# Introduction

With the ThreadPool Bulkhead configuration complete, the next step is to integrate it into the WorkSphere application.

Unlike Semaphore Bulkhead, which executes the downstream call using the caller thread, ThreadPool Bulkhead executes the operation asynchronously using a dedicated worker thread pool.

For this reason, the service method must return a `CompletableFuture<T>` instead of returning the response object directly.

This section explains the implementation in `DepartmentResilienceService`.

---

# Existing Implementation

The current implementation already supports:

- Retry
- Circuit Breaker
- Rate Limiter
- Semaphore Bulkhead

The existing method should **not** be modified.

It will continue to demonstrate Semaphore Bulkhead.

Example:

```java
public DepartmentResponse getDepartment(Long departmentId)
```

This method remains unchanged.

---

# Why Create a New Method?

ThreadPool Bulkhead works differently from Semaphore Bulkhead.

Semaphore Bulkhead

```text
DepartmentResponse
```

ThreadPool Bulkhead

```text
CompletableFuture<DepartmentResponse>
```

Since both implementations return different types, creating a separate method provides:

- Better readability
- Easier testing
- Cleaner comparison
- No impact on the existing implementation

---

# New Method

Create the following method inside:

```text
DepartmentResilienceService
```

```java
@Retry(name = "departmentService", fallbackMethod = "departmentThreadPoolFallback")

@CircuitBreaker(name = "departmentService", fallbackMethod = "departmentThreadPoolFallback")

@RateLimiter(name = "departmentService", fallbackMethod = "departmentThreadPoolFallback")

@ThreadPoolBulkhead(
        name = "departmentServiceThreadPool",
        fallbackMethod = "departmentThreadPoolFallback"
)
public CompletableFuture<DepartmentResponse> getDepartmentUsingThreadPool(
        Long departmentId) {

    log.info("Calling Department Service using ThreadPool Bulkhead");

    return CompletableFuture.completedFuture(
            departmentFeignClient.getDepartment(departmentId)
    );

}
```

---

# Annotation Explanation

The new method still uses:

```java
@Retry
```

This means failed requests are retried before giving up.

---

```java
@CircuitBreaker
```

Protects the Department Service from repeated failures.

---

```java
@RateLimiter
```

Restricts excessive traffic.

---

```java
@ThreadPoolBulkhead
```

Executes the method using the configured worker thread pool.

Instead of using the Tomcat request thread.

---

# Why CompletableFuture?

ThreadPool Bulkhead executes asynchronously.

Therefore,

instead of returning

```java
DepartmentResponse
```

the method returns

```java
CompletableFuture<DepartmentResponse>
```

This allows the request to execute inside another thread.

---

# Request Flow

Execution now becomes

```text
EmployeeServiceImpl

↓

DepartmentGateway

↓

DepartmentGatewayImpl

↓

DepartmentResilienceService

↓

ThreadPool Bulkhead

↓

Worker Thread

↓

DepartmentFeignClient

↓

Department Service
```

Notice that the Tomcat request thread is no longer responsible for executing the remote call.

---

# Why completedFuture()?

The Feign Client is synchronous.

Current call

```java
departmentFeignClient.getDepartment(departmentId);
```

returns immediately.

To satisfy the asynchronous method signature,

the result is wrapped using

```java
CompletableFuture.completedFuture(...)
```

This creates an already completed future.

---

# Current Limitation

Although the method returns

```java
CompletableFuture
```

the Feign Client itself is still synchronous.

Therefore,

the remote HTTP call is still blocking.

In future,

we will improve this further using:

- TimeLimiter
- Async execution
- Executor Service

This will make the implementation fully asynchronous.

---

# Method Responsibilities

The new method now performs the following responsibilities.

- Execute Retry
- Execute Circuit Breaker
- Execute Rate Limiter
- Execute ThreadPool Bulkhead
- Invoke Feign Client
- Return CompletableFuture

Business logic remains outside this class.

---

# Comparison

Semaphore Bulkhead

```java
DepartmentResponse getDepartment(...)
```

ThreadPool Bulkhead

```java
CompletableFuture<DepartmentResponse> getDepartmentUsingThreadPool(...)
```

This difference reflects the synchronous versus asynchronous execution model.

---

# Summary

A new asynchronous method has been introduced into `DepartmentResilienceService` to demonstrate ThreadPool Bulkhead.

The existing Semaphore Bulkhead implementation remains unchanged, allowing both resilience patterns to coexist within WorkSphere.

The method uses the configured ThreadPool Bulkhead instance and returns a `CompletableFuture`, preparing the application for more advanced asynchronous processing in future chapters.

---
## 22.7 Architecture Decision – Choosing ThreadPoolTaskExecutor

---

# Introduction

Before implementing ThreadPool Bulkhead, an important architectural decision must be made regarding asynchronous task execution.

Spring Boot provides multiple ways to execute asynchronous operations. While all of them are technically correct, not every approach is suitable for enterprise applications.

The goal of WorkSphere is not only to demonstrate Resilience4j but also to follow production-grade architectural practices.

For this reason, a dedicated asynchronous execution strategy has been selected.

---

# Possible Approaches

Several approaches were considered.

## Option 1 – Java Common ForkJoinPool

```java
CompletableFuture.supplyAsync(() ->
        departmentFeignClient.getDepartment(id));
```

### Advantages

- Very simple implementation
- No additional configuration
- Suitable for small applications

### Disadvantages

- Uses Java's shared ForkJoinPool.
- Shared with every asynchronous task inside the JVM.
- No isolation.
- Difficult to monitor.
- Difficult to tune.
- Not suitable for enterprise systems.

### Decision

Rejected.

---

## Option 2 – ExecutorService

```java
ExecutorService executor =
Executors.newFixedThreadPool(5);
```

### Advantages

- Dedicated thread pool
- Better control than ForkJoinPool
- Common Java approach

### Disadvantages

- Manual lifecycle management
- Manual shutdown required
- Not managed by Spring
- Harder integration with Spring features

### Decision

Rejected.

Although technically correct, it does not integrate naturally with the Spring Framework.

---

## Option 3 – ThreadPoolTaskExecutor

```java
@Bean
public ThreadPoolTaskExecutor departmentExecutor() {
    ...
}
```

### Advantages

- Managed by Spring Container
- Automatic lifecycle management
- Easy configuration
- Supports monitoring
- Supports graceful shutdown
- Works with @Async
- Works with CompletableFuture
- Enterprise standard

### Disadvantages

- Requires Spring configuration

### Decision

Accepted.

---

# Why ThreadPoolTaskExecutor?

ThreadPoolTaskExecutor is Spring's recommended abstraction for asynchronous execution.

Unlike Java's default ExecutorService, ThreadPoolTaskExecutor integrates directly with the Spring Framework.

Benefits include:

- Bean lifecycle management
- Dependency Injection
- Centralized configuration
- Thread naming
- Graceful shutdown
- Better observability
- Reusability across the application

---

# Enterprise Architecture

The asynchronous execution layer will become part of the application's infrastructure.

```text
Application

│

├── Controller

├── Service

├── Gateway

├── Resilience

├── Client

└── Infrastructure

        │

        ▼

ThreadPoolTaskExecutor
```

Notice that the executor belongs to the Infrastructure layer rather than the Resilience layer.

This separation follows Clean Architecture principles.

---

# Future Reusability

The same executor will later be reused for several enterprise features.

```text
ThreadPoolTaskExecutor

│

├── ThreadPool Bulkhead

├── TimeLimiter

├── Notification Service

├── Email Service

├── Kafka Producer

├── Payroll Processing

├── Report Generation

└── File Upload
```

Instead of creating multiple thread pools, a centralized asynchronous infrastructure will be maintained.

---

# Package Structure

A new configuration package will be introduced.

```text
employee-service

│

├── config

│     └── AsyncConfig.java

│

├── controller

├── service

├── gateway

├── resilience

├── client

└── dto
```

The configuration package will contain infrastructure components shared across the application.

---

# Design Principles Followed

This implementation follows several software engineering principles.

### Single Responsibility Principle

The executor configuration is responsible only for thread management.

---

### Separation of Concerns

Business logic remains inside services.

Resilience logic remains inside the resilience layer.

Thread management remains inside configuration.

---

### Dependency Injection

The executor will be injected wherever asynchronous execution is required.

No component will instantiate its own thread pool.

---

### Reusability

A single executor can support multiple enterprise features.

---

# Summary

After evaluating multiple asynchronous execution strategies, ThreadPoolTaskExecutor has been selected as the standard asynchronous execution mechanism for WorkSphere.

This decision provides better integration with Spring Boot, improves maintainability, supports future scalability, and follows enterprise development practices.

The next step is to implement the AsyncConfig class and create the dedicated ThreadPoolTaskExecutor bean.## 22.8 Injecting the Enterprise Executor into DepartmentResilienceService

---

# Introduction

The ThreadPoolTaskExecutor created in the previous section is now part of the application's infrastructure.

The next step is to inject this executor into the resilience layer so that all asynchronous operations execute using the dedicated worker thread pool instead of Java's default thread pool.

This ensures complete control over thread creation, monitoring, and resource utilization.

---

# Existing Architecture

Current execution flow

EmployeeController

↓

EmployeeServiceImpl

↓

DepartmentGateway

↓

DepartmentGatewayImpl

↓

DepartmentResilienceService

↓

DepartmentFeignClient

The executor will now be injected into the DepartmentResilienceService.

---

# Dependency Injection

Spring creates the executor bean during application startup.

Instead of creating a new executor manually, the existing bean should be injected.

This follows the Dependency Injection principle.

---

# Add Import

```java
import java.util.concurrent.Executor;
```

---

# Inject Executor

Inside

```java
DepartmentResilienceService
```

add

```java
@Resource(name = "departmentExecutor")
private Executor departmentExecutor;
```

---

# Complete Example

Your service should now contain something similar to:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentResilienceService {

    private final DepartmentFeignClient departmentFeignClient;

    @Resource(name = "departmentExecutor")
    private Executor departmentExecutor;

}
```

---

# Why @Resource?

There are three common approaches.

Option 1

```java
@Autowired
```

Option 2

```java
@Autowired
@Qualifier("departmentExecutor")
```

Option 3

```java
@Resource(name = "departmentExecutor")
```

For named infrastructure beans,

@Resource is concise and clearly expresses that a specific bean should be injected.

---

# Why Inject Executor Instead of Creating One?

Incorrect

```java
Executor executor =
Executors.newFixedThreadPool(4);
```

Problems

- Every service creates its own thread pool.
- Memory waste.
- Difficult monitoring.
- Difficult shutdown.
- No Spring lifecycle.

Correct

```java
@Resource(name = "departmentExecutor")
private Executor departmentExecutor;
```

Advantages

- One shared executor
- Spring managed
- Graceful shutdown
- Reusable
- Centralized configuration

---

# Enterprise Principle

Infrastructure components should be managed by Spring.

Business classes should consume infrastructure components instead of creating them.

This follows:

- Dependency Injection
- Inversion of Control (IoC)
- Single Responsibility Principle

---

# Request Flow

Application Startup

↓

Spring creates

↓

departmentExecutor Bean

↓

Spring Container

↓

DepartmentResilienceService

↓

Executor Injected

↓

Ready for Async Execution

---

# Summary

The DepartmentResilienceService now has access to the enterprise ThreadPoolTaskExecutor.

This executor will be used in the next section to execute downstream service calls asynchronously using CompletableFuture.

No business logic has changed.

Only the infrastructure dependency has been introduced.

---
## 22.9 Implementing Asynchronous Department Service Call

---

# Introduction

The next step is to create a new asynchronous method inside
`DepartmentResilienceService`.

Unlike the existing synchronous implementation, this method will execute
the Department Service call using the enterprise executor configured in
AsyncConfig.

The method will return a `CompletableFuture` instead of returning the
response directly.

This allows ThreadPool Bulkhead to isolate slow downstream operations from
Tomcat request threads.

---

# Method Signature

The asynchronous method will return

```java
CompletableFuture<DepartmentResponse>
```

instead of

```java
DepartmentResponse
```

This indicates that the response will become available sometime in the
future after the worker thread completes execution.

---

# Implementation

Add the following method inside
DepartmentResilienceService.

```java
@Retry(
        name = "departmentService",
        fallbackMethod = "departmentAsyncFallback"
)
@CircuitBreaker(
        name = "departmentService",
        fallbackMethod = "departmentAsyncFallback"
)
@RateLimiter(
        name = "departmentService",
        fallbackMethod = "departmentAsyncFallback"
)
@ThreadPoolBulkhead(
        name = "departmentServiceThreadPool",
        fallbackMethod = "departmentAsyncFallback"
)
public CompletableFuture<DepartmentResponse> getDepartmentAsync(
        Long departmentId) {

    log.info("Fetching Department {} using ThreadPool Bulkhead",
            departmentId);

    return CompletableFuture.supplyAsync(() -> {

        log.info(
                "Executing on Thread : {}",
                Thread.currentThread().getName()
        );

        return departmentFeignClient.getDepartment(departmentId);

    }, departmentExecutor);

}
```

---

# Execution Flow

Client Request

↓

Employee Service

↓

Department Gateway

↓

DepartmentResilienceService

↓

CompletableFuture

↓

departmentExecutor

↓

Worker Thread

↓

Feign Client

↓

Department Service

↓

Department Response

↓

CompletableFuture Completed

↓

Client Response

---

# Why supplyAsync()?

`CompletableFuture.supplyAsync()` executes the supplied task in another
thread.

Instead of blocking the Tomcat request thread,

the task is delegated to

departmentExecutor.

---

# Why pass departmentExecutor?

Wrong

```java
CompletableFuture.supplyAsync(() ->
        departmentFeignClient.getDepartment(id));
```

This uses Java's common ForkJoinPool.

Correct

```java
CompletableFuture.supplyAsync(
        () -> departmentFeignClient.getDepartment(id),
        departmentExecutor
);
```

Now the execution uses the dedicated enterprise thread pool.

---

# Thread Logging

The following statement is intentionally added.

```java
log.info(
        "Executing on Thread : {}",
        Thread.currentThread().getName()
);
```

During testing you should see

```text
department-executor-1

department-executor-2

department-executor-3
```

instead of

```text
http-nio-8081-exec-1
```

This confirms that execution has moved away from the Tomcat thread pool.

---

# Summary

The new asynchronous implementation delegates the Department Service call
to the enterprise executor.

The Tomcat request thread no longer performs the remote call directly.

This is the foundation required for ThreadPool Bulkhead and TimeLimiter.