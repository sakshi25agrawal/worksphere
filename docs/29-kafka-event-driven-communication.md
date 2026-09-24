# WorkSphere – Kafka Event-Driven Communication

## 1. Overview

WorkSphere uses Apache Kafka for asynchronous, event-driven communication between microservices.

Instead of making every operation synchronous through REST APIs, services can publish events to Kafka and other services can consume those events independently.

For example:

Employee Service
|
| EmployeeCreatedEvent
v
Kafka: employee-created
|
+------------------+
|                  |
v                  v
Payroll Service      Leave Service

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
Employee Service
      |
      | EmployeeCreatedEvent
      v
Kafka: employee-created
      |
      +------------------+
      |                  |
      v                  v
Payroll Service      Leave Service

````
The important point is that Kafka acts as the communication layer between the producer and consumers.

## 8. Employee Service as Kafka Producer

Employee Service publishes an `EmployeeCreatedEvent` to Kafka.

The Kafka publisher is located at:

employee-service/src/main/java/com/worksphere/employee/kafka/EmployeeKafkaPublisher.java

Its responsibility is to publish:

EmployeeCreatedEvent

to the Kafka topic:

employee-created

The Employee Service therefore acts as a Kafka producer.

The current implementation uses the employee ID as the Kafka message key:

kafkaTemplate.send(
EMPLOYEE_CREATED_TOPIC,
String.valueOf(event.employeeId()),
event
);

So the Kafka message contains:

Topic → employee-created
Key   → employee ID converted to String
Value → EmployeeCreatedEvent

Using the employee ID as the message key allows Kafka to consistently associate events for the same employee with the same partition.

The event is then consumed independently by downstream services such as Payroll Service and Leave Service.

The important point is that Kafka acts as the communication layer between the producer and consumers.

Employee Service does not need to make a synchronous REST call to Payroll Service or Leave Service to trigger these downstream operations.


## 9. Why Employee ID is Used as the Kafka Key

The employee ID is used as the Kafka message key.

For example:

Key: "13"

Value:
{
"employeeId": 13,
"firstName": "Amey",
"lastName": "Sharma",
"email": "amey.sharma@example.com",
"salary": 65000.0,
"departmentId": 1
}

Kafka uses the message key when determining the partition for the message.

Using employee ID as the key helps ensure that events for the same employee are consistently routed to the same partition.

This is useful when ordering matters for multiple events belonging to the same employee.

For example, an employee may generate multiple events during the lifecycle of the employee:

EmployeeCreated
EmployeeUpdated
EmployeeDepartmentChanged

If the same employee ID is used as the key for these events, Kafka can route them consistently to the same partition.

This allows the consumer to process events for that employee in partition order.

### Important Kafka Ordering Rule

Kafka guarantees message ordering only within a partition.

It does not provide a global ordering guarantee across all partitions of a topic.

Therefore, using a stable key such as employee ID is useful when the application needs ordering for events belonging to the same entity.


## 10. Kafka Producer Configuration

The reusable producer configuration is maintained in:

kafka-module/src/main/java/com/worksphere/kafka/config/KafkaProducerConfig.java

The producer is configured to serialize:

Key   → String
Value → JSON

The important serializers are:

StringSerializer
JsonSerializer

Therefore, an `EmployeeCreatedEvent` is converted into JSON before being sent to Kafka.

Example event:

{
"employeeId": 13,
"firstName": "Amey",
"lastName": "Sharma",
"email": "amey.sharma@example.com",
"salary": 65000.0,
"departmentId": 1
}

### Configurable Kafka Bootstrap Server

The producer configuration does not hardcode the Kafka server address.

Instead, it reads the bootstrap server from:

spring.kafka.bootstrap-servers

The shared configuration uses:

localhost:9092

as the default value.

This allows the same Kafka module to work in different environments.

For example:

Local application
→ localhost:9092

When the application runs inside Docker:

Docker application
→ kafka:9092

Docker Compose provides the Docker-specific value:

SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

This distinction is important because `localhost` inside a Docker container refers to the current container itself, not to the Kafka container.

Therefore, Dockerized WorkSphere services communicate with Kafka using the Docker service name:

kafka:9092

while applications running directly on the host machine can use:

localhost:9092


## 11. Kafka Consumer Configuration

