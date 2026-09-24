# 33 — Docker & Containerization

## 1. Overview

WorkSphere is a Maven multi-module Spring Boot microservices application.

Docker is used to package and run the application components in isolated containers.

The current Docker architecture includes:

- MySQL
- Kafka
- Eureka Server
- Auth Service
- Employee Service
- Department Service
- Leave Service
- Payroll Service

The API Gateway is part of the application architecture but its Docker Compose integration and final Gateway-only exposure are part of the remaining containerization/security hardening work.

The goal of containerization is to make the WorkSphere environment reproducible and to provide consistent service-to-service networking.

---

## 2. Why Docker Is Used

Without Docker, a developer would need to install and configure:

- Java
- Maven
- MySQL
- Kafka
- ZooKeeper or Kafka controller components
- service configuration
- environment variables
- databases
- networking

Docker packages infrastructure and services into reproducible environments.

Conceptually:

Developer Machine
|
+--> Docker Engine
|
+--> MySQL
+--> Kafka
+--> Eureka
+--> Auth
+--> Employee
+--> Department
+--> Leave
+--> Payroll

---

## 3. Containerization vs Virtual Machines

A virtual machine generally contains:

    Application
    + Libraries
    + Operating System
    + Virtualized hardware

A container generally contains:

    Application
    + Required runtime
    + Libraries

while sharing the host operating system kernel.

Containers are therefore generally lighter and faster to start than full virtual machines.

---

## 4. Docker Architecture

The WorkSphere Docker environment can be visualized as:

    +------------------------------------------------------+
    |                  Docker Environment                  |
    |                                                      |
    |  +-----------+       +-----------+                   |
    |  |   MySQL   |       |   Kafka   |                   |
    |  +-----------+       +-----------+                   |
    |                                                      |
    |  +-----------+       +-----------+                   |
    |  |  Eureka   |       |   Auth    |                   |
    |  +-----------+       +-----------+                   |
    |                                                      |
    |  +-----------+  +------------+  +--------+           |
    |  | Employee  |  | Department |  | Leave  |           |
    |  +-----------+  +------------+  +--------+           |
    |                                                      |
    |  +-----------+                                      |
    |  |  Payroll  |                                      |
    |  +-----------+                                      |
    +------------------------------------------------------+

All current Compose services are connected to:

    worksphere-network

---

## 5. Docker Compose

The environment is defined using:

    docker/docker-compose.yml

Docker Compose allows multiple containers to be described and started together.

Instead of manually running every container, Compose defines:

- images
- builds
- ports
- environment variables
- volumes
- networks
- dependencies
- health checks

---

## 6. Compose Service Model

Each application component becomes a Compose service.

Current services include:

    mysql
    kafka
    eureka-server
    auth-service
    employee-service
    department-service
    leave-service
    payroll-service

The service name is important because it becomes a DNS name inside the Docker network.

For example:

    kafka

can be resolved by other containers as:

    kafka

---

## 7. Docker Network

WorkSphere defines:

    worksphere-network

using:

    driver: bridge

A Docker bridge network allows containers attached to the network to communicate with each other.

Conceptually:

    worksphere-network
           |
     +-----+-----+-----+-----+
     |     |     |     |     |
    MySQL Kafka Eureka Auth Employee
     |     |     |     |     |
    Dept  Leave Payroll ...

---

## 8. Why a Dedicated Network Is Useful

A dedicated network gives the application an isolated communication environment.

Instead of every container communicating through host ports, containers can communicate directly using service names.

For example:

    employee-service -> mysql:3306

instead of:

    employee-service -> localhost:3307

The same principle applies to Kafka and Eureka.

---

## 9. Docker DNS

Docker Compose automatically provides service-name DNS within the Compose network.

For example:

    kafka

resolves to the Kafka container.

Similarly:

    mysql

resolves to the MySQL container.

And:

    eureka-server

resolves to the Eureka container.

This is one of the most important concepts when moving Spring Boot applications from local development to Docker.

---

## 10. localhost Inside Containers

A common Docker mistake is assuming:

    localhost

means:

    another container

It does not.

Inside a container:

    localhost

means:

    the current container itself.

Therefore:

    employee-service -> localhost:3306

would try to find MySQL inside the Employee container.

The correct Docker connection is:

    mysql:3306

---

## 11. MySQL Container

The current Compose file uses:

    image: mysql:8.4

Container name:

    worksphere-mysql

The MySQL root password is currently configured as:

    root

This is suitable for local portfolio development but should be replaced with secure credentials in a production environment.

---

## 12. MySQL Port Mapping

The current Compose configuration contains:

    3307:3306

This means:

    Host port 3307
        |
        v
    Container port 3306

Therefore a host application can connect using:

    localhost:3307

while containers should connect using:

    mysql:3306

---

## 13. Why Host and Container Ports Differ

Docker separates:

    host networking

from:

    container networking

Example:

    Host
      |
      | localhost:3307
      v
    MySQL container
      |
      | 3306
      v
    MySQL server

Inside the Docker network:

    mysql:3306

is the preferred address.

---

## 14. MySQL Persistent Volume

The MySQL service uses:

    mysql-data:/var/lib/mysql

This creates persistent database storage.

Without a volume:

    Container deleted
         |
         v
    Database data may disappear

With a volume:

    Container deleted
         |
         v
    Volume remains
         |
         v
    Database data can persist

---

## 15. Why Database Persistence Matters

Microservice containers are intended to be replaceable.

For example:

    Stop Employee container
    Remove Employee container
    Build new Employee image
    Start new Employee container

Application state should not depend on the container filesystem.

Database state therefore belongs in persistent storage.

---

## 16. MySQL Initialization Scripts

The Compose configuration mounts:

    ./mysql/init:/docker-entrypoint-initdb.d:ro

into the MySQL container.

This allows SQL initialization scripts stored under:

    docker/mysql/init

to be executed by the MySQL image during initial database creation.

The mount is read-only:

    :ro

