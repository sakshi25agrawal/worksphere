# 25. CompletableFuture Async Orchestration

## Overview

In the previous section, we implemented **Employee Profile Orchestration** using synchronous service-to-service communication.

The Employee Service retrieves data from multiple microservices and combines the results into a single response.

Current flow:

```
Client
   │
   ▼
Employee Service
   │
   ├────────► Employee Database
   │
   ├────────► Department Service
   │
   └────────► Payroll Service
```

Although this implementation is simple and easy to understand, it is **not optimal for performance**.

---

## Problem with Synchronous Calls

The current implementation executes external service calls **one after another**.

```java
DepartmentResponse department =
        departmentGateway.getDepartmentById(employee.getDepartmentId());

PayrollResponse payroll =
        payrollGateway.getPayrollByEmployeeId(employee.getId());
```

Execution order:

```
Employee Lookup
      │
      ▼
Department Service
      │
      ▼
Payroll Service
      │
      ▼
Return Response
```

The Payroll Service call only starts **after** the Department Service has completed.

If each service takes:

| Service | Response Time |
|----------|--------------:|
| Department Service | 250 ms |
| Payroll Service | 350 ms |

Total execution time:

```
250 + 350 = 600 ms
```

As the number of downstream services increases, response time increases linearly.

---

## Why This Becomes a Problem

Imagine an Employee Profile contains:

- Employee
- Department
- Payroll
- Address
- Manager
- Leave Balance
- Project Details

If each service takes approximately **250 ms**, then:

| Services | Total Time |
|----------|-----------:|
| 2 Services | 500 ms |
| 4 Services | 1000 ms |
| 6 Services | 1500 ms |

Even though these services are independent of each other, the application waits for every service call to finish before executing the next one.

This unnecessary waiting increases latency and degrades user experience.

---

## Better Approach

Department and Payroll services are independent.

There is no dependency between them.

Therefore, both service calls can execute simultaneously.

Instead of:

```
Department
      │
      ▼
Payroll
```

we execute:

```
Department
      │
      ├─────────────┐
      │             │
      ▼             ▼
 Parallel Execution
      │             │
      └──────┬──────┘
             ▼
       Combined Response
```

Both services execute concurrently, reducing the overall response time.

---

## Expected Performance Improvement

Suppose:

Department Service = **250 ms**

Payroll Service = **350 ms**

### Synchronous

```
250 + 350 = 600 ms
```

### Asynchronous

```
max(250, 350) = 350 ms
```

The response time is reduced significantly because both service calls execute in parallel.

---

## Java CompletableFuture

Java provides the **CompletableFuture API** to perform asynchronous computations.

CompletableFuture allows multiple independent tasks to execute concurrently and combines the results when all tasks have completed.

Example:

```java
CompletableFuture<String> future =
        CompletableFuture.supplyAsync(() -> "Hello");
```

Unlike traditional threads, CompletableFuture provides a fluent API for:

- Parallel execution
- Result composition
- Exception handling
- Non-blocking programming

This makes it an ideal choice for microservice orchestration.

---

## Why CompletableFuture for WorkSphere

WorkSphere is built using a microservices architecture.

Employee Service communicates with multiple downstream services.

Instead of waiting for every service sequentially, CompletableFuture enables:

- Faster response times
- Better resource utilization
- Higher throughput
- Improved scalability

This aligns with enterprise backend development practices where aggregation services frequently retrieve information from multiple independent services.

---

# Understanding CompletableFuture

CompletableFuture represents a computation that will complete at some point in the future.

Instead of blocking the current thread while waiting for a result, the computation runs asynchronously.

Basic syntax:

```java
CompletableFuture<T> future =
        CompletableFuture.supplyAsync(() -> {
            // Long running task
            return result;
        });
```

The task immediately starts executing in another thread.

Meanwhile, the current thread is free to perform other work.

---

# supplyAsync()

The most commonly used factory method is:

```java
CompletableFuture.supplyAsync()
```

It is used when a task returns a value.

Example:

```java
CompletableFuture<String> future =
        CompletableFuture.supplyAsync(() -> {

            System.out.println("Running...");

            return "Hello WorkSphere";
        });
```

Later, the result can be retrieved using:

```java
String result = future.join();
```

Output:

```
Running...

Hello WorkSphere
```

---

# Running Multiple Tasks

Suppose we have two independent service calls.

Department Service

```java
CompletableFuture<DepartmentResponse> departmentFuture =
        CompletableFuture.supplyAsync(() ->
                departmentGateway.getDepartmentById(departmentId));
```

Payroll Service

```java
CompletableFuture<PayrollResponse> payrollFuture =
        CompletableFuture.supplyAsync(() ->
                payrollGateway.getPayrollByEmployeeId(employeeId));
```

Both tasks begin execution immediately.

```
Department Service
        │
        ▼

Payroll Service
        │
        ▼

Running in Parallel
```

Neither task waits for the other.

---

# Waiting for Results

Once both tasks are running, we collect the results.

```java
DepartmentResponse department =
        departmentFuture.join();

PayrollResponse payroll =
        payrollFuture.join();
```

`join()` waits until the computation finishes.

Unlike `Future.get()`, `join()` throws unchecked exceptions, making the code cleaner.

---

# allOf()

Sometimes we need to wait for multiple asynchronous tasks before continuing.

Java provides:

```java
CompletableFuture.allOf()
```

Example:

```java
CompletableFuture.allOf(
        departmentFuture,
        payrollFuture
).join();
```

Flow:

```
Department
      │
      ├─────┐
      │     │
Payroll     │
      │     │
      └─────┘
            │
            ▼
      allOf().join()
            │
            ▼
     Continue Execution
```

After `allOf()` completes, we safely retrieve the results.

```java
DepartmentResponse department =
        departmentFuture.join();

PayrollResponse payroll =
        payrollFuture.join();
```

---

# Why join() instead of get()

Both methods wait for task completion.

### Future.get()

```java
future.get();
```

Requires checked exception handling.

```java
try {

    future.get();

} catch (InterruptedException e) {

} catch (ExecutionException e) {

}
```

---

### CompletableFuture.join()

```java
future.join();
```

No checked exceptions.

Cleaner code.

More readable.

For this reason, most modern Spring Boot applications prefer `join()`.

---

# Thread Utilization

Without CompletableFuture:

```
Main Thread

Employee Lookup

↓

Department Service

↓

Payroll Service

↓

Return Response
```

Only one task executes at a time.

---

With CompletableFuture:

```
Main Thread

Employee Lookup

↓

Start Department Task

↓

Start Payroll Task

↓

Wait for Completion

↓

Build Response
```

Multiple worker threads execute tasks simultaneously.

This improves throughput and reduces latency.

---

# When Should CompletableFuture Be Used?

CompletableFuture is useful when tasks are:

- Independent
- Time consuming
- I/O bound
- Remote service calls
- Database queries
- REST API calls

Typical enterprise examples:

- Microservice orchestration
- Notification sending
- Report generation
- Parallel database lookups
- External API integrations

It should **not** be used for very small computations because thread creation and scheduling introduce overhead.

---

# Current WorkSphere Implementation

The current Employee Profile implementation performs external service calls synchronously.

```java
DepartmentResponse department =
        departmentGateway.getDepartmentById(
                employee.getDepartmentId());

PayrollResponse payroll =
        payrollGateway.getPayrollByEmployeeId(
                employee.getId());

return new EmployeeDetailsResponse(
        EmployeeMapper.toResponse(employee),
        department,
        payroll
);
```

Execution Flow:

```
Employee Lookup
       │
       ▼
Department Service
       │
       ▼
Payroll Service
       │
       ▼
Build Response
```

Although this implementation is correct, the Department Service and Payroll Service are completely independent.

