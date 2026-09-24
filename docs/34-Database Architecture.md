# 34 — Database Architecture

## 1. Overview

WorkSphere follows a microservice-oriented database architecture.

Each major business service owns its own database.

The current databases are:

    auth_db
    worksphere_employee
    worksphere_department
    worksphere_leave
    worksphere_payroll

The main principle is:

    One Service
        |
        v
    Owns Its Data
        |
        v
    Own Database

This provides logical data isolation between microservices.

---

## 2. Why Database-per-Service

In a microservice architecture, services should ideally own the data required for their business responsibility.

For WorkSphere:

    Auth Service
        -> auth_db

    Employee Service
        -> worksphere_employee

    Department Service
        -> worksphere_department

    Leave Service
        -> worksphere_leave

    Payroll Service
        -> worksphere_payroll

This prevents one service from directly depending on another service's database tables.

---

## 3. Database Ownership

Database ownership means:

    Employee Service
        owns employee data

    Department Service
        owns department data

    Leave Service
        owns leave data

    Payroll Service
        owns payroll data

    Auth Service
        owns authentication and authorization data

A service should normally access another service's business data through its API or an event rather than directly querying its database.

---

## 4. High-Level Database Architecture

    +-------------------+
    |    Auth Service   |
    +---------+---------+
              |
              v
           auth_db


    +-------------------+
    | Employee Service  |
    +---------+---------+
              |
              v
    worksphere_employee


    +-------------------+
    | Department Service|
    +---------+---------+
              |
              v
    worksphere_department


    +-------------------+
    |  Leave Service    |
    +---------+---------+
              |
              v
    worksphere_leave


    +-------------------+
    |  Payroll Service  |
    +---------+---------+
              |
              v
    worksphere_payroll

---

## 5. Shared MySQL Server vs Shared Database

There is an important distinction.

The WorkSphere Docker environment currently uses one MySQL server/container:

    worksphere-mysql

but multiple logical databases:

    auth_db
    worksphere_employee
    worksphere_department
    worksphere_leave
    worksphere_payroll

So the architecture is:

    One MySQL instance
          |
          +--> auth_db
          +--> worksphere_employee
          +--> worksphere_department
          +--> worksphere_leave
          +--> worksphere_payroll

This provides logical database separation while keeping local development infrastructure simple.

---

## 6. Why This Is Useful for a Portfolio Project

Running one MySQL container is easier for local development than running five separate MySQL containers.

At the same time, separate databases demonstrate the important microservice principle of:

    data ownership

This provides a practical compromise:

    Shared infrastructure
        +
    Separate logical databases

---

## 7. Auth Database

Auth Service uses:

    auth_db

Its database contains authentication/authorization-related data.

Conceptually:

    auth_db
       |
       +--> app_users
       +--> roles
       +--> permissions
       +--> user_roles
       +--> role_permissions
       +--> resources
       +--> related authorization tables

The exact tables depend on the current entity model and initialization state.

---

## 8. Employee Database

Employee Service uses:

    worksphere_employee

This database owns employee-related information.

Conceptually:

    worksphere_employee
          |
          +--> employees
          +--> employee-related tables

Employee Service should be the owner of this information.

Other services should not directly modify employee tables.

---

## 9. Department Database

Department Service uses:

    worksphere_department

This database owns department-related information.

Conceptually:

    worksphere_department
          |
          +--> departments
          +--> department-related data

Employee Service can obtain department information through the Department Service rather than directly querying this database.

---

## 10. Leave Database

Leave Service uses:

    worksphere_leave

The Leave domain contains multiple related entities.

The important tables are:

    leave_types
    leave_balances
    leave_requests

These tables belong to Leave Service.

---

## 11. Payroll Database

Payroll Service uses:

    worksphere_payroll

Payroll owns payroll-related data.

Conceptually:

    worksphere_payroll
          |
          +--> payroll records
          +--> payroll-related data

Employee creation can result in a payroll record through Kafka without Payroll directly querying the Employee database.

---

## 12. Database Connection Pattern

A service connects to its own database.

For example:

    Employee Service
          |
          v
    worksphere_employee

    Payroll Service
          |
          v
    worksphere_payroll

    Leave Service
          |
          v
    worksphere_leave

---

## 13. Local Database Configuration

The application YAML files currently use localhost-style database URLs for local execution.

