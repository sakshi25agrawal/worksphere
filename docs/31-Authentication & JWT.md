# 31 — Authentication & JWT

## 1. Overview

WorkSphere uses a dedicated Authentication Service to handle user authentication and JWT token generation.

The authentication architecture is based on:

- Spring Security
- JWT
- BCrypt password hashing
- Database-backed users
- Database-backed roles
- Role-based permissions
- Stateless authentication
- API Gateway integration

The main authentication flow is:

Client
↓
POST /api/v1/auth/login
↓
Auth Service
↓
Authenticate username + password
↓
Load user and roles
↓
Load permissions
↓
Generate JWT
↓
Return JWT to client

The generated JWT contains:

- Username
- Roles
- Permissions
- Issued-at timestamp
- Expiration timestamp

---

## 2. Why a Dedicated Authentication Service

Authentication is separated from business services so that identity management does not need to be duplicated across:

- Employee Service
- Department Service
- Leave Service
- Payroll Service

The Auth Service owns authentication-related data and responsibilities.

Business services can remain focused on their own domains.

Conceptually:

                    ┌──────────────────────┐
                    │      Auth Service    │
                    │                      │
                    │ Users                │
                    │ Roles                │
                    │ Permissions          │
                    │ JWT                  │
                    └──────────┬───────────┘
                               │
                               │ JWT
                               ↓
                         API Gateway
                               │
                 ┌─────────────┼─────────────┐
                 ↓             ↓             ↓
             Employee      Department      Leave
                 │
                 ↓
              Payroll

---

## 3. Authentication vs Authorization

Authentication answers:

"Who are you?"

Authorization answers:

"What are you allowed to do?"

WorkSphere separates these concepts.

### Authentication

The Auth Service verifies:

- Username
- Password

If authentication succeeds, it generates a JWT.

### Authorization

The JWT contains:

- Roles
- Permissions

The API Gateway uses the authenticated user's security information when applying authorization rules.

Therefore:

Authentication
↓
Auth Service

Authorization
↓
API Gateway

---

## 4. Authentication Endpoint

The current login endpoint is:

POST /api/v1/auth/login

The AuthenticationController receives a validated AuthenticationRequest.

The controller delegates authentication to AuthenticationService.

The response contains an AuthenticationResponse with the generated JWT.

---

## 5. Login Flow

The current login flow is:

Client
↓
POST /api/v1/auth/login
↓
AuthenticationController
↓
AuthenticationService
↓
AuthenticationManager
↓
UsernamePasswordAuthenticationToken
↓
UserDetailsService
↓
AppUserRepository
↓
Verify password
↓
Load AppUser
↓
Load roles
↓
Load permissions
↓
Generate JWT
↓
AuthenticationResponse
↓
Client

---

## 6. AuthenticationController

AuthenticationController exposes the authentication API.

The controller is mapped to:

/api/v1/auth

The login operation is:

/login

Therefore the complete endpoint is:

POST /api/v1/auth/login

The request body is validated using:

@Valid

The controller delegates all authentication logic to AuthenticationService rather than implementing authentication logic directly.

---

## 7. AuthenticationService

AuthenticationService coordinates the complete authentication process.

Its main responsibilities are:

1. Authenticate username and password
2. Load the authenticated user
3. Load permissions for the user's roles
4. Generate JWT
5. Return the authentication response

The service uses:

- AuthenticationManager
- JwtService
- AppUserRepository
- PermissionService

---

## 8. AuthenticationManager

Spring Security's AuthenticationManager performs username/password authentication.

The service creates:

UsernamePasswordAuthenticationToken

using:

username
password

The AuthenticationManager then delegates authentication to the configured Spring Security authentication infrastructure.

Conceptually:

Username
+
Password
↓
AuthenticationManager
↓
UserDetailsService
↓
Database User
↓
PasswordEncoder
↓
Authentication Success / Failure

---

## 9. CustomUserDetailsService

WorkSphere implements Spring Security's UserDetailsService through CustomUserDetailsService.

Its responsibility is to load a user by username.

The lookup is performed through:

AppUserRepository

If the user cannot be found, the service throws:

UsernameNotFoundException

This connects Spring Security authentication with the application's database-backed AppUser entity.

---

## 10. AppUser Entity

The main authentication user entity is:

AppUser

It implements:

UserDetails

Important fields include:

- id
- username
- password
- enabled
- roles

The username is unique in the database.

The password is stored in encoded form rather than as plain text.

---

## 11. Why AppUser Implements UserDetails

Spring Security expects a UserDetails representation for authenticated users.

