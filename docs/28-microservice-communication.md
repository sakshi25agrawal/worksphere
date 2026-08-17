# 28. Microservice Communication in WorkSphere

## 1. Introduction

WorkSphere follows a Microservices Architecture where different business capabilities are implemented as independent services.

The major services in WorkSphere are:

- API Gateway
- Eureka Server
- Auth Service
- Employee Service
- Department Service
- Payroll Service
- Kafka Module
- Common Library

Since these services are independently deployed applications, they need a reliable mechanism to communicate with each other.

In WorkSphere, two major communication patterns are used:

1. Synchronous communication using REST APIs and OpenFeign
2. Asynchronous communication using Apache Kafka

These two communication styles solve different problems.

Synchronous communication is used when one service needs an immediate response from another service.

Asynchronous communication is used when a service needs to publish an event without waiting for another service to complete processing.

---

## 2. Why Microservices Need Communication

In a monolithic application, different modules can directly call each other's methods.

For example:

```text
EmployeeService
       |
       v
DepartmentService

```
The Employee module could directly call a Java method from the Department module.

However, in a microservices architecture, each service is a separate application.

For example:

``````
+------------------+
| Employee Service |
+------------------+

+--------------------+
| Department Service |
+--------------------+

+----------------+
| Payroll Service |
+----------------+
``````
These applications have:

Separate processes
Separate deployments
Separate databases
Separate lifecycle
Independent scaling

Therefore, direct Java method calls cannot be used between services.

Communication must happen over the network.

## 3. Communication Patterns Used in WorkSphere

WorkSphere uses both synchronous and asynchronous communication.

``````
                    WorkSphere
                        |
          +-------------+-------------+
          |                           |
          v                           v
   Synchronous                  Asynchronous
   Communication                Communication
          |                           |
          v                           v
   REST / OpenFeign                Kafka
          |                           |
          v                           v
 Immediate Response             Event Driven
``````

## 4. Synchronous Communication

Synchronous communication means the caller sends a request to another service and waits for a response.

For example:

``````
Employee Service
       |
       | GET Department
       v
Department Service
       |
       | DepartmentResponse
       v
Employee Service

``````
The Employee Service cannot complete that particular operation until it receives a response from the Department Service.

##  5. REST API Communication

REST is one of the most common communication mechanisms between microservices.

For example, Department Service exposes:

``````
GET /api/departments/{departmentId}
``````

Employee Service can call this API over HTTP.

Conceptually:
``````
Employee Service
       |
       | HTTP GET
       |
       v
Department Service
       |
       | JSON Response
       v
Employee Service
``````
Example response:
``````
{
  "id": 1,
  "name": "Engineering",
  "description": "Engineering Department"
}
``````

## 6. Why OpenFeign Is Used

A service could use Spring's RestClient, WebClient, or another HTTP client to communicate with another service.

However, WorkSphere uses OpenFeign to simplify synchronous service-to-service communication.

Instead of manually writing HTTP client code, we define an interface.

For example:
``````
@FeignClient(name = "department-service")
public interface DepartmentFeignClient {

    @GetMapping("/api/departments/{departmentId}")
    DepartmentResponse getDepartment(
            @PathVariable Long departmentId
    );
}
``````
The developer can call:
``````
departmentFeignClient.getDepartment(departmentId);
``````
instead of manually creating an HTTP request.

## 7. OpenFeign Architecture

The communication flow is:
``````
Employee Service
       |
       v
DepartmentFeignClient
       |
       v
Spring Cloud OpenFeign
       |
       v
Eureka Service Discovery
       |
       v
Department Service
``````
The important point is that Employee Service does not need to hardcode the Department Service IP address.

## 8. Service Discovery with Eureka

WorkSphere uses Eureka Server for service discovery.

Eureka maintains information about registered services.

For example:

``````
Eureka Server
|
+-- employee-service
|
+-- department-service
|
+-- payroll-service
|
+-- auth-service
|
+-- api-gateway
``````
When a service starts, it registers itself with Eureka.

For example:
``````
Department Service
        |
        | Register
        v
   Eureka Server
``````

Employee Service can then discover Department Service through Eureka.

## 9. Why Service Discovery Is Important

Without service discovery, services may need to use fixed URLs.

For example:
``````
http://localhost:8082/api/departments/1
``````
This creates problems in distributed environments.
The service might run on