For example:

    jdbc:mysql://localhost:3306/worksphere_employee

The Employee Service configuration also uses:

    username: root
    password: root

The same pattern exists for the other local service configurations.

---

## 14. Docker Database Configuration

Docker Compose overrides the datasource URL.

For Employee Service:

    jdbc:mysql://mysql:3306/worksphere_employee

For Auth Service:

    jdbc:mysql://mysql:3306/auth_db

The important difference is:

    Local:
        localhost:3306

    Docker:
        mysql:3306

---

## 15. Why Docker Uses mysql

Inside Docker:

    localhost

means the current application container.

The MySQL service is another container.

Therefore:

    mysql

is the Docker DNS/service name of the MySQL container.

The application connects using:

    mysql:3306

---

## 16. Database Port Mapping

The MySQL container uses:

    3306

internally.

Compose maps:

    3307:3306

Therefore:

    Host
        |
        | localhost:3307
        v
    MySQL Container
        |
        | 3306
        v
    MySQL Server

Applications running inside Docker use:

    mysql:3306

---

## 17. Database-per-Service Principle

The desired ownership model is:

    Auth Service
        |
        +--> auth_db

    Employee Service
        |
        +--> worksphere_employee

    Department Service
        |
        +--> worksphere_department

    Leave Service
        |
        +--> worksphere_leave

    Payroll Service
        |
        +--> worksphere_payroll

This reduces coupling between services.

---

## 18. Why Services Should Not Share Tables

Suppose Payroll directly queries:

    worksphere_employee.employees

Now Payroll depends on:

- Employee table structure
- Employee column names
- Employee schema changes
- Employee database availability

If Employee changes its schema, Payroll may break.

Instead:

    Payroll
       |
       v
    Employee API / Kafka Event
       |
       v
    Required employee information

This keeps service boundaries clearer.

---

## 19. API-Based Data Access

If one service needs current data owned by another service, it can use:

    REST
    OpenFeign
    another service API

For example:

    Leave Service
         |
         | EmployeeFeignClient
         v
    Employee Service

Leave Service can validate that an employee exists without directly querying:

    worksphere_employee

---

## 20. Event-Based Data Flow

Some information can be propagated asynchronously.

For example:

    Employee created
          |
          v
    Employee Service
          |
          v
    Kafka
          |
          +----------> Payroll
          |
          +----------> Leave

Payroll can create payroll data from the event.

Leave can initialize employee leave balances from the same event.

This avoids direct database coupling.

---

## 21. Employee Database Ownership

Employee Service owns:

    employee identity
    employee name
    employee email
    salary
    department relationship
    other employee-specific fields

Other services should treat this as externally owned data.

---

## 22. Payroll Data Ownership

Payroll Service owns:

    employeeId
    basicSalary
    bonus
    tax
    netSalary

Payroll does not need to own the entire Employee entity.

Instead it stores the employee identifier required for payroll processing.

---

## 23. Why Payroll Stores employeeId

Payroll records need to identify which employee they belong to.

Therefore Payroll stores:

    employeeId

But the Employee entity remains owned by Employee Service.

This is an example of a reference across microservice boundaries.

The important distinction is:

    employeeId
        !=
    foreign key into another service's database

It is an application-level reference.

---

## 24. Avoiding Cross-Database Foreign Keys

A microservice database should generally avoid foreign keys that directly reference another service's database.

For example, Payroll should not require:

    payroll.employee_id
        FK
    worksphere_employee.employees.id

because that creates database-level coupling.

Instead:

    payroll.employee_id
        |
        v
    Employee Service identity

The relationship is managed at the application/event level.

---

## 25. Leave and Employee Relationship

Leave Service needs employee information.

The current implementation validates employees using an Employee Service Feign client.

Conceptually:

    Leave Service
          |
          | GET employee
          v
    Employee Service
          |
          v
    worksphere_employee

Leave does not need direct database access to Employee's database.

---

## 26. Leave Database Model

The Leave database contains three important concepts:

    Leave Type
    Leave Balance
    Leave Request

The relationships are primarily within the Leave Service boundary.

---

## 27. Leave Types

Leave Type represents the definition of a leave category.

Examples could include:

    CL
    SL
    PL

depending on configured business data.

A Leave Type contains concepts such as:

    code
    name
    annualAllocation
    description
    active

---

## 28. Leave Type Constraints

