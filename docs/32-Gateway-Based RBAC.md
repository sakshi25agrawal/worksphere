# 32 — Gateway-Based RBAC

## 1. Overview

WorkSphere uses Role-Based Access Control (RBAC) to control which authenticated users can access which APIs.

The authorization architecture is implemented primarily at the API Gateway.

The Gateway performs two major security responsibilities:

1. Validate the JWT presented by the client.
2. Determine whether the authenticated user's permissions allow access to the requested resource.

The overall flow is:

Client
|
v
API Gateway
|
+--> Validate JWT
|
+--> Extract permissions from JWT
|
+--> Ask Auth Service which permission is required
|
+--> Compare required permission with JWT permissions
|
+--> Allow or reject request
|
v
Downstream Microservice

This makes the Gateway the main entry point for external API traffic.

---

## 2. Why RBAC Is Needed

Authentication answers:

"Who are you?"

Authorization answers:

"What are you allowed to do?"

For example:

A user may successfully log in and receive a valid JWT.

That does NOT automatically mean the user should be able to:

- create employees
- delete employees
- manage departments
- approve leave
- manage payroll

The application therefore separates:

Authentication
|
v
Identity verification

from:

Authorization
|
v
Permission verification

---

## 3. Authentication vs Authorization

### Authentication

Authentication verifies the user's identity.

In WorkSphere:

Client
|
v
POST /auth-service/api/v1/auth/login
|
v
Auth Service
|
v
Username + password verification
|
v
JWT generated

The JWT contains security information including:

- username
- roles
- permissions
- issued-at time
- expiration time

---

## 4. Authorization

Authorization happens after authentication.

The Gateway receives:

Authorization: Bearer <JWT>

The Gateway:

1. Reads the JWT.
2. Validates the signature.
3. Validates expiration.
4. Extracts permissions.
5. Identifies the HTTP method.
6. Identifies the requested resource path.
7. Asks Auth Service which permission is required.
8. Checks whether the JWT contains that permission.
9. Allows or rejects the request.

---

## 5. Why Authorization Is Centralized at the Gateway

Without centralized authorization, every microservice would need to independently implement:

- JWT parsing
- token validation
- role extraction
- permission extraction
- resource matching
- authorization rules
- unauthorized handling
- forbidden handling

That creates duplicated security logic.

With Gateway-based authorization:

Client
|
v
Gateway
|
+--> Authentication
|
+--> Authorization
|
v
Employee Service
Department Service
Leave Service
Payroll Service

The business services can focus primarily on business logic.

---

## 6. API Gateway Technology

The WorkSphere Gateway is implemented using Spring Cloud Gateway.

The Gateway uses the reactive WebFlux model.

The current configuration exposes the Gateway on:

    8080

The application name is:

    api-gateway

Service discovery is enabled through Eureka.

---

## 7. Gateway Discovery

The Gateway uses Eureka discovery locator.

Conceptually:

Gateway
|
v
Eureka
|
+--> employee-service
+--> department-service
+--> leave-service
+--> payroll-service
+--> auth-service

This allows the Gateway to discover registered services instead of hardcoding every downstream service address.

The current Gateway configuration enables:

    discovery.locator.enabled: true

and:

    lower-case-service-id: true

---

## 8. Gateway URL Structure

With discovery-based routing, a request can contain the service ID as the first path segment.

Example:

    /employee-service/api/v1/employees/10

The first segment identifies:

    employee-service

The remaining path represents the actual business API:

    /api/v1/employees/10

The Gateway authorization logic uses this distinction.

---

## 9. Global Security Filter

The main Gateway security implementation is:

    JwtAuthenticationFilter

It implements:

    GlobalFilter
    Ordered

Because it is a GlobalFilter, the security logic applies across Gateway requests.

This is important because authentication and authorization should happen before a request reaches a protected downstream service.

---

## 10. Why GlobalFilter Is Used

A Gateway GlobalFilter is suitable because the security logic needs access to:

- incoming request path
- HTTP method
- HTTP headers
- JWT
- Gateway filter chain
- downstream routing

The filter can decide:

    Continue request

or:

    Stop request

before the downstream service is called.

---

## 11. Filter Ordering

The current filter returns:

    -100

from:

    getOrder()

This places the security filter early in the Gateway filter chain.

Conceptually:

Incoming Request
|
v
JWT Authentication Filter
|
v
Authorization Check
|
v
Gateway Routing
|
v
Microservice

---

## 12. Public Authentication Endpoint

The Gateway allows authentication requests without requiring a JWT.

The current filter checks whether the path starts with:

    /auth-service/api/v1/auth/

If it does, the request continues without JWT validation.

This is necessary because a user cannot provide a JWT before logging in for the first time.

Flow:

Client
|
| username + password
v
Gateway
|
| public authentication path
v
Auth Service
|
v
JWT

---

## 13. Swagger Endpoints

The current Gateway filter also allows:

    /swagger-ui

and:

    /v3/api-docs

without JWT validation.

This is currently intentional so API documentation remains accessible.

Production deployments may choose to protect Swagger endpoints or expose them only in non-production environments.

---

## 14. Authorization Header