By implementing UserDetails, AppUser can directly provide:

- Username
- Password
- Authorities
- Account status

The application therefore does not need a separate adapter object just to represent the database user.

---

## 12. Password Storage

Passwords are protected using:

BCryptPasswordEncoder

The PasswordEncoder is registered as a Spring Bean in SecurityConfig.

Conceptually:

Plain Password
↓
BCryptPasswordEncoder
↓
Encoded Password
↓
Database

During authentication:

Entered Password
↓
BCrypt verification
↓
Stored BCrypt hash
↓
Authentication result

The application therefore does not store the original password in the database.

---

## 13. Why BCrypt Is Used

BCrypt is designed for password hashing rather than reversible encryption.

The application should never need to decrypt a user's password.

Instead, the submitted password is compared against the stored BCrypt hash.

This is the appropriate model for password authentication.

---

## 14. Role Model

AppUser has a many-to-many relationship with Role.

The mapping uses:

user_roles

Conceptually:

app_users
|
| user_roles
|
+-------- roles

This allows a user to have one or more roles.

For example:

User A
↓
ADMIN

User B
↓
EMPLOYEE

A future user could also have multiple roles.

---

## 15. User Roles Mapping

The current AppUser mapping uses:

@ManyToMany(fetch = FetchType.EAGER)

with the join table:

user_roles

The join table contains:

user_id
role_id

Conceptually:

user_roles

user_id | role_id
--------|--------
1       | 1
2       | 3

The exact numeric IDs depend on database state.

---

## 16. Why Roles Are Loaded Eagerly

AppUser currently loads roles eagerly.

This is useful during authentication because the roles are required when:

- Creating Spring Security authorities
- Generating JWT claims
- Calculating permissions

Therefore, authentication can access the roles immediately after loading the user.

However, eager relationships should be used carefully in larger object graphs because they can increase query and memory usage.

---

## 17. Spring Security Authorities

AppUser converts each role into a Spring Security authority.

The current format is:

ROLE_<ROLE_NAME>

For example:

Role:

ADMIN

becomes:

ROLE_ADMIN

Role:

EMPLOYEE

becomes:

ROLE_EMPLOYEE

This follows Spring Security's conventional role-authority naming.

---

## 18. Role Entity

The Role entity contains:

- id
- name
- rolePermissions

The role name is unique.

Roles are therefore represented as database records rather than hardcoded only inside controller methods.

---

## 19. Permission Model

WorkSphere also contains a permission layer.

The permission relationship is:

User
↓
Role
↓
RolePermission
↓
Permission

A role can therefore be associated with multiple permissions.

For example, conceptually:

ADMIN
↓
EMPLOYEE_READ
EMPLOYEE_CREATE
EMPLOYEE_UPDATE
EMPLOYEE_DELETE

The actual permissions depend on the current database configuration.

---

## 20. PermissionService

PermissionService retrieves permission codes associated with a user's roles.

The service:

1. Receives the user's roles
2. Retrieves permissions for each role
3. Combines the permission codes
4. Removes duplicates
5. Returns the final permission list

This means a user with multiple roles can receive the combined permissions from those roles.

---

## 21. Permission Deduplication

PermissionService uses distinct filtering.

Example:

Role A:

EMPLOYEE_READ
EMPLOYEE_UPDATE

Role B:

EMPLOYEE_READ
LEAVE_READ

Combined result:

EMPLOYEE_READ
EMPLOYEE_UPDATE
LEAVE_READ

EMPLOYEE_READ appears only once.

This avoids duplicate permission claims inside the JWT.

---

## 22. JWT Generation

JwtService is responsible for generating JWT tokens.

The generated token contains:

subject
roles
permissions
issuedAt
expiration

The subject is the authenticated username.

Conceptually:

JWT
├── subject = username
├── roles = [...]
├── permissions = [...]
├── issuedAt
└── expiration

---

## 23. JWT Subject

The JWT subject contains:

user.getUsername()

Therefore the token identifies the authenticated user using the username.

For example:

subject = employee

The actual value depends on the authenticated account.

---

## 24. JWT Roles Claim

JwtService creates a:

roles

claim.

The values are extracted from the user's Role entities.

Conceptually:

roles = [
"ADMIN"
]

or:

roles = [
"EMPLOYEE"
]

For users with multiple roles, multiple role names can be present.

---

## 25. JWT Permissions Claim

JwtService also creates a:

permissions

claim.

These permissions are retrieved by PermissionService.

Conceptually:

permissions = [
"EMPLOYEE_READ",
"EMPLOYEE_CREATE",
"LEAVE_READ"
]