Leave Type uses a unique code.

Conceptually:

    leave_types
        |
        +--> code UNIQUE

This prevents duplicate leave type codes.

Database constraints are valuable because application validation alone is not always sufficient.

---

## 29. Leave Balance

Leave Balance represents an employee's available leave for a particular leave type and year.

Conceptually:

    Employee
       |
       +--> Leave Type
                |
                +--> Year
                      |
                      +--> allocatedDays
                      +--> usedDays
                      +--> remainingDays

---

## 30. Leave Balance Uniqueness

The Leave Balance entity uses a unique constraint across:

    employee_id
    leave_type_id
    year

Therefore an employee should have at most one balance record for a particular:

    employee
    +
    leave type
    +
    year

This provides database-level protection against duplicate annual balances.

---

## 31. Why the Unique Constraint Matters

Suppose two application requests attempt to initialize the same employee's balance simultaneously.

Application logic might check:

    Does balance exist?

Both requests could theoretically see:

    No

and both attempt to insert.

A database unique constraint provides a second layer of protection.

Therefore:

    Application validation
        +
    Database constraint

is safer than application validation alone.

---

## 32. Leave Request

Leave Request represents an employee's actual leave application.

Conceptually:

    leave_requests
        |
        +--> employee_id
        +--> leave_type_id
        +--> start_date
        +--> end_date
        +--> reason
        +--> status
        +--> approver_id
        +--> rejection_reason
        +--> timestamps

---

## 33. Leave Request Status

The Leave domain supports statuses such as:

    APPLIED
    APPROVED
    REJECTED
    CANCELLED

The status represents the lifecycle of a leave request.

---

## 34. Leave Request Indexes

The Leave Request table contains indexes around frequently queried data such as:

    employee_id
    status
    start_date
    end_date

Indexes can improve query performance for common operations such as:

    Get employee leave history
    Find requests by status
    Check date overlap

---

## 35. Why Indexes Matter

Without an appropriate index, the database may need to scan many rows.

For example:

    Find all leave requests for employee 100

can become expensive as the table grows.

An index on:

    employee_id

can make this lookup significantly more efficient.

---

## 36. Composite Query Considerations

Leave overlap queries can involve:

    employee_id
    start_date
    end_date
    status

The exact optimal index strategy depends on:

- data volume
- query patterns
- database execution plans
- cardinality

Indexes should therefore be based on actual workload rather than added blindly.

---

## 37. JPA Entity Mapping

WorkSphere uses JPA/Hibernate for persistence.

The general flow is:

    Controller
        |
        v
    Service
        |
        v
    Repository
        |
        v
    JPA Entity
        |
        v
    Hibernate
        |
        v
    MySQL

---

## 38. Repository Layer

Repositories abstract database access.

Typical pattern:

    Entity
       |
       v
    JpaRepository
       |
       v
    MySQL

This avoids writing low-level JDBC code for ordinary CRUD operations.

---

## 39. Entity-to-Table Mapping

A JPA entity such as:

    LeaveRequestEntity

maps to:

    leave_requests

Similarly:

    LeaveBalanceEntity
        ->
    leave_balances

and:

    LeaveTypeEntity
        ->
    leave_types

The entity defines the object representation while Hibernate maps it to relational structures.

---

## 40. Primary Keys

Each major entity uses an identifier.

Conceptually:

    leave_requests
        id

    leave_balances
        id

    leave_types
        id

The primary key uniquely identifies a database row.

---

## 41. Surrogate IDs

The services generally use generated numeric identifiers rather than business fields as the primary key.

For example:

    id = 101

can identify a leave request.

The employee ID remains a business relationship/reference rather than being used as the Leave Request primary key.

---

## 42. Unique Constraints vs Primary Keys

A primary key answers:

    Which row is this?

A unique constraint answers:

    Can two rows have the same value combination?

Example:

    leave_balances.id

is the primary key.

While:

    employee_id + leave_type_id + year

is protected by a unique constraint.

---

## 43. Foreign Keys Inside a Service

Foreign keys are useful when the related entities belong to the same service/database.

For example:

    leave_balances
          |
          v
    leave_types

can be modeled within the Leave database.

This maintains relational integrity inside one bounded context.

---

## 44. Cross-Service References

For relationships crossing microservice boundaries, prefer:

    API calls
    events
    application-level identifiers

