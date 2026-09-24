# 30 — Leave Management

## 1. Overview

WorkSphere includes a dedicated Leave Management microservice responsible for managing employee leave requests, leave types, leave balances, approvals, rejections, and cancellations.

The Leave Service is designed as an independent Spring Boot microservice and communicates with other services through:

- REST/OpenFeign for employee validation
- Kafka for employee-created events
- MySQL for persistent leave data
- Eureka for service discovery
- API Gateway for external access and authorization

The service provides a complete leave-request lifecycle:

Employee → Apply Leave → Approval / Rejection → Cancellation

The implementation also validates:

- Employee existence
- Leave type existence
- Leave type active status
- Date validity
- Leave balance
- Overlapping leave requests
- Valid leave state transitions

---

## 2. Why Leave Management Is a Separate Microservice

Leave management has its own business rules and data lifecycle.

Keeping it as a separate service provides:

- Independent deployment
- Independent database ownership
- Clear business boundaries
- Easier scaling
- Easier testing
- Reduced coupling between employee and leave functionality
- Better separation of responsibilities

The Employee Service owns employee information.

The Leave Service owns:

- Leave types
- Leave balances
- Leave requests
- Leave approval state
- Leave-related business rules

The Leave Service does not directly own employee data.

Instead, it validates employees through the Employee Service.

---

## 3. Leave Service Responsibilities

The Leave Service currently handles:

1. Applying for leave
2. Retrieving a leave request
3. Retrieving employee leave history
4. Approving leave
5. Rejecting leave
6. Cancelling leave
7. Maintaining yearly leave balances
8. Validating employee existence
9. Validating leave types
10. Validating leave dates
11. Preventing overlapping leave
12. Validating available balance
13. Restoring balance after cancellation of approved leave
14. Initializing balances when an employee is created
15. Kafka retry processing
16. Kafka dead-letter handling

---

## 4. Leave Management Domain Model

The main entities are:

- LeaveTypeEntity
- LeaveBalanceEntity
- LeaveRequestEntity

The relationship can be represented as:

Leave Type
↓
Leave Balance
↓
Employee

and:

Employee
↓
Leave Request
↓
Leave Type

---

## 5. Leave Type

A leave type represents a category of leave available to employees.

Examples could include:

- CASUAL
- SICK
- EARNED
- ANNUAL

The exact available leave types depend on the data initialized in the application database.

The LeaveType entity contains:

- id
- code
- name
- annualAllocation
- description
- active

The leave type code is unique.

The active flag determines whether employees can currently apply for that leave type.

---

## 6. Leave Type Database Protection

The Leave Type entity defines a unique constraint for the leave type code.

Conceptually:

leave_types
|
+-- id
+-- code          UNIQUE
+-- name
+-- annual_allocation
+-- description
+-- active

This prevents duplicate leave-type codes at the database level.

Application validation also checks whether a leave type is active before allowing a leave request.

---

## 7. Leave Balance

Each employee receives a yearly balance for each active leave type.

A balance contains:

- employeeId
- leaveType
- allocatedDays
- usedDays
- remainingDays
- year

Example:

Employee 101

CASUAL
Allocated = 12
Used = 3
Remaining = 9

SICK
Allocated = 10
Used = 2
Remaining = 8

---

## 8. Leave Balance Uniqueness

The Leave Balance entity has a database-level unique constraint across:

employee_id
leave_type_id
year

Therefore, an employee can have only one balance record for a particular leave type in a particular year.

Conceptually:

(employee_id, leave_type_id, year) → UNIQUE

This prevents duplicate yearly balance records.

---

## 9. Leave Balance Initialization

When an employee is created, the Leave Service can receive an EmployeeCreatedEvent through Kafka.

The Leave Service then:

1. Reads the employee ID
2. Determines the current year
3. Retrieves all active leave types
4. Checks whether a balance already exists
5. Creates the balance if it does not exist
6. Sets allocated days from the leave type
7. Sets used days to zero
8. Sets remaining days equal to allocated days

