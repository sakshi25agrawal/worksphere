21 – Enterprise Gateway & Resilience Architecture

21.1 Introduction
Overview

As the WorkSphere project evolved, the communication between the Employee Service and Department Service became increasingly sophisticated. Initially, the project contained only a simple Feign Client responsible for invoking the Department Service.

As new Resilience4j features such as Retry, Circuit Breaker, Rate Limiter, and Bulkhead were introduced, all resilience logic was implemented inside a single service class named:

DepartmentClientService

Although this implementation was functionally correct, it gradually became difficult to maintain because one class was responsible for multiple concerns.

The service was performing:

Business communication
Feign client invocation
Retry handling
Circuit Breaker handling
Rate Limiter handling
Bulkhead handling
Exception mapping
Fallback implementation

As additional resilience features were introduced, the class continued growing, violating several software engineering principles.

To address this issue, the architecture was refactored into a layered enterprise design where each class has a single responsibility.

Why Refactoring Was Required

Initially the implementation looked similar to the following:

EmployeeServiceImpl
│
▼
DepartmentClientService
│
├── Feign Client
├── Retry
├── Circuit Breaker
├── Rate Limiter
├── Bulkhead
├── Exception Handling
└── Fallback

This architecture worked correctly for a single downstream microservice.

However, WorkSphere is designed as an enterprise platform where many additional services will eventually be introduced, including:

Notification Service
Payroll Service
Attendance Service
Leave Service
Project Service
Audit Service

If the same approach were followed for every service, each client service would contain duplicate resilience logic, resulting in code duplication, reduced readability, and difficult maintenance.

Therefore, an enterprise-grade architecture became necessary.

Objectives of the Refactoring

The primary objectives of this architectural refactoring were:

1. Separate Business Logic from Infrastructure Logic

Business services should only contain business operations.

They should never contain:

Retry logic
Circuit Breaker logic
Feign communication
Resilience annotations
Infrastructure-specific code
2. Follow the Single Responsibility Principle (SRP)

Each class should have only one responsibility.

Instead of one large service performing multiple tasks, responsibilities were divided across dedicated layers.

3. Improve Maintainability

Future developers should be able to understand the project quickly without navigating large service classes containing mixed responsibilities.

4. Improve Scalability

The same architecture should support future microservices without requiring redesign.

5. Improve Reusability

The resilience layer should become reusable for every downstream microservice.

Instead of rewriting Retry, Circuit Breaker, and Bulkhead logic multiple times, the same architectural pattern can be reused consistently.

Refactoring Outcome

After the refactoring, the communication flow became:

Controller
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

Each layer now has a clearly defined responsibility, resulting in a cleaner, scalable, and enterprise-ready architecture.

Key Benefits Achieved

The new architecture provides:

Clear separation of concerns
Better maintainability
Higher scalability
Improved readability
Reduced code duplication
Easier testing
Easier debugging
Better support for future microservices
Enterprise-standard design principles
Technologies Used During Refactoring

The following technologies and frameworks are involved in the new architecture:

Technology	Purpose
Spring Boot	Backend framework
OpenFeign	Inter-service communication
Resilience4j Retry	Retry failed requests
Resilience4j Circuit Breaker	Prevent cascading failures
Resilience4j Rate Limiter	Limit request rate
Resilience4j Bulkhead	Isolate concurrent requests
Spring Dependency Injection	Layered architecture
Gateway Pattern	Abstract downstream communication
Conclusion

This refactoring represents a significant architectural improvement within the WorkSphere project.

Instead of treating resilience as an implementation detail inside business services, it has now become a dedicated infrastructure layer. This approach aligns closely with enterprise software engineering practices and establishes a scalable foundation for integrating future microservices while maintaining clean, modular, and maintainable code.

21.2 Problems with the Previous Architecture
Previous Architecture

Before the architectural refactoring, the communication between Employee Service and Department Service was implemented using a single service class named:

DepartmentClientService

The request flow looked like this:

Controller
│
▼
EmployeeServiceImpl
│
▼
DepartmentClientService
│
▼
DepartmentFeignClient
│
▼
Department Service

Initially, this architecture worked well because the project only contained a simple Feign Client responsible for communicating with the Department Service.

However, as the project evolved and enterprise resiliency features were introduced, the responsibilities of DepartmentClientService continued to grow.

Responsibilities of DepartmentClientService

Over time, this single class became responsible for:

Calling Department Service using OpenFeign
Applying Retry
Applying Circuit Breaker
Applying Rate Limiter
Applying Semaphore Bulkhead
Handling fallback methods
Mapping Feign exceptions
Throwing business exceptions
Logging failures

Instead of acting as a communication layer, it gradually became a large service responsible for multiple concerns.

Architecture Before Refactoring
DepartmentClientService

                 +-----------------------------+
                 |                             |
                 |  Feign Communication        |
                 |                             |
                 |  Retry                      |
                 |                             |
                 |  Circuit Breaker            |
                 |                             |
                 |  Rate Limiter               |
                 |                             |
                 |  Bulkhead                   |
                 |                             |
                 |  Exception Handling         |
                 |                             |
                 |  Fallback Logic             |
                 |                             |
                 +-----------------------------+

This violated one of the most important software engineering principles:

A class should have only one responsibility.

Problem 1 – Violation of Single Responsibility Principle (SRP)

The Single Responsibility Principle (SRP) states that:

A class should have only one reason to change.

However, DepartmentClientService had multiple reasons to change.

For example:

Change	Required Modification
Retry configuration changes	DepartmentClientService
Circuit Breaker configuration changes	DepartmentClientService
Feign changes	DepartmentClientService
Logging changes	DepartmentClientService
Bulkhead changes	DepartmentClientService
Business communication changes	DepartmentClientService

A single class becoming responsible for multiple unrelated concerns makes maintenance difficult.

Problem 2 – Mixed Business and Infrastructure Logic

Business services should only focus on business operations.

However, resilience logic is considered infrastructure logic.

The following responsibilities were mixed together:

Business Logic

↓