rather than direct database foreign keys.

Example:

    payroll.employee_id

can identify an employee owned by Employee Service.

But Payroll should not create a database foreign key into:

    worksphere_employee

---

## 45. Lazy Relationships

Leave Request uses a lazy relationship for Leave Type.

Conceptually:

    LeaveRequest
         |
         | LAZY
         v
    LeaveType

Lazy loading means the related entity is not necessarily loaded immediately when the parent entity is retrieved.

---

## 46. Why Lazy Loading Helps

Suppose a request only needs:

    leaveRequest.id
    leaveRequest.status

There may be no reason to immediately load the complete Leave Type object.

Lazy loading can reduce unnecessary database work.

However, it must be handled carefully around transaction/session boundaries.

---

## 47. Eager Relationships in Auth

Auth's user-role relationship currently uses:

    FetchType.EAGER

This means roles are loaded with the user entity.

This is useful for the current authentication flow because authentication needs role information to construct authorities and JWT claims.

However, eager loading should be used deliberately because large relationship graphs can increase query cost.

---

## 48. JPA ddl-auto

The current application configurations use:

    ddl-auto: update

for the services.

This is convenient during development because Hibernate can update the schema based on entity changes.

---

## 49. Why ddl-auto: update Is Convenient

During active development:

    Entity changes
        |
        v
    Application starts
        |
        v
    Hibernate updates schema

This reduces the need to manually change tables for every small entity modification.

---

## 50. Why ddl-auto: update Is Not Ideal for Production

Automatically modifying production schemas can be risky.

Potential problems include:

- unexpected schema changes
- difficult rollback
- insufficient migration review
- differences between environments
- limited control over destructive changes

Production systems generally benefit from explicit database migration tooling.

Examples:

    Flyway
    Liquibase

---

## 51. Target Production Database Strategy

A stronger production workflow is:

    Entity Change
        |
        v
    Migration Script
        |
        v
    Code Review
        |
        v
    CI/CD
        |
        v
    Database Migration
        |
        v
    Application Deployment

The application can then use:

    ddl-auto: validate

instead of automatically changing the schema.

---

## 52. ddl-auto: validate

With:

    ddl-auto: validate

Hibernate checks whether the existing database schema matches the expected entity mapping.

It does not automatically modify the schema.

This makes schema drift easier to detect.

---

## 53. Database Initialization

Docker Compose mounts SQL initialization files into:

    /docker-entrypoint-initdb.d

The host-side location is:

    docker/mysql/init

The purpose is to initialize the MySQL environment when the database is initialized.

---

## 54. Initialization vs Migration

Initialization and migration are different concepts.

### Initialization

Creates a fresh database environment.

Example:

    New MySQL volume
        |
        v
    Create databases/tables/data

### Migration

Changes an already existing database from:

    Version N

to:

    Version N+1

For production systems, migrations should be versioned and repeatable.

---

## 55. MySQL Docker Volume

The MySQL container uses:

    mysql-data

mounted to:

    /var/lib/mysql

This preserves database state across normal container recreation.

---

## 56. Database Persistence Flow

    MySQL Container
          |
          v
    /var/lib/mysql
          |
          v
    mysql-data volume

Therefore:

    Remove container
        !=
    Automatically remove database volume

unless the volume itself is explicitly removed.

---

## 57. Warning About Volume Removal

Removing:

    mysql-data

can destroy the local persisted database.

Therefore commands that remove Docker volumes should be treated as destructive operations.

For example, a full reset may intentionally require:

    docker compose down -v

but this should only be used when database data can be discarded.

---

## 58. Database Initialization in Development

A fresh environment can be useful when verifying:

- schema completeness
- initialization scripts
- constraints
- indexes
- default data
- service startup

A useful validation process is:

    Remove development DB volume
        |
        v
    Start MySQL
        |
        v
    Execute initialization
        |
        v
    Verify databases
        |
        v
    Verify tables
        |
        v
    Start services

---

## 59. Database Names

Current logical databases are:

    auth_db
    worksphere_employee
    worksphere_department
    worksphere_leave
    worksphere_payroll

These names clearly identify service ownership.

---

## 60. Database Isolation in Docker

All databases currently run through:

    worksphere-mysql

but application services connect only to their intended database.

For example:

    Employee
       |
       v
    worksphere_employee

Payroll:

    Payroll
       |
       v
    worksphere_payroll