``````
10.10.1.15
``````
Or
``````
10.10.1.20
``````

or another Kubernetes pod.

Hardcoding these addresses is not suitable for a dynamic microservice environment.

With Eureka, the service communicates using the logical service name:
``````
department-service
``````
The actual instance location is resolved dynamically.

##  10. Employee → Department Communication

WorkSphere uses synchronous communication between Employee Service and Department Service.

The flow is:
``````
Client
  |
  v
API Gateway
  |
  v
Employee Service
  |
  v
DepartmentFeignClient
  |
  v
Eureka
  |
  v
Department Service
``````
For example, when Employee Service needs department information:
``````
DepartmentResponse department =
        departmentFeignClient.getDepartment(departmentId);
``````

The Feign client performs the HTTP communication.

## 11. DepartmentFeignClient

The Feign client provides an abstraction over the REST call.

Example:
``````
@FeignClient(name = "department-service")
public interface DepartmentFeignClient {

    @GetMapping("/api/departments/{departmentId}")
    DepartmentResponse getDepartment(
            @PathVariable Long departmentId
    );
}
``````
The Employee Service does not need to manually construct the URL.

It only works with the interface.

## 12. Employee → Payroll Communication

Employee Service also communicates synchronously with Payroll Service.

The architecture is:

``````
Employee Service
       |
       v
PayrollFeignClient
       |
       v
Eureka
       |
       v
Payroll Service
``````
For example:

``````
PayrollResponse payroll =
        payrollFeignClient.getPayrollByEmployeeId(employeeId);
``````
The Payroll Service provides the corresponding REST endpoint.

## 13. Why a Separate Resilience Layer Is Used

Calling another microservice introduces network-related failures.

For example:
``````
Employee Service
       |
       | HTTP Request
       v
Department Service
       |
       X
   Service Down
``````

Possible failures include:

* Connection timeout
* Connection refused
* Service unavailable
* Too many requests
* Slow downstream service
* Temporary network failure
* Downstream service overload

Therefore, WorkSphere does not treat a remote call as a normal local method call.

The call needs resilience mechanisms.

## 14. Resilience4j with Microservice Communication

WorkSphere uses Resilience4j around the Department Service communication.

The implemented resilience mechanisms include:

Retry
Circuit Breaker
Rate Limiter
Bulkhead
Fallback

The flow is:
``````
Employee Service
       |
       v
Department Resilience Service
       |
       +--> Retry
       |
       +--> Circuit Breaker
       |
       +--> Rate Limiter
       |
       +--> Bulkhead
       |
       v
DepartmentFeignClient
       |
       v
Department Service
``````
## 15. DepartmentResilienceService

WorkSphere contains:
``````
employee-service
    |
    +-- resilience
          |
          +-- DepartmentResilienceService
          |
          +-- PayrollResilienceService
``````
The Department resilience service wraps the Feign client call.

Example:

``````
@Service
public class DepartmentResilienceService {

    private final DepartmentFeignClient departmentFeignClient;

    public DepartmentResilienceService(
            DepartmentFeignClient departmentFeignClient) {

        this.departmentFeignClient =
                departmentFeignClient;
    }