Infrastructure Logic

↓

HTTP Communication

↓

Fallback Logic

This mixing of concerns reduced code readability and made debugging more difficult.

Problem 3 – Difficult Scalability

Initially, only one downstream service existed:

Department Service

Future WorkSphere modules include:

Notification Service

Payroll Service

Attendance Service

Leave Service

Project Service

Audit Service

If the same architecture were followed, each new microservice would require another large client service.

For example:

DepartmentClientService

NotificationClientService

PayrollClientService

AttendanceClientService

ProjectClientService

Each class would duplicate:

Retry
Circuit Breaker
Rate Limiter
Bulkhead
Exception Mapping

leading to significant code duplication.

Problem 4 – Poor Maintainability

As new Resilience4j features were added, the class continuously expanded.

The implementation became increasingly difficult to understand because infrastructure concerns dominated the business communication.

Developers had to navigate through:

Multiple annotations
Large fallback methods
Several exception types
Feign-specific handling

before understanding the actual business operation.

Problem 5 – Difficult Testing

Since multiple responsibilities existed inside the same class, unit testing became more complex.

Testing required consideration of:

Retry scenarios
Circuit Breaker scenarios
Rate Limiter scenarios
Bulkhead scenarios
Feign exceptions
Business exceptions

All within a single class.

This increased testing complexity significantly.

Problem 6 – Poor Reusability

Suppose another microservice required communication with Notification Service.

The same implementation would need to be recreated:

@Retry

@CircuitBreaker

@RateLimiter

@Bulkhead

public NotificationResponse getNotification(...)

followed by another fallback implementation.

This duplicated the same resilience pattern across multiple services.

Problem 7 – Difficult Future Enhancements

The project roadmap includes several additional enterprise features:

TimeLimiter
Redis Cache
Observability
Metrics
Distributed Tracing
Logging Correlation

Adding these features directly into DepartmentClientService would make the class even larger and more difficult to maintain.

Problem Summary

The previous architecture suffered from several design issues:

Issue	Impact
Multiple Responsibilities	Violated SRP
Mixed Infrastructure & Business Logic	Reduced readability
Large Service Class	Difficult maintenance
Code Duplication	Poor scalability
Complex Testing	Increased development effort
Difficult Future Enhancements	Reduced flexibility
Poor Reusability	Same code repeated for every microservice
Why Refactoring Became Necessary

Although the previous implementation was technically correct and functionally working, it was no longer suitable for an enterprise-scale application.

As WorkSphere continues to grow with multiple downstream microservices and advanced resilience patterns, a more modular and layered architecture became essential.

The solution was to introduce a dedicated Gateway Architecture combined with a separate Resilience Layer, allowing business logic and infrastructure logic to evolve independently.

This architectural refactoring significantly improved maintainability, scalability, readability, and long-term extensibility of the application.

21.3 Enterprise Gateway Architecture (New Architecture Design)
Introduction

To overcome the limitations of the previous implementation, WorkSphere adopted a layered enterprise architecture that clearly separates business logic from infrastructure logic.

Instead of allowing a single service to manage Feign communication, Resilience4j annotations, fallback methods, and exception handling, responsibilities have been distributed across multiple dedicated layers.

Each layer now has a well-defined purpose and follows the Single Responsibility Principle (SRP).

New Enterprise Architecture

The new architecture implemented in WorkSphere is shown below.

                         Employee Service

                      Controller Layer
                             │
                             ▼
                    EmployeeServiceImpl
                     (Business Layer)
                             │
                             ▼
                    DepartmentGateway
                          (Interface)
                             │
                             ▼
                 DepartmentGatewayImpl
                     (Gateway Layer)
                             │
                             ▼
              DepartmentResilienceService
                 (Infrastructure Layer)
                             │
                             ▼
                 DepartmentFeignClient
                  (Communication Layer)
                             │
                             ▼
                    Department Service
Layer Description

The architecture is divided into five logical layers.

1. Business Layer

Class

EmployeeServiceImpl

Responsibilities

Employee business validation
Employee creation
Employee update
Employee deletion
Business rules
Calls DepartmentGateway

The business layer has no knowledge of:

OpenFeign
Retry
Circuit Breaker
Rate Limiter
Bulkhead
HTTP communication

Example

DepartmentResponse department =
departmentGateway.getDepartment(request.departmentId());

Business logic remains clean and focused.

2. Gateway Layer

Interface

DepartmentGateway

Implementation

DepartmentGatewayImpl

Responsibilities

Acts as an abstraction between business and infrastructure
Hides implementation details
Delegates communication to the resilience layer

Example

@Override
public DepartmentResponse getDepartment(Long departmentId) {

    return departmentResilienceService.getDepartment(departmentId);

}

Notice that this class contains no business logic and no resilience logic.

It simply delegates the request.

3. Resilience Layer

Class

DepartmentResilienceService

This is the heart of the new architecture.

Responsibilities

Retry
Circuit Breaker
Rate Limiter
Bulkhead
Exception Mapping
Fallback Handling

Current implementation includes:

@Retry

@CircuitBreaker

@RateLimiter

@Bulkhead

This class communicates directly with

DepartmentFeignClient

and converts infrastructure exceptions into business exceptions.

Example

departmentFeignClient.getDepartment(departmentId);
4. Communication Layer

Class

DepartmentFeignClient

Responsibilities

HTTP communication
REST API invocation
Request serialization
Response deserialization

Nothing else.

This layer should never contain:

Retry
Circuit Breaker
Business logic
Exception mapping

Its only responsibility is communication.

5. Remote Microservice
   Department Service

The Department Service remains completely independent.

It has no knowledge of:

Retry
Bulkhead
Rate Limiter
Circuit Breaker

These concerns belong entirely to the Employee Service.

Communication Flow

The complete request flow now becomes:

Create Employee Request

        │

        ▼

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

        │

        ▼

DepartmentResponse

        │

        ▼

EmployeeServiceImpl

        │

        ▼

Employee Saved

Every layer performs one specific responsibility.

Package Structure