which prevents the container from modifying the host-side initialization files.

---

## 17. Important MySQL Initialization Behavior

MySQL initialization scripts are primarily intended for initialization of a fresh database volume.

If:

    mysql-data

already contains an initialized MySQL database, changing an initialization SQL script does not necessarily cause the script to run again.

Therefore testing schema initialization often requires a fresh volume.

Conceptually:

    Existing volume
        |
        v
    MySQL starts
        |
        v
    Existing database retained

Whereas:

    New empty volume
        |
        v
    MySQL initializes database
        |
        v
    /docker-entrypoint-initdb.d scripts execute

---

## 18. MySQL Health Check

The Compose configuration defines a MySQL health check using:

    mysqladmin ping

The health check allows Compose to distinguish:

    Container started

from:

    MySQL actually ready

This distinction is important.

A process can start before the database is ready to accept connections.

---

## 19. depends_on

Application services use:

    depends_on

for infrastructure dependencies.

For example:

    auth-service
        |
        +--> mysql
        +--> eureka-server

The configuration uses conditions such as:

    service_healthy

for MySQL.

This is better than simply saying:

    container started

because the application may require an actually healthy database.

---

## 20. Kafka Container

The current Compose configuration uses:

    image: apache/kafka:4.0.0

Container name:

    worksphere-kafka

Kafka is configured as a combined:

    broker + controller

node.

The configuration uses Kafka's KRaft architecture.

---

## 21. Kafka KRaft Mode

The Kafka configuration contains:

    KAFKA_PROCESS_ROLES: broker,controller

This means the Kafka node performs both roles.

The configuration also defines:

    KAFKA_NODE_ID: 1

and:

    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093

This is a single-node development-oriented Kafka configuration.

---

## 22. Kafka Listeners

The Docker Kafka configuration defines:

    INTERNAL://:9092
    EXTERNAL://:29092
    CONTROLLER://:9093

These listeners serve different purposes.

Conceptually:

    INTERNAL
        |
        v
    Docker containers

    EXTERNAL
        |
        v
    Host machine

    CONTROLLER
        |
        v
    Kafka controller communication

---

## 23. Kafka Internal Address

Inside Docker:

    kafka:9092

is the Kafka broker address used by WorkSphere containers.

For example:

    Employee Service
          |
          v
    kafka:9092

and:

    Payroll Service
          |
          v
    kafka:9092

---

## 24. Kafka External Address

The Compose configuration maps:

    29092:29092

and advertises:

    localhost:29092

for the external listener.

Therefore a Kafka client running directly on the host can use:

    localhost:29092

while a container should use:

    kafka:9092

---

## 25. Why Kafka Has Two Addresses

The same Kafka broker needs to be reachable from different network contexts.

Host:

    localhost:29092

Docker:

    kafka:9092

If a Docker container tries to use:

    localhost:9092

it looks for Kafka inside that same container.

This was an important networking issue encountered while containerizing WorkSphere.

---

## 26. Kafka Environment Configuration

The Employee Service uses:

    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

The Payroll Service also uses:

    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

This allows Spring Boot to pass the Docker Kafka address into the application.

---

## 27. Shared Kafka Configuration

WorkSphere's Kafka producer and consumer configuration reads the Spring property:

    spring.kafka.bootstrap-servers

with a local-development fallback:

    localhost:9092

This is important because the same code can run in different environments.

Conceptually:

Local:

    spring.kafka.bootstrap-servers
        =
    localhost:9092

Docker:

    spring.kafka.bootstrap-servers
        =
    kafka:9092

---

## 28. Why Hardcoded localhost Was a Problem

Originally, Kafka configuration used:

    localhost:9092

directly in the shared producer/consumer configuration.

Inside Docker this caused Kafka connection failures because:

    localhost

referred to the application container rather than Kafka.

The configuration was changed to read:

    spring.kafka.bootstrap-servers

from environment-specific configuration.

---

## 29. Kafka Persistent Volume

Kafka uses:

    kafka-data:/var/lib/kafka/data

This allows Kafka data to persist across container recreation.

Persistent Kafka data can include:

- broker data
- topic data
- offsets
- internal Kafka state

The exact retention behavior is controlled by Kafka configuration.

---

## 30. Kafka Health Check

The Compose file checks Kafka using:

    kafka-topics.sh
    --bootstrap-server localhost:9092
    --list

The check verifies that the broker is responsive enough to execute a topic listing request.

---

## 31. Kafka Default Partition Configuration

The Docker Kafka configuration contains:

    KAFKA_NUM_PARTITIONS: 3

This establishes the broker's default number of partitions for newly created topics when no explicit partition count is supplied.

It should not automatically be interpreted as proof that every existing topic contains exactly three partitions.

Topic-level configuration should be inspected separately when exact partition count matters.

---

## 32. Eureka Container

The Eureka service is built from the shared Dockerfile.

Compose passes:

    SERVICE_NAME: eureka-server

to the Docker build.

The container is named:

    worksphere-eureka

and exposes:

    8761:8761

---

## 33. Eureka Port

Eureka runs on:

    8761

The host can therefore access:

    localhost:8761

The application services use Eureka for service registration and discovery.

---

## 34. Eureka Service Discovery

The architecture is:

    Employee Service
          |
          v
       Eureka
          ^
          |
    Department Service
    Leave Service
    Payroll Service
    Auth Service

Services register themselves with Eureka.

The Gateway can then discover services dynamically.

---

## 35. Auth Service Container

The Auth Service is built using:

    SERVICE_NAME: auth-service

Container:

    worksphere-auth

Host mapping:

    8085:8085

The Auth Service connects to MySQL using:

    mysql:3306

and Eureka using:

    eureka-server:8761

---

## 36. Auth Database Configuration

The Docker configuration uses:

    jdbc:mysql://mysql:3306/auth_db

This is important because the Auth Service runs inside Docker.

The database host is:

    mysql

not:

    localhost

---

## 37. Auth Eureka Configuration

The Docker environment uses:

    http://eureka-server:8761/eureka/