The reusable consumer configuration is maintained in:

kafka-module/src/main/java/com/worksphere/kafka/config/KafkaConsumerConfig.java

The consumer is configured to deserialize Kafka messages into Java objects.

The configuration uses:

JsonDeserializer

The trusted package configuration allows the Kafka event class to be deserialized safely.

For WorkSphere:

com.worksphere.kafka.event

is configured as a trusted package.

This allows the consumer to deserialize the JSON message back into the corresponding Java event object:

EmployeeCreatedEvent

### Configurable Kafka Bootstrap Server

The consumer configuration reads the Kafka bootstrap server from:

spring.kafka.bootstrap-servers

The default value is:

localhost:9092

When running inside Docker, the application receives:

kafka:9092

through the Docker environment configuration.

Therefore:

Host machine
→ localhost:9092

Docker containers
→ kafka:9092

### Consumer Groups

WorkSphere uses separate Kafka consumer groups for the services consuming the `employee-created` event.

Payroll Service uses:

worksphere-payroll-group

Leave Service uses:

worksphere-leave-group

The architecture is:

                         employee-created
                               |
                    +----------+----------+
                    |                     |
                    v                     v
             Payroll Service        Leave Service
             worksphere-            worksphere-
             payroll-group          leave-group
                    |                     |
                    v                     v
             Create Payroll       Initialize Employee
                                   Leave Balances

Because Payroll Service and Leave Service use different consumer groups, each service receives and processes its own copy of the `employee-created` event.

This allows multiple independent services to react to the same business event.

The producer does not need to know which downstream services are consuming the event.

This is one of the key benefits of event-driven architecture: the producer and consumers remain loosely coupled.
## 12. Initial Serialization Problem

During development, Payroll Service initially produced a message conversion error because the Kafka payload was received as a `String` instead of being deserialized into the expected `EmployeeCreatedEvent` object.

The problem was conceptually:

Kafka message
|
v
JSON payload
|
v
Received as String
|
v
Listener expects EmployeeCreatedEvent
|
v
MessageConversionException

The producer and consumer serialization/deserialization configuration was then aligned.

The producer uses:

StringSerializer
JsonSerializer

and the consumer uses:

JsonDeserializer

with the WorkSphere Kafka event package configured as trusted:

com.worksphere.kafka.event

After the configuration was corrected, Payroll Service successfully received the message as:

EmployeeCreatedEvent

This demonstrates an important Kafka concept:

The producer and consumer must agree on how the message key and value are serialized and deserialized.


## 13. Payroll Service as Kafka Consumer

Payroll Service contains the Kafka consumer:

payroll-service/src/main/java/com/worksphere/payroll/kafka/EmployeeEventConsumer.java

This class consumes events from:

employee-created

using the consumer group:

worksphere-payroll-group

The listener is conceptually:

@KafkaListener(
topics = "employee-created",
groupId = "worksphere-payroll-group"
)

The listener receives:

EmployeeCreatedEvent

and processes the employee information.

Payroll Service therefore acts as an independent Kafka consumer.

It does not need Employee Service to directly call its payroll API when an employee is created.


## 14. Payroll Event Processing

When Payroll Service receives an `EmployeeCreatedEvent`, it performs the following steps:

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

The initial payroll request is created using the employee information from the event.

The salary received from the employee event is used as the basic salary.

The initial values are:

basicSalary = employee salary
bonus       = 0
tax         = 0

The payroll service then calculates:

Net Salary = Basic Salary + Bonus - Tax

For example:

Basic Salary = 70000
Bonus        = 0
Tax          = 0

Net Salary   = 70000 + 0 - 0
= 70000

The resulting payroll record is then stored in the Payroll Service database.


## 15. Idempotency

Kafka consumers must consider the possibility that an event may be processed more than once.

WorkSphere implements an idempotency check in Payroll Service.

Before creating payroll, the consumer checks whether payroll already exists for the employee:

if (payrollRepository.existsByEmployeeId(event.employeeId())) {
log.info(
"Payroll already exists for employeeId={}. Skipping duplicate event.",
event.employeeId()
);
return;
}

This means that if the same `EmployeeCreatedEvent` is delivered again, Payroll Service does not create another payroll record.

Example:

EmployeeCreatedEvent(employeeId=13)
|
v
Payroll does not exist
|
v
Create payroll

If the same event is delivered again:

EmployeeCreatedEvent(employeeId=13)
|
v
Payroll already exists
|
v
Skip duplicate event

This protects the business operation from duplicate event processing.


## 16. Database-Level Protection Against Duplicates

Application-level idempotency is also supported by a database unique constraint.

The Payroll entity contains a unique constraint on `employee_id`.

Conceptually:

payroll
-------------------------
employee_id UNIQUE

The employee ID column is therefore unique in the Payroll table.

This gives WorkSphere two layers of duplicate protection:

Kafka duplicate event
|
v
Application idempotency check
|
v
Database unique constraint

The application check prevents unnecessary processing.

The database constraint provides an additional data-integrity guarantee.

This is an important enterprise design principle: critical uniqueness rules should not rely only on application code.


## 17. Consumer Groups

A Kafka consumer group allows multiple instances of the same service to share the processing of a topic.

Payroll Service uses:

worksphere-payroll-group

Leave Service uses:

worksphere-leave-group

These are separate consumer groups.

This means both services independently consume the same `employee-created` event.

Conceptually:

                     employee-created
                            |
              +-------------+-------------+
              |                           |
              v                           v
      Payroll Consumer              Leave Consumer
      payroll-group                 leave-group
              |                           |
              v                           v
       Create Payroll             Initialize Leave
                                  Balances

If Payroll Service has multiple instances:

worksphere-payroll-group
|
+------+------+
|      |      |
v      v      v
Payroll  Payroll  Payroll
1        2        3

Kafka can distribute partitions among the active consumers in the same group.

This allows a consumer service to scale horizontally.

Consumers belonging to different groups receive independent copies of the topic's records.


## 18. Kafka Partitions

Kafka stores messages in partitions.

WorkSphere's Docker Kafka broker is configured with:

KAFKA_NUM_PARTITIONS=3

This establishes three as the broker's default partition count for newly created topics when a partition count is not otherwise specified.

Conceptually, a topic can contain:

employee-created

Partition 0
Partition 1
Partition 2

Messages are assigned to partitions when they are produced.

Because Employee Service uses employee ID as the message key, Kafka can consistently route messages for the same key to the same partition.

Partitions provide the foundation for Kafka's parallel processing model.

Multiple partitions allow multiple consumers within the same consumer group to process different partitions concurrently.


## 19. Kafka Offset

Every Kafka record has an offset within its partition.

For example:

Partition 0

Offset 0
Offset 1
Offset 2
Offset 3

The offset represents the record's position within that partition.

Kafka consumers use offsets to keep track of their processing position.

For example:

CURRENT-OFFSET = 2
LOG-END-OFFSET = 2
LAG            = 0

This indicates that the consumer has caught up with the available records for that partition.

Offsets are maintained per consumer group and partition.

Therefore, two different consumer groups can have different offsets for the same Kafka topic.


## 20. Kafka Consumer Lag

Consumer lag represents records that have not yet been consumed up to the current end of the partition.

A simplified formula is:

Lag = Log End Offset - Current Offset

Example:

Current Offset = 5
Log End Offset = 8

Lag = 8 - 5
= 3

This means the consumer is three records behind the current end of the partition.

Consumer lag is an important operational metric.

A continuously increasing lag can indicate that:

Producer rate > Consumer processing rate

Possible causes include:

Slow processing
Insufficient consumer instances
Database latency
External service latency
Consumer failures
Resource constraints

Monitoring consumer lag is therefore important in production Kafka systems.


## 21. Inspecting Kafka Consumer Groups

During Docker testing, Kafka consumer group information can be inspected from inside the Kafka container.

For example:

docker exec -it worksphere-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group worksphere-payroll-group --describe

The command can provide information such as:

GROUP
TOPIC
PARTITION
CURRENT-OFFSET
LOG-END-OFFSET
LAG

This is useful when troubleshooting whether Payroll Service is consuming events correctly.

The Kafka container itself can communicate with the Kafka broker using:

localhost:9092

This is different from the address used by other Docker containers.


## 22. Resetting Kafka Offsets

During development and testing, Kafka consumer offsets can be reset.

For example:

docker exec -it worksphere-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group worksphere-payroll-group --topic employee-created --reset-offsets --to-earliest --execute

The:

--to-earliest

option moves the consumer group's offset to the earliest available record.

This does not create new Kafka messages.

It changes the position from which the consumer reads the existing records.

This feature is particularly useful when testing event replay and idempotency.


## 23. Kafka Event Replay

Kafka retains records according to its topic retention configuration.

A consumer can replay existing records by moving its consumer group offset backwards.

For example:

Kafka Topic

Offset 0 ---- Offset 1 ---- Offset 2 ---- Offset 3 ---- Offset 4
^
|
Current position

After resetting the offset:

Offset 0 ---- Offset 1 ---- Offset 2 ---- Offset 3 ---- Offset 4
^
|
Consumer starts reading again

This allows previously produced events to be processed again.

Event replay can be useful for:

- Debugging
- Reprocessing events
- Recovering derived data
- Testing consumers
- Rebuilding downstream state

Replay also demonstrates why idempotent consumers are important.


## 24. Duplicate Event Handling

A duplicate event can occur for several reasons, including:

- Consumer retry
- Consumer restart
- Offset replay
- At-least-once processing
- Manual offset reset
- Temporary processing failure

WorkSphere protects Payroll processing using:

Employee ID
+
Application idempotency check
+
Database unique constraint

Therefore, receiving the same employee-created event more than once does not automatically create multiple payroll records.

This is especially important when using retry mechanisms, because a failed processing attempt may cause the same event to be attempted again.


## 25. At-Least-Once Processing

The current WorkSphere Kafka design should be understood using an at-least-once processing model.

This means an event may potentially be delivered or processed more than once.

Therefore, Kafka consumers should be designed to tolerate duplicate delivery.

Payroll Service follows this principle by checking:

payrollRepository.existsByEmployeeId(event.employeeId())

before creating payroll.

The important architectural principle is:

Kafka delivery
|
v
Consumer processing
|
v
Idempotent business operation

The application should not assume that every event will result in exactly one business-side effect.


## 26. Retry Processing

WorkSphere uses Spring Kafka retry support for the employee-created event consumers.

Payroll Service and Leave Service use:

@RetryableTopic(
attempts = "4",
backoff = @Backoff(
delay = 2000,
multiplier = 2.0
)
)

The configuration means that a failed event-processing attempt can be retried according to the configured retry policy.

The configured backoff starts at:

2000 ms

with a multiplier of:

2.0

Conceptually, the retry delay increases between attempts.

The purpose of retry processing is to handle temporary failures without immediately losing the event.

Examples of temporary failures can include:

Temporary database problem
Temporary downstream dependency failure
Transient infrastructure issue

Retry is different from ignoring an exception.

The consumer processing must fail in a way that allows Spring Kafka's retry mechanism to handle the event.


## 27. Dead Letter Topic

If an event continues to fail after the configured retry attempts, Spring Kafka can route the failed record to a Dead Letter Topic (DLT).

WorkSphere defines a DLT handler using:

@DltHandler

Conceptually:

employee-created
|
v
Consumer
|
v
Processing fails
|
v
Retry
|
v
Retry
|
v
Retry
|
v
Retry exhausted
|
v
Dead Letter Topic

The DLT prevents a permanently failing record from continuously blocking normal processing.

The DLT handler logs information about the employee event that was moved to the DLT.


## 28. Retry and DLT Flow in Payroll Service

The Payroll Service processing flow is:

Kafka
|
v
employee-created
|
v
Payroll Consumer
|
+---- Success ----> Create Payroll
|
+---- Failure
|
v
Retry
|
+---- Success ----> Create Payroll
|
+---- Failure
|
v
Retry Again
|
v
Retry Exhausted
|
v
DLT

The consumer group is:

worksphere-payroll-group

The retry configuration is:

Attempts       → 4
Initial delay  → 2000 ms
Multiplier     → 2.0

The DLT handler records the failed event so that the failure can be investigated.


## 29. Retry and DLT Flow in Leave Service

Leave Service also consumes:

employee-created

using:

worksphere-leave-group

Its consumer performs:

Receive EmployeeCreatedEvent
|
v
Extract employeeId
|
v
Initialize employee leave balances

The consumer uses the same retry pattern:

Attempts       → 4
Initial delay  → 2000 ms
Multiplier     → 2.0

If processing continues to fail after the configured attempts, the event can be handled by the DLT handler.

The Leave Service consumer therefore follows the same resilience pattern as the Payroll consumer.


## 30. Leave Service as Kafka Consumer

Leave Service contains an employee event consumer:

leave-service/src/main/java/com/worksphere/leave/kafka/EmployeeEventConsumer.java

It listens to:

employee-created

using:

worksphere-leave-group

When an `EmployeeCreatedEvent` is received, Leave Service calls its leave balance service:

EmployeeCreatedEvent
|
v
EmployeeEventConsumer
|
v
LeaveBalanceService
|
v
Initialize Employee Leave Balances

This means employee creation can automatically trigger leave balance initialization without Employee Service directly calling Leave Service.


## 31. Employee Created Event Fan-Out

The same Kafka event can be consumed by multiple independent services.

The current WorkSphere architecture is:

                         Employee Service
                                |
                                |
                     EmployeeCreatedEvent
                                |
                                v
                       Kafka: employee-created
                                |
                 +--------------+--------------+
                 |                             |
                 v                             v
          Payroll Service                Leave Service
          payroll-group                 leave-group
                 |                             |
                 v                             v
          Create Payroll              Initialize Leave
                                      Balances

The producer publishes the event once.

Each consumer group independently receives the event.

This is commonly referred to as event fan-out.


## 32. Why Separate Consumer Groups Matter

Payroll Service and Leave Service intentionally use different consumer groups.

Payroll:

worksphere-payroll-group

Leave:

worksphere-leave-group

If both services used the same consumer group, Kafka would treat them as consumers belonging to the same processing group.

With separate groups:

employee-created
|
+----------------------+
|                      |
v                      v
payroll-group              leave-group
|                      |
v                      v
Payroll processing       Leave processing

This allows the same business event to trigger independent downstream workflows.

This is one of the main reasons Kafka is useful for event-driven microservice architectures.


## 33. Kafka in Docker

WorkSphere runs Kafka using Docker Compose.

The Kafka container is:

worksphere-kafka

The Docker Compose Kafka service uses separate listeners for internal Docker communication and external host access.

The important addresses are:

Docker containers
→ kafka:9092

Host machine
→ localhost:29092

Kafka container itself
→ localhost:9092

This distinction is important.

Inside a Docker container, `localhost` refers to that container.

Therefore, an application container should not use:

localhost:9092

to reach another container's Kafka broker.

Instead, Dockerized WorkSphere services use:

kafka:9092


## 34. Kafka Internal and External Listeners

The Kafka Docker configuration defines:

INTERNAL://kafka:9092
EXTERNAL://localhost:29092

The listener configuration separates:

Internal Docker communication

from:

Host machine communication

Conceptually:

                 Docker Network
                      |
        +-------------+-------------+
        |                           |
        v                           v
Employee Service              Payroll Service
|                           |
+-------------+-------------+
|
v
kafka:9092
|
Kafka Broker
|
v
localhost:29092
for host access

This configuration allows Dockerized services to communicate using the Docker service name while still allowing host-based tools to connect through the external listener.


## 35. Docker Kafka Networking Problem

During Docker testing, Kafka initially used:

localhost:9092

inside shared producer and consumer configuration.

This caused problems for Dockerized services.

Inside the Payroll container:

localhost

meant:

Payroll container

rather than:

Kafka container

As a result, the Kafka consumer could not correctly communicate with the broker.

The same issue can occur with Kafka producers.

The solution was to make the Kafka bootstrap server configurable.


## 36. Environment-Specific Kafka Configuration

The shared producer and consumer configurations read:

spring.kafka.bootstrap-servers

instead of hardcoding the Docker address.

The default value is:

localhost:9092

This is useful when running an application directly from the host environment.

Dockerized services receive:

SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

Therefore:

Local application
→ localhost:9092

Dockerized application
→ kafka:9092

Host-based Kafka client
→ localhost:29092

This approach keeps the shared Kafka module reusable across environments.

The application code does not need to be changed when moving between local execution and Docker execution.


## 37. End-to-End Employee → Kafka → Payroll Flow

The complete employee creation flow is:

Client
|
| POST Employee
v
Employee Service
|
| Save Employee
|
| Publish EmployeeCreatedEvent
v
Kafka
|
| employee-created
|
+----------------------------+
|                            |
v                            v
Payroll Service             Leave Service
|                            |
| Create Payroll             | Initialize Leave Balances
v                            v
Payroll Database            Leave Database

This architecture separates the initial employee operation from downstream processing.

Employee Service publishes the business event.

Payroll Service independently reacts to the event.

Leave Service independently reacts to the same event.


## 38. End-to-End Docker Verification

The Kafka integration was verified using the Dockerized WorkSphere environment.

A new employee was created through Employee Service.

The created employee received:

Employee ID: 2
Salary: 70000

Employee Service published:

EmployeeCreatedEvent

to:

employee-created

Payroll Service consumed the event and created the payroll record.

The resulting Payroll record was verified from inside the Payroll container.

The response was:

{
"id": 1,
"employeeId": 2,
"basicSalary": 70000.00,
"bonus": 0.00,
"tax": 0.00,
"netSalary": 70000.00
}

This verifies the complete flow:

Employee Service
|
v
Kafka employee-created
|
v
Payroll Service
|
v
Payroll Database

The verification demonstrates that the Dockerized Kafka producer and consumer can communicate successfully using:

kafka:9092


## 39. Verifying Payroll Consumer Processing

The Payroll Service consumer can be monitored through Docker logs.

For example:

docker logs worksphere-payroll

Useful Kafka-related information includes:

Consumer started
Partition assignment
Consumer group
Kafka broker connection
EmployeeCreatedEvent processing

The Kafka consumer group can also be inspected from the Kafka container.

The combination of:

Application logs
+
Kafka consumer group information
+
Database verification

provides a practical way to verify event-driven processing.


## 40. Common Kafka Troubleshooting

### Problem 1: Consumer Cannot Connect to Kafka

Possible error:

Timed out waiting for a node assignment

Check the Kafka bootstrap server.

For Dockerized applications:

kafka:9092

For host-based applications:

localhost:9092

For host-based access to the Docker Kafka external listener:

localhost:29092


### Problem 2: Message Conversion Error

Example:

MessageConversionException

Check:

Producer Serializer
Consumer Deserializer
Trusted Packages
Event Class

The producer should serialize the event as JSON and the consumer should deserialize it into:

EmployeeCreatedEvent


### Problem 3: Consumer Receives Nothing

Check:

Topic name
Consumer group
Kafka connection
Consumer logs
Consumer offsets
Consumer lag

Also verify that the producer actually published the event.


### Problem 4: Duplicate Payroll

Check:

payrollRepository.existsByEmployeeId(...)

and the database unique constraint on:

employee_id


### Problem 5: Docker Uses localhost

If a Dockerized service is configured with:

localhost:9092

verify whether it is actually intended to connect to the Kafka broker inside the same container.

For communication with the Kafka Docker service, use:

kafka:9092


## 41. REST vs Kafka Communication

WorkSphere uses both synchronous REST communication and asynchronous Kafka communication.

### REST

REST is appropriate when an immediate response is required.

Example:

Client
|
| GET Employee
v
Employee Service
|
v
Response

The caller waits for the response.

### Kafka

Kafka is appropriate when an event can be processed asynchronously.

Example:

Employee Service
|
| EmployeeCreatedEvent
v
Kafka
|
+------------+
|            |
v            v
Payroll        Leave

The producer publishes the event without requiring a synchronous response from every downstream consumer.

The two communication styles serve different purposes and can coexist in the same microservice architecture.


## 42. Current Kafka Implementation Status

The current WorkSphere Kafka implementation includes:

### Implemented

- Shared `kafka-module`
- `EmployeeCreatedEvent`
- `KafkaTopics`
- Kafka producer configuration
- Kafka consumer configuration
- JSON serialization
- JSON deserialization
- Employee Service Kafka producer
- Payroll Service Kafka consumer
- Leave Service Kafka consumer
- Separate consumer groups
- Employee ID as Kafka message key
- Payroll idempotency check
- Database uniqueness protection
- Retry configuration
- DLT handlers
- Docker Kafka
- Internal Docker Kafka networking
- External Kafka listener
- Environment-specific bootstrap server configuration

### Verified