The Employee Service package structure now follows an enterprise organization.

employee-service

└── controller
EmployeeController

└── service
EmployeeService
EmployeeServiceImpl

└── gateway
DepartmentGateway

└── gateway
└── impl
DepartmentGatewayImpl

└── resilience
DepartmentResilienceService

└── client
DepartmentFeignClient

This structure is significantly easier to understand compared to a large service class handling everything.

Dependency Flow

Dependencies move in only one direction.

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

No layer directly accesses a lower infrastructure component unless required.

This improves maintainability and testability.

Benefits of the New Architecture

The new architecture provides several improvements.

Clear Separation of Concerns

Each class performs one task only.

Better Readability

Developers can immediately identify where:

Business logic exists
Infrastructure logic exists
HTTP communication occurs
Easier Maintenance

Future changes affect only the responsible layer.

For example:

Changing Retry configuration requires modifications only inside

DepartmentResilienceService

Employee business logic remains unchanged.

Easier Testing

Each layer can now be tested independently.

For example:

EmployeeServiceImpl

Business Tests

DepartmentResilienceService

Retry Tests
Circuit Breaker Tests
Rate Limiter Tests
Bulkhead Tests

DepartmentFeignClient

Communication Tests
Better Extensibility

Future features such as

TimeLimiter
Cache
Metrics
Distributed Tracing

can be added inside the Resilience Layer without modifying business services.

Enterprise Design Philosophy

The architecture follows the principle:

Business services should never know how infrastructure works.

Business services simply request information.

Infrastructure decides how that information is obtained.

This separation significantly improves code quality and aligns WorkSphere with enterprise software engineering practices.

Conclusion

The Enterprise Gateway Architecture introduced in WorkSphere replaces a monolithic communication service with a clean, layered design.

Each class has a single responsibility, dependencies are clearly organized, and resilience features are isolated from business logic.

This architecture not only improves the current implementation but also establishes a reusable foundation that will be applied consistently across all future microservices within WorkSphere.

21.4 Package Structure & Detailed Class Responsibilities
Introduction

One of the primary goals of the enterprise architecture refactoring was to organize the project into well-defined packages where every layer has a single responsibility.

Instead of placing communication, resilience, and business logic inside one service class, the project is now divided into dedicated packages.

The package structure follows common enterprise Spring Boot practices and improves maintainability, readability, scalability, and testability.

Package Structure

The Employee Service package structure after refactoring is shown below.

employee-service
│
├── controller
│      EmployeeController
│
├── service
│      EmployeeService
│      EmployeeServiceImpl
│
├── gateway
│      DepartmentGateway
│
├── gateway
│      └── impl
│             DepartmentGatewayImpl
│
├── resilience
│      DepartmentResilienceService
│
├── client
│      DepartmentFeignClient
│
├── repository
│
├── entity
│
├── dto
│
├── exception
│
└── security

Every package now has a clearly defined responsibility.

Controller Layer

Package

controller

Class

EmployeeController

Responsibilities

Accept HTTP requests
Validate request body
Call business service
Return HTTP response

Example

@PostMapping
public ResponseEntity<EmployeeResponse> createEmployee(
@RequestBody EmployeeRequest request) {

    return ResponseEntity.ok(employeeService.createEmployee(request));

}

The controller should never communicate directly with:

Feign Client
Gateway
Resilience Layer
Repository

Its responsibility ends after calling the business service.

Business Layer

Package

service

Classes

EmployeeService

EmployeeServiceImpl

Responsibilities

Business validations
Employee creation
Employee update
Employee deletion
Business rules

Example

DepartmentResponse department =
departmentGateway.getDepartment(request.departmentId());

Notice that EmployeeServiceImpl only knows about

DepartmentGateway

It does not know:

How HTTP communication works
Which resilience pattern is used
Whether Retry executes
Whether Circuit Breaker is open
Whether Bulkhead is full

This keeps business logic completely independent of infrastructure.

Gateway Layer

Package

gateway

Class

DepartmentGateway

Responsibilities

Acts as an abstraction between the business layer and the infrastructure layer.

Current Interface

public interface DepartmentGateway {

    DepartmentResponse getDepartment(Long departmentId);

}

The interface allows the implementation to change without affecting business logic.

Gateway Implementation

Package

gateway.impl

Class

DepartmentGatewayImpl

Responsibilities

Implements DepartmentGateway
Delegates request to DepartmentResilienceService

Current Implementation

@Override
public DepartmentResponse getDepartment(Long departmentId) {

    return departmentResilienceService.getDepartment(departmentId);

}

This class intentionally contains almost no logic.

Its only purpose is delegation.

Why Do We Need Gateway?

Without Gateway

EmployeeServiceImpl

↓

DepartmentResilienceService

Business logic becomes tightly coupled to the resilience implementation.

With Gateway

EmployeeServiceImpl

↓

DepartmentGateway

↓

DepartmentGatewayImpl

The business layer depends only on an interface.

This follows the Dependency Inversion Principle.

Resilience Layer

Package

resilience

Class

DepartmentResilienceService

This is the most important infrastructure class.

Responsibilities

Retry
Circuit Breaker
Rate Limiter
Bulkhead
Exception Mapping
Fallback Handling
Feign Communication Delegation

Current annotations

@Retry

@CircuitBreaker

@RateLimiter

@Bulkhead

Business services never need to know these annotations exist.

Current Flow Inside DepartmentResilienceService
Request

↓

Retry

↓

Circuit Breaker

↓

Rate Limiter

↓

Bulkhead

↓

Feign Client

↓

Department Service

↓

Fallback

↓

Business Exception

Every infrastructure concern remains isolated inside this class.

Communication Layer

Package

client

Class

DepartmentFeignClient

Responsibilities

REST communication
HTTP request creation
HTTP response mapping

Example

@GetMapping("/{departmentId}")
DepartmentResponse getDepartment(
@PathVariable Long departmentId);

This layer should never contain:

Retry
Circuit Breaker
Bulkhead
Business validations

Its only responsibility is communication.