This is another example of Docker service-name communication.

Local development may use:

    http://localhost:8761/eureka/

Docker uses:

    http://eureka-server:8761/eureka/

---

## 38. JWT Secret in Docker

The current Docker Compose file supplies:

    JWT_SECRET

to services that validate or use JWTs.

The current value is a development placeholder.

Production should use:

- environment secret injection
- Docker secrets
- Kubernetes Secrets
- cloud secret manager
- another secure secrets-management solution

The signing secret should not be treated as ordinary source-controlled configuration.

---

## 39. Employee Service Container

Employee Service uses:

    SERVICE_NAME: employee-service

Container:

    worksphere-employee

Host mapping:

    8081:8081

The service connects to:

    mysql:3306

and:

    eureka-server:8761

Kafka:

    kafka:9092

---

## 40. Employee Service and Kafka

Employee Service publishes:

    EmployeeCreatedEvent

to Kafka.

Conceptually:

    Employee Service
          |
          | Employee created
          v
       Kafka
          |
          v
    employee-created

Consumers can then process the event asynchronously.

---

## 41. Department Service Container

Department Service uses:

    SERVICE_NAME: department-service

Container:

    worksphere-department

Host mapping:

    8082:8082

It connects to:

    mysql:3306

and:

    eureka-server:8761

---

## 42. Leave Service Container

Leave Service uses:

    SERVICE_NAME: leave-service

Container:

    worksphere-leave

The Compose configuration uses:

    expose:
      - "8085"

rather than a host port mapping.

This means the port is made available to other containers on the Docker network without publishing it directly to the host.

This is aligned with the eventual internal-service architecture.

---

## 43. Leave Service Dependencies

Leave Service depends on:

    mysql
    eureka-server
    kafka

The database must be healthy.

Eureka must be started.

Kafka must be started.

This reflects Leave Service's current responsibilities:

- persistence
- service discovery
- Kafka employee-created event consumption

---

## 44. Payroll Service Container

Payroll Service uses:

    SERVICE_NAME: payroll-service

Container:

    worksphere-payroll

The Compose configuration uses:

    expose:
      - "8083"

rather than publishing:

    8083:8083

This keeps Payroll internal to the Docker network.

---

## 45. Payroll Service Dependencies

Payroll depends on:

    mysql
    eureka-server
    kafka

It consumes EmployeeCreatedEvent from Kafka.

Conceptually:

    Employee
       |
       v
    Kafka
       |
       +--------> Payroll
       |
       +--------> Leave

---

## 46. Why Leave and Payroll Use expose

There is an architectural difference between:

    ports

and:

    expose

### ports

Publishes a container port to the host.

Example:

    8081:8081

### expose

Documents/makes the port available to other containers without publishing it to the host.

Example:

    expose:
      - "8083"

For internal microservices, expose is closer to the desired final architecture.

---

## 47. Current Port Exposure

Current Compose exposure is approximately:

    MySQL:
        3307 -> 3306

    Kafka:
        29092 -> 29092

    Eureka:
        8761 -> 8761

    Auth:
        8085 -> 8085

    Employee:
        8081 -> 8081

    Department:
        8082 -> 8082

    Leave:
        internal 8085

    Payroll:
        internal 8083

The Gateway is not yet represented in the current Compose file.

---

## 48. Current vs Target Exposure

### Current

Some services are directly reachable from the host:

    Auth
    Employee
    Department

while:

    Leave
    Payroll

are internal.

### Target

The intended production-style architecture is:

    Client
       |
       v
    API Gateway
       |
       v
    Internal Docker Network
       |
       +--> Auth
       +--> Employee
       +--> Department
       +--> Leave
       +--> Payroll

Only the Gateway should expose business APIs externally.

---

## 49. Dockerfile

WorkSphere uses one shared Dockerfile:

    docker/Dockerfile

It is parameterized by:

    SERVICE_NAME

This allows one Dockerfile to build multiple microservices.

For example:

    SERVICE_NAME=employee-service

builds Employee Service.

Similarly:

    SERVICE_NAME=payroll-service

builds Payroll Service.

---

## 50. Why One Dockerfile Is Useful

Without a shared Dockerfile, every service could require:

    employee-service/Dockerfile
    department-service/Dockerfile
    leave-service/Dockerfile
    payroll-service/Dockerfile
    auth-service/Dockerfile
    ...

That creates duplicated build logic.

The shared Dockerfile centralizes the common build process.

---

## 51. Multi-Stage Docker Build

The Dockerfile uses two stages:

    Stage 1:
        builder

    Stage 2:
        runtime

Conceptually:

    Source Code
        |
        v
    Maven Builder Image
        |
        | mvn package
        v
    Executable JAR
        |
        v
    Java Runtime Image
        |
        v
    Final Container

---

## 52. Build Stage

The builder uses:

    maven:3.9-eclipse-temurin-21

This provides:

- Maven
- JDK 21
- build tooling

The working directory is:

    /workspace

---

## 53. Parent POM Copy

The Dockerfile first copies:

    pom.xml

This is the Maven parent POM.

The parent POM defines the multi-module project structure.

---

## 54. Module POM Copy

The Dockerfile copies module POM files before copying source code.

Examples:

    common-library/pom.xml
    employee-service/pom.xml
    department-service/pom.xml
    eureka-server/pom.xml
    api-gateway/pom.xml
    payroll-service/pom.xml
    auth-service/pom.xml
    kafka-module/pom.xml
    leave-service/pom.xml

This ordering helps Docker reuse cached layers when source code changes but dependency definitions remain unchanged.

---

## 55. Docker Layer Caching

Docker builds images layer by layer.

If dependency-related files have not changed, Docker may reuse previous layers.

Therefore:

    Copy POMs
          |
          v
    Resolve/build dependency layers
          |
          v
    Copy source

is generally more cache-friendly than copying the entire repository immediately.

---

## 56. Source Code Copy

After the POM files, the Dockerfile copies module source directories.