    @Retry(
            name = "departmentServiceRetry",
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
    @Bulkhead(
            name = "departmentServiceBulkhead",
            type = Bulkhead.Type.SEMAPHORE,
            fallbackMethod = "departmentFallback"
    )
    public DepartmentResponse getDepartment(
            Long departmentId) {

        return departmentFeignClient
                .getDepartment(departmentId);
    }
}
``````

This keeps resilience concerns separate from the business logic.

## 16. Retry

Retry handles temporary failures.

For example:
``````
Employee Service
       |
       | Request
       v
Department Service
       |
       X
 Temporary Failure
       |
       v
     Retry
       |
       v
Department Service
       |
       v
    Success
``````
A retry is useful when the failure may be temporary.

Examples:

Temporary network problem
Short service interruption
Transient connection failure

Retry should not be used indefinitely because repeated calls to an unhealthy service can increase the load.

## 17. Circuit Breaker

Circuit Breaker protects the application from repeatedly calling an unhealthy service.

The basic states are:
``````
       +--------+
       | CLOSED |
       +--------+
           |
           | failures
           v
       +--------+
       |  OPEN  |
       +--------+
           |
           | wait
           v
     +------------+
     | HALF_OPEN  |
     +------------+
        |       |
    success    failure
        |       |
        v       v
     CLOSED    OPEN
``````
When the downstream service repeatedly fails, the circuit can open.

After that, calls can be rejected immediately instead of continuously calling the unhealthy service.

## 18. Rate Limiter

Rate Limiter controls how many requests are allowed within a configured period.

For example:
``````
100 requests / second
``````
If the configured limit is exceeded:
``````
Request
   |
   v
Rate Limiter
   |
   +---- Allowed ----> Department Service
   |
   +---- Rejected ---> Fallback
``````
In WorkSphere, rate-limit failures are converted into:
``````
RateLimitExceededException
``````
## 19. Bulkhead

Bulkhead limits concurrent access to a downstream operation.

The purpose is to prevent one dependency from consuming all available application resources.

For example:
``````
Employee Service
|
+-- Department Calls
|       |
|       +-- Request 1
|       +-- Request 2
|       +-- Request 3
|
+-- Other Operations
``````
If too many Department requests are executing simultaneously, the Bulkhead can reject additional requests.

This prevents uncontrolled resource consumption.

## 20. Fallback Handling

WorkSphere uses a fallback method for resilience failures.

Example:

``````
private DepartmentResponse departmentFallback(
        Long departmentId,
        Exception ex) {

    log.error(
            "Department fallback: {}",
            ex.getClass().getSimpleName()
    );

    if (ex instanceof RequestNotPermitted) {

        throw new RateLimitExceededException(
                "Too many requests. Please try again after some time."
        );
    }

    if (ex instanceof CallNotPermittedException) {

        throw new DepartmentServiceUnavailableException(
                "Department Service is temporarily unavailable. " +
                "Please try again later."
        );
    }

    if (ex instanceof BulkheadFullException) {

        throw new DepartmentServiceUnavailableException(
                "Department Service is busy. Please try again later."
        );
    }

    if (ex instanceof FeignException.NotFound) {

        throw new ResourceNotFoundException(
                "Department",
                "id",
                departmentId
        );
    }

    if (ex instanceof FeignException) {

        throw new DepartmentServiceUnavailableException(
                "Department Service is temporarily unavailable. " +
                "Please try again later."
        );
    }

    throw new RuntimeException(ex);
}
``````
The fallback converts technical failures into meaningful application-level exceptions.

## 21. Synchronous Communication Summary

The synchronous communication architecture in WorkSphere is:
``````
                  API Gateway
                       |
                       v
                Employee Service
                       |
             +---------+---------+
             |                   |
             v                   v
      DepartmentFeignClient   PayrollFeignClient
             |                   |
             v                   v
          Eureka              Eureka
             |                   |
             v                   v
      Department Service    Payroll Service
``````
Resilience4j is applied around the downstream Department communication.

## 22. Asynchronous Communication with Kafka

Not every interaction should be synchronous.

Some operations can be handled asynchronously.

WorkSphere uses Apache Kafka for event-driven communication.

For example, when a new employee is created:

````
Employee Service
       |
       | EmployeeCreatedEvent
       v
     Kafka
       |
       +----------------------+
       |                      |
       v                      v
Department Service       Payroll Service
````

The Employee Service publishes an event.

The consumers process the event independently.

## 23. Why Kafka Is Used

Kafka provides asynchronous communication between services.

The producer does not need to directly call every consumer.

Instead:

````
Producer
   |
   v
 Kafka Topic
   |
   +----> Consumer 1
   |
   +----> Consumer 2
   |
   +----> Consumer 3
````
This reduces direct coupling between services.

## 24. EmployeeCreatedEvent

WorkSphere defines a shared event:

````
public record EmployeeCreatedEvent(
        Long employeeId,
        String firstName,
        String lastName,
        String email,
        Double salary,
        Long departmentId
) {
}
````
This event represents the creation of an employee.

## 25. Kafka Topic

The Employee Created event is published to:

````
employee-created
````
The topic is defined centrally in the Kafka module.

For example:

````
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String EMPLOYEE_CREATED =
            "employee-created";
}
````
This avoids duplicating topic names across services.

## 26. Kafka Module

WorkSphere contains a dedicated:

````
kafka-module
````
The module contains shared Kafka infrastructure such as:
````
kafka-module
|
+-- config
|    |
|    +-- KafkaProducerConfig
|    +-- KafkaConsumerConfig
|
+-- event
|    |
|    +-- EmployeeCreatedEvent
|
+-- topic
     |
     +-- KafkaTopics
````
This allows multiple microservices to reuse the same Kafka configuration and event models.

## 27. Kafka Producer

Employee Service publishes an event when an employee is created.

Conceptually:

````
EmployeeService
      |
      v
EmployeeKafkaPublisher
      |
      v
KafkaTemplate
      |
      v
employee-created topic
````
Example:

````
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeKafkaPublisher {

    private final KafkaTemplate<String, EmployeeCreatedEvent>
            kafkaTemplate;

    public void publishEmployeeCreated(
            EmployeeCreatedEvent event) {

        kafkaTemplate.send(
                KafkaTopics.EMPLOYEE_CREATED,
                String.valueOf(event.employeeId()),
                event
        );

        log.info(
                "Published EmployeeCreatedEvent for employeeId={}",
                event.employeeId()
        );
    }
}
````
## 28. Kafka Consumer

Payroll Service consumes the EmployeeCreatedEvent.

Example:

````
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final PayrollService payrollService;
    private final PayrollRepository payrollRepository;

    @KafkaListener(
            topics = "employee-created",
            groupId = "worksphere-payroll-group"
    )
    public void handleEmployeeCreated(
            EmployeeCreatedEvent event) {

        log.info(
                "Received EmployeeCreatedEvent for employeeId={}",
                event.employeeId()
        );

        if (payrollRepository.existsByEmployeeId(
                event.employeeId())) {

            log.info(
                    "Payroll already exists for employeeId={}. " +
                    "Skipping duplicate event.",
                    event.employeeId()
            );

            return;
        }

        CreatePayrollRequest request =
                new CreatePayrollRequest(
                        event.employeeId(),
                        BigDecimal.valueOf(event.salary()),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        payrollService.createPayroll(request);

        log.info(
                "Payroll created from EmployeeCreatedEvent " +
                "for employeeId={}",
                event.employeeId()
        );
    }
}
````

## 29. Kafka Consumer Group

Payroll Service uses:

````
worksphere-payroll-group
````
as its consumer group.

A consumer group allows Kafka to distribute partitions among consumers belonging to the same group.

For example:

````
employee-created
       |
 +-----+-----+
 |     |     |
 P0    P1    P2
 |     |     |
 +-----+-----+
       |
       v
worksphere-payroll-group
````
If multiple Payroll Service instances are running, Kafka can distribute partitions among them.

## 30. Kafka Partition

A Kafka topic can contain multiple partitions.

For example:

````
employee-created

Partition 0
Partition 1
Partition 2
````
Messages are assigned to partitions.

The Employee ID is used as the Kafka message key in the WorkSphere producer.

Example:

````
Key = employeeId
Value = EmployeeCreatedEvent
````
This allows Kafka's partitioning mechanism to consistently associate messages with the same key.

## 31. Kafka Offset

Every message in a Kafka partition has an offset.

Example:

````
Partition 0

Offset 0 -> Employee 10
Offset 1 -> Employee 13
Offset 2 -> Employee 15
````
The consumer tracks the position it has processed.

For example:

````
CURRENT-OFFSET = 2
LOG-END-OFFSET = 2
LAG = 0
````
This means the consumer has caught up with the available messages for that partition.

## 32. Kafka Consumer Lag

Lag indicates how many messages are waiting to be processed.

Formula:
````
Lag = Log End Offset - Current Offset
````
For example:

````
Current Offset = 5
Log End Offset = 8

Lag = 8 - 5
    = 3
````
This means three messages are still pending for that consumer group and partition.

## 33. Idempotency in Kafka Consumers

Kafka consumers should be designed carefully because a message can potentially be processed more than once.

WorkSphere handles duplicate employee events using an idempotency check.

The Payroll consumer checks:

````
if (payrollRepository.existsByEmployeeId(
        event.employeeId())) {

    return;
}
````
This means:

````
EmployeeCreatedEvent
        |
        v
Payroll Consumer
        |
        v
Does payroll already exist?
        |
      +---+---+
      |       |
     YES      NO
      |       |
      v       v
    Skip    Create
````
This prevents duplicate payroll records for the same employee.

## 34. Database Constraint for Idempotency