Repository Layer

Package

repository

Responsibilities

Database operations
CRUD
JPA Queries

The repository has no knowledge of:

Gateway
Feign
Resilience
Entity Layer

Package

entity

Responsibilities

Database mapping
Table representation

Example

Employee

No business logic should exist here.

DTO Layer

Package

dto

Responsibilities

Request Objects
Response Objects

Examples

EmployeeRequest

EmployeeResponse

DepartmentResponse

DTOs are only responsible for transferring data.

Exception Layer

Package

exception

Responsibilities

Contains custom exceptions such as

ResourceNotFoundException

DepartmentServiceUnavailableException

RateLimitExceededException

Also contains

GlobalExceptionHandler

which converts exceptions into standard REST responses.

Security Layer

Package

security

Responsibilities

JWT Authentication
JWT Filter
Spring Security Configuration
Authentication Entry Point

Completely independent from resilience.

Dependency Relationship

Dependencies always move downward.

Controller

↓

Business Layer

↓

Gateway

↓

Gateway Implementation

↓

Resilience Layer

↓

Feign Client

↓

Department Service

No lower layer should depend on an upper layer.

This maintains loose coupling.

Advantages of This Package Structure
Package	Responsibility
controller	HTTP API
service	Business Logic
gateway	Business Abstraction
gateway.impl	Delegation
resilience	Retry, CB, RL, Bulkhead
client	REST Communication
repository	Database
entity	Database Model
dto	Data Transfer
exception	Error Handling
security	Authentication & Authorization
Package Dependency Diagram
Controller
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

This dependency hierarchy keeps the architecture clean, modular, and easy to evolve.

Conclusion

The new package structure clearly separates business logic, infrastructure concerns, communication, persistence, and security into independent layers.

Each package has a well-defined responsibility, making the application easier to understand, easier to maintain, and highly scalable for future microservices.

This organization serves as the standard architecture for all future services that will be introduced into the WorkSphere platform.

21.5 Complete Request Flow, Sequence Diagram & End-to-End Execution
Introduction

Understanding the request flow is essential in any enterprise application. Although the implementation is divided into multiple layers, every request follows a well-defined execution path.

The Gateway Architecture ensures that each layer performs exactly one responsibility while maintaining loose coupling between business logic and infrastructure.

This section explains how an Employee Creation request travels through the system from the REST Controller to the Department Service and back.

High-Level Request Flow

The complete request flow implemented in WorkSphere is shown below.

                Client (Postman)

                       │
                       ▼

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

         Retry
         Circuit Breaker
         Rate Limiter
         Bulkhead

                       │
                       ▼

          DepartmentFeignClient

                       │
                       ▼

            Department Service

                       │
                       ▼

           Department Response

                       │
                       ▼

      DepartmentResilienceService

                       │
                       ▼

          DepartmentGatewayImpl

                       │
                       ▼

            EmployeeServiceImpl

                       │
                       ▼

             Save Employee

                       │
                       ▼

          EmployeeResponse

                       │
                       ▼

               Client
Step-by-Step Execution
Step 1 – Client Sends Request

The client sends a request to create a new employee.

Example:

POST /api/v1/employees

Request Body

{
"firstName": "Sakshi",
"lastName": "Agrawal",
"email": "sakshi@gmail.com",
"departmentId": 1
}
Step 2 – EmployeeController

The request reaches

EmployeeController

Controller Responsibilities

Accept HTTP Request
Validate Request
Delegate to Business Service

Controller Code

@PostMapping
public ResponseEntity<EmployeeResponse> createEmployee(
@RequestBody EmployeeRequest request) {

    return ResponseEntity.ok(employeeService.createEmployee(request));

}

Notice that the controller has no knowledge of:

Feign
Retry
Circuit Breaker
Bulkhead
Step 3 – EmployeeServiceImpl

Business processing begins.

Responsibilities

Validate employee
Check duplicate email
Validate business rules
Verify department exists

Instead of calling Feign directly:

departmentGateway.getDepartment(request.departmentId());

The business layer remains completely independent of communication details.

Step 4 – DepartmentGateway

The request enters the Gateway layer.

DepartmentGateway

This layer provides an abstraction between business logic and infrastructure.

EmployeeServiceImpl does not know:

Which communication technology is used
Whether Feign exists
Whether Retry executes
Whether Bulkhead executes

It simply calls the interface.

Step 5 – DepartmentGatewayImpl

Implementation delegates the request.

Current implementation:

@Override
public DepartmentResponse getDepartment(Long departmentId) {

    return departmentResilienceService.getDepartment(departmentId);

}

This class intentionally contains almost no business logic.

Its only purpose is delegation.

Step 6 – DepartmentResilienceService

This is the infrastructure layer.

Current implementation contains:

@Retry

@CircuitBreaker

@RateLimiter

@Bulkhead

Before calling the Department Service, Resilience4j intercepts the request.

Execution Order

Request

↓

Retry

↓

Circuit Breaker

↓

Rate Limiter

↓

Bulkhead

↓

Feign Client

Every resilience feature executes automatically before the actual HTTP request is sent.

Step 7 – DepartmentFeignClient

The Feign Client performs the HTTP communication.

Example

@GetMapping("/{departmentId}")
DepartmentResponse getDepartment(
@PathVariable Long departmentId);

Responsibilities

Create HTTP request
Serialize request
Receive response
Deserialize response

No business logic exists here.

Step 8 – Department Service

The request reaches the remote microservice.

Possible scenarios

Scenario 1

Department exists

Response

{
"departmentId": 1,
"departmentName": "Engineering"
}
Scenario 2

Department not found

Department Service returns

404 Not Found

Feign throws

FeignException.NotFound

DepartmentResilienceService converts it into

ResourceNotFoundException
Scenario 3

Department Service unavailable

Feign throws

FeignException

Fallback converts it into

DepartmentServiceUnavailableException
Scenario 4

Rate Limiter exceeded

Resilience4j throws

RequestNotPermitted

Fallback converts it into

RateLimitExceededException
Scenario 5