The actual permissions depend on the roles assigned to the authenticated user.

---

## 26. JWT Issued At

The token includes:

issuedAt

This represents the time at which the JWT was generated.

It is useful when inspecting token lifetime and debugging authentication behavior.

---

## 27. JWT Expiration

The current JwtService configures:

EXPIRATION_TIME = 1000 * 60 * 60

This represents:

1 hour

Therefore, the generated JWT expires one hour after issuance.

After expiration, the token is no longer considered valid by the JwtService validation logic.

---

## 28. JWT Signing Algorithm

The JWT is signed using:

HS256

The signing key is derived from:

jwt.secret

The secret is converted to a SecretKey using:

Keys.hmacShaKeyFor(...)

The token is then signed using the generated key.

Conceptually:

jwt.secret
↓
SecretKey
↓
HS256
↓
Signed JWT

---

## 29. Why JWT Is Signed

Signing protects the integrity of the token.

A client can read JWT payload information, but modifying the payload invalidates the signature unless the attacker possesses the signing secret.

The server therefore verifies the signature before trusting the token claims.

---

## 30. JWT Secret Configuration

The JWT signing secret is injected from configuration using:

${jwt.secret}

The secret should be supplied through secure configuration in deployment environments.

It should not be committed as a real production secret in source control.

Docker deployment should provide the secret through environment-specific configuration or secret management.

---

## 31. JWT Validation

JwtService provides:

isTokenValid(token, username)

The validation checks:

1. Extract username from token
2. Compare it with the expected username
3. Check token expiration

Conceptually:

Token
↓
Verify signature
↓
Extract username
↓
Compare username
↓
Check expiration
↓
Valid / Invalid

---

## 32. JWT Claims Extraction

JwtService provides methods to extract:

- Username
- Roles
- Permissions
- Expiration

This keeps JWT parsing logic inside one dedicated service.

Business services should not duplicate token parsing logic unnecessarily.

---

## 33. Signed Claims Parsing

The current implementation uses the JJWT parser with the configured signing key.

The parser verifies the signed JWT before returning its claims.

This means the application does not simply decode the token and trust its contents.

Signature verification is part of the parsing process.

---

## 34. Stateless Security

SecurityConfig configures:

SessionCreationPolicy.STATELESS

This means the authentication architecture does not rely on a server-side HTTP session for maintaining login state.

The client sends its JWT with subsequent authenticated requests.

Conceptually:

Login
↓
JWT
↓
Client stores JWT
↓
Authenticated Request + JWT
↓
Server validates JWT

---

## 35. CSRF Configuration

The current SecurityConfig disables CSRF:

csrf.disable()

This is consistent with the application's stateless token-based API architecture.

The authentication model is based on JWT rather than browser session cookies.

CSRF considerations should still be evaluated if the authentication mechanism or browser interaction model changes.

---

## 36. Protected Endpoints

SecurityConfig explicitly permits authentication-related endpoints and API documentation endpoints.

Permitted paths currently include:

/api/v1/auth/login
/api/v1/authorization/**
/api/v1/auth/**
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**

Other endpoints require authentication through:

.anyRequest().authenticated()

This means authentication is required for protected application APIs.

---

## 37. Authentication and Authorization Separation

An important architectural distinction is:

Auth Service
↓
Authenticates user
↓
Creates JWT

API Gateway
↓
Uses authentication/security information
↓
Applies authorization rules

Therefore, JWT generation and gateway-based RBAC are related but separate responsibilities.

Detailed gateway authorization is documented in:

32 — Gateway-Based RBAC

---

## 38. HTTP Basic Configuration

The current SecurityConfig also enables:

httpBasic(Customizer.withDefaults())

This means Spring Security's HTTP Basic mechanism is currently configured in addition to the JWT-oriented architecture.

The main application login flow, however, is exposed through the dedicated:

POST /api/v1/auth/login

endpoint and returns a JWT.

If HTTP Basic is not required by the final deployment architecture, it can be reviewed during security hardening.

---

## 39. Authentication Database

The Auth Service persists authentication data in its own database.

The important domain tables include concepts such as:

app_users
roles
user_roles
role_permissions

The Auth Service therefore owns authentication and authorization metadata rather than storing it in the Employee Service database.

---

## 40. Authentication Data Flow

The complete database-backed authentication flow is:

Client
↓
username + password
↓
AuthenticationController
↓
AuthenticationService
↓
AuthenticationManager
↓
CustomUserDetailsService
↓
AppUserRepository
↓
AppUser
↓
Roles
↓
PermissionService
↓
Permissions
↓
JwtService
↓
JWT

---

## 41. Default User Bootstrap

The current application contains a DataInitializer.

Its purpose is to create default users when they do not already exist.

The current initializer creates:

- admin
- employee

The users are assigned their corresponding roles.

The passwords are encoded using PasswordEncoder before persistence.

This is currently used for development and RBAC testing.

---

## 42. Default Admin Bootstrap

The initializer creates the admin account if the username does not already exist.

The user receives:

ADMIN

role.

The password is passed through BCryptPasswordEncoder before being stored.

The actual bootstrap credentials are development/test credentials and should not be treated as production secrets.

---

## 43. Default Employee Bootstrap

The initializer also creates an employee account if it does not already exist.

The account receives:

EMPLOYEE

role.

Its password is also encoded before persistence.

This account is currently useful for testing authorization scenarios.

---

## 44. Why Hardcoded Bootstrap Users Are Temporary

Hardcoded credentials inside application code are not suitable as a final production user-management strategy.

The current implementation keeps them for development and RBAC testing.

A future production-oriented implementation should provide:

- User creation API
- Password hashing
- Role assignment
- Account enable/disable
- Credential management
- Secure initial-admin bootstrap
- Removal of fixed test credentials from source code

This is a planned improvement rather than a current feature.

---

## 45. Current Authentication Lifecycle

The complete lifecycle is:

1. User sends username and password
2. AuthenticationController receives request
3. AuthenticationService invokes AuthenticationManager
4. CustomUserDetailsService loads user
5. BCrypt verifies password
6. AppUser is loaded with roles
7. PermissionService resolves permissions
8. JwtService generates token
9. JWT is returned
10. Client sends JWT on protected requests
11. Security infrastructure validates authentication
12. Gateway applies authorization rules

---

## 46. JWT Example Structure

A conceptual JWT payload is:

{
"sub": "employee",
"roles": [
"EMPLOYEE"
],
"permissions": [
"EMPLOYEE_READ",
"LEAVE_READ"
],
"iat": "...",
"exp": "..."
}

The exact permissions depend on the database role-permission configuration.

The token is signed using HS256.

---

## 47. JWT Request Model

After login, a client should send the JWT as a Bearer token when calling protected APIs.

Conceptually:

Authorization: Bearer <JWT>

The Gateway/security layer can then authenticate the request and apply authorization rules.

---

## 48. Authentication Failure

If username/password authentication fails, Spring Security's AuthenticationManager rejects the authentication attempt.

The authentication flow therefore does not generate a JWT for invalid credentials.

This prevents unauthenticated users from obtaining valid application tokens through the login endpoint.

---

## 49. User Enabled Status

AppUser contains:

enabled

The value is exposed through UserDetails:

isEnabled()

The current default value is:

true

This provides a foundation for disabling accounts without deleting them.

A disabled user can therefore be prevented from authenticating through Spring Security's account-status checks.

---

## 50. Account Status Methods

AppUser implements the standard UserDetails account-status methods:

isAccountNonExpired()
isAccountNonLocked()
isCredentialsNonExpired()
isEnabled()

The current implementation returns true for:

- Account non-expired
- Account non-locked
- Credentials non-expired

The enabled flag controls whether the account is enabled.

---

## 51. Authentication Security Layers

WorkSphere's authentication design contains multiple security layers:

### Layer 1 — Password Security

BCrypt password hashing.

### Layer 2 — Authentication

AuthenticationManager validates username/password.

### Layer 3 — JWT

JwtService generates signed tokens.

### Layer 4 — Stateless Requests

JWT-based authentication avoids server-side login sessions.

### Layer 5 — Authorization

Roles and permissions are carried in JWT claims and used by the gateway authorization architecture.

---

## 52. Why JWT Fits a Microservice Architecture

JWT is useful in a distributed system because services do not need to maintain a shared login session.

Conceptually:

Client
↓
JWT
↓
API Gateway
↓
Microservices

The token can carry the authenticated identity and authorization-related claims required by the application's security architecture.

This reduces dependence on centralized HTTP session state.

---

## 53. Auth Service and API Gateway

The Auth Service and API Gateway have different responsibilities.

Auth Service:

- Validates credentials
- Loads user
- Loads roles
- Resolves permissions
- Generates JWT

Gateway:

- Receives API requests
- Applies authentication/security processing
- Applies route-level authorization
- Forwards authorized requests to internal services

The detailed gateway design is documented separately in:

32 — Gateway-Based RBAC

---

## 54. Auth Service and Business Services

Business services do not need to own authentication credentials.

For example:

Employee Service
→ employee data

Department Service
→ department data

Leave Service
→ leave data

Payroll Service
→ payroll data

Auth Service
→ authentication and authorization metadata

This separation keeps domain ownership clear.

---

## 55. Authentication Security Boundary

The intended external request path is:

Client
↓
API Gateway
↓
Protected Microservice

The Auth Service provides the login/token-generation capability.

The Gateway then becomes the main external entry point for protected APIs in the containerized architecture.

---

## 56. Current Implementation Status

### Implemented

- Spring Security
- AuthenticationController
- Login endpoint
- AuthenticationService
- AuthenticationManager
- CustomUserDetailsService
- Database-backed AppUser
- BCryptPasswordEncoder
- Database-backed roles
- User-role many-to-many mapping
- Role permissions
- PermissionService
- JWT generation
- JWT signing
- JWT validation
- JWT username extraction
- JWT role extraction
- JWT permission extraction
- JWT expiration
- Stateless session configuration
- Protected endpoint authentication
- Development user bootstrap

### Implemented but should be reviewed during hardening

- HTTP Basic is enabled alongside the JWT-oriented login flow.
- The JWT secret is configuration-driven and must be securely supplied in deployment.
- Default development credentials remain in DataInitializer.

### Not yet the final production user-management model

- Proper user creation API
- Secure administrative user provisioning
- Password reset workflow
- Account lifecycle management
- Production secret management
- Removal of fixed development credentials

---

## 57. Security Improvements for Production

The following improvements can be considered for production hardening:

- Remove hardcoded passwords from source code
- Use environment variables or secret management for JWT secrets
- Use a secure initial-admin bootstrap process
- Add user creation API
- Add password reset/change functionality
- Add account lockout or throttling for repeated login failures
- Add refresh-token strategy if required
- Add token revocation strategy if required
- Add audit logging for authentication events
- Review whether HTTP Basic is required
- Add comprehensive security integration tests
- Apply strict Gateway authorization rules
- Avoid exposing internal services directly

These are future hardening items and are not represented as already implemented.

---

## 58. Interview Explanation

A concise interview explanation is:

"WorkSphere uses a dedicated Authentication Service built with Spring Security and JWT. During login, the AuthenticationManager validates the username and password using a database-backed UserDetailsService and BCrypt password encoding. Once authenticated, the service loads the user's roles and resolves permissions through PermissionService. JwtService then generates a signed HS256 token containing the username, roles, permissions, issued-at time, and expiration time. The application uses stateless security, so subsequent requests carry the JWT instead of relying on an HTTP session. The API Gateway uses the authentication and authorization information to enforce access to protected microservice APIs."

---

## 59. Key Interview Concepts Demonstrated

This module demonstrates:

- Spring Security
- AuthenticationManager
- UserDetailsService
- UserDetails
- BCrypt
- JWT
- JJWT
- HS256
- Stateless authentication
- Roles
- Authorities
- Permissions
- Many-to-many relationships
- Database-backed authentication
- Authentication vs authorization
- API Gateway security
- Token expiration
- Signed claims
- Microservice security boundaries

---

## 60. Final Authentication Architecture

The current authentication architecture can be summarized as:

                         Client
                           |
                           | username + password
                           ↓
                 ┌─────────────────────┐
                 │    Auth Service     │
                 │                     │
                 │ Authentication      │
                 │ UserDetailsService  │
                 │ BCrypt              │
                 │ Roles              │
                 │ Permissions        │
                 │ JwtService         │
                 └──────────┬──────────┘
                            |
                            | Signed JWT
                            ↓
                     API Gateway
                            |
                            | Authorized Request
                            ↓
              ┌─────────────┼─────────────┐
              ↓             ↓             ↓
          Employee      Department      Leave
              |                           |
              └──────────────┬────────────┘
                             ↓
                          Payroll

---

## 61. Summary

The WorkSphere authentication implementation provides a database-backed, JWT-based authentication architecture using Spring Security.

The main flow is:

Username + Password
↓
AuthenticationManager
↓
CustomUserDetailsService
↓
AppUser
↓
BCrypt verification
↓
Roles
↓
Permissions
↓
JwtService
↓
Signed JWT
↓
Client
↓
API Gateway
↓
Authorization
↓
Protected Microservices

The current implementation provides the foundation for centralized authentication and Gateway-based authorization while keeping authentication data separate from business-service databases.

The remaining production-hardening work primarily concerns secure user provisioning, removal of fixed development credentials, secret management, and further security testing.