This includes:

    common-library/src
    employee-service/src
    department-service/src
    eureka-server/src
    api-gateway/src
    payroll-service/src
    auth-service/src
    kafka-module/src
    leave-service/src

---

## 57. Building One Service

The Dockerfile defines:

    ARG SERVICE_NAME

Then runs:

    mvn -pl ${SERVICE_NAME} -am clean package -DskipTests

The important Maven options are:

    -pl

and:

    -am

---

## 58. Meaning of -pl

The Maven option:

    -pl

means:

    project list

It tells Maven which module should be built.

Example:

    -pl employee-service

means:

    build Employee Service module

---

## 59. Meaning of -am

The Maven option:

    -am

means:

    also make required projects

This is important for WorkSphere because services depend on internal modules such as:

    common-library
    kafka-module

Maven can therefore build the selected service together with required reactor dependencies.

---

## 60. Why Tests Are Skipped During Image Build

The Dockerfile currently uses:

    -DskipTests

This means tests are not executed as part of the image packaging command.

This can make local Docker builds faster.

However, CI/CD should ideally run tests before building the deployable image.

A production pipeline can therefore use:

    Build
      |
      v
    Unit Tests
      |
      v
    Integration Tests
      |
      v
    Docker Image
      |
      v
    Deployment

---

## 61. Runtime Stage

The runtime image uses:

    eclipse-temurin:21-jre

This is smaller in purpose than a full Maven/JDK build environment.

The runtime container only needs to execute the packaged Spring Boot JAR.

---

## 62. Why Multi-Stage Builds Are Useful

The build stage needs:

    Maven
    JDK
    Source code
    Build dependencies

The runtime stage needs mainly:

    Java runtime
    Application JAR

Keeping the build environment out of the final image can reduce image size and attack surface.

---

## 63. Runtime Working Directory

The runtime container uses:

    /app

The executable JAR is copied as:

    app.jar

The container starts:

    java -jar app.jar

---

## 64. Spring Boot Executable JAR

The Spring Boot Maven plugin is configured so services are packaged as executable Spring Boot JARs.

This is important because a plain Maven JAR may not contain the correct executable boot structure.

The Docker runtime expects:

    java -jar app.jar

Therefore the artifact must be a runnable Spring Boot application.

---

## 65. Packaging Problem Encountered

During containerization, some services initially produced JARs that were not executable Spring Boot JARs.

The container then failed with an error equivalent to:

    no main manifest attribute

The solution was to configure Spring Boot's:

    repackage

goal

in the affected service POMs.

After repackaging, the resulting JARs became executable Spring Boot artifacts.

---

## 66. Why This Matters

There are two different concepts:

### Ordinary JAR

Contains compiled Java classes.

### Spring Boot executable JAR

Contains:

- application classes
- dependencies
- Spring Boot loader metadata
- executable manifest information

The second is required for:

    java -jar app.jar

---

## 67. Runtime EXPOSE

The shared Dockerfile contains:

    EXPOSE 8080

This is documentation metadata in the image.

The actual externally published port is controlled by Docker Compose.

For example:

    ports:
      - "8081:8081"

determines host/container port publishing for Employee Service.

Therefore the shared Dockerfile's EXPOSE line should not be interpreted as meaning every service runs on port 8080.

---

## 68. Container Startup

The Dockerfile uses:

    ENTRYPOINT ["java", "-jar", "app.jar"]

Therefore when the container starts:

    Docker
      |
      v
    java -jar app.jar
      |
      v
    Spring Boot application
      |
      v
    Service starts

---

## 69. Environment Variables

Docker Compose supplies environment-specific configuration through environment variables.

Examples:

    SERVER_PORT

    SPRING_DATASOURCE_URL

    SPRING_DATASOURCE_USERNAME

    SPRING_DATASOURCE_PASSWORD

    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE

    SPRING_KAFKA_BOOTSTRAP_SERVERS

    JWT_SECRET

This keeps deployment configuration outside the Java source code.

---

## 70. Spring Boot Environment Variable Mapping

Spring Boot maps environment variables to configuration properties.

For example:

    SPRING_DATASOURCE_URL

maps to:

    spring.datasource.url

Similarly:

    SERVER_PORT

maps to:

    server.port

and:

    SPRING_KAFKA_BOOTSTRAP_SERVERS

maps to:

    spring.kafka.bootstrap-servers

---

## 71. Environment Separation

The same application image can theoretically run in different environments.

For example:

Development:

    DATABASE -> localhost
    KAFKA -> localhost:9092

Docker:

    DATABASE -> mysql:3306
    KAFKA -> kafka:9092

Production:

    DATABASE -> production database endpoint
    KAFKA -> production Kafka endpoint

The application artifact does not need to change simply because the environment changes.

---

## 72. Service Discovery Configuration

Dockerized services use:

    http://eureka-server:8761/eureka/

rather than:

    http://localhost:8761/eureka/

This is another example of replacing host-local networking with Docker DNS.

---

## 73. Database Service Communication

Inside Docker:

    jdbc:mysql://mysql:3306/<database>

is the correct style.

The pattern is:

    jdbc:mysql://
        <docker-service-name>
        :
        <container-port>
        /
        <database>

---

## 74. Kafka Service Communication

Inside Docker:

    kafka:9092

is used.

The pattern is:

    <docker-service-name>:<internal-listener-port>

Therefore:

    kafka:9092

means:

    Kafka service
    +
    Kafka internal listener

---

## 75. Service Name vs Container Name

Docker Compose provides service-level DNS.

For example, the Compose service:

    kafka:

can be reached as:

    kafka

The explicit:

    container_name: worksphere-kafka

is mainly a human-friendly container name.

Application configuration should generally prefer Compose service names for network communication.

---

## 76. Health Checks vs depends_on

A useful distinction:

    depends_on

controls startup ordering/dependency conditions.

A:

    healthcheck

determines whether a service is actually healthy.