Circuit Breaker Open

Resilience4j throws

CallNotPermittedException

Fallback converts it into

DepartmentServiceUnavailableException
Scenario 6

Bulkhead Full

Resilience4j throws

BulkheadFullException

Fallback converts it into

DepartmentServiceUnavailableException
Response Flow

Once DepartmentResponse is received,

the response flows backward.

Department Service

↓

Feign Client

↓

DepartmentResilienceService

↓

DepartmentGatewayImpl

↓

EmployeeServiceImpl

↓

EmployeeController

↓

Client
Success Scenario Sequence Diagram
Client

│

│ POST /employees

▼

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

│

│ DepartmentResponse

▼

DepartmentFeignClient

▼

DepartmentResilienceService

▼

DepartmentGatewayImpl

▼

EmployeeServiceImpl

│

│ Save Employee

▼

EmployeeRepository

▼

EmployeeController

▼

Client
Failure Flow Example

Suppose Department Service is down.

Flow becomes

Feign

↓

FeignException

↓

DepartmentResilienceService

↓

Fallback

↓

DepartmentServiceUnavailableException

↓

GlobalExceptionHandler

↓

HTTP 503

↓

Client

Notice that EmployeeServiceImpl never handles FeignException directly.

The resilience layer completely hides infrastructure exceptions.

Advantages of This Flow

The new request flow provides:

Clear separation between business and infrastructure
Automatic resilience handling
Centralized exception conversion
Better readability
Easier debugging
Easier unit testing
Easier scalability
Enterprise-standard execution pipeline
Key Observation

The most important architectural improvement is that EmployeeServiceImpl never communicates directly with OpenFeign or Resilience4j.

Its only dependency is:

DepartmentGateway

This makes the business layer completely independent of the underlying communication mechanism and resilience implementation.

If the communication technology changes in the future (for example, from OpenFeign to gRPC or Kafka Request-Reply), the business layer will remain unchanged.

Conclusion

The new request execution pipeline ensures that every request follows a predictable, maintainable, and scalable path. Business logic remains isolated from infrastructure concerns, while resilience features operate transparently in the background. This architecture significantly improves code quality and establishes a robust foundation for all future microservice integrations within the WorkSphere platform.

21.6 SOLID Principles, Design Patterns & Enterprise Best Practices Used
Introduction

The Enterprise Gateway & Resilience Architecture implemented in WorkSphere is not simply a structural refactoring. It is based on well-established software engineering principles and design patterns that are widely used in enterprise-grade applications.

By applying these principles, the application becomes:

Easier to maintain
Easier to extend
Easier to test
More scalable
More resilient to change

This section explains how the architecture aligns with the SOLID principles and which design patterns have been implemented.

SOLID Principles

The SOLID principles are five object-oriented design principles that help developers create maintainable and extensible software.

1. Single Responsibility Principle (SRP)
   Definition

A class should have only one reason to change.

Before Refactoring

Previously, DepartmentClientService handled multiple responsibilities:

Feign communication
Retry
Circuit Breaker
Rate Limiter
Bulkhead
Exception mapping
Fallback logic

Any change to one of these concerns required modifying the same class.

After Refactoring

Responsibilities are now divided:

Class	Responsibility
EmployeeServiceImpl	Business Logic
DepartmentGateway	Business Abstraction
DepartmentGatewayImpl	Delegation
DepartmentResilienceService	Resilience Logic
DepartmentFeignClient	HTTP Communication

Each class now has only one reason to change.

2. Open/Closed Principle (OCP)
   Definition

Software entities should be open for extension but closed for modification.

Example

Suppose we decide to add:

TimeLimiter
Redis Cache
Metrics
Observability

We can extend the behavior by modifying only:

DepartmentResilienceService

The following classes remain unchanged:

EmployeeServiceImpl
DepartmentGateway
DepartmentGatewayImpl

The business layer is therefore closed for modification but open for extension.

3. Liskov Substitution Principle (LSP)
   Definition

Objects of a superclass or interface should be replaceable with objects of its implementation.

Example

EmployeeServiceImpl depends on:

DepartmentGateway

not on:

DepartmentGatewayImpl

Therefore, we can replace the implementation without changing business logic.

Example:

Current implementation:

DepartmentGatewayImpl

Future implementation:

DepartmentGrpcGatewayImpl

or

DepartmentKafkaGatewayImpl

EmployeeServiceImpl continues to work without modification.

4. Interface Segregation Principle (ISP)
   Definition

Clients should not be forced to depend on methods they do not use.

Example

Current interface:

public interface DepartmentGateway {

    DepartmentResponse getDepartment(Long departmentId);

}

The interface contains only the operations required by Employee Service.

It is small, focused, and easy to implement.

5. Dependency Inversion Principle (DIP)
   Definition

High-level modules should not depend on low-level modules. Both should depend on abstractions.

Before Refactoring
EmployeeServiceImpl

↓

DepartmentClientService

The business layer depended directly on an implementation.

After Refactoring
EmployeeServiceImpl

↓

DepartmentGateway

↓

DepartmentGatewayImpl

Now EmployeeServiceImpl depends on an abstraction.

This significantly reduces coupling.

Design Patterns Used

The architecture also incorporates several common design patterns.

1. Gateway Pattern
   Purpose

Provides a single entry point for communication with external systems.

Current implementation:

DepartmentGateway

Benefits

Hides communication details
Decouples business logic
Allows implementation changes without affecting business services
2. Adapter Pattern

DepartmentGatewayImpl adapts business requests to the resilience layer.

Business Layer

↓

Gateway

↓

Resilience Layer

This acts as an adapter between two independent layers.

3. Proxy Pattern

DepartmentResilienceService behaves like a proxy.

Instead of calling Feign directly,

the request passes through:

Retry
Circuit Breaker
Rate Limiter
Bulkhead

before reaching the remote service.

The resilience layer therefore acts as a protective proxy.

4. Facade Pattern

EmployeeServiceImpl only sees one method:

departmentGateway.getDepartment(id);