The following flow has been verified in Docker:

Employee Service
|
v
employee-created
|
v
Payroll Service
|
v
Payroll Database

A new employee event resulted in automatic payroll creation.

### Conceptual / Operational Topics

The documentation also covers:

- Consumer lag
- Offset management
- Offset reset
- Event replay
- Horizontal consumer scaling
- Retry behavior
- Dead Letter Topics
- At-least-once processing

These concepts are important for understanding how the implementation behaves in larger production environments.


## 43. Kafka Scalability

Kafka supports horizontal scaling through partitions and consumer groups.

For example:

Topic
|
+---- Partition 0
|
+---- Partition 1
|
+---- Partition 2

A consumer group can have multiple instances:

worksphere-payroll-group

       |
+---+---+
|   |   |
v   v   v
C1   C2   C3

Kafka can assign different partitions to different consumer instances.

This allows the workload to be processed concurrently.

However, the maximum useful consumer parallelism within a group is constrained by the number of partitions available to that topic.


## 44. Kafka Event-Driven Architecture Benefits

The WorkSphere Kafka implementation provides several architectural benefits.

### Loose Coupling

Employee Service does not need direct knowledge of every downstream operation.

### Asynchronous Processing

Payroll and Leave processing can occur independently after the employee event is published.

### Independent Scaling

Payroll and Leave consumers can be scaled independently.

### Resilience

Retry and DLT mechanisms provide a way to handle processing failures.

### Replay

Kafka offsets allow consumers to replay retained events when required.

### Extensibility

Additional services can consume the same event using their own consumer group.

For example, a future service could consume:

employee-created

for another independent business process without modifying Employee Service's publishing logic.


## 45. Interview Explanation

A concise interview explanation of the WorkSphere Kafka implementation is:

In WorkSphere, I used Apache Kafka for asynchronous communication between microservices.

When an employee is created, Employee Service publishes an EmployeeCreatedEvent to the employee-created topic.

The employee ID is used as the Kafka message key, which helps keep events for the same employee associated with the same partition.

Payroll Service consumes the event using the worksphere-payroll-group consumer group and automatically creates the initial payroll record.

Leave Service independently consumes the same event using a separate worksphere-leave-group consumer group and initializes the employee's leave balances.

The producer uses StringSerializer and JsonSerializer, while the consumer uses JsonDeserializer with the WorkSphere Kafka event package configured as trusted.

For reliability, Payroll and Leave consumers use retry and DLT handling.

Payroll also has an idempotency check and a database unique constraint on employee_id to prevent duplicate payroll records.

For Docker, applications inside the Docker network connect to Kafka using kafka:9092, while host-based access uses the external listener localhost:29092.

This gives WorkSphere asynchronous processing, loose coupling, independent consumer scaling, retry handling, and the ability to replay events using Kafka offsets.


## 46. Final Architecture

The current WorkSphere Kafka architecture can be summarized as:

                         +------------------+
                         |      Client      |
                         +--------+---------+
                                  |
                                  | REST
                                  v
                         +------------------+
                         | Employee Service |
                         +--------+---------+
                                  |
                                  | EmployeeCreatedEvent
                                  | Key = employeeId
                                  v
                         +------------------+
                         |      Kafka       |
                         | employee-created |
                         +--------+---------+
                                  |
                     +------------+------------+
                     |                         |
                     |                         |
                     v                         v
          +-------------------+      +-------------------+
          |  Payroll Service  |      |   Leave Service   |
          |                   |      |                   |
          | payroll-group     |      | leave-group       |
          +---------+---------+      +---------+---------+
                    |                          |
                    v                          v
          +-------------------+      +-------------------+
          | Payroll Database  |      |  Leave Database   |
          +-------------------+      +-------------------+

The complete event-driven flow is:

Employee creation
|
v
Employee Service
|
| EmployeeCreatedEvent
v
Kafka topic: employee-created
|
+--------------------------+
|                          |
v                          v
Payroll Service              Leave Service
|                          |
v                          v
Create Payroll             Initialize Leave
Balances

The key architectural idea is that Employee Service publishes a business event, while downstream services independently decide how to react to that event.

This keeps the services loosely coupled and allows WorkSphere to evolve by adding new event consumers without requiring the Employee Service to synchronously call every downstream service.