Conceptually:

Employee Created
↓
Kafka employee-created
↓
Leave Service Consumer
↓
Find active leave types
↓
Check existing balances
↓
Create missing yearly balances

The initialization logic is idempotent at the application level because existing balances are skipped.

---

## 10. Leave Request

A leave request represents an employee's request to take leave.

The Leave Request entity contains:

- id
- employeeId
- leaveType
- startDate
- endDate
- numberOfDays
- reason
- status
- appliedAt
- approvedAt
- rejectedAt
- cancelledAt
- approverId
- rejectionReason
- createdAt
- updatedAt

The database table is:

leave_requests

---

## 11. Leave Request Indexes

The Leave Request entity defines indexes for commonly queried fields.

Indexes exist for:

- employee_id
- status
- start_date and end_date

These indexes support common operations such as:

- Finding an employee's leave history
- Filtering by leave status
- Checking date ranges for overlapping leave

The date index is particularly relevant to leave-overlap queries.

---

## 12. Leave Status Lifecycle

The current Leave Service uses the following states:

APPLIED
APPROVED
REJECTED
CANCELLED

A typical lifecycle is:

Employee
↓
APPLIED
├──→ APPROVED
│       ↓
│   CANCELLED
│
├──→ REJECTED
│
└──→ CANCELLED

The service validates the current state before performing approval, rejection, or cancellation.

---

## 13. Applying for Leave

The main endpoint for applying for leave is:

POST /api/v1/leaves

The request is validated before the leave request is persisted.

The application flow is:

Client
↓
LeaveController
↓
LeaveService
↓
Validate dates
↓
Validate employee
↓
Validate leave type
↓
Calculate number of days
↓
Check overlapping leave
↓
Check leave balance
↓
Create LeaveRequestEntity
↓
Save to database
↓
Return response

---

## 14. Date Validation

The service validates the requested date range.

The following rules are currently implemented:

### Rule 1 — Start date cannot be after end date

Invalid:

startDate = 2026-10-20
endDate   = 2026-10-15

This request is rejected.

### Rule 2 — Past leave cannot be applied

The start date cannot be before the current date.

Therefore, the service prevents applying for leave that has already started in the past.

---

## 15. Leave Day Calculation

Leave days are calculated using:

ChronoUnit.DAYS.between(startDate, endDate) + 1

The calculation is inclusive of both start and end dates.

Example:

Start Date = 2026-10-10
End Date   = 2026-10-12

Number of Days:

12 - 10 + 1 = 3

Therefore:

numberOfDays = 3

---

## 16. Employee Validation

The Leave Service does not maintain the employee master data.

Instead, it uses an EmployeeFeignClient to communicate with the Employee Service.

Conceptually:

Leave Service
|
| REST through OpenFeign
↓
Employee Service
|
↓
Employee exists?

If the employee cannot be retrieved, the Leave Service throws:

EmployeeNotFoundException

This keeps employee ownership inside Employee Service.

---

## 17. OpenFeign Communication

The Leave Service uses OpenFeign for synchronous employee validation.

This provides a declarative HTTP client instead of manually creating HTTP requests.

The business flow remains:

Leave Service
↓
EmployeeFeignClient
↓
Employee Service
↓
Employee response

This is an example of synchronous service-to-service communication.

---

## 18. Leave Type Validation

When applying leave, the service retrieves the requested leave type from the database.

If the leave type does not exist:

LeaveTypeNotFoundException

is raised.

The service also verifies that the leave type is active.

If the leave type is inactive, the request is rejected.

This prevents employees from applying for disabled leave categories.

---

## 19. Leave Balance Validation

Before creating a leave request, the service checks the employee's balance for:

- employee
- leave type
- current year

The service retrieves the corresponding LeaveBalanceEntity.

If no balance exists, the request fails.

If:

remainingDays < requestedDays

the request is rejected with:

InsufficientLeaveBalanceException

