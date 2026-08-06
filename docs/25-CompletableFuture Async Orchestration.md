# 25. CompletableFuture Async Orchestration

---

# Introduction

Modern microservice-based applications frequently need to collect information from multiple independent services before returning a response to the client. Executing these service calls sequentially increases response time and negatively impacts user experience.

Java's **CompletableFuture** provides a simple and efficient mechanism to execute independent tasks concurrently, allowing multiple service calls to run in parallel and significantly reducing overall response time.

In the WorkSphere project, asynchronous orchestration has been implemented using **CompletableFuture** to fetch Department and Payroll information simultaneously while retrieving Employee profile details.

Instead of waiting for one service to complete before invoking the next service, both services execute concurrently using a dedicated thread pool.

---

# Learning Objectives

After completing this document, you will understand:

- What CompletableFuture is
- Why asynchronous orchestration is important
- Problems with synchronous service calls
- How CompletableFuture works internally
- How WorkSphere implements asynchronous orchestration
- How multiple microservices execute concurrently
- Benefits of using ThreadPoolTaskExecutor
- Best practices for asynchronous programming in Spring Boot

---

# Business Problem

The Employee Profile screen requires information from multiple microservices.

The response contains:

- Employee Details
- Department Details
- Payroll Details

The Employee Service owns only Employee information.

Department information belongs to Department Service.

Payroll information belongs to Payroll Service.

Therefore, Employee Service must communicate with two external services before sending the final response.

---

# Existing Synchronous Flow

Initially, the Employee Service called both services one after another.

```
Client
   │
   ▼
Employee Service
   │
   ▼
Department Service
   │
(wait until complete)
   │
   ▼
Payroll Service
   │
(wait until complete)
   │
   ▼
Employee Profile Response
```

Execution Flow

```
Employee

↓

Department

↓

Payroll

↓

Return Response
```

Only after the Department Service completed was the Payroll Service invoked.

---

# Problems with Sequential Execution

Sequential execution introduces several performance bottlenecks.

### Increased Response Time

Suppose:

Department Service takes **3 seconds**

Payroll Service takes **3 seconds**

Total execution time becomes:

```
Department (3 sec)

↓

Payroll (3 sec)

↓

Total = 6 seconds
```

The user waits for both operations to finish sequentially.

---

### Poor Resource Utilization

While waiting for Department Service, the application thread remains idle.

Similarly, Payroll Service begins only after Department Service completes.

The CPU remains underutilized because independent tasks are executed one after another.

---

### Reduced Scalability

As the number of external services increases, the response time grows linearly.

Example:

```
Department

↓

Payroll

↓

Leave

↓

Attendance

↓

Performance
```

Each additional service increases overall latency.

---

### Bad User Experience

Slow API responses directly affect application performance.

Users perceive the application as slow even when external services are independent and capable of executing simultaneously.

---

# Why CompletableFuture?

CompletableFuture enables multiple independent tasks to execute concurrently.

Instead of waiting for one service call to complete before invoking another service, both service calls start immediately.

```
Employee Service

        │

 ┌──────┴────────┐

 ▼               ▼

Department     Payroll

Service         Service

 │               │

 └──────┬────────┘

        ▼

Employee Profile Response
```

Both services execute simultaneously using separate threads.

The final response is created only after both asynchronous operations complete.

This significantly improves API response time without changing the business logic.

---

# CompletableFuture Fundamentals

## What is CompletableFuture?

`CompletableFuture` is a feature introduced in **Java 8** under the `java.util.concurrent` package. It is an implementation of the `Future` interface that provides a powerful framework for writing asynchronous and non-blocking code.

Unlike the traditional `Future`, `CompletableFuture` allows developers to:

- Execute tasks asynchronously
- Chain multiple asynchronous operations
- Combine results from multiple tasks
- Handle exceptions elegantly
- Execute tasks in parallel
- Build responsive and scalable applications

In WorkSphere, `CompletableFuture` is used to execute independent microservice calls simultaneously instead of sequentially.

---

# Traditional Future vs CompletableFuture

| Future | CompletableFuture |
|---------|-------------------|
| Introduced in Java 5 | Introduced in Java 8 |
| Blocking API | Supports Non-blocking APIs |
| Cannot chain operations | Supports method chaining |
| Manual polling | Callback-based execution |
| Difficult exception handling | Built-in exception handling |
| Sequential programming | Parallel programming |
| Limited functionality | Rich functional programming support |