Leave:

    Leave
       |
       v
    worksphere_leave

---

## 61. Why Logical Isolation Is Important

Even when one MySQL instance is used, separate databases help establish ownership boundaries.

This reduces accidental access such as:

    Payroll -> Employee tables

and makes future migration to independently managed database instances easier.

---

## 62. Scaling Database Infrastructure

As the system grows, each database can theoretically be moved to independent infrastructure.

For example:

    Employee Service
        |
        v
    Employee MySQL instance

    Payroll Service
        |
        v
    Payroll MySQL instance

The application-level ownership model remains the same.

The infrastructure can evolve independently.

---

## 63. Independent Database Scaling

Different domains can have different workloads.

For example:

    Employee data
        -> moderate read/write workload

    Payroll
        -> potentially periodic high-volume processing

    Leave
        -> seasonal or business-cycle workload

Independent databases can eventually allow different scaling and operational strategies.

---

## 64. Database Availability

If Employee database becomes unavailable:

    Employee Service
        |
        X
    Employee DB

Other services may still remain available depending on whether they require Employee Service synchronously.

This is one advantage of service/data isolation.

However, synchronous dependencies such as Feign calls can still propagate failures.

---

## 65. Database Failure vs Service Failure

These are different failure scenarios.

Database failure:

    Service
       |
       X
    Database

Service failure:

    Consumer
       |
       X
    Service

Kafka can reduce some synchronous coupling by allowing event-driven processing.

---

## 66. Transactions

A database transaction provides atomicity within a database boundary.

For example, Leave approval can involve:

    Update Leave Request
          +
    Update Leave Balance

These changes should be treated as one logical transaction inside Leave Service.

---

## 67. Transaction Boundary

The important principle is:

    One business transaction
        |
        v
    One service/database boundary

For example:

    Leave Service
       |
       +--> leave_requests
       |
       +--> leave_balances

can participate in the same local transaction.

---

## 68. Cross-Service Transactions

Suppose:

    Employee Service
        |
        v
    Kafka
        |
        v
    Payroll Service

This is not one distributed database transaction.

Instead:

    Employee transaction
        |
        v
    Event publication
        |
        v
    Payroll transaction

The system uses asynchronous event-driven consistency.

---

## 69. Eventual Consistency

When Employee creation publishes:

    employee-created

Payroll may process it shortly afterward.

Therefore:

    Employee created
        |
        | small delay
        v
    Payroll created

The system does not require both databases to be updated at exactly the same instant.

This is eventual consistency.

---

## 70. Why Eventual Consistency Is Useful

It allows services to remain independently deployable and reduces distributed transaction coupling.

Instead of:

    Employee
       |
       | synchronous transaction
       v
    Payroll DB

the architecture uses:

    Employee
       |
       v
    Kafka
       |
       v
    Payroll

This improves decoupling.

---

## 71. Consistency Trade-Off

Microservices trade some immediate consistency for:

    independence
    scalability
    resilience
    loose coupling

The design must therefore explicitly handle:

- retries
- duplicate events
- failures
- ordering
- eventual consistency

WorkSphere addresses these topics through Kafka retry/DLT and idempotency mechanisms.

---

## 72. Database and Kafka Relationship

Kafka is not a replacement for MySQL.

Kafka provides:

    event transport

MySQL provides:

    persistent relational state

For example:

    Employee DB
         |
         v
    EmployeeCreatedEvent
         |
         v
       Kafka
         |
         v
    Payroll DB

Kafka moves the event.

Payroll persists the resulting business state.

---

## 73. Database and REST Relationship

REST is also not a replacement for the database.

REST provides:

    service-to-service API communication

The target service reads/writes its own database.

Example:

    Leave
      |
      | REST/Feign
      v
    Employee
      |
      v
    Employee DB

---

## 74. Database Access Layer

A typical WorkSphere persistence flow is:

    HTTP Request
         |
         v
    Controller
         |
         v
    Service
         |
         v
    Repository
         |
         v
    Hibernate/JPA
         |
         v
    MySQL

This maintains separation of concerns.

---

## 75. Why Controllers Should Not Query MySQL Directly

The controller should handle:

- HTTP request
- validation
- response

Business logic belongs in the service layer.

Persistence belongs in repositories.

Therefore:

    Controller
        |
        v
    Service
        |
        v
    Repository
        |
        v
    Database