It has no knowledge of:

Retry
Bulkhead
Feign
Circuit Breaker

The gateway acts as a facade hiding internal complexity.

5. Dependency Injection Pattern

Spring injects dependencies automatically.

Example

private final DepartmentGateway departmentGateway;

This avoids manual object creation and promotes loose coupling.

Enterprise Best Practices Followed

The new architecture follows several enterprise development practices.

Layered Architecture

Each layer performs one responsibility.

Controller

↓

Business

↓

Gateway

↓

Resilience

↓

Feign

↓

Remote Service
Separation of Concerns

Business logic and infrastructure logic remain independent.

Loose Coupling

Business services depend only on interfaces.

High Cohesion

Each class performs one clearly defined task.

Centralized Exception Handling

Infrastructure exceptions are converted into business exceptions inside the resilience layer.

Business services never process Feign exceptions directly.

Infrastructure Isolation

Only one class understands:

Retry
Circuit Breaker
Rate Limiter
Bulkhead

This makes future maintenance much easier.

Why This Architecture Is Enterprise Ready

Many enterprise systems contain dozens of downstream services.

Examples

User Service

Notification Service

Payment Service

Inventory Service

Order Service

Email Service

Using the same layered approach ensures consistency across all integrations.

Each new service can follow the same pattern:

NotificationGateway

↓

NotificationGatewayImpl

↓

NotificationResilienceService

↓

NotificationFeignClient

No redesign is required.

Key Advantages
Principle / Pattern	Benefit
SRP	One responsibility per class
OCP	Easy feature extension
LSP	Replace implementations easily
ISP	Small focused interfaces
DIP	Business depends on abstractions
Gateway Pattern	Hides infrastructure details
Proxy Pattern	Controls external communication
Facade Pattern	Simplifies business interactions
Dependency Injection	Loose coupling
Layered Architecture	Clear separation of responsibilities
Conclusion

The Enterprise Gateway & Resilience Architecture implemented in WorkSphere is built upon widely accepted software engineering principles and enterprise design patterns.

By following SOLID principles, introducing a dedicated Gateway layer, isolating resilience concerns, and maintaining clear separation of responsibilities, the application becomes significantly more maintainable, extensible, and scalable.

This architectural foundation not only improves the current implementation but also provides a reusable blueprint for integrating future microservices while maintaining consistency across the entire WorkSphere platform.

21.7 Benefits, Comparison with Previous Architecture & Future Scalability
Introduction

Refactoring is successful only when it provides measurable improvements in software quality. The Enterprise Gateway & Resilience Architecture introduced in WorkSphere was designed not only to organize the codebase but also to improve maintainability, scalability, readability, extensibility, and overall software quality.

This section compares the previous implementation with the new architecture and explains how the current design prepares WorkSphere for future enterprise-level growth.

Architecture Comparison
Previous Architecture
Controller
│
▼
EmployeeServiceImpl
│
▼
DepartmentClientService
│
├── Feign Client
├── Retry
├── Circuit Breaker
├── Rate Limiter
├── Bulkhead
├── Exception Mapping
└── Fallback
│
▼
Department Service

Characteristics

One large service
Mixed responsibilities
Infrastructure tightly coupled with business logic
Difficult to maintain
Difficult to extend
New Enterprise Architecture
Controller
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

Characteristics

Layered architecture
Clear responsibilities
Infrastructure isolated
Easy to maintain
Enterprise ready
Detailed Comparison
Previous Design	New Enterprise Design
One large service class	Multiple focused classes
Mixed business and infrastructure logic	Complete separation of concerns
Tight coupling	Loose coupling
Direct implementation dependency	Interface-based dependency
Difficult testing	Independent testing per layer
Difficult scalability	Highly scalable
Code duplication for every service	Reusable architecture
Large fallback methods	Dedicated resilience layer
Hard to understand	Easy to understand
Benefits Achieved
1. Improved Readability

Every developer can now understand the project quickly.

Example

Business Layer

EmployeeServiceImpl

Infrastructure Layer

DepartmentResilienceService

Communication Layer

DepartmentFeignClient

Instead of searching through one large class, developers immediately know where a particular responsibility exists.

2. Improved Maintainability

Suppose the Retry configuration changes.

Old Architecture

Modify DepartmentClientService

New Architecture

Modify DepartmentResilienceService

No business classes need to change.

Maintenance becomes significantly easier.

3. Better Testability

Every layer can now be tested independently.

Example

EmployeeServiceImpl

Test

Employee validation
Business rules
Employee creation
DepartmentResilienceService

Test

Retry
Circuit Breaker
Rate Limiter
Bulkhead
Exception mapping
DepartmentFeignClient

Test

REST communication

Testing responsibilities are now clearly separated.

4. Reduced Code Duplication

Suppose another microservice is introduced.

Example

Notification Service

Old Design

Need another

NotificationClientService

with

Retry
Circuit Breaker
Rate Limiter
Bulkhead

implemented again.

New Design

Simply create

NotificationGateway

↓

NotificationGatewayImpl

↓

NotificationResilienceService

↓

NotificationFeignClient

The same architecture is reused.

5. Improved Scalability

WorkSphere will eventually contain multiple downstream services.

Examples

Department Service

Notification Service

Attendance Service

Payroll Service

Leave Service

Project Service

Audit Service

Reporting Service

Every service will follow the same architecture.

This creates consistency across the entire application.

6. Easier Future Enhancements

The current architecture allows new infrastructure features to be introduced without changing business services.

Upcoming features

ThreadPool Bulkhead
TimeLimiter
Cache
Metrics
Observability
Distributed Tracing
Logging Correlation
API Monitoring

These additions will primarily affect

DepartmentResilienceService

Business services remain unchanged.

7. Better Team Collaboration

Different developers can now work on different layers independently.

Example

Developer A

EmployeeServiceImpl

Developer B

DepartmentResilienceService

Developer C

DepartmentFeignClient

Since responsibilities are separated, merge conflicts and dependency issues are reduced.

8. Easier Code Reviews