---

# Why CompletableFuture in WorkSphere?

Employee Profile API requires data from multiple independent services.

- Employee Service
- Department Service
- Payroll Service

Department Service does **not** depend on Payroll Service.

Payroll Service does **not** depend on Department Service.

Since both services are completely independent, there is no reason to execute them sequentially.

Instead, both services can execute simultaneously.

```
Employee

       │

 ┌─────┴────────┐

 ▼              ▼

Department    Payroll

Service        Service

 │              │

 └──────┬───────┘

        ▼

Employee Profile
```

This is exactly what CompletableFuture helps us achieve.

---

# Why Not Use @Async?

Spring Boot provides another mechanism for asynchronous programming using the `@Async` annotation.

However, WorkSphere uses `CompletableFuture` because it provides significantly more flexibility.

| @Async | CompletableFuture |
|----------|-------------------|
| Fire-and-forget execution | Full control over execution |
| Limited task composition | Supports chaining multiple tasks |
| Difficult result combination | Supports combining multiple futures |
| Basic exception handling | Rich exception handling APIs |
| Less flexible | Highly flexible |

Since Employee Service needs to combine multiple service responses into a single object, `CompletableFuture` is the preferred choice.

---

# CompletableFuture Lifecycle

Every CompletableFuture generally follows four stages.

## Step 1 - Create

An asynchronous task is created.

```java
CompletableFuture.supplyAsync(...)
```

---

## Step 2 - Execute

The task executes on a background thread.

```
ThreadPoolTaskExecutor

↓

Worker Thread

↓

Execute Task
```

---

## Step 3 - Wait

Multiple tasks execute independently.

```
Department Future

Payroll Future

↓

CompletableFuture.allOf()
```

The application waits until every task completes.

---

## Step 4 - Collect Results

After completion, results are retrieved.

```java
departmentFuture.join();

payrollFuture.join();
```

Finally,

```java
EmployeeDetailsResponse
```

is created.

---

# WorkSphere Before Async Refactoring

Before implementing CompletableFuture, Employee Service followed the architecture below.

```
EmployeeService

        │

        ▼

Department Gateway

        │

        ▼

Department Service

        │

(wait)

        ▼

Payroll Gateway

        │

        ▼

Payroll Service

        │

(wait)

        ▼

Return Response
```

Only one service executed at a time.

---

# WorkSphere After Async Refactoring

The architecture was redesigned to execute both services concurrently.

```
                    EmployeeService

                           │

        ┌──────────────────┴──────────────────┐

        ▼                                     ▼

Department Gateway                     Payroll Gateway

        │                                     │

        ▼                                     ▼

DepartmentAsyncService               PayrollAsyncService

        │                                     │

        ▼                                     ▼

DepartmentResilienceService        PayrollResilienceService

        │                                     │

        ▼                                     ▼

DepartmentFeignClient              PayrollFeignClient
```

Both asynchronous services execute independently using separate threads.

Employee Service simply waits for both operations to complete before preparing the final response.

---

# Benefits of the New Architecture

The new architecture provides several advantages.

### Better Separation of Concerns

| Component | Responsibility |
|------------|---------------|
| EmployeeService | Business Logic |
| Gateway | External Service Abstraction |
| Async Service | Parallel Execution |
| Resilience Service | Retry / Circuit Breaker |
| Feign Client | HTTP Communication |

Each layer has a single responsibility, making the application easier to maintain.

---

### Parallel Execution

Department Service and Payroll Service no longer wait for each other.

Both execute simultaneously.

---

### Cleaner Business Logic

Employee Service focuses only on business operations.

It no longer contains asynchronous implementation details.

---

### Improved Scalability

Additional services can be added without significantly increasing response time.

Future integrations such as:

- Leave Service
- Attendance Service
- Performance Service

can also be executed asynchronously using the same architecture.

---

# Summary

By introducing CompletableFuture, WorkSphere transformed its Employee Profile API from a sequential orchestration model into a parallel orchestration model.

This reduces overall response time, improves scalability, keeps the business layer clean, and aligns the application with enterprise microservice design principles.

---

# Implementation

This section explains how asynchronous orchestration has been implemented in the WorkSphere project.