For example:

    mysql container starts
        |
        v
    MySQL initialization
        |
        v
    Health check succeeds
        |
        v
    dependent service starts

This is more reliable than assuming process startup equals service readiness.

---

## 77. Kafka and Application Startup

Kafka consumers may start before Kafka is fully ready.

This can produce connection/retry messages during startup.

The important distinction is:

    temporary startup connection issue

versus:

    persistent Kafka connectivity failure

The latter usually indicates a configuration problem such as:

    localhost:9092

being used from inside a container.

---

## 78. Docker Troubleshooting: no main manifest attribute

Symptom:

    no main manifest attribute, in app.jar

Likely cause:

    JAR is not repackaged as a Spring Boot executable JAR.

Check:

    spring-boot-maven-plugin

and:

    repackage

configuration.

Then rebuild the affected image.

---

## 79. Docker Troubleshooting: JWT_SECRET Missing

Symptom:

Application fails during startup because:

    jwt.secret

is missing.

Cause:

JWT configuration expects:

    JWT_SECRET

Solution:

Provide:

    JWT_SECRET

through Docker Compose environment configuration.

---

## 80. Docker Troubleshooting: Database Connection

Symptom:

    Cannot connect to MySQL

Common Docker mistake:

    localhost:3306

inside the application container.

Correct:

    mysql:3306

Also verify:

    MySQL container healthy
    database exists
    credentials correct
    application connected to worksphere-network

---

## 81. Docker Troubleshooting: Kafka Connection

Symptom:

    Timed out waiting for a node assignment

or broker connection failures.

First check:

    bootstrap server

Inside Docker it should normally be:

    kafka:9092

not:

    localhost:9092

---

## 82. Docker Troubleshooting: Eureka Registration

If a service cannot register with Eureka, check:

    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE

Inside Docker it should use:

    http://eureka-server:8761/eureka/

Also verify:

    Eureka container is running
    Eureka is listening on 8761
    service is on worksphere-network

---

## 83. Docker Troubleshooting: Check Running Containers

Useful command:

    docker ps

This shows:

- container name
- image
- status
- ports
- uptime

For WorkSphere:

    docker ps

should show the application/infrastructure containers that are currently running.

---

## 84. Docker Troubleshooting: Container Logs

Use:

    docker logs <container-name>

Examples:

    docker logs worksphere-employee

    docker logs worksphere-payroll

    docker logs worksphere-kafka

Logs are usually the first place to look when a container fails during startup.

---

## 85. Docker Troubleshooting: Follow Logs

Use:

    docker logs -f <container-name>

The:

    -f

option follows new log entries.

This is useful while restarting a service and observing startup behavior.

---

## 86. Docker Troubleshooting: Compose Logs

You can also inspect Compose service logs:

    docker compose -f docker/docker-compose.yml logs

For one service:

    docker compose -f docker/docker-compose.yml logs employee-service

For following logs:

    docker compose -f docker/docker-compose.yml logs -f employee-service

---

## 87. Docker Troubleshooting: Rebuild Image

When source code or configuration changes, rebuild the affected service image.

Example:

    docker compose -f docker/docker-compose.yml build employee-service

For a clean rebuild:

    docker compose -f docker/docker-compose.yml build --no-cache employee-service

---

## 88. Docker Troubleshooting: Recreate Container

Rebuilding an image does not necessarily mean an already-running container has automatically been replaced.

Use:

    docker compose -f docker/docker-compose.yml up -d --force-recreate employee-service

This ensures the container is recreated from the updated image.

---

## 89. Docker Troubleshooting: Verify Recent Logs

After recreating a service, inspect logs.

For example:

    docker logs worksphere-employee --since 5m

This can help verify that old configuration errors are no longer appearing.

---

## 90. Docker Troubleshooting: Inspect Network

Use:

    docker network ls

to list Docker networks.

Then:

    docker network inspect docker_worksphere-network

or the actual Compose-generated network name.

This allows you to inspect:

- connected containers
- container IP addresses
- network configuration

---

## 91. Why Network Inspection Is Useful

If Employee Service cannot reach Kafka, network inspection can answer:

    Are both containers on the same network?

For example:

    Employee
       |
       +--> worksphere-network
       |
       +--> Kafka
              |
              +--> worksphere-network

If they are not on the same network, service-name communication will fail.

---

## 92. Dockerized Employee -> Kafka -> Payroll Flow

The WorkSphere event flow is:

    Client
       |
       v
    Employee Service
       |
       | Employee created
       v
    Kafka
       |
       | employee-created
       +----------------+
       |                |
       v                v
    Payroll           Leave
    Consumer          Consumer
       |
       v
    Payroll Record

This is an important example of Docker + Kafka + microservice integration.

---

## 93. End-to-End Docker Verification

The Employee -> Kafka -> Payroll flow was verified using a newly created employee.

The flow was:

    Employee created
        |
        v
    Employee Service
        |
        v
    Kafka employee-created
        |
        v
    Payroll Consumer
        |
        v
    Payroll Database
        |
        v
    GET /api/payroll/employee/{employeeId}

The resulting payroll record confirmed that event-driven communication was working inside Docker.

---

## 94. Why Earlier Events May Not Appear

If an employee was created before the Kafka Docker configuration was corrected, the event may have been published using the incorrect broker address.

Therefore an older employee can exist without a corresponding payroll record.

A newly created employee after Kafka networking is corrected provides a cleaner end-to-end verification.

This illustrates why event-driven systems require attention to event timing and consumer availability.

---

## 95. Container Isolation

Each service runs in its own container.

For example:

    worksphere-employee

contains Employee Service.

    worksphere-payroll

contains Payroll Service.

    worksphere-leave

contains Leave Service.

This provides process isolation while allowing controlled network communication.

---

## 96. Container Lifecycle

A container can be:

    created
    started
    stopped
    restarted
    removed
    recreated

The application should therefore not assume that:

    container identity = application state

Persistent state should be stored in:

- database
- persistent volumes
- external storage

rather than relying on the container filesystem.

---