Smaller focused classes are much easier to review.

Instead of reviewing a 300-line service containing multiple responsibilities, reviewers can focus on one concern at a time.

This improves code quality.

Future Scalability

The current architecture has been designed with future expansion in mind.

Suppose WorkSphere introduces Payroll.

Architecture

PayrollGateway

↓

PayrollGatewayImpl

↓

PayrollResilienceService

↓

PayrollFeignClient

↓

Payroll Service

No redesign is necessary.

The same architecture can be reused.

Suppose Notification Service is added.

Architecture

NotificationGateway

↓

NotificationGatewayImpl

↓

NotificationResilienceService

↓

NotificationFeignClient

↓

Notification Service

Again, no changes to EmployeeServiceImpl are required.

Reusable Enterprise Template

The current architecture now serves as the standard template for all downstream integrations.

Template

Business Service

↓

Gateway Interface

↓

Gateway Implementation

↓

Resilience Service

↓

Feign Client

↓

Remote Microservice

This template will be reused throughout the WorkSphere project.

Long-Term Benefits

As WorkSphere grows, the architecture will continue to provide:

Consistent project structure
Reduced maintenance cost
Faster onboarding of new developers
Easier debugging
Better code reviews
Improved scalability
Higher code quality
Enterprise-standard design
Key Learnings

During this refactoring, several important architectural lessons were learned.

A service class should never contain multiple unrelated responsibilities.
Infrastructure concerns should be isolated from business logic.
Business services should communicate through abstractions rather than implementations.
Resilience should be centralized rather than duplicated across multiple services.
A layered architecture significantly improves maintainability and scalability.

These principles will continue to guide future development within WorkSphere.

Conclusion

The Enterprise Gateway & Resilience Architecture represents a significant improvement over the previous implementation. By introducing dedicated layers for business logic, gateway abstraction, resilience handling, and communication, WorkSphere now follows a clean, modular, and scalable architecture suitable for enterprise applications.

This architecture not only solves current design challenges but also establishes a reusable framework for integrating all future microservices in a consistent and maintainable manner.

21.8 Future Roadmap, Architecture Evolution & Best Practices for Upcoming Microservices
Introduction

One of the primary reasons for introducing the Enterprise Gateway & Resilience Architecture was to prepare WorkSphere for future expansion.

At the time of implementation, only the communication between Employee Service and Department Service existed. However, WorkSphere is intended to evolve into a complete enterprise Human Resource Management System (HRMS) consisting of multiple independent microservices.

The new architecture has therefore been designed not only to solve current challenges but also to serve as the standard communication framework for all future services.

Current Architecture

Currently, communication exists only between:

Employee Service
│
▼
Department Service

The implemented classes are:

DepartmentGateway

↓

DepartmentGatewayImpl

↓

DepartmentResilienceService

↓

DepartmentFeignClient

This architecture has been fully tested using:

Retry
Circuit Breaker
Rate Limiter
Semaphore Bulkhead
Future Architecture

As WorkSphere grows, additional microservices will be introduced.

Future system architecture:

                    Employee Service
                           │
      ┌────────────────────┼─────────────────────┐
      │                    │                     │
      ▼                    ▼                     ▼

Department          Notification          Attendance
Service               Service              Service

      │                    │                     │

Payroll             Leave Service        Project Service

      │                    │                     │

Audit Service       Reporting Service    Analytics Service

Each communication path will follow exactly the same architecture.

Standard Communication Template

Every downstream microservice should follow the same pattern.

Business Service

↓

Gateway Interface

↓

Gateway Implementation

↓

Resilience Service

↓

Feign Client

↓

Remote Microservice

This template will become the official architecture standard for WorkSphere.

Example 1 – Notification Service

Future implementation:

NotificationGateway

↓

NotificationGatewayImpl

↓

NotificationResilienceService

↓

NotificationFeignClient

↓

Notification Service

EmployeeServiceImpl will simply call:

notificationGateway.sendNotification(...);

without knowing anything about:

Retry
Circuit Breaker
Bulkhead
OpenFeign
Example 2 – Payroll Service

Future implementation:

PayrollGateway

↓

PayrollGatewayImpl

↓

PayrollResilienceService

↓

PayrollFeignClient

↓

Payroll Service
Example 3 – Attendance Service

Future implementation:

AttendanceGateway

↓

AttendanceGatewayImpl

↓

AttendanceResilienceService

↓

AttendanceFeignClient

↓

Attendance Service
Example 4 – Leave Service

Future implementation:

LeaveGateway

↓

LeaveGatewayImpl

↓

LeaveResilienceService

↓

LeaveFeignClient

↓

Leave Service
Example 5 – Project Service

Future implementation:

ProjectGateway

↓

ProjectGatewayImpl

↓

ProjectResilienceService

↓

ProjectFeignClient

↓

Project Service
Why Use the Same Architecture?

Using the same architecture across every service provides consistency.

Advantages:

Every developer follows one standard.
New developers learn the project faster.
Reduced code duplication.
Easier debugging.
Easier testing.
Predictable project structure.
Lower maintenance cost.
Planned Resilience Evolution

The resilience layer has been intentionally designed so that additional features can be introduced without modifying business services.

Current implementation:

Retry

Circuit Breaker

Rate Limiter

Semaphore Bulkhead

Future implementation:

Retry

Circuit Breaker

Rate Limiter

Semaphore Bulkhead

↓

ThreadPool Bulkhead

↓

TimeLimiter

↓

Metrics

↓

Observability

↓

Distributed Tracing

↓

Logging Correlation

↓

Performance Monitoring

All these additions will primarily affect:

DepartmentResilienceService

Business services remain untouched.

21.9 Interview Questions, Real-World Scenarios & Common Discussion Points
Introduction

The Enterprise Gateway & Resilience Architecture implemented in WorkSphere follows enterprise software engineering practices that are commonly discussed during backend and microservices interviews.

Understanding not only how the architecture works but also why it was designed this way is essential for explaining design decisions confidently during interviews.