Unlike the earlier synchronous implementation, WorkSphere now executes Department Service and Payroll Service in parallel using **CompletableFuture** and a dedicated **ThreadPoolTaskExecutor**.

---

# Async Configuration

To execute asynchronous tasks, Spring Boot requires an Executor.

Instead of using the default Java thread pool, WorkSphere uses a dedicated `ThreadPoolTaskExecutor`.

File Location

```
employee-service
└── config
      └── AsyncConfig.java
```

Implementation

```java
@Configuration
public class AsyncConfig {

    @Bean(name = "departmentExecutor")
    public Executor departmentExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);

        executor.setMaxPoolSize(4);

        executor.setQueueCapacity(10);

        executor.setThreadNamePrefix("department-executor-");

        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        return executor;
    }
}
```

---

# Why ThreadPoolTaskExecutor?

Using a dedicated thread pool provides several advantages over creating new threads manually.

### Controlled Thread Management

Instead of creating a new thread for every request, existing worker threads are reused.

This reduces thread creation overhead and improves application performance.

---

### Better Resource Utilization

The thread pool limits the maximum number of concurrent threads.

```
Core Threads : 2

Maximum Threads : 4

Queue Capacity : 10
```

This prevents the application from creating excessive threads under heavy load.

---

### Easy Monitoring

Every asynchronous thread is prefixed with

```
department-executor-
```

Example

```
department-executor-1

department-executor-2
```

This makes debugging significantly easier.

---

# Why Use a Dedicated Executor?

CompletableFuture can execute tasks using Java's default **ForkJoinPool**.

However, WorkSphere intentionally avoids using the common pool because:

- Shared pool with other tasks
- Difficult to monitor
- Limited configuration
- Not recommended for enterprise applications

Instead, a dedicated executor provides:

- Isolation
- Better monitoring
- Custom thread naming
- Independent scaling

---

# Department Async Service

To keep the Service layer clean, asynchronous execution is isolated into a separate component.

File Location

```
employee-service

└── orchestrator

        └── DepartmentAsyncService.java
```

Responsibilities

- Execute Department Service asynchronously
- Use ThreadPoolTaskExecutor
- Return CompletableFuture
- Delegate business logic to Resilience Layer

Architecture

```
DepartmentAsyncService

        │

        ▼

DepartmentResilienceService

        │

        ▼

DepartmentFeignClient
```

The async service contains no business logic.

Its only responsibility is asynchronous execution.

---

# Payroll Async Service

Payroll follows the same design.

File Location

```
employee-service

└── orchestrator

        └── PayrollAsyncService.java
```

Architecture

```
PayrollAsyncService

        │

        ▼

PayrollResilienceService

        │

        ▼

PayrollFeignClient
```

Both asynchronous services use the same executor.

---

# Why Separate Async Service?

A common question during interviews is:

> Why didn't we call CompletableFuture directly from EmployeeService?

WorkSphere follows the **Single Responsibility Principle**.

EmployeeService should contain only business logic.

Responsibilities are divided as follows.

| Layer | Responsibility |
|---------|---------------|
| EmployeeService | Business Logic |
| Gateway | External Communication Abstraction |
| Async Service | Parallel Execution |
| Resilience Service | Retry, Circuit Breaker |
| Feign Client | REST Communication |

This architecture makes the codebase cleaner and easier to maintain.

---

# Gateway Layer

The Gateway layer acts as an abstraction between the Service layer and external services.

Instead of directly calling Feign clients, EmployeeService communicates only with Gateways.

Department Flow

```
EmployeeService

        │

        ▼

DepartmentGateway

        │

        ▼

DepartmentGatewayImpl

        │

        ▼

DepartmentAsyncService
```

Payroll Flow

```
EmployeeService

        │

        ▼

PayrollGateway

        │

        ▼

PayrollGatewayImpl

        │

        ▼

PayrollAsyncService
```

This abstraction allows future implementation changes without affecting EmployeeService.

---

# Department Gateway

Department Gateway exposes two operations.

```
DepartmentResponse getDepartmentById(...)

CompletableFuture<DepartmentResponse>
getDepartmentAsync(...)
```

The synchronous method is still available because some business operations may not require asynchronous execution.

The asynchronous method is specifically used for Employee Profile orchestration.

---

# Payroll Gateway

Payroll Gateway follows the same design.

```
PayrollResponse getPayrollByEmployeeId(...)

CompletableFuture<PayrollResponse>
getPayrollAsync(...)
```