## 97. Immutable Image Concept

A useful production principle is:

    Build image once
          |
          v
    Run same image
          |
          v
    Configure through environment

Instead of modifying files manually inside a running container.

This makes deployments reproducible.

---

## 98. Why Manual Container Modification Is Not Preferred

Suppose someone enters:

    docker exec -it worksphere-employee sh

and manually changes application files.

Those changes disappear when the container is recreated.

The correct workflow is:

    Change source
       |
       v
    Build image
       |
       v
    Recreate container

---

## 99. Configuration vs Image

A good container architecture separates:

    Application code
        |
        v
    Docker image

from:

    Environment configuration
        |
        v
    Docker Compose / environment variables

For example:

    employee-service image

can be reused with:

    development configuration
    Docker configuration
    test configuration
    production configuration

---

## 100. Security: Current State

The current Compose setup already has some internalization:

    Leave Service -> expose
    Payroll Service -> expose

while other services still have host port mappings.

The final target is stronger:

    Gateway -> externally reachable

    Auth/Employee/Department/Leave/Payroll
        -> internal network only

---

## 101. Why Direct Service Exposure Is Risky

Suppose Employee Service is exposed directly:

    localhost:8081

A client can potentially bypass:

    API Gateway

and directly access Employee Service.

That weakens centralized:

    authentication
    authorization
    rate limiting
    routing
    observability

Therefore production architecture should prevent direct external access to internal services.

---

## 102. Target Gateway-Only Architecture

The intended final architecture is:

    External Client
          |
          v
    +----------------+
    |  API Gateway   |
    |     :8080      |
    +-------+--------+
            |
            v
    +--------------------------+
    |   Internal Docker Net   |
    |                          |
    | Auth                     |
    | Employee                 |
    | Department               |
    | Leave                    |
    | Payroll                  |
    | Eureka                   |
    | Kafka                    |
    | MySQL                    |
    +--------------------------+

Only the Gateway should be published for application API traffic.

---

## 103. Docker Network Security

Network isolation provides an additional security boundary.

Application authorization:

    JWT
      |
      v
    RBAC

Network security:

    Docker internal network
      |
      v
    Prevent direct external access

These controls solve different problems.

---

## 104. Docker and JWT Security

Docker does not replace JWT security.

Docker answers:

    Can this network connection reach the service?

JWT/RBAC answers:

    Is this authenticated user allowed to perform this operation?

A secure architecture uses both.

---

## 105. Docker and Kafka Security

Kafka currently uses:

    PLAINTEXT

listeners in the development Compose configuration.

This is acceptable for a local portfolio environment.

Production Kafka deployments should consider:

- TLS
- SASL
- authentication
- authorization
- network restrictions
- encrypted credentials
- secure listener configuration

---

## 106. Docker and Database Security

The current local configuration uses:

    root

for MySQL credentials.

For production:

- use a dedicated application database user
- use a strong password
- avoid root for application access
- store credentials securely
- restrict database network access
- apply least privilege

---

## 107. Docker Image Security

Production Docker images should be scanned for:

- vulnerable OS packages
- vulnerable Java dependencies
- outdated base images
- unnecessary packages
- leaked secrets

A CI/CD pipeline can include image scanning before deployment.

---

## 108. Runtime User

The current Dockerfile does not explicitly create a non-root runtime user.

For production hardening, a non-root user can reduce the impact of container compromise.

Conceptually:

    root container
        |
        v
    greater privileges

versus:

    non-root application user
        |
        v
    reduced privileges

This is a future hardening improvement.

---

## 109. Image Size Optimization

The multi-stage build already separates Maven build dependencies from the runtime image.

Further optimizations could include:

- smaller runtime base image
- dependency cleanup
- JVM tuning
- layered Spring Boot images
- build cache optimization

These are optimization opportunities rather than requirements for the current portfolio implementation.

---

## 110. Docker Compose and Production

Docker Compose is excellent for:

- local development
- integration testing
- demonstrations
- portfolio projects
- small environments

Large production environments may use:

- Kubernetes
- ECS
- Nomad
- another container orchestration platform

The Docker concepts learned here remain directly applicable.

---

## 111. Docker Compose vs Kubernetes

Compose:

    Multiple containers
    + Local orchestration
    + Simple configuration

Kubernetes:

    Containers
    + Scheduling
    + Scaling
    + Service discovery
    + Self-healing
    + Rolling deployment
    + Secrets/configuration
    + Load balancing

WorkSphere currently uses Docker Compose as the container orchestration mechanism.

---

## 112. Scaling a Stateless Service

Suppose Employee Service is stateless.

Conceptually:

    Employee-1
    Employee-2
    Employee-3

A load balancer can distribute requests.

Persistent state belongs in:

    MySQL

rather than inside the Employee containers.

This is one of the key benefits of containerized microservices.

---

## 113. Kafka Consumer Scaling

Kafka consumers can scale using consumer groups.

For example:

    Payroll Consumer 1
    Payroll Consumer 2
    Payroll Consumer 3

within:

    worksphere-payroll-group

Kafka can distribute partitions among consumers.

The exact scaling behavior depends on the topic partition count and consumer group configuration.

---

## 114. Docker + Kafka + Consumer Groups

The architecture becomes:

    Employee
       |
       v
    Kafka Topic
       |
       +---- Partition 0
       +---- Partition 1
       +---- Partition 2
              |
              v
       Payroll Consumer Group

This connects containerization with event-driven scalability.

---

## 115. Container Restart and Idempotency

Containers can restart.

Kafka messages can be retried.

Consumers can restart before or after processing.

Therefore event consumers should be designed to tolerate duplicate delivery.

WorkSphere Payroll contains duplicate-protection logic for payroll creation.

This is an important reliability principle:

    Container restart
        +
    Event retry
        +
    Idempotent consumer
        =
    Safer event processing

---

## 116. Docker Dependency Graph