For protected requests, the Gateway expects:

    Authorization: Bearer <JWT>

The filter reads the HTTP Authorization header.

Expected format:

    Bearer eyJ...

The Gateway checks that the header exists and starts with:

    Bearer

---

## 15. Missing Authorization Header

If the Authorization header is missing:

    401 Unauthorized

is returned.

The request does not continue to the downstream service.

Flow:

Client
|
| No Authorization header
v
Gateway
|
v
401 Unauthorized

---

## 16. Invalid Authorization Format

If the Authorization header does not start with:

    Bearer

the request is also rejected.

Example:

    Authorization: Basic abc123

This does not satisfy the Gateway's JWT authentication requirement.

The Gateway returns:

    401 Unauthorized

---

## 17. Extracting the JWT

After validating the Bearer prefix, the Gateway removes:

    Bearer 

from the Authorization header.

Conceptually:

    Authorization = "Bearer <token>"

becomes:

    <token>

The resulting token is passed to JwtService.

---

## 18. Gateway JwtService

The Gateway contains its own:

    JwtService

The service is responsible for validating the JWT received by the Gateway.

It creates an HMAC signing key from:

    jwt.secret

The secret is injected from configuration.

---

## 19. Shared JWT Secret

The Auth Service generates the JWT.

The Gateway validates the JWT.

Therefore both components need compatible signing configuration.

Flow:

Auth Service
|
| signs JWT
v
JWT
|
v
Gateway
|
| verifies signature
v
Valid / Invalid

The Gateway must use the same signing secret as the Auth Service when using the current HMAC-based implementation.

---

## 20. JWT Signature Verification

The Gateway uses the signing key to parse signed claims.

Conceptually:

    JWT
     |
     v
Verify signature
|
+---- invalid ---> 401
|
+---- valid
|
v
Read claims

This prevents a client from simply modifying the JWT payload and assigning itself additional permissions.

---

## 21. JWT Expiration

The Gateway also checks the token expiration.

The token is considered valid only when:

    expiration > current time

An expired token is rejected.

Example:

JWT expiration:
10:00 AM

Current time:
10:05 AM

Result:

    Invalid JWT

The Gateway returns:

    401 Unauthorized

---

## 22. Invalid JWT Handling

If JWT parsing or validation throws an exception, the Gateway treats the token as invalid.

The security principle is:

    Invalid token = reject request

The request is never forwarded to the downstream business service.

---

## 23. JWT Permissions

After successful JWT validation, the Gateway extracts:

    permissions

from the JWT.

These permissions were generated by Auth Service during login.

Example conceptual JWT payload:

    {
        "sub": "employee",
        "roles": ["EMPLOYEE"],
        "permissions": [
            "EMPLOYEE_READ",
            "LEAVE_CREATE"
        ],
        "iat": "...",
        "exp": "..."
    }

The exact permission values depend on the configured database mappings.

---

## 24. Why Permissions Instead of Only Roles

A role is a broad business classification.

For example:

    ADMIN
    MANAGER
    EMPLOYEE

A permission represents a specific capability.

For example:

    EMPLOYEE_READ
    EMPLOYEE_CREATE
    EMPLOYEE_UPDATE
    LEAVE_CREATE
    LEAVE_APPROVE
    PAYROLL_READ

This gives the application more flexibility.

Conceptually:

Role
|
+--> Permission
+--> Permission
+--> Permission

---

## 25. Role-to-Permission Relationship

The authorization model can be represented as:

User
|
v
Role
|
v
RolePermission
|
v
Permission

For example:

    EMPLOYEE
       |
       +--> EMPLOYEE_READ
       +--> LEAVE_CREATE

A MANAGER may have a different permission set.

An ADMIN may have a broader permission set.

The Gateway ultimately checks the permissions contained in the user's JWT.

---

## 26. Permission Resolution

The Gateway does not hardcode every resource-to-permission mapping inside the Java filter.

Instead, it asks Auth Service:

    What permission is required for this HTTP method and API path?

The Gateway calls:

    GET /api/v1/authorization/resource

with:

    method
    path

---

## 27. Authorization Controller

Auth Service exposes:

    GET /api/v1/authorization/resource

It accepts:

    method
    path

and returns the permission code associated with the matching resource.

Conceptually:

Gateway
|
| GET /api/v1/authorization/resource
| method=GET
| path=/api/v1/employees/10
v
Auth Service
|
v
Resource mapping
|
v
EMPLOYEE_READ
|
v
Gateway

---

## 28. Resource Entity

The Auth Service stores API resource metadata in the:

    resources

table.

A resource contains concepts such as:

    code
    name
    httpMethod
    pathPattern
    permission

For example:

    HTTP Method:
        GET

    Path Pattern:
        /api/v1/employees/{id}

    Permission:
        EMPLOYEE_READ

This creates a database-driven authorization model.

---

## 29. Why Resource Mapping Is Database Driven

A database-driven resource model avoids putting every authorization rule directly into Gateway source code.

Instead of:

    if path == "/employees" and method == "GET"
        require EMPLOYEE_READ

the application stores the mapping as data.

This makes the authorization model easier to extend.