Maintaining the same interface design across services keeps the project consistent.

---

# Overall Async Architecture

The final implementation looks as follows.

```
                    EmployeeController

                            │

                            ▼

                  EmployeeServiceImpl

                            │

        ┌───────────────────┴───────────────────┐

        ▼                                       ▼

 DepartmentGateway                     PayrollGateway

        │                                       │

        ▼                                       ▼

 DepartmentGatewayImpl               PayrollGatewayImpl

        │                                       │

        ▼                                       ▼

 DepartmentAsyncService             PayrollAsyncService

        │                                       │

        ▼                                       ▼

 DepartmentResilienceService       PayrollResilienceService

        │                                       │

        ▼                                       ▼

 DepartmentFeignClient             PayrollFeignClient
```

This layered architecture provides excellent separation of concerns while supporting asynchronous orchestration.

---

# Employee Service Implementation

The Employee Profile API is responsible for returning a consolidated response containing data from multiple microservices.

The response includes:

- Employee Details
- Department Details
- Payroll Details

Employee information is stored within the Employee Service.

Department information is retrieved from Department Service.

Payroll information is retrieved from Payroll Service.

Instead of invoking these services sequentially, WorkSphere executes both service calls concurrently using `CompletableFuture`.

---

# Previous Implementation (Synchronous)

Initially, the Employee Profile API followed a sequential execution model.

```java
DepartmentResponse department =
        departmentGateway.getDepartmentById(
                employee.getDepartmentId()
        );

PayrollResponse payroll =
        payrollGateway.getPayrollByEmployeeId(
                employee.getId()
        );

return new EmployeeDetailsResponse(
        EmployeeMapper.toResponse(employee),
        department,
        payroll
);
```

Execution Flow

```
Employee

↓

Department

↓

Payroll

↓

Return Response
```

Although simple, this approach introduces unnecessary waiting.

The Payroll Service starts only after Department Service finishes.

---

# Current Implementation (Asynchronous)

WorkSphere now executes both service calls simultaneously.

Implementation

```java
CompletableFuture<DepartmentResponse> departmentFuture =
        departmentGateway.getDepartmentAsync(
                employee.getDepartmentId()
        );

CompletableFuture<PayrollResponse> payrollFuture =
        payrollGateway.getPayrollAsync(
                employee.getId()
        );

CompletableFuture.allOf(
        departmentFuture,
        payrollFuture
).join();

return new EmployeeDetailsResponse(
        EmployeeMapper.toResponse(employee),
        departmentFuture.join(),
        payrollFuture.join()
);
```

Both asynchronous tasks start immediately without waiting for each other.

---

# Step-by-Step Execution

## Step 1

Employee information is fetched from the local database.

```
Employee Repository

↓

Employee Entity
```

At this point, the application knows:

- Employee Id
- Department Id

---

## Step 2

Department Service request is submitted.

```
departmentGateway.getDepartmentAsync(...)
```

Instead of executing immediately on the current thread, the request is submitted to the configured thread pool.

---

## Step 3

Payroll Service request is submitted.

```
payrollGateway.getPayrollAsync(...)
```

This request is also submitted to the thread pool.

At this point, both requests execute independently.

---

# Parallel Execution

```
Main Thread

        │

        ├──────────────┐

        ▼              ▼

Department Future    Payroll Future

        │              │

        ▼              ▼

Department        Payroll

Service           Service

        │              │

        └──────┬───────┘

               ▼

EmployeeDetailsResponse
```

The Main Thread does not execute either service directly.

Instead, worker threads execute both tasks.

---

# CompletableFuture.allOf()

The following statement waits until every asynchronous task completes.

```java
CompletableFuture.allOf(
        departmentFuture,
        payrollFuture
).join();
```

## What does allOf() do?

`CompletableFuture.allOf()` creates a new CompletableFuture that completes only when every supplied CompletableFuture completes.

Example

```
Department Future

        │

Payroll Future

        │

──────────────

allOf()

        │

Completed
```

It does not return the actual results.

It simply waits for all asynchronous operations to finish.

---

# Why join() after allOf()?

After all tasks complete, results are extracted individually.

```java
departmentFuture.join();

payrollFuture.join();
```

At this stage, no waiting occurs because both futures have already completed.

The values are immediately returned.

---

# join() vs get()

