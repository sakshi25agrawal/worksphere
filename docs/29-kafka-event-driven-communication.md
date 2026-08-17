# WorkSphere – Kafka Event-Driven Communication

## 1. Overview

WorkSphere uses Apache Kafka for asynchronous, event-driven communication between microservices.

Instead of making every operation synchronous through REST APIs, services can publish events to Kafka and other services can consume those events independently.

For example:

Employee Service
|
| EmployeeCreatedEvent
v
Kafka
|
+--------------------+
|                    |
v                    v
Department Service      Payroll Service

When an employee is created, Employee Service publishes an `EmployeeCreatedEvent`.

Payroll Service consumes the event and automatically creates the initial payroll record for that employee.

This reduces direct coupling between services and allows the system to process business events asynchronously.

---

## 2. Why Kafka is Used in WorkSphere

Kafka is used for asynchronous communication and event-driven processing.

The main benefits are:

- Loose coupling between microservices
- Asynchronous communication
- Reliable event delivery
- Event replay using offsets
- Consumer groups
- Horizontal scalability
- Independent service processing
- Better resilience between services
- Ability to process high-volume events

For example, Employee Service does not need to directly call Payroll Service when an employee is created.

Instead:

Employee Service
|
| Publish EmployeeCreatedEvent
v
Kafka
|
v
Payroll Service

Employee Service only needs to know about the event contract.

Payroll Service can independently consume and process the event.

---

## 3. Kafka Architecture in WorkSphere

The Kafka implementation is organized into a reusable Kafka module.

Current structure:

kafka-module
│
├── pom.xml
│
└── src
└── main
└── java
└── com
└── worksphere
└── kafka
├── config
│   ├── KafkaConsumerConfig.java
│   └── KafkaProducerConfig.java
│
├── event
│   └── EmployeeCreatedEvent.java
│
└── topic
└── KafkaTopics.java

The `kafka-module` contains common Kafka configuration and event contracts that can be reused by multiple microservices.

---

## 4. Kafka Module

The `kafka-module` is a shared Maven module.

Its purpose is to centralize Kafka-related components that are common across WorkSphere services.

It contains:

- Kafka producer configuration
- Kafka consumer configuration
- Event definitions
- Kafka topic constants

Other services can include this module as a Maven dependency.

Example:

```xml
<dependency>
    <groupId>com.worksphere</groupId>
    <artifactId>kafka-module</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

This allows Employee Service, Department Service, Payroll Service, and future services to reuse the same Kafka infrastructure.

## 5. EmployeeCreatedEvent

The event exchanged between services is:
````
EmployeeCreatedEvent
````
It represents the business event:

"An employee has been successfully created."

The event contains employee-related information required by downstream services.

Current event structure:
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
Using a Java record provides an immutable event object.

This is useful for event-driven communication because events should generally represent a fixed fact that has already happened.

## 6. Kafka Topic

The event is published to the following Kafka topic:
````
employee-created
````
The topic constant is maintained in:
````
KafkaTopics.java
````
Example:

````
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String EMPLOYEE_CREATED = "employee-created";
}
````
Using constants avoids duplicating topic names throughout the application.

## 7. Event Flow

The current WorkSphere employee creation flow is:
````
Client
  |
  | POST /employees
  v
Employee Service
  |
  | Save Employee
  |
  | Publish EmployeeCreatedEvent
  v
Kafka Topic
employee-created
  |
  +--------------------------+
  |                          |
  v                          v
Department Service       Payroll Service
Consumer                 Consumer

````
The important point is that Kafka acts as the communication layer between the producer and consumers.

## 8. Employee Service as Kafka Producer

Employee Service publishes an event after successfully creating an employee.

The Kafka publisher is located at:
````
employee-service/src/main/java/com/worksphere/employee/kafka/EmployeeKafkaPublisher.java
````
Its responsibility is to publish:
````
EmployeeCreatedEvent
````
to
````
employee-created
````
The Employee Service therefore acts as a Kafka producer.

Conceptually:
````
kafkaTemplate.send(
        KafkaTopics.EMPLOYEE_CREATED,
        employeeId,
        event
);
````
The employee ID is used as the Kafka message key.

## 9. Why Employee ID is Used as the Kafka Key

The employee ID is used as the Kafka message key.

For example:
````
Key: 13

Value:
{
    "employeeId": 13,
    "firstName": "Amey",
    "lastName": "Sharma",
    "email": "amey.sharma@example.com",
    "salary": 65000.0,
    "departmentId": 1
}
````
Kafka uses the key when determining the partition for the message.

Using employee ID as the key helps ensure that events for the same employee are routed consistently to the same partition.

This is useful when ordering matters for events belonging to the same employee.

## 10. Kafka Producer Configuration

The reusable producer configuration is maintained in:

````
kafka-module/src/main/java/com/worksphere/kafka/config/KafkaProducerConfig.java
````
The producer is configured to serialize:
````
Key   -> String
Value -> JSON
````
The important serializers are:
````
StringSerializer
JsonSerializer
````
Therefore, an EmployeeCreatedEvent is converted into JSON before being sent to Kafka.

Example event:

````
{
  "employeeId": 13,
  "firstName": "Amey",
  "lastName": "Sharma",
  "email": "amey.sharma@example.com",
  "salary": 65000.0,
  "departmentId": 1
}
````
## 11. Kafka Consumer Configuration

The reusable consumer configuration is maintained in:
````
kafka-module/src/main/java/com/worksphere/kafka/config/KafkaConsumerConfig.java
````
The consumer is configured to deserialize Kafka messages into Java objects.

The configuration uses:
````
JsonDeserializer
````
The trusted package configuration allows the Kafka event class to be deserialized safely.

For WorkSphere:
````
com.worksphere.kafka.event
````
is configured as a trusted package.

## 12. Initial Serialization Problem

During development, Payroll Service initially produced this error:
````
MessageConversionException:
Cannot convert from [java.lang.String]
to [com.worksphere.kafka.event.EmployeeCreatedEvent]
````
The Kafka message payload was received as a JSON string instead of being converted into:
````
EmployeeCreatedEvent
````
The payload looked like:
````
{
  "employeeId": 13,
  "firstName": "Amey",
  "lastName": "Sharma",
  "email": "amey.sharma@example.com",
  "salary": 65000.0,
  "departmentId": 1
}
````
But Spring Kafka was receiving it as:
````
java.lang.String
````
while the listener expected:
````
EmployeeCreatedEvent
````
The producer and consumer serialization/deserialization configuration was corrected.

After the correction, Payroll Service successfully received the event as:

````
EmployeeCreatedEvent
````
## 13. Payroll Service as Kafka Consumer

Payroll Service contains:
````
payroll-service/src/main/java/com/worksphere/payroll/kafka/EmployeeEventConsumer.java
````
This class consumes:
````
employee-created
````
using:
````
@KafkaListener(
        topics = "employee-created",
        groupId = "worksphere-payroll-group"
)
````
The listener receives:
````
EmployeeCreatedEvent event
````
and processes the employee information.

## 14. Payroll Event Processing

When Payroll Service receives an EmployeeCreatedEvent, it performs the following steps:
````
Receive EmployeeCreatedEvent
        |
        v
Extract employeeId
        |
        v
Check whether payroll already exists
        |
        +---- Yes ----> Skip duplicate event
        |
        No
        |
        v
Create CreatePayrollRequest
        |
        v
Create Payroll
        |
        v
Calculate Net Salary
        |
        v
Save Payroll
````
The initial payroll request is created using:
````
CreatePayrollRequest request = new CreatePayrollRequest(
        event.employeeId(),
        BigDecimal.valueOf(event.salary()),
        BigDecimal.ZERO,
        BigDecimal.ZERO
);
````
Therefore:

````
basicSalary = employee salary
bonus       = 0
tax         = 0
````
The payroll service then calculates:
````
Net Salary = Basic Salary + Bonus - Tax
````
## 15. Idempotency

Kafka consumers must consider the possibility that an event may be processed more than once.

WorkSphere implements an idempotency check in Payroll Service.

Before creating payroll:
````
if (payrollRepository.existsByEmployeeId(event.employeeId())) {

    log.info(
            "Payroll already exists for employeeId={}. " +
                    "Skipping duplicate event.",
            event.employeeId()
    );

    return;
}
````
This means that if the same EmployeeCreatedEvent is delivered again, Payroll Service does not create another payroll record.

Example:
````
EmployeeCreatedEvent(employeeId=13)
        |
        v
Payroll does not exist
        |
        v
Create payroll
````
If the same event is delivered again:
````
EmployeeCreatedEvent(employeeId=13)
        |
        v
Payroll already exists
        |
        v
Skip event
````
This protects the system from duplicate business processing.

## 16. Database-Level Protection Against Duplicates

Application-level idempotency is also supported by a database unique constraint.

The Payroll entity contains:
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
The employee ID column is also unique:
````
@Column(
        name = "employee_id",
        nullable = false,
        unique = true
)
private Long employeeId;
````
Therefore, the system has two layers of protection:
````
Kafka duplicate event
        |
        v
Application idempotency check
        |
        v
Database unique constraint
````
This is an important enterprise design principle.

## 17. Consumer Group

Payroll Service uses the consumer group:

```` worksphere-payroll-group ````

A consumer group allows Kafka to coordinate consumption among multiple instances of the same service.
For example:

````
worksphere-payroll-group

        |
        +---- Payroll Instance 1
        |
        +---- Payroll Instance 2
        |
        +---- Payroll Instance 3
````
Kafka distributes partitions among active consumers in the group.

This allows Payroll Service to scale horizontally.

## 18. Kafka Partitions

The employee-created topic currently has multiple partitions.

For example:
````
employee-created

Partition 0
Partition 1
Partition 2
````
Kafka stores messages in partitions.

A message is assigned to a partition when it is produced.

The consumer group then processes those partitions.

Example:
````
Topic: employee-created

Partition 0
    |
    +-- Employee Event
    +-- Employee Event

Partition 1
    |
    +-- Employee Event
    +-- Employee Event

Partition 2
    |
    +-- Employee Event
````
Partitions allow Kafka to process messages concurrently.

## 19. Kafka Offset

Every Kafka message has an offset within its partition.

For example:
````
Partition 0

Offset 0
Offset 1
Offset 2
Offset 3
````
The offset identifies the position of a message within a partition.

Kafka consumers maintain their progress using offsets.

For example:
````
CURRENT-OFFSET = 2
LOG-END-OFFSET = 2
LAG            = 0
````
This means the consumer has processed all available messages in that partition.

## 20. Kafka Consumer Lag

Consumer lag represents how many messages are waiting to be processed.

Formula:
````
Lag = Log End Offset - Current Offset
````
Example:
````
Current Offset = 5
Log End Offset = 8

Lag = 8 - 5
    = 3
````
Therefore, three messages are still waiting to be consumed.

In our testing, we used:
````
docker exec -it worksphere-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group worksphere-payroll-group --describe
````
This allowed us to inspect:
````
CURRENT-OFFSET
LOG-END-OFFSET
LAG
````
## 21. Resetting Kafka Offsets

During testing, Kafka consumer offsets can be reset.

For example:
````
docker exec -it worksphere-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group worksphere-payroll-group --topic employee-created --reset-offsets --to-earliest --execute
````
Then:
````
--to-earliest
````
option moves the consumer group's offsets to the earliest available messages.

This does not create new messages.

It changes where the consumer starts reading messages from.

## 22. What Happens After Offset Reset

Suppose the topic contains:
````
Offset 0
Offset 1
Offset 2
````
The consumer has already processed:
````
Offset 0
Offset 1
Offset 2
````
The current offset is at the end.

If we reset to earliest:

````
Offset 0
   |
   v
Consumer starts reading again
````
The consumer can process the old messages again.

This is useful for testing replay behavior.

Because Payroll Service has idempotency protection, replaying an existing employee-created event should not create another payroll record.

## 23. Kafka Event Replay

Kafka's retained messages allow consumers to replay historical events by changing their offsets.

Example:
````
Kafka Topic

0 ---- 1 ---- 2 ---- 3 ---- 4
                    ^
                    |
              Current position
````
After resetting:
````
0 ---- 1 ---- 2 ---- 3 ---- 4
^
|
Consumer starts again
````
This is useful for:

* Debugging
* Reprocessing events
* Recovering from failures
* Testing
* Rebuilding derived data

## 24. Duplicate Event Handling

A duplicate event can occur for several reasons.

For example:

Consumer retry
Consumer restart
Offset replay
At-least-once delivery
Manual offset reset
Temporary processing failure

WorkSphere handles duplicate employee-created events using:
````
Employee ID
+
Database uniqueness
+
Application idempotency
````
Therefore, duplicate delivery does not automatically mean duplicate business data.

## 25. At-Least-Once Processing

The current Kafka design should be considered with an at-least-once processing mindset.

An event may potentially be delivered or processed more than once.

Therefore, consumers should be idempotent.

Payroll Service follows this principle by checking:
````
payrollRepository.existsByEmployeeId(event.employeeId())
````
before creating payroll.

This is preferable to assuming that every Kafka message will be delivered exactly once from the application's business perspective.

## 26. Kafka Retry Dependency

The WorkSphere Kafka module uses Spring Kafka.

The dependency tree was verified using:
````
mvn dependency:tree "-pl" "payroll-service" "-Dincludes=org.springframework.kafka"
````
The project currently resolves:
````
org.springframework.kafka:spring-kafka:3.3.8
````
The Spring Kafka dependency also brings Spring Retry:

````
org.springframework.retry:spring-retry:2.0.12
````
The dependency was verified using:
````
mvn dependency:tree "-pl" "payroll-service" "-Dincludes=org.springframework.retry"
````
## 27. Kafka Retry and Failure Handling

Kafka processing can fail because of:

Temporary service failures
Database failures
Invalid data
Network failures
Serialization problems
Business processing exceptions

Spring Kafka provides mechanisms for handling failed listener processing.

The WorkSphere project is being developed toward retry and dead-letter handling so that temporary failures do not immediately result in permanent message loss.

The Kafka retry infrastructure uses separate consumer groups/topics where configured.

## 28. Dead Letter Topic Concept

A Dead Letter Topic (DLT) is used for messages that cannot be successfully processed after the configured retry attempts.

Conceptually:
````
employee-created
       |
       v
Payroll Consumer
       |
       | Failure
       v
Retry
       |
       | Failure again
       v
Retry
       |
       | Maximum attempts reached
       v
Dead Letter Topic
````
This prevents a permanently problematic message from continuously blocking normal processing.

## 29. Kafka Retry Topics

During the WorkSphere Kafka retry implementation, retry consumer groups such as:
````
worksphere-payroll-group-retry-2000
worksphere-payroll-group-retry-4000
worksphere-payroll-group-retry-8000
````
and the DLT consumer group:
```` worksphere-payroll-group-dlt````

were observed during testing.

The retry groups represent delayed retry processing.

The exact retry behavior is controlled by the Spring Kafka retry configuration implemented in the project.

## 30. Docker Kafka Setup

Kafka is currently run using Docker.

The Kafka container is:
````
worksphere-kafka
````
The exposed port is:
````
9092
````
The container was verified using:

````
docker ps --format "table {{.Names}}\t{{.Ports}}"
````
Example:
````
NAMES              PORTS
worksphere-kafka   0.0.0.0:9092->9092/tcp
````
Kafka is therefore accessible from the local WorkSphere services using:

```` localhost:9092 ````

## 31. Kafka Docker Compose

Kafka infrastructure is maintained under:
````
docker/docker-compose.yml
````
The Docker Compose file allows Kafka infrastructure to be started consistently.

Example command:
````
docker compose -f docker/docker-compose.yml up -d
````
To verify the container:
````
docker ps
````
To stop the infrastructure:
````
docker compose -f docker/docker-compose.yml down
````

## 32. Kafka Troubleshooting

One of the connection errors observed during development was:

````
Bootstrap broker localhost:9092 disconnected
````
and:
````
Connection to node -1 (localhost/127.0.0.1:9092)
could not be established.
Node may not be available.
````
This occurred when Kafka was not available on the expected port.

The Kafka Docker container was then started.

After Kafka became available on:

````
localhost:9092
````
the Payroll Service consumer successfully connected.

## 33. Verifying Kafka Consumer Group

The following command can be used to inspect the Payroll consumer group

````
docker exec -it worksphere-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group worksphere-payroll-group --describe
````

The output provides:
````
GROUP
TOPIC
PARTITION
CURRENT-OFFSET
LOG-END-OFFSET
LAG
CONSUMER-ID
HOST
CLIENT-ID
````
This is useful for monitoring Kafka consumption.

## 34. Example Consumer Group Output

Example:
````
GROUP                    TOPIC            PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
worksphere-payroll-group employee-created 0          2               2               0
worksphere-payroll-group employee-created 1          3               3               0
worksphere-payroll-group employee-created 2 
````
The first two partitions have:
````
LAG = 0
````
meaning they are fully consumed.

The third partition has:
````
LAG = 1
````
meaning one message remains to be processed.
## 35. End-to-End Employee Creation Flow

The complete current flow is:
````
Client
  |
  | Create Employee
  v
API Gateway
  |
  v
Employee Service
  |
  | Validate request
  |
  | Save employee in database
  |
  | Publish EmployeeCreatedEvent
  v
Kafka
  |
  | employee-created
  |
  +--------------------------+
  |                          |
  v                          v
Department Consumer      Payroll Consumer
                              |
                              v
                       Check employeeId
                              |
                     +--------+--------+
                     |                 |
                  Exists             Not Exists
                     |                 |
                     v                 v
                   Skip          Create Payroll
                                       |
                                       v
                                  Save Payroll
````
This demonstrates asynchronous communication between WorkSphere microservices.

## 36. REST vs Kafka Communication

WorkSphere uses both synchronous and asynchronous communication.

## REST / OpenFeign

Used when the caller needs an immediate response.

Example:
````
Employee Service
      |
      | REST / Feign
      v
Department Service
````
The Employee Service waits for the Department Service response.

## Kafka

Used when an event can be processed asynchronously.

Example:
````

Employee Service
      |
      | EmployeeCreatedEvent
      v
Kafka
      |
      v
Payroll Service
````
Employee Service does not need to wait for Payroll Service to complete payroll processing.

## 37. Why Kafka Instead of Direct REST for Employee Creation Events

A direct REST approach would create tighter coupling:
````
Employee Service
      |
      | POST /payroll
      v
Payroll Service
````
If Payroll Service is unavailable, Employee Service may also be affected.

With Kafka:
````
Employee Service
      |
      | Publish Event
      v
Kafka
      |
      v
Payroll Service
````
Employee Service and Payroll Service are decoupled.

Kafka retains the event so the consumer can process it when it becomes available, subject to the configured retention and consumer behavior.

## 38. Advantages of the WorkSphere Kafka Design

The current architecture provides:

## Loose Coupling

Employee Service does not directly depend on Payroll Service for employee creation processing.

## Asynchronous Processing

Payroll creation can happen independently after the employee creation event is published.

## Scalability

Multiple Payroll Service instances can consume events using the same consumer group.

## Replayability

Kafka offsets allow historical events to be replayed.

## Idempotency

Duplicate events do not create duplicate payroll records.

## Resilience

Retry and dead-letter mechanisms can isolate failed event processing.

## Reusability

Common Kafka configuration and event contracts are maintained in the shared kafka-module.

## 39. Current Kafka Components in WorkSphere

The current implementation contains:
````
kafka-module
    |
    +-- KafkaProducerConfig
    |
    +-- KafkaConsumerConfig
    |
    +-- EmployeeCreatedEvent
    |
    +-- KafkaTopics
````
Employee Service:
````
EmployeeKafkaPublisher
````
Department Service:
````
EmployeeEventConsumer
````
Payroll Service:
````
EmployeeEventConsumer
````
Infrastructure:
````
docker/docker-compose.yml
````
## 40. Important Kafka Concepts Used in WorkSphere

The implementation demonstrates the following Kafka concepts:

````
Producer
Consumer
Topic
Partition
Message Key
Consumer Group
Offset
Consumer Lag
Serialization
Deserialization
Event
Asynchronous Communication
Idempotency
Retry
Dead Letter Topic
Event Replay
Horizontal Scaling
````
These concepts form the foundation of the WorkSphere event-driven architecture.

## 41. Testing Checklist

Kafka functionality can be tested using the following checklist:

````
[ ] Start Kafka using Docker
[ ] Start Eureka Server
[ ] Start required microservices
[ ] Create an employee
[ ] Verify EmployeeCreatedEvent is published
[ ] Verify Payroll Service receives the event
[ ] Verify payroll record is created
[ ] Create/replay the same event
[ ] Verify duplicate payroll is not created
[ ] Check consumer group offsets
[ ] Check consumer lag
[ ] Test offset reset
[ ] Test retry behavior
[ ] Test failed message handling
[ ] Verify retry topics
[ ] Verify Dead Letter Topic behavior
````
## 42. Example Kafka Development Commands

Start Kafka:
````
docker compose -f docker/docker-compose.yml up -d
````
Check running containers:
```` docker ps  ````

Check Kafka container logs:
````
docker logs worksphere-kafka --tail 20
````
Check Payroll consumer group:
````
docker exec -it worksphere-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group worksphere-payroll-group --describe
````
Reset Payroll consumer offsets to earliest:
````
docker exec -it worksphere-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group worksphere-payroll-group --topic employee-created --reset-offsets --to-earliest --execute
````
## 43. Lessons Learned During Implementation

Several practical Kafka issues were encountered while developing WorkSphere.

## Serialization and Deserialization Must Match

The producer and consumer must agree on the message format.

Initially the consumer received:

```` String  ````
instead of:
```` EmployeeCreatedEvent ````

This caused a MessageConversionException.

Correct Kafka serializer/deserializer configuration resolved the problem.

## Kafka Must Be Available Before Consumers Can Connect

If Kafka is unavailable, the consumer repeatedly attempts to reconnect.

Typical messages include:
```` Bootstrap broker localhost:9092 disconnected ````

## Consumer Groups Maintain Progress

Kafka does not simply "delete" a message after a consumer reads it.

The consumer group tracks its position using offsets.

## Duplicate Processing Must Be Expected

Consumers should not assume that an event will only ever be processed once.

Business operations should be designed to be idempotent where appropriate.

## Offset Reset Is a Testing Tool

Resetting offsets allows old messages to be replayed without creating new messages.

This was useful for validating WorkSphere's duplicate-event handling.

## 44. Interview Explanation

A concise explanation of the WorkSphere Kafka implementation is:

"WorkSphere uses Kafka for asynchronous event-driven communication between microservices. When an employee is created, Employee Service publishes an EmployeeCreatedEvent to the employee-created Kafka topic. Payroll Service consumes that event using the worksphere-payroll-group consumer group and automatically creates the employee's initial payroll. We use employeeId as the Kafka message key, JSON serialization/deserialization for event communication, and multiple partitions for scalability. We also implemented idempotency in Payroll Service by checking whether payroll already exists for the employee before processing the event. Kafka consumer offsets allow us to track processing progress and replay events when required. We also have retry and dead-letter handling for failed message processing."

## 45. Current Architecture Summary

The Kafka-based architecture can be summarized as:
````
                    WorkSphere
                         |
             +-----------+-----------+
             |                       |
       Synchronous              Asynchronous
       Communication            Communication
             |                       |
        REST / Feign                Kafka
             |                       |
             v                       v
      Department Service       employee-created
                                      |
                                      v
                              Payroll Service
````
Kafka provides the asynchronous communication backbone for business events while REST/OpenFeign remains available for synchronous request-response communication.

## 46. Conclusion

Kafka provides WorkSphere with an event-driven communication model that reduces direct coupling between microservices.

The current implementation demonstrates:

````
Employee Creation
       |
       v
EmployeeCreatedEvent
       |
       v
Kafka Topic
       |
       +--------------------+
       |                    |
       v                    v
Department Service     Payroll Service
                            |
                            v
                    Idempotent Processing
                            |
                            v
                      Payroll Database
````
The implementation also covers important production-oriented Kafka concepts such as:

````
Consumer Groups
Partitions
Offsets
Consumer Lag
Event Replay
Idempotency
Retry
Dead Letter Topics
Serialization
Deserialization
Docker-based Kafka Infrastructure
````
This forms the event-driven communication foundation of the WorkSphere platform.