For example:

    Resource
        GET /api/v1/employees/{id}
        -> EMPLOYEE_READ

    Resource
        POST /api/v1/employees
        -> EMPLOYEE_CREATE

---

## 30. Path Pattern Matching

Auth Service contains a custom path matching implementation.

It compares:

    pathPattern

with:

    actualPath

The matcher splits both paths into segments.

Example:

Pattern:

    /api/v1/employees/{id}

Actual path:

    /api/v1/employees/10

The matcher treats:

    {id}

as a variable segment.

Therefore the paths match.

---

## 31. Why Path Variables Matter

Microservice APIs commonly contain dynamic IDs.

Examples:

    /employees/10
    /employees/25
    /departments/5
    /leaves/100

It would be impractical to store every possible ID as a separate resource.

Instead:

    /api/v1/employees/{id}

matches:

    /api/v1/employees/10

    /api/v1/employees/25

    /api/v1/employees/999

---

## 32. HTTP Method Is Part of Authorization

Authorization is not based only on the URL.

The HTTP method also matters.

For example:

    GET /api/v1/employees/10

might require:

    EMPLOYEE_READ

while:

    DELETE /api/v1/employees/10

might require:

    EMPLOYEE_DELETE

Therefore the authorization lookup uses:

    HTTP Method + API Path

---

## 33. Authorization Decision

After Auth Service returns the required permission, Gateway compares it with the permissions inside the JWT.

Conceptually:

    requiredPermission = EMPLOYEE_READ

    userPermissions =
        [
            EMPLOYEE_READ,
            LEAVE_CREATE
        ]

Because:

    EMPLOYEE_READ

exists in the user's permissions:

    Access Granted

---

## 34. Access Granted

When the user has the required permission, the Gateway executes:

    chain.filter(exchange)

This allows the request to continue through the Gateway.

Flow:

Client
|
v
Gateway
|
+--> JWT valid
|
+--> Permission found
|
+--> User has permission
|
v
Downstream Service

---

## 35. Access Denied

If the JWT is valid but the required permission is not present:

    403 Forbidden

is returned.

Example:

User permissions:

    EMPLOYEE_READ

Requested operation:

    DELETE /api/v1/employees/10

Required permission:

    EMPLOYEE_DELETE

The user does not have:

    EMPLOYEE_DELETE

Therefore:

    403 Forbidden

---

## 36. 401 vs 403

This distinction is important for interviews.

### 401 Unauthorized

Means authentication failed or is missing.

Examples:

- no JWT
- malformed Bearer header
- invalid JWT
- expired JWT

### 403 Forbidden

Means the JWT is valid, but the user does not have the required permission.

Example:

    Valid JWT
    +
    Missing required permission
    =
    403

---

## 37. Fail-Closed Authorization

The Gateway follows a fail-closed approach.

If authorization cannot be verified, the request is not allowed through.

For example:

Gateway
|
| Authorization lookup
v
Auth Service unavailable
|
v
Reject request

This is safer than:

    Auth Service unavailable
        |
        v
    Allow request anyway

Allowing requests when authorization cannot be verified could bypass security controls.

---

## 38. Missing Resource Mapping

If Auth Service does not return a required permission for the requested resource, the current Gateway rejects the request.

This is another fail-closed behavior.

Conceptually:

    Resource mapping found?
          |
       +--+--+
       |     |
      Yes    No
       |     |
       v     v
Continue  Reject

This avoids accidentally exposing an API simply because an authorization mapping was forgotten.

---

## 39. Auth Service Authorization Dependency

The current architecture makes Gateway authorization dependent on Auth Service.

The flow is:

Gateway
|
| "What permission does this API require?"
v
Auth Service
|
v
Resource + Permission
|
v
Gateway
|
v
Authorization decision

Therefore Auth Service acts as the authorization metadata provider.

---

## 40. Reactive WebClient

The Gateway uses:

    WebClient

to call Auth Service.

WebClient is appropriate because Spring Cloud Gateway is reactive.

The current implementation returns a:

    Mono<Void>

and processes the authorization response reactively.

---

## 41. Why .block() Should Not Be Used

Reactive Gateway code should avoid blocking calls.

The current implementation explicitly uses reactive processing:

    bodyToMono(...)
        .flatMap(...)
        .onErrorResume(...)

This allows the Gateway to remain non-blocking.

Using:

    .block()

inside the reactive request path could introduce blocking behavior and reduce the scalability benefits of WebFlux.

---

## 42. Gateway Authorization Flow

Complete authorization flow:

    Client
       |
       | Authorization: Bearer JWT
       v
    API Gateway
       |
       | Validate JWT
       v
    JwtService
       |
       | Valid?
       +------ No ------> 401
       |
       Yes
       |
       v
    Extract permissions
       |
       v
    Extract HTTP method
       |
       v
    Extract resource path
       |
       v
    Auth Service
       |
       | Find required permission
       v
    Required Permission
       |
       v
    Compare with JWT permissions
       |
       +------ Missing ------> 403
       |
       Present
       |
       v
    Gateway Routing
       |
       v
    Downstream Service

---

## 43. Complete Login-to-API Flow

A complete user journey looks like:

Step 1:

Client sends:

    POST /auth-service/api/v1/auth/login

Step 2:

Auth Service validates:

    username
    password

Step 3:

Auth Service loads:

    User
    Roles
    Permissions

Step 4:

Auth Service generates JWT.

Step 5:

Client stores the JWT.

Step 6:

Client calls a protected API:

    GET /employee-service/api/v1/employees/10

Step 7:

Gateway validates JWT.

Step 8:

Gateway extracts permissions.

Step 9:

Gateway asks Auth Service:

    GET /api/v1/authorization/resource
    method=GET
    path=/api/v1/employees/10

Step 10:

Auth Service resolves the required permission.

Step 11:

Gateway compares:

    Required Permission
           vs
    User Permissions

Step 12:

If authorized:

    Request -> Employee Service

Otherwise:

    403 Forbidden

---

## 44. ADMIN / MANAGER / EMPLOYEE Model

WorkSphere defines role concepts including:

    ADMIN
    MANAGER
    EMPLOYEE

The important architectural principle is:

    Role -> Permissions

rather than:

    Gateway -> hardcoded role checks everywhere

The Gateway ultimately evaluates the permissions present in the JWT.

---

## 45. Example: Employee Access

Suppose an employee has:

    EMPLOYEE_READ

The employee calls:

    GET /employee-service/api/v1/employees/10

Authorization mapping:

    GET
    /api/v1/employees/{id}
    -> EMPLOYEE_READ

Gateway checks:

    EMPLOYEE_READ
    exists in JWT permissions?

If yes:

    Request allowed

---

## 46. Example: Employee Without Delete Permission

Suppose an employee has:

    EMPLOYEE_READ

but does not have:

    EMPLOYEE_DELETE

The employee calls:

    DELETE /employee-service/api/v1/employees/10

Authorization mapping:

    DELETE
    /api/v1/employees/{id}
    -> EMPLOYEE_DELETE

Gateway checks the JWT.

Result:

    EMPLOYEE_DELETE not present

Therefore:

    403 Forbidden

---

## 47. Example: Leave Authorization

Suppose a leave API requires:

    LEAVE_CREATE

The user calls:

    POST /leave-service/api/v1/leaves

Gateway:

    1. validates JWT
    2. extracts permissions
    3. asks Auth Service for required permission
    4. receives LEAVE_CREATE
    5. checks JWT permissions
    6. forwards if present

---

## 48. Example: Manager Approval

A manager-only operation could be mapped to a permission such as:

    LEAVE_APPROVE

The authorization process remains the same:

    HTTP Method
          +
    API Path
          |
          v
    Required Permission
          |
          v
    JWT Permissions
          |
          v
    Allow / Deny

The actual permission names are controlled by the WorkSphere authorization data.

---

## 49. Example: Payroll Authorization

A payroll endpoint can similarly be associated with a permission.

For example:

    GET /api/v1/payroll/{employeeId}

could be mapped to a payroll read permission.

The Gateway does not need separate Java logic for every payroll endpoint.

It uses the same authorization mechanism:

    method + path
          |
          v
    resource mapping
          |
          v
    required permission
          |
          v
    JWT permission check

---

## 50. Why This Is More Flexible Than URL-Based Role Checks