The current environment can be represented as:

    MySQL
      |
      +----> Auth
      +----> Employee
      +----> Department
      +----> Leave
      +----> Payroll

    Eureka
      |
      +----> Auth
      +----> Employee
      +----> Department
      +----> Leave
      +----> Payroll

    Kafka
      |
      +----> Employee producer
      +----> Leave consumer
      +----> Payroll consumer

The Gateway belongs above these services as the intended external entry point.

---

## 117. Startup Architecture

A simplified startup sequence is:

    1. Docker network created
    2. MySQL starts
    3. Kafka starts
    4. Eureka starts
    5. Auth starts
    6. Employee starts
    7. Department starts
    8. Leave starts
    9. Payroll starts
    10. Gateway starts

Actual container startup timing can overlap depending on Compose dependency conditions and application readiness.

---

## 118. Important Distinction: Started vs Ready

A container being:

    running

does not guarantee that the application is:

    ready

For example:

    MySQL container running
        !=
    MySQL ready for connections

This is why health checks and retry mechanisms are useful.

---

## 119. Docker Observability

Containerized applications should expose logs and health information.

Useful operational information includes:

- startup status
- database connection status
- Kafka connection status
- Eureka registration
- request failures
- consumer failures
- retry attempts
- DLT events

These topics will be covered further in the Observability documentation.

---

## 120. Docker Logging Principle

Application logs should be written to standard output/error so Docker can collect them.

Then:

    docker logs

can display the application logs.

Production environments can forward those logs to centralized systems.

---

## 121. Current Implementation Status

### Implemented

- Shared Dockerfile
- Multi-stage Maven build
- Java 21 builder
- Java 21 JRE runtime
- Parameterized SERVICE_NAME
- Maven multi-module build
- Spring Boot executable JAR packaging
- Docker Compose
- MySQL container
- Kafka container
- Eureka container
- Auth container
- Employee container
- Department container
- Leave container
- Payroll container
- Docker bridge network
- MySQL persistent volume
- Kafka persistent volume
- MySQL health check
- Kafka health check
- Docker service-name networking
- Docker-specific Kafka bootstrap configuration
- Environment variable configuration
- Internal Leave/Payroll exposure
- Employee -> Kafka -> Payroll Docker verification

---

## 122. Current Implementation Status: Remaining Hardening

### Not yet fully completed

- API Gateway Compose service
- Gateway-only external API exposure
- Removal of unnecessary host port mappings
- Full internal-only service exposure
- Environment-specific Gateway -> Auth configuration
- Production secrets management
- Non-root runtime user
- Production TLS/security configuration
- Production Kafka authentication/encryption
- Production database credentials
- Container image vulnerability scanning
- Resource/CPU/memory limits
- Production orchestration

These belong to future hardening rather than the core Docker implementation already completed.

---

## 123. Docker Security Target

The final target should be:

    Public Network
          |
          v
    +----------------+
    | API Gateway    |
    |     :8080      |
    +-------+--------+
            |
            v
    +--------------------------+
    | Docker Internal Network  |
    +--------------------------+
       |       |       |
       v       v       v
     Auth   Employee  Department
       |       |       |
       +-------+-------+
               |
          +----+----+
          |         |
          v         v
        Leave     Payroll
          |
          v
        Kafka
          |
          v
        MySQL

---

## 124. Interview Explanation

### "How did you dockerize your microservices?"

Answer:

"I created a shared multi-stage Dockerfile for the Maven multi-module project. The build stage uses Maven with Java 21 and builds the selected service using -pl and -am, so required internal modules are built as well. The runtime stage uses a Java 21 JRE image and runs the Spring Boot executable JAR. Docker Compose then orchestrates MySQL, Kafka, Eureka and the individual microservices on a dedicated bridge network. Environment variables are used for service-specific configuration such as database URLs, Eureka endpoints and Kafka bootstrap servers."

---

## 125. Interview Explanation: Docker Networking

### "How do your containers communicate?"

Answer:

"They communicate through a dedicated Docker bridge network. Instead of using localhost, containers use Docker service names such as mysql, kafka and eureka-server. For example, the Employee and Payroll services use kafka:9092 for Kafka communication. Host-based clients use the externally advertised Kafka listener localhost:29092."

---

## 126. Interview Explanation: localhost Problem

### "Why can't you use localhost inside Docker?"

Answer:

"Because localhost inside a container refers to that container itself. When Employee Service needs Kafka, localhost:9092 would point to Employee Service's own container. Docker service discovery allows the Employee container to resolve kafka to the Kafka container, so kafka:9092 is used instead."

---

## 127. Interview Explanation: Multi-Stage Build

### "Why use a multi-stage Dockerfile?"

Answer:

"The build stage needs Maven and the JDK to compile and package the application, but the running application only needs a Java runtime and the packaged JAR. Separating those stages keeps build tooling out of the final runtime image and can reduce the final image footprint and attack surface."

---

## 128. Interview Explanation: -pl and -am

### "What are -pl and -am in Maven?"

Answer:

"-pl selects the Maven module I want to build, while -am means also make the required reactor modules. In WorkSphere this allows me to build a service such as payroll-service together with internal dependencies such as common-library or kafka-module."

---

## 129. Interview Explanation: Health Checks

### "Why did you add health checks?"

Answer:

"A running container does not necessarily mean the application or infrastructure is ready. The MySQL and Kafka health checks allow Compose to determine whether those dependencies are actually responding before dependent services are started."

---

## 130. Interview Explanation: Docker Volumes

### "Why do you use volumes?"

Answer:

"I use persistent volumes for stateful infrastructure such as MySQL and Kafka. Containers are replaceable, but database and messaging state should survive container recreation. Therefore persistent data is stored outside the container's writable layer."

---

## 131. Interview Explanation: Docker + Kafka

### "How did you handle Kafka in Docker?"

Answer:

"I configured separate internal and external Kafka listeners. Containers use kafka:9092, while host-based clients use localhost:29092. I also changed the shared Kafka producer and consumer configuration to read spring.kafka.bootstrap-servers so the same code can work with different environments."