is preferred.

---

## 76. Repository Responsibility

Repository classes should focus on persistence operations.

Examples:

    findById
    save
    delete
    custom query methods

They should not contain business workflows such as:

    approve leave
    calculate payroll
    assign permissions

Those belong in service/business layers.

---

## 77. Service Responsibility

The service layer coordinates:

- validation
- business rules
- repository operations
- API calls
- events
- transactions

For example, Leave approval can coordinate:

    Validate request
        |
        v
    Validate balance
        |
        v
    Deduct balance
        |
        v
    Update request status

---

## 78. Database Constraints as a Second Defense

Application code should validate business rules.

The database should also protect important invariants where practical.

Examples:

    UNIQUE username
    UNIQUE role name
    UNIQUE leave type code
    UNIQUE employee + leave type + year

This gives:

    Application validation
        +
    Database integrity

---

## 79. Why Application Validation Alone Is Not Enough

Two concurrent requests can pass the same validation.

Example:

    Request A:
        "Does this username exist?"
        -> No

    Request B:
        "Does this username exist?"
        -> No

Both attempt:

    INSERT username

A database unique constraint ensures only one succeeds.

This is why database constraints remain important even with strong service-layer validation.

---

## 80. Database Indexing Strategy

Indexes should support actual query patterns.

Common WorkSphere access patterns include:

    employee_id
    status
    dates
    username
    leave type code

Indexes should be reviewed as data volume grows.

Too few indexes:

    slow reads

Too many indexes:

    higher write/storage cost

---

## 81. Query Performance

For a production database, performance should be evaluated using:

- query execution plans
- slow query logs
- row counts
- index usage
- response latency
- database CPU
- I/O
- connection pool usage

Do not assume that adding an index always improves performance.

---

## 82. Connection Pooling

Spring Boot applications generally use a database connection pool.

The pool keeps reusable database connections available rather than creating a new connection for every request.

Conceptually:

    Requests
       |
       v
    Connection Pool
       |
       +--> DB Connection 1
       +--> DB Connection 2
       +--> DB Connection 3
       +--> DB Connection N
       |
       v
    MySQL

---

## 83. Why Connection Pools Matter

Creating database connections repeatedly can be expensive.

A pool reduces connection setup overhead.

However, pool size should be configured based on:

- application concurrency
- database capacity
- query duration
- number of service instances

---

## 84. Database Security

The current development configuration uses:

    root
    root

for local Docker database access.

This is convenient for development but should not be used as the production application credential strategy.

---

## 85. Production Database Credentials

Production should use:

    dedicated application user

rather than:

    root

The application user should receive only the permissions it requires.

This follows:

    Principle of Least Privilege

---

## 86. Secrets Management

Database passwords should not be hardcoded in production source configuration.

Better options include:

    environment secrets
    Docker secrets
    Kubernetes Secrets
    cloud secret managers

The same principle applies to:

    JWT_SECRET

and other sensitive configuration.

---

## 87. Database Encryption

Production systems may require:

    encryption in transit

using TLS between:

    Application
        |
        v
    MySQL

Depending on the security requirements, encryption at rest may also be required for database storage.

---

## 88. Database Backups

Production databases should have:

- regular backups
- backup verification
- retention policies
- recovery procedures
- disaster recovery plans

A backup is useful only if restoration has also been tested.

---

## 89. Database High Availability

For production workloads, database availability can be improved using mechanisms such as:

- replication
- managed database services
- failover
- clustering
- automated backups

The exact strategy depends on infrastructure requirements.

---

## 90. Database Migration Tooling

A future WorkSphere improvement is to introduce:

    Flyway

or:

    Liquibase

Migration files can then be versioned.

Example:

    V1__initial_schema.sql
    V2__add_employee_status.sql
    V3__add_leave_index.sql

This gives explicit schema evolution.

---

## 91. Recommended Production JPA Configuration

A production-oriented strategy would be approximately:

    spring.jpa.hibernate.ddl-auto=validate

and schema changes handled by:

    Flyway/Liquibase

This separates:

    schema evolution

from:

    application startup

---

## 92. Database Testing

Database behavior should be tested at multiple levels.

### Unit tests

Test business logic without requiring a real database where appropriate.

### Integration tests

Test:

    Repository
        |
        v
    Database

### End-to-end tests