The Payroll Service unnecessarily waits for the Department Service to finish.

---

# Refactoring to CompletableFuture

Instead of executing the service calls sequentially, both requests can execute in parallel.

The first step is wrapping each gateway call inside a CompletableFuture.

```java
CompletableFuture<DepartmentResponse> departmentFuture =
        CompletableFuture.supplyAsync(() ->
                departmentGateway.getDepartmentById(
                        employee.getDepartmentId()));

CompletableFuture<PayrollResponse> payrollFuture =
        CompletableFuture.supplyAsync(() ->
                payrollGateway.getPayrollByEmployeeId(
                        employee.getId()));
```

Both tasks start immediately.

```
Employee Lookup
       │
       ├──────────────┐
       ▼              ▼
Department        Payroll
       │              │
       └──────┬───────┘
              ▼
      Build Response
```

---

# Waiting for Completion

Before constructing the final response, both tasks must complete.

```java
CompletableFuture.allOf(
        departmentFuture,
        payrollFuture
).join();
```

After completion:

```java
DepartmentResponse department =
        departmentFuture.join();

PayrollResponse payroll =
        payrollFuture.join();
```

Now all required information is available.

---

# Final Response

The response construction remains unchanged.

```java
return new EmployeeDetailsResponse(
        EmployeeMapper.toResponse(employee),
        department,
        payroll
);
```

The difference is that both downstream services executed simultaneously.

---

# Performance Comparison

### Before

```
Employee Lookup

↓

Department Service (250 ms)

↓

Payroll Service (350 ms)

↓

Response

Total = 600 ms
```

---

### After

```
Employee Lookup

↓

Department Service ─────────┐

Payroll Service ────────────┘

↓

Response

Total = 350 ms
```

The overall response time is reduced because both external services run concurrently.

---

# Custom Executor

By default, CompletableFuture uses Java's common ForkJoinPool.

For enterprise applications, creating a dedicated thread pool is recommended.

Example:

```java
@Configuration
public class AsyncConfig {

    @Bean
    public Executor orchestrationExecutor() {

        return Executors.newFixedThreadPool(10);
    }
}
```

Then use:

```java
CompletableFuture.supplyAsync(
        () -> departmentGateway.getDepartmentById(
                employee.getDepartmentId()),
        orchestrationExecutor
);
```

Advantages:

- Better thread management
- Controlled resource usage
- Isolation from unrelated asynchronous tasks
- Improved scalability

---

# Exception Handling

When asynchronous tasks fail, exceptions should be handled gracefully.

Example:

```java
CompletableFuture<DepartmentResponse> departmentFuture =
        CompletableFuture.supplyAsync(
                () -> departmentGateway.getDepartmentById(
                        employee.getDepartmentId()))
        .exceptionally(ex -> {

            log.error("Department Service failed", ex);

            return null;
        });
```

This prevents one failing service from crashing the entire orchestration process.

Depending on business requirements, fallback responses or default values may also be returned.

---

# Enterprise Best Practices

When using CompletableFuture in Spring Boot applications:

- Execute only independent tasks in parallel.
- Avoid creating new threads manually.
- Prefer a dedicated Executor over the common thread pool.
- Keep business logic inside the service layer.
- Keep Feign clients lightweight.
- Handle failures gracefully.
- Log asynchronous failures with sufficient context.
- Avoid excessive parallelism for very small operations.

---

# WorkSphere Roadmap

The current implementation of Employee Profile orchestration is synchronous.

In the next enhancement, it will be upgraded to asynchronous execution using CompletableFuture.

Future improvements include:

- Parallel Department and Payroll retrieval
- Dedicated thread pool configuration
- Circuit Breaker integration with Resilience4j
- Timeout handling
- Fallback responses
- Performance benchmarking

This evolution demonstrates how enterprise applications gradually improve scalability and responsiveness while keeping the business logic clean and maintainable.