Both methods retrieve the result from a CompletableFuture.

| join() | get() |
|----------|--------|
| Throws unchecked CompletionException | Throws checked exceptions |
| No try-catch required | Requires try-catch |
| Cleaner code | More verbose |
| Preferred in Spring Boot | Common in Java SE |

WorkSphere uses `join()` because it keeps the service layer clean.

---

# Thread Execution

EmployeeService executes on the request thread.

```
http-nio-8081-exec-1
```

The asynchronous tasks execute on worker threads.

Example

```
department-executor-1

department-executor-2
```

The request thread simply waits until both worker threads complete.

---

# Actual Runtime Execution

Testing produced the following output.

```
department-executor-1 -> Department Started

department-executor-2 -> Payroll Started

department-executor-2 -> Payroll Completed

department-executor-1 -> Department Completed
```

This proves that:

- Department Service executed on Thread 1.
- Payroll Service executed on Thread 2.
- Both tasks started almost simultaneously.
- Neither task waited for the other.

This validates that asynchronous orchestration is functioning correctly.

---

# Performance Comparison

## Before CompletableFuture

```
Department (3 seconds)

↓

Payroll (3 seconds)

↓

Total Response Time

≈ 6 seconds
```

---

## After CompletableFuture

```
Department (3 seconds)

        │

Payroll (3 seconds)

        │

──────────────

Total Response Time

≈ 3 seconds
```

The overall response time is reduced because both services execute concurrently.

---

# Why This Improves Performance

CompletableFuture does not make Department Service faster.

CompletableFuture does not make Payroll Service faster.

Instead, it removes unnecessary waiting by executing independent tasks simultaneously.

Instead of

```
3 + 3 = 6 seconds
```

the application effectively waits only for the slowest task.

```
Maximum(3,3)

=

3 seconds
```

This significantly improves API responsiveness.

---

# Benefits Achieved

After introducing asynchronous orchestration, WorkSphere now provides:

- Parallel microservice execution
- Reduced API response time
- Better CPU utilization
- Cleaner service implementation
- Proper separation of concerns
- Scalable orchestration architecture
- Enterprise-ready asynchronous processing
- Easy integration of additional services in the future

The Employee Profile API now behaves like a modern enterprise microservice orchestration layer rather than a simple sequential service aggregator.

---

# Testing Strategy

After implementing asynchronous orchestration, it is important to verify that the application behaves correctly under real execution.

The following tests were performed in the WorkSphere project.

---

# Functional Testing

The first step was to verify that the API still returned the expected response after refactoring.

Request

```
GET /api/v1/employees/{id}/profile
```

Example

```
GET /api/v1/employees/1/profile
```

Expected Response

```json
{
    "employee": {
        "id": 1,
        "firstName": "Sakshi",
        "lastName": "Agrawal",
        "email": "sakshi@gmail.com",
        "salary": 65000,
        "departmentId": 1
    },
    "department": {
        "id": 1,
        "departmentName": "Engineering",
        "departmentCode": "ENG",
        "departmentHead": "John",
        "location": "Indore"
    },
    "payroll": {
        "employeeId": 1,
        "basicSalary": 65000,
        "bonus": 5000,
        "tax": 3500,
        "netSalary": 66500
    }
}
```

The response remained unchanged after introducing asynchronous execution.

---

# Parallel Execution Verification

To confirm that Department Service and Payroll Service executed simultaneously, temporary logging statements were added inside the asynchronous services.

Example

```java
log.info("Department Started");

Thread.sleep(3000);

log.info("Department Completed");
```

Similarly,

```java
log.info("Payroll Started");

Thread.sleep(3000);

log.info("Payroll Completed");
```

These delays were introduced only for testing purposes.

---

# Console Output

During execution, the following logs were observed.

```
department-executor-1 -> Department Started

department-executor-2 -> Payroll Started

department-executor-2 -> Payroll Completed

department-executor-1 -> Department Completed
```

---

# Analysis

The logs confirm that:

- Department Service started on **department-executor-1**
- Payroll Service started on **department-executor-2**
- Both tasks executed on different worker threads
- Payroll completed before Department
- Both services were executing simultaneously

If the implementation had been synchronous, the expected log would have been:

```
Department Started

Department Completed

Payroll Started

Payroll Completed
```

This did not occur, confirming true parallel execution.

---

# Performance Benchmark