Test:

    API
      |
      v
    Service
      |
      v
    Database

---

## 93. Test Database Isolation

Automated tests should avoid depending on a developer's personal local database state.

Possible approaches include:

- dedicated test database
- Testcontainers
- disposable containers
- migration-based test setup

This makes tests reproducible.

---

## 94. Testcontainers

A future improvement for WorkSphere testing is:

    Test
       |
       v
    Testcontainers
       |
       v
    MySQL Container
       |
       v
    Repository Integration Test

This gives a real database environment without requiring developers to manually maintain test schemas.

---

## 95. Database Initialization Testing

The initialization scripts should be tested against a clean environment.

A useful validation process is:

    Fresh MySQL volume
        |
        v
    Initialization scripts
        |
        v
    Verify databases
        |
        v
    Verify tables
        |
        v
    Verify constraints
        |
        v
    Verify indexes
        |
        v
    Start services

---

## 96. Schema Drift

Schema drift occurs when:

    Application expects Schema A

but:

    Database contains Schema B

For example:

    Entity has new column

but:

    Database does not contain it.

Using:

    ddl-auto: validate

and versioned migrations helps detect/prevent this problem.

---

## 97. Database Ownership and Deployment

A service deployment should not unexpectedly modify another service's database.

For example:

    Deploy Employee Service
        |
        v
    Employee database changes

should not require:

    Payroll database changes

unless there is an explicitly coordinated contract/migration.

This supports independent service deployment.

---

## 98. Database Ownership and Team Boundaries

In a larger organization:

    Employee Team
        -> Employee DB

    Payroll Team
        -> Payroll DB

    Leave Team
        -> Leave DB

Each team can evolve its own persistence model within agreed service contracts.

This is one of the organizational benefits of microservices.

---

## 99. Database Architecture and Service Boundaries

The WorkSphere boundaries are:

    Authentication
        |
        v
    Auth Service
        |
        v
    auth_db


    Employee Management
        |
        v
    Employee Service
        |
        v
    worksphere_employee


    Department Management
        |
        v
    Department Service
        |
        v
    worksphere_department


    Leave Management
        |
        v
    Leave Service
        |
        v
    worksphere_leave


    Payroll Management
        |
        v
    Payroll Service
        |
        v
    worksphere_payroll

---

## 100. Current Implementation Status

### Implemented

- MySQL persistence
- Separate logical database per major service
- Auth database
- Employee database
- Department database
- Leave database
- Payroll database
- JPA/Hibernate
- Repository-based persistence
- Primary keys
- Unique constraints
- Leave balance uniqueness
- Leave indexes
- Docker MySQL
- Persistent MySQL volume
- Database initialization mount
- Docker service-name database connectivity
- Local vs Docker datasource configuration
- Transactional Leave business operations
- Cross-service employee validation through service API
- Event-driven Payroll/Leave integration

---

## 101. Current Development Configuration

The current services use:

    ddl-auto: update

for development.

Database credentials are currently development-oriented.

The Docker environment uses:

    mysql:3306

for service-to-database communication.

These settings should be hardened before production deployment.

---

## 102. Current vs Production Database Strategy

### Current development strategy

    One MySQL container
        |
        +--> Multiple logical databases
        |
        +--> ddl-auto: update
        |
        +--> Development credentials

### Production-oriented strategy

    Managed/HA database infrastructure
        |
        +--> Service-owned databases
        |
        +--> Versioned migrations
        |
        +--> ddl-auto: validate
        |
        +--> Dedicated DB users
        |
        +--> Secure secrets
        |
        +--> Backups
        |
        +--> Monitoring

---

## 103. Important Interview Concept

### "Does database-per-service mean every service needs a separate MySQL server?"

No.

Database-per-service is primarily an ownership and isolation principle.

You can have:

    One MySQL server
        |
        +--> Database A
        +--> Database B
        +--> Database C

during development.

Infrastructure can later evolve to:

    Service A -> DB Server A
    Service B -> DB Server B
    Service C -> DB Server C

without changing the fundamental ownership model.

---

## 104. Interview Explanation: Why Database-per-Service?

Answer:

"I use separate logical databases for the major WorkSphere microservices. This keeps data ownership within service boundaries and prevents services from directly coupling themselves to another service's tables. When one service needs data owned by another service, it uses APIs such as OpenFeign or asynchronous Kafka events. This gives us better service independence and makes future independent scaling or database migration easier."