A simpler implementation could contain rules such as:

    /employees/** -> ADMIN

But this becomes difficult when requirements become more detailed.

For example:

    GET employee -> EMPLOYEE_READ
    POST employee -> EMPLOYEE_CREATE
    PUT employee -> EMPLOYEE_UPDATE
    DELETE employee -> EMPLOYEE_DELETE

Permission-based authorization supports this granularity.

---

## 51. Resource and Permission Separation

WorkSphere separates:

Resource

from:

Permission

A Resource represents:

    What API is being accessed?

A Permission represents:

    What capability is required?

For example:

    Resource:
        GET /api/v1/employees/{id}

    Permission:
        EMPLOYEE_READ

This separation makes the authorization model reusable.

---

## 52. Role, Permission and Resource Relationship

The overall model can be visualized as:

    AppUser
       |
       v
    Role
       |
       v
    RolePermission
       |
       v
    Permission
       ^
       |
    Resource
       |
       +--> HTTP Method
       |
       +--> Path Pattern

At login:

    User -> Roles -> Permissions -> JWT

At request time:

    Request -> Resource -> Required Permission

Then:

    JWT Permissions
          vs
    Required Permission

---

## 53. Security Boundary

The Gateway creates a security boundary between:

External Client

and:

Internal Microservices

Conceptually:

    Internet / Client
           |
           v
    +----------------+
    | API Gateway    |
    | Authentication |
    | Authorization  |
    +----------------+
           |
           v
    Internal Services

The intended architecture is that clients should not bypass the Gateway.

---

## 54. Gateway as Single Entry Point

The preferred production architecture is:

    Client
       |
       v
    API Gateway
       |
       +--> Auth Service
       +--> Employee Service
       +--> Department Service
       +--> Leave Service
       +--> Payroll Service

Instead of:

    Client -> Employee Service
    Client -> Leave Service
    Client -> Payroll Service

The Gateway provides the common external security boundary.

---

## 55. Gateway and Docker Network Security

Docker networking strengthens this architecture.

The intended deployment model is:

    External Network
          |
          v
      API Gateway
          |
          v
    Internal Docker Network
          |
          +--> Employee
          +--> Department
          +--> Leave
          +--> Payroll
          +--> Auth
          +--> Eureka
          +--> Kafka
          +--> MySQL

Only the Gateway should ultimately be externally exposed for business API traffic.

---

## 56. Important Docker Networking Detail

Inside Docker, services should communicate using Docker service names.

For example:

    kafka:9092

rather than:

    localhost:9092

Similarly, internal service-to-service communication should use the appropriate Docker service name.

This matters because:

    localhost

inside a container refers to that same container.

It does not mean:

    another container

---

## 57. Current Gateway/Auth Communication Detail

The current Gateway authorization filter calls:

    http://localhost:8085/api/v1/authorization/resource

This works when the Gateway and Auth Service are reachable through the host's localhost arrangement.

However, once the Gateway itself is fully containerized and communicating through the Docker network, this address should be changed to the Auth Service's Docker service name.

For example, conceptually:

    http://auth-service:8085/api/v1/authorization/resource

The exact final configuration should be environment-specific rather than hardcoded.

This is an important Docker hardening item.

---

## 58. Why Environment-Specific Configuration Matters

The same application may run in:

    Local development
    Docker
    Test environment
    Production

The Auth Service address may therefore differ.

Conceptually:

Local:

    http://localhost:8085

Docker:

    http://auth-service:8085

Production:

    https://auth.internal.example

The application should obtain this from configuration rather than embedding environment-specific addresses in business logic.

---

## 59. Current Gateway Configuration

The Gateway currently has:

    server:
      port: 8080

Application name:

    api-gateway

Eureka discovery locator:

    enabled: true

JWT secret:

    ${JWT_SECRET}

This allows the JWT signing secret to be supplied through an environment variable.

---

## 60. Why JWT_SECRET Should Be Externalized

A JWT signing secret should not be committed as a plaintext secret in source control.

The current configuration uses:

    ${JWT_SECRET}

This is preferable to:

    jwt.secret: my-secret

inside a committed configuration file.

Production systems should use an appropriate secrets-management mechanism.

---

## 61. Management Endpoints

The Gateway configuration currently exposes Spring Boot management endpoints through:

    management.endpoints.web.exposure.include: "*"

The Gateway endpoint is configured as:

    read-only

This should be reviewed carefully before production deployment.

Not every management endpoint should necessarily be publicly accessible.

A production setup should distinguish:

    Internal management access

from:

    Public API access

---

## 62. Why Gateway Security Should Be Defense in Depth

Gateway authorization should not necessarily be the only security mechanism in a production architecture.

A stronger design can be:

    Client
       |
       v
    Gateway
       |
       | JWT + RBAC
       v
    Internal Service
       |
       | optional service-level validation
       v
    Database

The Gateway provides centralized access control.

Sensitive downstream services can additionally validate trusted identity/context where required.

---

## 63. Current Implementation: What Is Implemented

The current code implements:

- Spring Cloud Gateway
- Reactive GlobalFilter
- JWT validation
- JWT expiration validation
- Bearer token handling
- Permission extraction from JWT
- Resource-based authorization lookup
- HTTP method matching
- Dynamic path-variable matching
- Permission comparison
- 401 handling
- 403 handling
- Fail-closed authorization
- Reactive WebClient communication
- Eureka discovery-based Gateway routing
- Externalized JWT secret configuration

---

## 64. Current Implementation: What Is Not Fully Hardened

The current implementation still has areas for future production hardening.

Examples:

1. Gateway-to-Auth Service URL is currently embedded as localhost:8085.
2. Resource authorization lookup currently loads all resources and performs in-memory matching.
3. Swagger is currently publicly accessible.
4. Management endpoints are broadly exposed.
5. Authorization metadata lookup is performed for each protected request.
6. The Gateway's security logic uses console logging rather than structured application logging.
7. Internal service exposure still needs to be completed as part of the Docker network security phase.
8. Downstream service-level defense-in-depth can be considered.

These are implementation/hardening observations, not indications that the current RBAC mechanism is conceptually incorrect.

---

## 65. Resource Lookup Scalability

The current ResourceService obtains resources and performs matching in application memory.

Conceptually:

    Database
       |
       v
    findAll()
       |
       v
    Java filtering
       |
       v
    Matching Resource

For a small portfolio project this is understandable.

At larger scale, this can be improved by:

- indexing resource data
- querying by HTTP method
- caching resource mappings
- precomputing normalized paths
- using a more sophisticated path matcher
- caching authorization metadata

---

## 66. Authorization Lookup Caching

The required permission for a resource generally changes much less frequently than API traffic.

Therefore a production design could cache:

    HTTP Method + Path Pattern
             |
             v
    Required Permission

For example:

    GET + /api/v1/employees/{id}
        ->
    EMPLOYEE_READ

This reduces repeated database work.

Possible technologies include:

    Redis
    Caffeine
    Spring Cache

The current WorkSphere implementation does not need to claim these optimizations are already implemented.

They are future scalability improvements.

---

## 67. Token Permission Snapshot

An important architectural property of the current design is that permissions are placed into the JWT during login.

Therefore:

    Login
      |
      v
    Roles
      |
      v
    Permissions
      |
      v
    JWT

The Gateway then reads permissions from the JWT.

This avoids needing to load the user's role-permission mapping on every request.

The Gateway does still call Auth Service to determine the permission required by the requested resource.

---

## 68. Permission Changes and Existing Tokens

Because permissions are included in the JWT, changing a user's permissions in the database does not automatically modify an already-issued JWT.

The existing token retains its original permission claims until it expires.

This creates an important security design consideration.

Possible production solutions include:

- short-lived access tokens
- refresh tokens
- token revocation
- authorization versioning
- centralized token introspection
- cache invalidation

The current JWT expiration strategy therefore matters.

---

## 69. JWT Expiration Trade-Off

Shorter JWT lifetime:

    Better security
    +
    Faster permission changes

but:

    More frequent authentication/refresh operations

Longer JWT lifetime:

    Fewer token refreshes

but:

    Permission changes may take longer to take effect

This is a standard security-versus-convenience trade-off.

---

## 70. Fail-Closed vs Fail-Open

This is an important interview concept.

### Fail Open

If authorization cannot be verified:

    Allow request

Security risk:

    Unauthorized access may occur.

### Fail Closed

If authorization cannot be verified:

    Reject request

WorkSphere currently follows the second approach in the Gateway authorization flow.

---

## 71. 401 Unauthorized Flow

Scenario:

    Client
       |
       | Missing/invalid JWT
       v
    Gateway
       |
       v
    401 Unauthorized

The downstream service is not called.

---

## 72. 403 Forbidden Flow

Scenario:

    Client
       |
       | Valid JWT
       v
    Gateway
       |
       | Permission missing
       v
    403 Forbidden

Again, the downstream service is not called.

---

## 73. Successful Authorization Flow

Scenario:

    Client
       |
       | Valid JWT
       v
    Gateway
       |
       | Required permission found
       |
       | User has permission
       v
    Downstream Service
       |
       v
    Response

---

## 74. Example End-to-End Authorization

Suppose:

User:

    employee

JWT permissions:

    EMPLOYEE_READ
    LEAVE_CREATE

Request:

    GET /employee-service/api/v1/employees/10

Gateway extracts:

    method = GET

and:

    resourcePath = /api/v1/employees/10

Auth Service finds:

    GET
    /api/v1/employees/{id}

Required permission:

    EMPLOYEE_READ

Gateway checks:

    EMPLOYEE_READ
    in JWT permissions?

Result:

    Yes

Therefore:

    Request forwarded

---

## 75. Unauthorized Example

Request:

    GET /employee-service/api/v1/employees/10

Authorization header:

    missing

Result:

    401 Unauthorized

No resource authorization lookup is needed because authentication itself failed.

---

## 76. Forbidden Example

Request:

    DELETE /employee-service/api/v1/employees/10

JWT:

    valid

JWT permissions:

    EMPLOYEE_READ

Required permission:

    EMPLOYEE_DELETE

Result:

    403 Forbidden

---

## 77. Unknown Resource Example

Suppose a client requests an API for which no resource mapping exists.

Example:

    GET /employee-service/api/v1/unknown

The authorization lookup does not find a matching resource.

The current Gateway fails closed rather than forwarding the request.

This is safer than assuming:

    "No rule means allow."

---

## 78. Auth Service Failure Example

Suppose:

    Gateway -> Auth Service

fails because Auth Service is unavailable.

The current Gateway handles the error and rejects the request.

Conceptually:

    Gateway
       |
       X
    Auth Service unavailable
       |
       v
    Reject request

This prevents authorization failures from turning into authorization bypasses.

---

## 79. Security Flow by Component

### Client

Responsible for:

- login request
- receiving JWT
- sending JWT with protected requests

### Auth Service

Responsible for:

- authentication
- password verification
- roles
- permissions
- JWT generation
- resource-to-permission metadata

### API Gateway

Responsible for:

- JWT validation
- permission extraction
- resource authorization lookup
- access decision
- routing

### Business Services

Responsible for:

- business logic
- domain validation
- persistence
- business-level constraints

---

## 80. Why Gateway Authorization Fits Microservices

Microservices create many API endpoints.

Without centralized authorization:

    Employee -> security
    Department -> security
    Leave -> security
    Payroll -> security

With Gateway authorization:

    Client
       |
       v
    Gateway Security
       |
       +--> Employee
       +--> Department
       +--> Leave
       +--> Payroll

This reduces duplicated cross-cutting security logic.

---

## 81. Important Interview Question

### "How does your Gateway implement RBAC?"

A strong WorkSphere-specific answer:

"WorkSphere uses permission-based RBAC at the API Gateway. During login, Auth Service authenticates the user and builds a JWT containing roles and permissions. For a protected request, the Gateway validates the JWT and extracts its permissions. It then uses the HTTP method and request path to ask Auth Service which permission is required for that resource. The Gateway compares the required permission with the permissions in the JWT. If the permission exists, the request is routed to the downstream service; otherwise it returns 403. Missing or invalid JWTs return 401. The authorization path is fail-closed."

---

## 82. Important Interview Question

### "Why did you use permissions instead of directly checking roles?"

Answer:

"Roles provide a coarse-grained grouping, while permissions represent specific capabilities. For example, instead of simply saying that a MANAGER can access an entire service, I can define permissions such as LEAVE_APPROVE or EMPLOYEE_UPDATE. Roles can then be mapped to those permissions. This makes authorization more granular and easier to evolve."

---

## 83. Important Interview Question

### "Where are permissions stored?"

Answer:

"Permissions are managed by the Auth Service. Users have roles, roles are associated with permissions, and resource definitions map HTTP methods and path patterns to the permission required for an API."

---

## 84. Important Interview Question

### "How does the Gateway know which permission an endpoint requires?"

Answer:

"The Gateway sends the HTTP method and normalized resource path to the Auth Service authorization endpoint. Auth Service matches the method and path against its resource mappings and returns the corresponding permission code."

---

## 85. Important Interview Question

### "How do you handle dynamic IDs?"

Answer:

"The resource model supports path patterns such as /api/v1/employees/{id}. The ResourceService splits the pattern and actual path into segments. Placeholder segments enclosed in braces are treated as dynamic values, allowing /employees/10 and /employees/20 to match the same resource definition."

---

## 86. Important Interview Question

### "What happens when the JWT is invalid?"

Answer:

"The Gateway rejects the request with HTTP 401 and does not forward it to the downstream service."

---

## 87. Important Interview Question

### "What happens when the JWT is valid but the user lacks permission?"

Answer:

"The Gateway returns HTTP 403 Forbidden because authentication succeeded but authorization failed."

---

## 88. Important Interview Question

### "What happens if Auth Service is unavailable during authorization?"

Answer:

"The current Gateway implementation fails closed. If the authorization lookup fails, the request is rejected rather than being allowed through."

---

## 89. Important Interview Question

### "Why use WebClient?"

Answer:

"The Gateway is based on Spring Cloud Gateway and WebFlux, so WebClient provides a reactive, non-blocking way to call Auth Service. The authorization flow uses reactive Mono processing instead of blocking the request thread."

---

## 90. Important Interview Question

### "Why not call .block()?"

Answer:

"Because the Gateway is reactive. Blocking inside the reactive request-processing path can reduce scalability and undermine the non-blocking execution model. The implementation therefore uses bodyToMono, flatMap and onErrorResume."

---

## 91. Important Interview Question

### "Why fail closed?"

Answer:

"If authorization cannot be verified, allowing the request would create a potential security bypass. Failing closed means that an authorization dependency failure results in rejection instead of accidental access."

---

## 92. Important Interview Question

### "Is Gateway authorization enough for production?"

Answer:

"Gateway authorization provides a centralized security boundary, but sensitive systems can also use defense-in-depth. Depending on the deployment, downstream services can additionally validate trusted identity or authorization context. Network controls should also prevent clients from bypassing the Gateway."

---

## 93. Current WorkSphere Security Architecture

The current conceptual architecture is:

    +----------------------+
    |        Client        |
    +----------+-----------+
               |
               | Login
               v
    +----------------------+
    |     API Gateway     |
    |       :8080         |
    +----------+-----------+
               |
               v
    +----------------------+
    |     Auth Service     |
    |       :8085         |
    +----------------------+
               |
               +--> User
               +--> Role
               +--> Permission
               +--> Resource
               |
               v
              JWT
               |
               v
    +----------------------+
    |     API Gateway     |
    | JWT + RBAC Filter   |
    +----------+-----------+
               |
       +-------+-------+--------+--------+
       |               |        |        |
       v               v        v        v
Employee       Department  Leave   Payroll
Service          Service   Service  Service

---

## 94. Detailed Authorization Architecture

    Client
      |
      | Authorization: Bearer JWT
      v
    API Gateway
      |
      +--> JwtService
      |       |
      |       +--> Verify signature
      |       +--> Check expiration
      |
      +--> Extract permissions
      |
      +--> Extract HTTP method
      |
      +--> Extract resource path
      |
      v
    Auth Service
      |
      +--> Resource
      |       |
      |       +--> HTTP method
      |       +--> path pattern
      |       +--> permission
      |
      v
    Required Permission
      |
      v
    Compare
      |
      +---- Missing ----> 403
      |
      +---- Present ----> Downstream Service

---

## 95. Security Decision Table

| Condition | Result |
|---|---|
| Authentication endpoint | Allow |
| Swagger endpoint | Allow currently |
| Missing Authorization header | 401 |
| Invalid Bearer format | 401 |
| Invalid JWT | 401 |
| Expired JWT | 401 |
| Resource mapping unavailable/not found | Reject |
| Required permission absent | 403 |
| Required permission present | Forward |
| Auth Service authorization call fails | Reject |

---

## 96. Gateway-Based RBAC vs Service-Level RBAC

### Gateway-Based RBAC

Advantages:

- centralized authorization
- less duplication
- consistent external API policy
- single security entry point
- easier cross-service policy management

### Service-Level RBAC

Advantages:

- stronger defense in depth
- service owns its own authorization
- safer when services can be called independently
- useful for internal authorization rules

A mature architecture can use both.

---

## 97. Business Authorization vs Technical Authorization

Not every authorization decision is purely role-based.

Gateway RBAC answers:

    "Does this user have permission to call this API?"

Business logic may additionally answer:

    "Is this user allowed to modify this particular employee?"

For example:

    MANAGER
       |
       v
    Can approve leave?

But business rules may further require:

    Is the employee part of this manager's team?

Such rules belong in the business/service layer rather than being forced into Gateway path-based RBAC.

---

## 98. Security Responsibility Boundaries

A useful architecture is:

Gateway:

    Authentication
    API-level authorization
    Routing

Business service:

    Business authorization
    Domain validation
    Business rules

Database:

    Persistence constraints
    Referential integrity
    Unique constraints

This separation prevents the Gateway from becoming responsible for business logic.

---

## 99. Current Implementation Status

### Implemented

- JWT validation at Gateway
- JWT expiration validation
- Permission extraction
- Resource authorization lookup
- HTTP method matching
- Path-variable matching
- 401 handling
- 403 handling
- Fail-closed authorization
- Reactive WebClient
- Eureka discovery
- Gateway routing
- Externalized JWT secret

### Verified at code level

- JwtAuthenticationFilter exists
- JwtService validates signed tokens
- Auth Service exposes authorization resource lookup
- ResourceService performs method/path matching
- Resource contains permission association

### Future hardening

- Move Gateway -> Auth URL to environment configuration
- Complete internal Docker-only service exposure
- Restrict management endpoints
- Review Swagger exposure
- Add authorization metadata caching
- Improve structured logging
- Consider service-level defense in depth
- Add automated RBAC integration tests
- Add security-focused observability

---

## 100. Relationship With Docker Security

Gateway RBAC and Docker network isolation solve different problems.

RBAC answers:

    "Is this authenticated user allowed to call this API?"

Docker networking answers:

    "Can this external client directly reach this internal service?"

Together:

    Client
       |
       v
    Gateway
       |
       | RBAC
       v
    Internal Network
       |
       +--> Employee
       +--> Department
       +--> Leave
       +--> Payroll

The target production architecture is therefore:

    External access
          |
          v
    API Gateway only
          |
          v
    Internal Docker network

---

## 101. Why Both Controls Matter

Suppose Gateway RBAC is perfect but Employee Service is directly exposed.

A client could attempt:

    Client -> Employee Service

and potentially bypass Gateway authorization.

Network isolation reduces this attack surface.

Conversely, network isolation alone does not decide which authenticated user can perform which operation.

Therefore:

    Network Security
           +
    Application Authorization
           =
    Stronger Security Boundary

---

## 102. Security Architecture Principle

The WorkSphere architecture follows the principle:

    Authenticate once
          |
          v
    Authorize centrally
          |
          v
    Route internally
          |
          v
    Apply business rules downstream

This is particularly useful in a microservice architecture where many services expose APIs.

---

## 103. Final End-to-End Security Flow

    +------------------+
    |      Client      |
    +--------+---------+
             |
             | POST /auth-service/api/v1/auth/login
             v
    +------------------+
    |   API Gateway    |
    +--------+---------+
             |
             v
    +------------------+
    |   Auth Service   |
    |                  |
    | User             |
    | Role             |
    | Permission       |
    +--------+---------+
             |
             | JWT
             v
    +------------------+
    |   API Gateway    |
    |                  |
    | Validate JWT     |
    | Extract perms    |
    +--------+---------+
             |
             | method + path
             v
    +------------------+
    |   Auth Service   |
    |                  |
    | Resource mapping |
    +--------+---------+
             |
             | required permission
             v
    +------------------+
    |   API Gateway    |
    |                  |
    | permission check |
    +--------+---------+
             |
        +----+----+
        |         |
      Allow      Deny
        |         |
        v         +----> 403
    Downstream
      Service

---

## 104. Summary

WorkSphere implements Gateway-based permission authorization using Spring Cloud Gateway.

The important sequence is:

    1. User logs in through Auth Service.
    2. Auth Service verifies credentials.
    3. Auth Service determines roles and permissions.
    4. Auth Service generates a signed JWT.
    5. Client sends JWT to Gateway.
    6. Gateway validates the JWT.
    7. Gateway extracts permissions.
    8. Gateway extracts HTTP method and resource path.
    9. Gateway asks Auth Service for the required permission.
    10. Auth Service matches the resource.
    11. Gateway compares required permission with JWT permissions.
    12. Missing permission results in 403.
    13. Invalid/missing JWT results in 401.
    14. Authorized requests are routed to downstream services.
    15. Authorization lookup failures fail closed.

The key architectural idea is:

    User
      |
      v
    Roles
      |
      v
    Permissions
      |
      v
    JWT
      |
      v
    Gateway
      |
      +--> Required Permission
      |
      v
    Authorization Decision
      |
      v
    Microservice

This provides a centralized, permission-based security boundary while leaving business-specific authorization rules to the appropriate service layer.