The Payroll entity also protects the database from duplicate employee payroll records.

The employee ID is unique:

````
@Table(
    name = "payroll",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_payroll_employee_id",
            columnNames = "employee_id"
        )
    }
)
````
The column is also marked as unique:
````
@Column(
        name = "employee_id",
        nullable = false,
        unique = true
)
private Long employeeId;
````
Therefore, idempotency is protected at two levels:

````
Application Level
        |
        v
existsByEmployeeId()
        |
        v
Database Level
        |
        v
UNIQUE constraint
````
This provides stronger protection against duplicate payroll creation.

## 35. Synchronous vs Asynchronous Communication

The two communication styles solve different problems.

````
| Feature          | Synchronous                       | Asynchronous                    |
| ---------------- | --------------------------------- | ------------------------------- |
| Technology       | REST / OpenFeign                  | Kafka                           |
| Response         | Immediate                         | Later                           |
| Coupling         | Higher                            | Lower                           |
| Caller waits     | Yes                               | No                              |
| Typical use      | Query / immediate response        | Events                          |
| Failure handling | Timeout / Retry / Circuit Breaker | Retry / DLT / Consumer handling |
| Example          | Employee → Department             | Employee → Payroll event        |


````
## 36. When to Use Synchronous Communication

Use synchronous communication when the caller requires an immediate response.

Examples:
````
Get Department Details
Get Payroll Details
Validate Resource
Fetch Employee Information
````
For example:

````
Employee Service
       |
       | Get Department
       v
Department Service
       |
       | Response required
       v
Employee Service
````
## 37. When to Use Asynchronous Communication

Use asynchronous communication when the operation can happen independently.

For example:

````
Employee Created
       |
       v
Publish Event
       |
       v
Kafka
       |
       +----> Payroll
       |
       +----> Other Consumers
````

The Employee Service does not need to wait for Payroll Service to finish processing the event.

## 38. Advantages of Synchronous Communication

Advantages:

Simple request-response model
Immediate response
Easy to understand
Suitable for queries
Easy REST API integration
OpenFeign reduces boilerplate

However, synchronous communication creates runtime dependency between services.

If the downstream service is unavailable, the caller may also be affected.

This is why WorkSphere uses Resilience4j around important synchronous calls.

## 39. Advantages of Asynchronous Communication

Advantages:

Loose coupling
Better scalability
Services can process events independently
Producer does not need to wait for consumers
Multiple consumers can react to the same event
Useful for event-driven workflows

Kafka provides durable event storage and consumer offsets, which allow consumers to continue processing messages from their tracked positions.

## 40. Overall WorkSphere Communication Architecture

The overall communication architecture can be represented as:

````
                         Client
                           |
                           v
                     API Gateway
                           |
                           v
                    Employee Service
                      /          \
                     /            \
                    v              v
          Synchronous          Asynchronous
              |                    |
              v                    v
        OpenFeign + Eureka       Kafka
              |                    |
        +-----+-----+              |
        |           |              |
        v           v              v
 Department      Payroll       Event Consumers
 Service         Service
````
More specifically:

````
                         +-------------+
                         | API Gateway |
                         +------+------+
                                |
                                v
                       +----------------+
                       | Employee       |
                       | Service        |
                       +---+--------+---+
                           |        |
                  OpenFeign|        |Kafka Producer
                           |        |
                           v        v
                    +-----------+  +----------------+
                    |  Eureka   |  | employee-created|
                    +-----------+  |     Topic       |
                           |       +--------+---------+
                           |                |
                    +------+-----+          |
                    |            |          |
                    v            v          v
             +-----------+ +-----------+ +-----------+
             | Department| |  Payroll  | | Consumers |
             | Service   | |  Service  | |           |
             +-----------+ +-----------+ +-----------+
````

## 41. Communication Responsibility

Each communication mechanism has a specific responsibility.

API Gateway

Responsible for:

Entry point for external clients
Routing
Authentication/security integration
Cross-cutting concerns
Eureka

Responsible for:

Service registration
Service discovery
Dynamic service locations
OpenFeign

Responsible for:

Declarative HTTP communication
Synchronous service-to-service calls
Resilience4j

Responsible for:

Retry
Circuit Breaker
Rate Limiter
Bulkhead
Fallback
Kafka

Responsible for:

Event-driven communication
Asynchronous processing
Decoupling producers and consumers
Event distribution

## 42. End-to-End Employee Creation Flow

The complete Employee creation flow can be represented as:

````
Client
  |
  v
API Gateway
  |
  v
Employee Service
  |
  +--------------------------+
  |                          |
  | Save Employee            |
  |                          |
  v                          |
Employee Database            |
                             |
                             v
                    EmployeeCreatedEvent
                             |
                             v
                           Kafka
                             |
                 +-----------+-----------+
                 |                       |
                 v                       v
          Payroll Service        Other Consumers
                 |
                 v
        Idempotency Check
                 |
          +------+------+
          |             |
       Exists          New
          |             |
          v             v
        Skip       Create Payroll
````
At the same time, when Employee Service needs Department information:

````
Employee Service
       |
       v
DepartmentResilienceService
       |
       +--> Retry
       +--> Circuit Breaker
       +--> Rate Limiter
       +--> Bulkhead
       |
       v
DepartmentFeignClient
       |
       v
Eureka
       |
       v
Department Service
````
## 43. Important Design Principle

A key principle in WorkSphere is:

Use synchronous communication when an immediate response is required, and asynchronous communication when the operation can be event-driven.

For example:

````
Need department information now?
        |
        v
REST + OpenFeign
````
Whereas:

````
Employee has been created.
Other services need to react.
        |
        v
Kafka Event
````
This prevents using one communication mechanism for every problem.

## 44. Communication and Failure Isolation

Microservice communication introduces distributed-system failures.

A service can fail independently.

For example:

````
Employee Service     UP
Department Service   DOWN
Payroll Service       UP
Kafka                 UP
````

The entire system should not automatically fail because one service is unavailable.

This is why WorkSphere combines:

````
Service Discovery
        +
OpenFeign
        +
Resilience4j
        +
Kafka
        +
Idempotency

````
Each component solves a different distributed-system problem.

## 45. WorkSphere Communication Stack

The current communication stack can be summarized as:
````
Client
  |
  v
API Gateway
  |
  v
Microservices
  |
  +--------------------+
  |                    |
  v                    v
OpenFeign             Kafka
  |                    |
  v                    v
Eureka              Event Topics
  |
  v
Service Discovery
````
For synchronous calls:
````
OpenFeign
    +
Eureka
    +
Resilience4j
````
For asynchronous calls:
````
Kafka
    +
Consumer Groups
    +
Offsets
    +
Idempotency
````
## 46. Key Takeaways

WorkSphere implements both synchronous and asynchronous microservice communication.

Synchronous Communication

Implemented using:

````
REST
OpenFeign
Eureka
````

Used for immediate request-response operations.

## Resilience

Implemented using:

````
Retry
Circuit Breaker
Rate Limiter
Bulkhead
Fallback
````
Used to protect synchronous service calls.

## Asynchronous Communication

Implemented using:

````
Apache Kafka
Kafka Topics
Kafka Producers
Kafka Consumers
Consumer Groups
Offsets
````

Used for event-driven communication.

## Idempotency

Implemented using:

````
existsByEmployeeId()
+
Database UNIQUE constraint
````
Used to prevent duplicate payroll creation when duplicate events are received.

## 47. Final Architecture

The final WorkSphere microservice communication architecture is:

````
                         CLIENT
                           |
                           v
                    +-------------+
                    | API Gateway |
                    +------+------+
                           |
                           v
                  +------------------+
                  | Employee Service |
                  +--------+---------+
                           |
              +------------+------------+
              |                         |
              | Synchronous             | Asynchronous
              |                         |
              v                         v
       +---------------+              Kafka
       | Resilience4j  |                |
       +-------+-------+                v
               |                employee-created
               v                     topic
        +-------------+                |
        | OpenFeign   |                +--------+
        +------+------+                |        |
               |                       v        v
               v                  Payroll    Other
            Eureka                Service   Consumers
               |
          +----+----+
          |         |
          v         v
     Department   Payroll
      Service     Service

````

This architecture provides:

Synchronous request-response communication
Asynchronous event-driven communication
Service discovery
Fault tolerance
Loose coupling
Retry and failure handling
Duplicate event protection
Independent service scalability

WorkSphere therefore combines REST-based synchronous communication and Kafka-based asynchronous communication to support a scalable and resilient microservices architecture.


