A temporary delay of **3 seconds** was introduced in both asynchronous services.

## Sequential Execution

```
Department

3 sec

↓

Payroll

3 sec

↓

Total

≈ 6 seconds
```

---

## Asynchronous Execution

```
Department

3 sec

        │

Payroll

3 sec

        │

──────────────

Total

≈ 3 seconds
```

The total response time is determined by the longest-running task rather than the sum of all tasks.

---

# Best Practices

When using CompletableFuture in enterprise applications, the following practices are recommended.

### Use Dedicated Thread Pools

Avoid Java's default ForkJoinPool.

Use a dedicated ThreadPoolTaskExecutor instead.

---

### Keep Business Logic Separate

EmployeeService should contain only business logic.

Async execution should be delegated to dedicated Async Services.

---

### Keep Feign Calls Behind Gateways

Avoid calling Feign clients directly from the Service layer.

Use Gateway implementations to abstract external communication.

---

### Use join() Carefully

Call `join()` only after all asynchronous tasks have completed.

Prefer:

```java
CompletableFuture.allOf(...).join();
```

before retrieving individual results.

---

### Avoid Blocking Operations

Do not perform unnecessary blocking operations inside asynchronous tasks.

Blocking reduces the benefits of asynchronous execution.

---

### Use Logging

Always include thread names while debugging asynchronous code.

Example

```java
log.info(
    "Executing on thread : {}",
    Thread.currentThread().getName()
);
```

---

# Common Mistakes

### Calling join() Immediately

Incorrect

```java
departmentFuture.join();

payrollFuture.join();
```

This can reduce parallelism.

Correct

```java
CompletableFuture.allOf(
        departmentFuture,
        payrollFuture
).join();
```

---

### Using the Default Thread Pool

Avoid

```java
CompletableFuture.supplyAsync(...)
```

without specifying an Executor.

Always provide a dedicated Executor.

---

### Mixing Business Logic and Async Logic

Keep asynchronous execution inside Async Services.

Business Services should remain clean.

---

# Interview Questions

## 1. What is CompletableFuture?

CompletableFuture is a Java 8 feature that enables asynchronous and non-blocking programming.

---

## 2. Why use CompletableFuture?

To execute independent tasks concurrently and improve application performance.

---

## 3. Why not use @Async?

CompletableFuture provides richer APIs for chaining, combining, and managing asynchronous tasks.

---

## 4. What does allOf() do?

It waits until every supplied CompletableFuture completes.

---

## 5. Does allOf() return results?

No.

It only waits for completion.

Individual results are retrieved using `join()`.

---

## 6. Difference between join() and get()?

`join()` throws unchecked exceptions.

`get()` throws checked exceptions.

---

## 7. Why use ThreadPoolTaskExecutor?

It provides better thread management, monitoring, and scalability than the default thread pool.

---

## 8. Why create Async Services?

To separate asynchronous execution from business logic.

---

## 9. Can one CompletableFuture fail independently?

Yes.

Each future executes independently.

Exceptions can be handled separately.

---

## 10. What happens if Department Service fails?

The CompletableFuture completes exceptionally.

The exception can be handled using methods such as:

- exceptionally()
- handle()
- whenComplete()

---

## 11. Why is CompletableFuture suitable for Microservices?

Microservices often require multiple independent service calls.

CompletableFuture allows these calls to execute concurrently.

---

## 12. Can more services be added?

Yes.

Additional futures can easily be included inside:

```java
CompletableFuture.allOf(...)
```

---

# Future Enhancements

The current implementation can be extended further.

Possible improvements include:

- Virtual Threads (Java 21)
- Structured Concurrency
- Spring WebFlux
- Reactive Programming
- Event-Driven Architecture
- Distributed Tracing
- Metrics using Micrometer
- OpenTelemetry Integration

---

# Conclusion

The Employee Profile API originally executed external service calls sequentially, resulting in unnecessary waiting and increased response time.

By introducing CompletableFuture, WorkSphere now executes Department Service and Payroll Service concurrently using a dedicated ThreadPoolTaskExecutor.

The implementation provides:

- Reduced response time
- Better scalability
- Cleaner architecture
- Proper separation of concerns
- Enterprise-ready asynchronous orchestration

This implementation demonstrates how modern Spring Boot microservices can efficiently coordinate multiple independent services while maintaining clean architecture and high performance.