This prevents employees from requesting more leave than their available balance.

---

## 20. Important Balance Behavior

The current implementation checks the available balance during leave application.

However, the actual deduction happens when the leave is approved.

Therefore:

APPLY
↓
Balance validation
↓
Leave remains APPLIED
↓
APPROVE
↓
Balance is deducted

This distinction is important.

Applying for leave does not immediately reduce usedDays.

---

## 21. Preventing Overlapping Leave

The service checks whether the employee already has an overlapping leave request.

Only requests with these statuses participate in the overlap check:

- APPLIED
- APPROVED

Rejected and cancelled requests do not block a new leave request.

Conceptually:

Existing Leave
start = 2026-10-10
end   = 2026-10-15
status = APPROVED

New Leave
start = 2026-10-13
end   = 2026-10-14

The dates overlap.

Therefore the new request is rejected.

---

## 22. Why Overlap Validation Is Important

Without overlap validation, an employee could create multiple active leave requests covering the same dates.

For example:

Request 1
10 Oct → 15 Oct

Request 2
12 Oct → 14 Oct

Request 3
13 Oct → 16 Oct

All three could potentially consume the same working days.

The overlap rule prevents this inconsistency.

---

## 23. Creating the Leave Request

After validation succeeds, the service maps the request DTO to a LeaveRequestEntity.

The service sets:

- Leave Type
- Number of Days
- Status = APPLIED
- Applied timestamp
- Created timestamp
- Updated timestamp

The entity is then persisted using LeaveRequestRepository.

The created leave request is returned as LeaveResponseDto.

---

## 24. Retrieving a Leave Request

Endpoint:

GET /api/v1/leaves/{leaveId}

The service retrieves the leave request by ID.

If the request does not exist:

LeaveNotFoundException

is thrown.

Otherwise, the entity is mapped to LeaveResponseDto.

---

## 25. Retrieving Employee Leave History

Endpoint:

GET /api/v1/leaves/employee/{employeeId}

The service:

1. Validates that the employee exists
2. Retrieves the employee's leave requests
3. Orders them by createdAt descending
4. Maps entities to response DTOs

This provides the employee's leave history with the newest records first.

---

## 26. Approving Leave

Endpoint:

PUT /api/v1/leaves/{leaveId}/approve?approverId={approverId}

Approval is allowed only when the current status is:

APPLIED

If the leave is already:

- APPROVED
- REJECTED
- CANCELLED

the service throws InvalidLeaveStateException.

---

## 27. Approval Processing

When a leave is approved:

1. Retrieve the leave request
2. Validate its current state
3. Find the employee's yearly balance
4. Verify sufficient remaining days
5. Increase usedDays
6. Decrease remainingDays
7. Set status to APPROVED
8. Store approverId
9. Store approvedAt
10. Update timestamps
11. Save the balance
12. Save the leave request

Conceptually:

APPLIED
↓
Check Balance
↓
usedDays += leaveDays
remainingDays -= leaveDays
↓
APPROVED

---

## 28. Approval and Transaction Management

LeaveServiceImpl is annotated with:

@Transactional

This means the business operation executes within a database transaction.

Approval updates two related pieces of state:

Leave Request
↓
status = APPROVED

Leave Balance
↓
usedDays increased
remainingDays decreased

Keeping these operations within the same transaction helps prevent a partial database update if an exception occurs during the operation.

---

## 29. Rejecting Leave

Endpoint:

PUT /api/v1/leaves/{leaveId}/reject

The rejection request contains:

- approverId
- rejectionReason

Rejection is allowed only from:

APPLIED

The service then sets:

status = REJECTED
approverId = supplied approver
rejectionReason = supplied reason
rejectedAt = current timestamp

No balance deduction is performed for a rejected leave request.

---

## 30. Cancelling Leave

Endpoint:

PUT /api/v1/leaves/{leaveId}/cancel

Cancellation is allowed when the leave is:

APPLIED

or:

APPROVED

If the leave is already:

REJECTED
CANCELLED

the cancellation is rejected because the state transition is invalid.

---

## 31. Cancelling an Applied Leave

For an APPLIED leave:

APPLIED
↓
CANCELLED

No balance was deducted yet, so no balance restoration is required.

---

## 32. Cancelling an Approved Leave

For an APPROVED leave:

APPROVED
↓
CANCELLED

The service restores the previously consumed balance.

For example:

Before cancellation:

Allocated = 12
Used = 5
Remaining = 7

If a 2-day approved leave is cancelled:

Used = 3
Remaining = 9

The cancellation therefore reverses the balance impact of the approved leave.

---

## 33. Leave Exceptions

The Leave Service defines domain-specific exceptions including:

- EmployeeNotFoundException
- InsufficientLeaveBalanceException
- InvalidLeaveStateException
- LeaveNotFoundException
- LeaveOverlapException
- LeaveTypeNotFoundException

These exceptions represent specific business failures instead of exposing low-level persistence or HTTP errors directly to the business layer.

---

## 34. DTO Layer

The Leave Service uses DTOs to separate API contracts from persistence entities.

Important DTOs include:

- LeaveRequestDto
- LeaveResponseDto
- LeaveRejectRequestDto
- LeaveBalanceResponseDto
- LeaveTypeResponseDto

This avoids exposing JPA entities directly through controller APIs.

---

## 35. Mapper Layer

LeaveMapper is responsible for converting between:

DTO
↕
Entity

This keeps mapping logic separate from the service layer.

The service therefore focuses on business rules rather than manually constructing every response object.

---

## 36. Repository Layer

The Leave Service contains repositories for:

- LeaveRequestRepository
- LeaveBalanceRepository
- LeaveTypeRepository

The repositories abstract database access using Spring Data JPA.

The service layer performs business validation and delegates persistence operations to these repositories.

---

## 37. Leave Request Repository

LeaveRequestRepository supports operations such as:

- Finding a leave by ID
- Finding leaves for an employee
- Detecting overlapping leave

The employee history query orders requests by:

createdAt DESC

This means the latest created leave request is returned first.

---

## 38. Leave Balance Repository

LeaveBalanceRepository supports:

- Finding balances for an employee and year
- Finding a specific employee/leave-type/year balance
- Checking whether a balance already exists

This repository is used by both:

- Leave application
- Leave approval
- Leave cancellation
- Employee balance initialization

---

## 39. Leave Type Repository

LeaveTypeRepository provides access to leave-type definitions.

The balance initialization process specifically retrieves active leave types.

Conceptually:

findByActiveTrue()

This means inactive leave types are not automatically assigned to newly created employees.

---

## 40. Kafka Integration

Leave Service also participates in WorkSphere's event-driven architecture.

It listens to:

employee-created

The event is published when an employee is created.

The Leave Service consumes this event independently from Payroll Service.

---

## 41. Employee Created Event Flow

The event-driven flow is:

Employee Service
↓
Employee Created
↓
Kafka topic: employee-created
↓
├──────────────→ Payroll Service
│
└──────────────→ Leave Service

The same event can therefore trigger multiple independent business processes.

---

## 42. Leave Kafka Consumer Group

Leave Service uses the consumer group:

worksphere-leave-group

Payroll Service uses a different consumer group:

worksphere-payroll-group

Because the consumer groups are different, both services receive their own copy of the employee-created event.

This is Kafka fan-out through independent consumer groups.

---

## 43. Leave Event Processing

The Leave Service consumer receives:

EmployeeCreatedEvent

It extracts:

employeeId

and calls:

LeaveBalanceService.initializeEmployeeBalances(employeeId)

The service then creates missing yearly balances for all active leave types.

The consumer logs successful processing.

---

## 44. Why Kafka Is Used for Leave Initialization

Without Kafka, Employee Service would need direct synchronous calls to Leave Service every time an employee is created.