This section documents common interview questions, expected explanations, and real-world scenarios related to the implemented architecture.

Interview Question 1
Why did you introduce DepartmentGateway when EmployeeServiceImpl could directly call DepartmentResilienceService?
Answer

The primary purpose of introducing DepartmentGateway was to decouple the business layer from the infrastructure layer.

Without a gateway:

EmployeeServiceImpl
│
▼
DepartmentResilienceService

The business layer would directly depend on the resilience implementation.

With a gateway:

EmployeeServiceImpl
│
▼
DepartmentGateway
│
▼
DepartmentGatewayImpl

The business layer now depends only on an abstraction (interface), which follows the Dependency Inversion Principle (DIP) and allows the underlying implementation to change without affecting business logic.

Interview Question 2
Why not call the Feign Client directly?
Answer

Calling the Feign Client directly would tightly couple business logic with HTTP communication.

Example:

departmentFeignClient.getDepartment(id);

Problems:

Business service becomes aware of Feign.
Feign exceptions leak into business logic.
Retry and Circuit Breaker must be managed by the business service.
Future communication technology changes (gRPC, Kafka, REST Template, etc.) would require modifications to business logic.

Instead, the business layer communicates only with the Gateway.

Interview Question 3
Why was DepartmentResilienceService introduced?
Answer

DepartmentResilienceService centralizes all infrastructure-related concerns.

Current responsibilities include:

Retry
Circuit Breaker
Rate Limiter
Semaphore Bulkhead
Fallback handling
Exception mapping

This keeps the business layer completely independent of resilience implementation.

Interview Question 4
Why not place Retry, Circuit Breaker, and Rate Limiter directly inside EmployeeServiceImpl?
Answer

Business services should contain only business logic.

Infrastructure concerns such as:

Retry
Circuit Breaker
Rate Limiter
Bulkhead

should remain isolated.

Mixing these concerns violates the Single Responsibility Principle (SRP) and makes the business service difficult to maintain.

Interview Question 5
Why create both Gateway and Gateway Implementation?
Answer

The Gateway interface provides abstraction.

The implementation provides flexibility.

Future implementations could include:

DepartmentGatewayImpl

DepartmentGrpcGatewayImpl

DepartmentKafkaGatewayImpl

EmployeeServiceImpl remains unchanged because it depends only on the interface.

Interview Question 6
Why is Feign Client kept separate from ResilienceService?
Answer

The Feign Client has only one responsibility:

HTTP communication

The ResilienceService has a different responsibility:

Retry
Circuit Breaker
Rate Limiter
Bulkhead
Exception mapping

Separating these responsibilities improves maintainability and testing.

Interview Question 7
Why convert FeignException into business exceptions?
Answer

Business services should not understand infrastructure exceptions.

Instead of exposing:

FeignException.NotFound

we convert it into:

ResourceNotFoundException

Similarly,

FeignException

becomes:

DepartmentServiceUnavailableException

This creates a clean separation between infrastructure and business domains.

Interview Question 8
Why is Bulkhead placed together with Retry and Circuit Breaker?
Answer

All resilience patterns protect communication with external services.

Grouping them inside the ResilienceService provides a single infrastructure layer responsible for protecting downstream communication.

Interview Question 9
What happens if tomorrow you replace OpenFeign with gRPC?
Answer

Only the communication layer changes.

Current:

DepartmentFeignClient

Future:

DepartmentGrpcClient

Neither:

EmployeeServiceImpl
DepartmentGateway
DepartmentGatewayImpl

requires modification.

This demonstrates loose coupling.

Interview Question 10
How will you implement Notification Service?
Answer

Exactly the same architecture.

NotificationGateway

↓

NotificationGatewayImpl

↓

NotificationResilienceService

↓

NotificationFeignClient

↓

Notification Service

This consistency is one of the biggest advantages of the architecture.

Real-World Scenario 1
Department Service is Down

Flow

DepartmentFeignClient

↓

FeignException

↓

DepartmentResilienceService

↓

Fallback

↓

DepartmentServiceUnavailableException

↓

GlobalExceptionHandler

↓

HTTP 503

Business services remain unaware of Feign exceptions.

Real-World Scenario 2
Department Not Found

Flow

Department Service

↓

404

↓

FeignException.NotFound

↓

DepartmentResilienceService

↓

ResourceNotFoundException

↓

HTTP 404

The exception becomes meaningful from a business perspective.

Real-World Scenario 3
Circuit Breaker Opens

Flow

CallNotPermittedException

↓

DepartmentResilienceService

↓

DepartmentServiceUnavailableException

↓

HTTP 503

No additional business logic is required.

Real-World Scenario 4
Rate Limit Exceeded

Flow

RequestNotPermitted

↓

DepartmentResilienceService

↓

RateLimitExceededException

↓

HTTP 429
Real-World Scenario 5
Bulkhead Full

Flow

BulkheadFullException

↓

DepartmentResilienceService

↓

DepartmentServiceUnavailableException

↓

HTTP 503
Common Mistakes to Avoid
❌ Calling Feign directly from business services

Correct approach:

Business

↓

Gateway

↓

Resilience

↓

Feign
❌ Mixing business and infrastructure logic

Keep them separate.

❌ Returning FeignException to clients

Always convert infrastructure exceptions into business exceptions.

❌ Duplicating resilience logic

Every downstream service should have its own dedicated ResilienceService following the same architecture.

Enterprise Best Practices
Keep business logic clean.
Use interfaces for communication.
Centralize resilience logic.
Hide infrastructure details from business services.
Reuse the same architecture across all microservices.
Convert infrastructure exceptions into business exceptions.
Maintain one responsibility per class.
Summary

The Enterprise Gateway & Resilience Architecture implemented in WorkSphere demonstrates clean architecture principles, proper layering, loose coupling, and centralized resilience handling.

It not only improves the current implementation but also serves as a reusable architectural template for all future microservices within the platform.

A strong understanding of this architecture enables developers to confidently discuss system design, resilience patterns, and enterprise software engineering practices during technical interviews and real-world development.
