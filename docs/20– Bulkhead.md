

# Phase 20.1 – Introduction

## What is Bulkhead?

Bulkhead is a resilience pattern that isolates failures by limiting the resources allocated to different parts of an application.

Its primary goal is to prevent one overloaded component from consuming all available resources and affecting the entire application.

Instead of allowing one service to exhaust all application threads or connections, Bulkhead creates separate resource pools so that failures remain isolated.

---

## Why is it called "Bulkhead"?

The name comes from ship engineering.

A ship is divided into multiple watertight compartments called **bulkheads**.

Example:

```
 _______________________________

| Room 1 | Room 2 | Room 3 | Room 4 |

------------------------------------
```

If water enters Room 2,

the bulkhead prevents the water from reaching Rooms 1, 3 and 4.

As a result,

the ship continues floating instead of sinking completely.

---

## Software Analogy

Imagine an Employee Service that communicates with multiple downstream services.

```
                Employee Service
                       │
      ┌──────────┬──────────┬──────────┐
      ▼          ▼          ▼
Department   Notification   Payroll
 Service        Service      Service
```

If the Notification Service becomes extremely slow,

it starts occupying all available application threads.

Without isolation,

Employee Service eventually runs out of threads.

As a result:

- Employee APIs stop responding.
- Department APIs stop responding.
- Payroll APIs stop responding.

Although only Notification Service is slow,

the entire application becomes unavailable.

---

## Bulkhead Solution

Bulkhead isolates resources.

```
                Employee Service

        ┌──────────────┐
        │ Thread Pool A│────► Department Service
        └──────────────┘

        ┌──────────────┐
        │ Thread Pool B│────► Notification Service
        └──────────────┘

        ┌──────────────┐
        │ Thread Pool C│────► Payroll Service
        └──────────────┘
```

If Notification Service consumes all threads in Pool B,

Department Service continues using Pool A.

Payroll Service continues using Pool C.

The failure remains isolated.

---

## Goal of Bulkhead

Bulkhead protects the application by preventing resource exhaustion.

Instead of one failing dependency affecting the entire application,

only the isolated component is impacted.

---

## Real-World Examples

### Netflix

Video recommendation service becomes slow.

Playback service continues working because both use different thread pools.

---

### Amazon

Product Recommendation service fails.

Customers can still:

- Search products
- Add items to cart
- Place orders

---

### Uber

Driver Location Service becomes slow.

Ride History and Payment services continue functioning independently.

---

### Banking Application

Statement Service becomes overloaded.

Money Transfer continues because it uses a different resource pool.

---

## Key Learning

Bulkhead is **not** about retrying failures.

Bulkhead is **not** about detecting failures.

Bulkhead is about **isolating resources** so that failures do not spread throughout the application.

---

# Phase 20.2 – Types of Bulkhead

After understanding why Bulkhead exists, the next step is to understand how it is implemented.

Resilience4j provides **two different types of Bulkhead**.

1. Semaphore Bulkhead
2. ThreadPool Bulkhead

Both solve the same problem but use different approaches.

---

# 1. Semaphore Bulkhead

Semaphore Bulkhead limits the number of concurrent executions of a method.

It does **not** create new threads.

Instead, it uses a semaphore (a counter).

Example:

Suppose we configure:

```text
Maximum Concurrent Calls = 3
```

Current requests:

```text
Request 1  ✅ Running

Request 2  ✅ Running

Request 3  ✅ Running

Request 4  ❌ Rejected

Request 5  ❌ Rejected
```

Only three requests can execute simultaneously.

The remaining requests are rejected immediately.

---

## How does it work?

```
Incoming Requests

        │

        ▼

Semaphore Counter

        │

        ▼

Allowed

        │

        ▼

Business Method
```

Every request first checks the semaphore.

If a permit is available,

the request proceeds.

Otherwise,

Resilience4j throws:

```text
BulkheadFullException
```

---

## Advantages

- Very lightweight
- No thread creation
- Very fast
- Low memory consumption
- Easy to configure

---

## Limitations

It does not isolate threads.

All requests still execute using the application's existing thread pool.

---

# Example

Tomcat Thread Pool

```
100 Threads
```

Semaphore Bulkhead

```
Employee Service

↓

Maximum Concurrent Calls = 5
```

Although Tomcat has 100 threads,

only 5 requests are allowed to enter the protected method.

---

# 2. ThreadPool Bulkhead

ThreadPool Bulkhead creates a completely separate thread pool.

Instead of using Tomcat threads,

requests are delegated to another thread pool.

Example:

```
Tomcat Thread

↓

ThreadPool Bulkhead

↓

Dedicated Worker Threads

↓

Department Service
```

Now,

slow Department calls do not block Tomcat threads.

---

## Example

Configuration:

```text
Core Thread Pool = 5

Maximum Thread Pool = 10

Queue Capacity = 20
```

Execution:

```
Incoming Requests

↓

Queue

↓

Dedicated Thread Pool

↓

Department Service
```

Requests first enter the queue.

Worker threads consume them independently.

---

## Advantages

- Complete thread isolation
- Better protection against slow services
- Better scalability

---

## Limitations

- Higher memory usage
- Thread management overhead
- Slightly more complex configuration

---

# Comparison

| Feature | Semaphore Bulkhead | ThreadPool Bulkhead |
|----------|-------------------|---------------------|
| Creates new threads | No | Yes |
| Uses existing thread pool | Yes | No |
| Lightweight | Yes | No |
| Better performance | Yes | Slightly lower |
| Complete thread isolation | No | Yes |
| Queue support | No | Yes |

---

# Which one does Spring Boot commonly use?

For synchronous REST APIs using Spring MVC,

the most commonly used implementation is:

✅ Semaphore Bulkhead

Reason:

Spring MVC already executes requests using Tomcat worker threads.

Adding another thread pool usually introduces unnecessary complexity.

---

# When is ThreadPool Bulkhead preferred?

ThreadPool Bulkhead is more suitable for:

- Long-running operations
- Slow external services
- Blocking IO
- Expensive computations
- Asynchronous workflows

---

# Which one will WorkSphere use?

WorkSphere currently uses:

- Spring Boot
- Spring MVC
- OpenFeign
- Synchronous REST communication

Therefore,

we will implement:

✅ **Semaphore Bulkhead**

This aligns with how most enterprise Spring Boot applications protect synchronous service calls.

ThreadPool Bulkhead will be discussed later when we introduce asynchronous processing and more advanced scenarios.

---

# Key Learning

Although both implementations are called Bulkheads,

they protect applications in different ways.

Semaphore Bulkhead protects by **limiting concurrent executions**.

ThreadPool Bulkhead protects by **isolating execution threads**.

Choosing the correct implementation depends on the application's architecture and workload.