With Kafka:

Employee Service
↓
Publish EmployeeCreatedEvent
↓
Kafka
↓
Leave Service

This reduces direct coupling.

Employee Service does not need to know the internal implementation of Leave Service.

---

## 45. Retry Configuration

The Leave Kafka consumer uses:

@RetryableTopic

with:

attempts = 4

and exponential backoff configured as:

initial delay = 2000 ms
multiplier = 2.0

Conceptually, failed processing can be retried with increasing delays.

The purpose is to handle transient failures without immediately losing the event.

---

## 46. Dead Letter Topic

The Leave consumer also defines:

@DltHandler

If event processing cannot successfully complete after the configured retry processing, the event can be routed to a dead-letter topic according to Spring Kafka's retry-topic mechanism.

The DLT handler logs the employee ID associated with the failed event.

The current implementation provides DLT handling, but it does not implement a separate automated business recovery workflow after the DLT handler.

---

## 47. Leave Balance Initialization Idempotency

Balance initialization checks whether a balance already exists before creating one.

The lookup uses:

employeeId
leaveTypeId
year

If a matching balance already exists:

continue

No duplicate balance is created.

This is important because Kafka processing is not inherently exactly-once at the business level.

The application therefore protects the initialization operation against repeated employee-created events.

---

## 48. Database-Level Balance Protection

The application-level existence check is complemented by a database-level unique constraint:

employee_id
leave_type_id
year

This provides another layer of protection against duplicate balance records.

The design therefore uses:

Application validation
+
Database constraint

This is stronger than relying only on application logic.

---

## 49. Leave Service and Employee Service Relationship

The relationship between the two services is intentionally different for two use cases.

### Synchronous validation

Leave Service
↓
OpenFeign
↓
Employee Service

Used when:

- Applying leave
- Fetching employee leave history

### Asynchronous employee creation event

Employee Service
↓
Kafka
↓
Leave Service

Used for:

- Initializing leave balances after employee creation

This demonstrates both synchronous and asynchronous microservice communication patterns in WorkSphere.

---

## 50. Leave Management API Summary

Current controller endpoints:

POST /api/v1/leaves

Creates a new leave request.

GET /api/v1/leaves/{leaveId}

Retrieves a leave request.

GET /api/v1/leaves/employee/{employeeId}

Retrieves employee leave history.

PUT /api/v1/leaves/{leaveId}/approve?approverId={approverId}

Approves an applied leave.

PUT /api/v1/leaves/{leaveId}/reject

Rejects an applied leave.

PUT /api/v1/leaves/{leaveId}/cancel

Cancels an applied or approved leave.

---

## 51. Example Leave Lifecycle

Example employee:

employeeId = 101

Suppose:

CASUAL leave
Allocated = 12
Used = 0
Remaining = 12

Employee requests:

Start = 2026-10-10
End = 2026-10-12

Number of days:

3

After application:

Leave status = APPLIED

Balance:

Allocated = 12
Used = 0
Remaining = 12

After approval:

Leave status = APPROVED

Balance:

Allocated = 12
Used = 3
Remaining = 9

If the approved leave is later cancelled:

Leave status = CANCELLED

Balance:

Allocated = 12
Used = 0
Remaining = 12

---

## 52. Example Rejection Flow

Initial state:

APPLIED

Manager rejects the request.

The service stores:

status = REJECTED
approverId = manager ID
rejectionReason = supplied reason
rejectedAt = current timestamp

The balance remains unchanged.

---

## 53. Example Overlap Scenario

Existing request:

Employee = 101
Start = 2026-11-10
End = 2026-11-12
Status = APPROVED

New request:

Employee = 101
Start = 2026-11-11
End = 2026-11-13

The requested range overlaps the existing approved request.

The new request is rejected with:

LeaveOverlapException

---

## 54. Example Insufficient Balance Scenario

Suppose:

Allocated = 10
Used = 8
Remaining = 2

Employee requests:

5 days

The validation detects:

remainingDays < requestedDays

Therefore:

5 > 2

The request is rejected with:

InsufficientLeaveBalanceException

---

## 55. Leave Service Architecture

The internal structure follows a layered architecture:

Controller
↓
Service
↓
Repository
↓
MySQL

Additional integrations:

Service
├──→ EmployeeFeignClient → Employee Service
│
└──→ LeaveMapper

Kafka:

Kafka
↓
EmployeeEventConsumer
↓
LeaveBalanceService
↓
LeaveBalanceRepository
↓
MySQL

---

## 56. Transaction Boundary

Business operations that modify leave state are handled within transactional service methods.

This is particularly important for approval and cancellation because these operations modify both:

- Leave request
- Leave balance

The objective is to keep related state changes consistent.

---

## 57. Lazy Entity Relationships

LeaveRequestEntity references LeaveTypeEntity using:

@ManyToOne(fetch = FetchType.LAZY)

LeaveBalanceEntity also references LeaveTypeEntity using a lazy relationship.

Lazy loading avoids automatically loading the related LeaveType entity every time the parent entity is retrieved.

This can reduce unnecessary database work when the relationship is not required.

---

## 58. Database Ownership

The Leave Service owns leave-specific data.

The main tables are:

leave_types
leave_balances
leave_requests

The Employee Service remains the owner of employee data.

This follows the microservice database ownership principle:

One service
↓
Owns its business data

Other services communicate through APIs or events instead of directly modifying another service's tables.

---

## 59. Security and Leave Management

External access to Leave APIs is intended to pass through the API Gateway.

The Gateway is responsible for authentication and authorization according to the WorkSphere security architecture.

The Leave Service itself should therefore be treated as an internal business service in the containerized deployment.

Authorization testing for:

- Employee
- Manager
- Admin

is documented separately in:

31 — Authentication & JWT

and:

32 — Gateway-Based RBAC

The Leave Service documentation therefore focuses on leave-domain behavior rather than duplicating gateway authorization implementation.

---

## 60. Docker Deployment

The Leave Service is containerized as part of the WorkSphere Docker environment.

Inside the Docker network, it communicates with other containers using service names rather than host localhost addresses.

For example:

Leave Service
↓
Employee Service

uses the Docker-resolvable service name for Employee Service.

Kafka communication similarly uses the internal Kafka address:

kafka:9092

The external host-machine Kafka listener is a separate configuration concern.

---

## 61. Leave Service with Kafka in Docker

The Docker architecture is:

worksphere-employee
|
| employee-created
↓
worksphere-kafka
|
├──────────────→ worksphere-payroll
|
└──────────────→ worksphere-leave

Leave Service uses its own consumer group:

worksphere-leave-group

Therefore Payroll and Leave process the event independently.

---

## 62. Failure Handling

Potential Leave Service failures include:

- Employee Service unavailable
- MySQL unavailable
- Invalid employee ID
- Invalid leave type
- Insufficient leave balance
- Overlapping leave
- Invalid leave state
- Kafka processing failure

Business validation failures are handled using domain exceptions.

Kafka event-processing failures use retry and DLT mechanisms.

---

## 63. Current Implementation Status

### Implemented

- Leave Service
- Leave request entity
- Leave balance entity
- Leave type entity
- Leave controller
- Leave service layer
- Leave repositories
- DTOs
- Mapper
- Employee validation through OpenFeign
- Leave date validation
- Leave day calculation
- Leave overlap validation
- Leave balance validation
- Leave approval
- Leave rejection
- Leave cancellation
- Balance deduction on approval
- Balance restoration on approved-leave cancellation
- Employee-created Kafka consumer
- Leave balance initialization
- Kafka retry configuration
- Kafka DLT handler
- Database uniqueness protection for yearly balances
- Docker deployment support

### Verified in the WorkSphere Docker architecture

The Employee Service publishes employee-created events to Kafka.

Leave Service consumes the event using:

worksphere-leave-group

and initializes the employee's leave balances.

### Important limitation

The current DLT handler logs failed employee-created events. It does not provide a separate automated administrative recovery or replay workflow.

---

## 64. Production Considerations

For a larger production system, the following areas could be extended:

- Manager identity validation through authenticated JWT claims
- Authorization directly based on the authenticated user context
- Holiday calendar integration
- Weekends exclusion from leave-day calculation
- Half-day leave
- Optional leave carry-forward
- Leave expiry
- Multi-level approval
- Approval audit history
- Notifications
- Email integration
- Event-driven leave status notifications
- DLT monitoring dashboard
- Automated DLT replay
- Optimistic locking for concurrent balance updates
- Database migration using Flyway or Liquibase
- Comprehensive integration testing
- Distributed tracing

These are future improvements and should not be represented as currently implemented features.

---

## 65. Interview Explanation

A concise interview explanation can be:

"WorkSphere has a dedicated Leave Management microservice that manages leave types, yearly employee balances, and leave requests. Employees can apply for leave, and the service validates employee existence, leave type, date range, overlapping requests, and available balance. Leave requests follow an APPLIED, APPROVED, REJECTED, or CANCELLED lifecycle. Balance deduction happens when leave is approved, and approved-leave cancellation restores the balance. The service communicates synchronously with Employee Service through OpenFeign for employee validation and asynchronously through Kafka for employee-created events. When a new employee is created, the Leave Service consumes the event and initializes yearly balances for all active leave types. Kafka retry and DLT handling are also configured for failed event processing."

---

## 66. Key Interview Concepts Demonstrated

This module demonstrates:

- Microservice boundaries
- Layered architecture
- REST APIs
- DTO pattern
- MapStruct-style mapping
- Spring Data JPA
- Transactions
- Lazy loading
- Database constraints
- Database indexing
- OpenFeign
- Synchronous service communication
- Kafka consumers
- Consumer groups
- Event-driven architecture
- Retry
- Dead Letter Topic
- Idempotent initialization
- Business validation
- State transitions
- Exception handling
- Docker networking
- Service discovery

---

## 67. Final Leave Architecture

The current Leave Management architecture can be summarized as:

                    ┌─────────────────────┐
                    │   Employee Service  │
                    └──────────┬──────────┘
                               │
                               │ EmployeeCreatedEvent
                               ↓
                    ┌─────────────────────┐
                    │       Kafka         │
                    │  employee-created   │
                    └──────────┬──────────┘
                               │
                               ↓
                    ┌─────────────────────┐
                    │    Leave Service    │
                    │                     │
                    │ EmployeeEventConsumer│
                    │ LeaveService        │
                    │ LeaveBalanceService │
                    └───────┬───────┬─────┘
                            │       │
                  OpenFeign │       │ JPA
                            │       │
                            ↓       ↓
                    Employee       MySQL
                    Service        Database

External clients:

Client
↓
API Gateway
↓
Leave Service

The Leave Service therefore combines synchronous REST communication, asynchronous Kafka messaging, transactional database operations, and domain-specific business validation in one independently deployable microservice.

---

## 68. Summary

The WorkSphere Leave Management module demonstrates how a real-world employee leave workflow can be implemented using Spring Boot microservices.

The core flow is:

Employee
↓
Employee Created
↓
Kafka
↓
Leave Service
↓
Initialize Yearly Balances

Then:

Employee
↓
Apply Leave
↓
Validate Employee
↓
Validate Leave Type
↓
Validate Dates
↓
Check Overlap
↓
Check Balance
↓
APPLIED
↓
Manager Approval
↓
APPROVED
↓
Balance Deduction

Alternative paths:

APPLIED → REJECTED

APPLIED → CANCELLED

APPROVED → CANCELLED → Balance Restored

The implementation demonstrates the combination of synchronous APIs, asynchronous event processing, database transactions, business validation, and resilient Kafka consumption required in a modern microservice-based backend.