---

## 132. Interview Explanation: Why Configuration Is Externalized

### "Why use environment variables?"

Answer:

"Environment variables allow the same application image to run in different environments. Database URLs, Eureka endpoints, Kafka bootstrap servers and JWT secrets can change without modifying the application artifact."

---

## 133. Interview Explanation: Security

### "How do you prevent users from bypassing the Gateway?"

Answer:

"The target architecture is to expose only the API Gateway externally and keep business services on the internal Docker network. Gateway RBAC then handles user-level authorization, while Docker network isolation prevents direct external access to internal services."

---

## 134. Interview Explanation: Current Limitation

If asked whether Gateway-only exposure is already fully completed:

Answer:

"The Docker foundation is implemented, including the internal network and internal exposure for Leave and Payroll. The final Gateway-only exposure is a hardening step: the Gateway needs to be included in Compose and unnecessary host port mappings for internal services need to be removed."

This is more accurate than claiming the final security boundary is already complete.

---

## 135. Docker Commands

Start the environment:

    docker compose -f docker/docker-compose.yml up -d

Build and start:

    docker compose -f docker/docker-compose.yml up -d --build

Stop:

    docker compose -f docker/docker-compose.yml down

View containers:

    docker ps

View logs:

    docker logs <container-name>

Follow logs:

    docker logs -f <container-name>

View Compose logs:

    docker compose -f docker/docker-compose.yml logs

Restart a service:

    docker compose -f docker/docker-compose.yml restart <service>

Rebuild a service:

    docker compose -f docker/docker-compose.yml build <service>

Recreate a service:

    docker compose -f docker/docker-compose.yml up -d --force-recreate <service>

Inspect networks:

    docker network ls

Inspect a network:

    docker network inspect <network>

---

## 136. Fresh Database Test

When testing MySQL initialization from scratch, a typical workflow is:

    1. Stop Compose
    2. Remove the development MySQL volume
    3. Start MySQL again
    4. Wait for health check
    5. Verify databases
    6. Verify tables
    7. Start dependent services

Care must be taken because removing the volume deletes persisted local database state.

---

## 137. Docker Development Workflow

The recommended development loop is:

    Modify Code
        |
        v
    Run Tests
        |
        v
    Build Maven Module
        |
        v
    Build Docker Image
        |
        v
    Recreate Container
        |
        v
    Check Logs
        |
        v
    Test API
        |
        v
    Verify Integration

---

## 138. Docker Deployment Workflow

A simplified deployment pipeline can be:

    Git Commit
        |
        v
    CI Build
        |
        v
    Unit Tests
        |
        v
    Package JAR
        |
        v
    Build Docker Image
        |
        v
    Security Scan
        |
        v
    Push Image
        |
        v
    Deploy
        |
        v
    Health Checks
        |
        v
    Observability

---

## 139. Important Production Improvements

Before calling the Docker environment production-ready, consider:

- non-root containers
- secure secrets
- TLS
- secure Kafka authentication
- database least privilege
- resource limits
- health/readiness endpoints
- centralized logging
- metrics
- tracing
- image scanning
- vulnerability management
- network policies
- Gateway-only exposure
- service-level authentication/authorization
- automated deployment

---

## 140. Final Architecture

The intended WorkSphere container architecture is:

    +----------------------------------------------------------+
    |                     External Clients                     |
    +-----------------------------+----------------------------+
                                  |
                                  v
                         +----------------+
                         |  API Gateway   |
                         |      :8080     |
                         +-------+--------+
                                 |
                                 v
    +----------------------------------------------------------------+
    |                    Docker Internal Network                     |
    |                                                                |
    |  +------------+       +------------+                           |
    |  |   Eureka   |       |    Auth    |                           |
    |  |    :8761   |       |    :8085   |                           |
    |  +------------+       +------------+                           |
    |                                                                |
    |  +------------+       +------------+       +------------+      |
    |  |  Employee  |       | Department |       |    Leave   |      |
    |  |    :8081   |       |    :8082   |       |    :8085   |      |
    |  +------------+       +------------+       +------------+      |
    |                                                                |
    |  +------------+                                                |
    |  |  Payroll   |                                                |
    |  |    :8083   |                                                |
    |  +------------+                                                |
    |                                                                |
    |  +------------+       +------------+                           |
    |  |   Kafka    |       |   MySQL    |                           |
    |  |   :9092    |       |    :3306   |                           |
    |  +------------+       +------------+                           |
    |                                                                |
    +----------------------------------------------------------------+

Kafka event flow:

    Employee Service
          |
          | employee-created
          v
        Kafka
          |
          +---------------> Payroll
          |
          +---------------> Leave

Database flow:

    Auth       -> auth_db
    Employee   -> worksphere_employee
    Department -> worksphere_department
    Leave      -> worksphere_leave
    Payroll    -> worksphere_payroll

---

## 141. Final Summary

Docker gives WorkSphere a reproducible microservice runtime environment.

The main concepts implemented are:

    Multi-stage Docker build
          |
          v
    Java 21 Spring Boot executable JAR
          |
          v
    Docker Compose
          |
          v
    Dedicated Docker network
          |
          +--> MySQL
          +--> Kafka
          +--> Eureka
          +--> Auth
          +--> Employee
          +--> Department
          +--> Leave
          +--> Payroll

The most important networking lesson is:

    Host -> localhost:<published-port>

while:

    Container -> <service-name>:<container-port>

For Kafka specifically:

    Host:
        localhost:29092

    Docker:
        kafka:9092

The most important architectural security target is:

    External Client
          |
          v
    API Gateway
          |
          v
    Internal Docker Network
          |
          +--> Internal Microservices

The current Docker implementation establishes the containerization foundation. The remaining Gateway-only exposure and production hardening will be completed as part of the network-security phase.

The Dockerized Employee -> Kafka -> Payroll flow has also been verified end-to-end, demonstrating that the containerized microservices, Kafka networking, event consumption and persistence work together as one distributed system.