---

## 105. Interview Explanation: Why Not One Shared Database?

Answer:

"A shared database would create tight coupling between services because each service could directly depend on another service's tables and schema. A schema change in one service could therefore affect another service. With database-per-service, each service owns its persistence model and exposes data through APIs or events."

---

## 106. Interview Explanation: Why Does Payroll Store employeeId?

Answer:

"Payroll needs to associate a payroll record with an employee, so it stores the employeeId as an application-level reference. It does not create a database foreign key into the Employee Service database. Employee data remains owned by Employee Service."

---

## 107. Interview Explanation: How Does Leave Validate Employees?

Answer:

"Leave Service uses the Employee Service API through OpenFeign to validate employee information. This avoids direct access to the Employee Service database and keeps database ownership within the Employee Service."

---

## 108. Interview Explanation: Why Kafka Instead of Direct Database Access?

Answer:

"When Employee Service creates an employee, it publishes an employee-created event. Payroll and Leave consume that event and perform their own local processing. This is preferable to directly writing into Payroll or Leave databases because it preserves service ownership and reduces coupling."

---

## 109. Interview Explanation: Why Use Database Constraints?

Answer:

"I use application-level validation together with database constraints. For example, Leave Balance has a uniqueness constraint on employee, leave type and year. This protects the invariant even if concurrent requests pass application-level existence checks."

---

## 110. Interview Explanation: Why ddl-auto validate in Production?

Answer:

"I would prefer explicit schema migrations in production, using a tool such as Flyway or Liquibase, and configure Hibernate with ddl-auto validate. That allows schema changes to be reviewed, versioned and deployed explicitly rather than allowing Hibernate to modify production tables automatically."

---

## 111. Final Database Architecture

    +-----------------------------------------------------------+
    |                    WorkSphere Services                    |
    +-----------------------------------------------------------+
         |          |           |          |           |
         v          v           v          v           v
       Auth     Employee    Department   Leave      Payroll
         |          |           |          |           |
         v          v           v          v           v
      auth_db   employee_db  dept_db   leave_db   payroll_db
         |          |           |          |           |
         +----------+-----------+----------+-----------+
                              |
                              v
                         MySQL Server
                         (Development)

Cross-service communication:

    Leave
      |
      | OpenFeign
      v
    Employee

    Employee
      |
      | employee-created
      v
    Kafka
      |
      +---------> Payroll
      |
      +---------> Leave

Database ownership:

    Auth      -> auth_db
    Employee  -> worksphere_employee
    Department-> worksphere_department
    Leave     -> worksphere_leave
    Payroll   -> worksphere_payroll

---

## 112. Final Principles

The main database architecture principles in WorkSphere are:

    1. Each major service owns its data.

    2. Services should not directly query another service's database.

    3. Cross-service data access happens through APIs or events.

    4. IDs can be used as application-level references across services.

    5. Database foreign keys should remain within a service's database boundary.

    6. Application validation should be backed by database constraints.

    7. Indexes should support actual query patterns.

    8. Transactions should remain within appropriate service/database boundaries.

    9. Kafka provides event transport, not relational persistence.

    10. MySQL provides persistent relational state.

    11. Docker volumes provide persistence for the development database.

    12. Production should use explicit schema migrations.

    13. Production credentials should be securely managed.

    14. Database backups and recovery should be part of production operations.

    15. Database architecture should preserve service independence.

---

## 113. Summary

WorkSphere uses a database-per-service logical architecture.

The current development environment uses one MySQL container with multiple logical databases:

    auth_db
    worksphere_employee
    worksphere_department
    worksphere_leave
    worksphere_payroll

This provides clear data ownership while keeping local infrastructure simple.

The architecture separates:

    Service Logic
        |
        v
    Service-Owned Database

and prevents:

    Service A
        |
        X
    Direct access to Service B's database

Instead, services communicate through:

    REST / OpenFeign

or:

    Kafka Events

This design supports:

    loose coupling
    independent deployment
    independent scaling
    clearer ownership
    better maintainability
    eventual consistency
    future database infrastructure separation

The current implementation is development-oriented with Hibernate schema updates and local credentials. A production version should introduce versioned migrations, validation-only Hibernate schema handling, dedicated database users, secure secrets, backups, monitoring and appropriate high-availability strategies.