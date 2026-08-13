# 27. Authentication & JWT Security

## 1. Authentication Overview

Authentication is the process of verifying the identity of a user before allowing access to protected application resources.

In WorkSphere, authentication is required because multiple microservices expose business APIs such as:

* Employee Service
* Payroll Service
* Department Service
* Other future WorkSphere services

We should not allow every client to directly access these services without verifying who the client is.

The authentication architecture therefore introduces a centralized security flow using:

```text
Spring Security
       +
Auth Service
       +
JWT
       +
API Gateway
       +
Eureka Service Discovery
```

The main goal is to authenticate the user once and then use the generated JWT token to access protected APIs.

---

## 2. Why Authentication Is Required

Consider a client trying to access:

```http
GET /api/v1/employees/1
```

Without authentication, anyone who knows the endpoint could potentially access employee information.

A secure system should instead work like:

```text
Client
   |
   | Who are you?
   v
Authentication
   |
   | Identity verified
   v
JWT Token
   |
   | Token attached to request
   v
Protected API
```

Therefore, authentication provides the first security layer around WorkSphere APIs.

---

## 3. Authentication in a Microservices Architecture

In a monolithic application, authentication can be implemented directly inside the application.

For example:

```text
Client
   |
   v
Application
   |
   +-- Login
   +-- User validation
   +-- JWT
   +-- Business APIs
```

However, WorkSphere follows a microservices architecture.

We have multiple independent services:

```text
                 WorkSphere
                     |
       +-------------+-------------+
       |             |             |
       v             v             v
 Employee        Payroll       Department
 Service         Service        Service
```

If every service implements its own authentication, we would duplicate:

* User authentication
* Password validation
* JWT generation
* JWT validation
* Security configuration
* User-related database logic

This creates unnecessary duplication and makes the system harder to maintain.

Therefore, authentication is separated into a dedicated service.

---

## 4. Centralized Authentication Approach

WorkSphere uses a dedicated:

```text
Auth Service
```

for authentication.

The responsibilities are separated as follows:

```text
Auth Service
    |
    +-- Authenticate user
    +-- Validate username/password
    +-- Generate JWT
    +-- Return JWT

API Gateway
    |
    +-- Receive client request
    +-- Validate JWT
    +-- Route request
    +-- Reject unauthorized requests

Employee Service
    |
    +-- Employee business logic
    +-- Employee database operations
```

This separation follows the **single responsibility principle**.

The Employee Service should concentrate on employee functionality instead of becoming responsible for user authentication.

---

## 5. Previous Authentication Approach

Before introducing the dedicated Auth Service, authentication-related code existed inside the Employee Service.

The Employee Service contained components responsible for:

```text
User
 |
 +-- AppUser
 +-- AppUserRepository
 +-- CustomUserDetailsService
 +-- AuthenticationService
 +-- AuthenticationController
 +-- AuthenticationRequest
 +-- AuthenticationResponse
 +-- JwtService
 +-- JwtAuthenticationFilter
 +-- SecurityConfig
 +-- DataInitializer
```

This meant that Employee Service was responsible for two different domains:

```text
Employee Service
      |
      +-- Employee management
      |
      +-- User authentication
```

Although this worked, it was not ideal for the microservices architecture we were building.

---

## 6. Why We Moved Authentication

The authentication implementation was moved out of Employee Service so that authentication could become a separate concern.

The new structure is:

```text
                 Client
                   |
                   v
             API Gateway
               :8080
                   |
          +--------+--------+
          |                 |
          v                 v
    Auth Service      Employee Service
       :8085               :8081
          |
          v
       MySQL
```

The Auth Service handles authentication.

The Employee Service handles employee operations.

The API Gateway becomes the entry point through which clients access the microservices.

---

## 7. Authentication vs Authorization

Authentication and authorization are related but different concepts.

### Authentication

Authentication answers:

> **Who are you?**

For example:

```text
username = admin
password = Admin@123
```

The Auth Service verifies these credentials.

If they are correct, a JWT is generated.

---

### Authorization

Authorization answers:

> **What are you allowed to access?**

For example:

```text
ADMIN
    |
    +-- Create employee
    +-- Update employee
    +-- Delete employee

USER
    |
    +-- View employee
```

Authentication must generally happen before authorization can be performed.

The current WorkSphere implementation establishes the authentication and JWT foundation. Role-based authorization can be extended later.

---

## 8. High-Level Authentication Flow

The complete authentication process consists of two major stages.

### Stage 1 — Login

The client sends credentials:

```text
Client
   |
   | username + password
   v
API Gateway
   |
   v
Auth Service
   |
   | Validate credentials
   |
   | Generate JWT
   v
Client
```

The client receives the JWT.

---

### Stage 2 — Access Protected API

The client then sends the JWT:

```text
Client
   |
   | Authorization: Bearer <JWT>
   v
API Gateway
   |
   | Validate JWT
   |
   +---- Invalid ----> 401 Unauthorized
   |
   +---- Valid
          |
          v
   Employee Service
```

This means the client does not send the username and password for every API request.

Instead, the JWT represents the authenticated session context.

---

## 9. Why JWT Is Used

JWT stands for:

```text
JSON Web Token
```

JWT is commonly used for stateless authentication in REST APIs and microservices.

After successful login, the Auth Service creates a signed token.

The client stores that token and sends it with subsequent requests.

For example:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The Gateway can then validate the token and determine whether the request should be allowed.

---

## 10. Stateless Authentication

The WorkSphere authentication flow is designed to be stateless.

Instead of maintaining a server-side HTTP session:

```text
Client
   |
   | JWT
   v
Gateway
   |
   | Validate token
   v
Service
```

Each request contains the authentication information required to validate the request.

This is particularly useful in microservices because requests can be routed between service instances without depending on a particular server-side session.

---

## 11. Components Used

The authentication implementation uses several Spring components.

### Spring Security

Used for:

* Authentication
* Security configuration
* Password encoding
* Security filters
* Authentication management

---

### BCrypt

Used for securely hashing passwords.

The password should not be stored as:

```text
Admin@123
```

Instead, the database stores a BCrypt hash.

Conceptually:

```text
Plain Password
      |
      v
 BCryptPasswordEncoder
      |
      v
Hashed Password
      |
      v
Database
```

During login, Spring Security compares the supplied password against the stored BCrypt hash.

---

### JWT

JWT is used as the authentication token returned after successful login.

```text
Username + Password
        |
        v
Authentication
        |
        v
       JWT
```

---

### Spring Cloud Gateway

The API Gateway acts as the entry point for client requests.

It is responsible for:

* Routing
* Service discovery
* JWT validation
* Rejecting unauthorized requests

---

### Eureka

Eureka provides service discovery.

The Gateway does not need to hardcode every service's host and port.

Instead:

```text
Gateway
   |
   v
Eureka
   |
   +-- AUTH-SERVICE
   +-- EMPLOYEE-SERVICE
   +-- API-GATEWAY
```

The Gateway can discover the available service instances dynamically.

---

## 12. Current Service Ports

For local WorkSphere development, the services are running on:

| Service          |   Port | Responsibility                  |
| ---------------- | -----: | ------------------------------- |
| Eureka Server    | `8761` | Service discovery               |
| API Gateway      | `8080` | Routing + JWT validation        |
| Auth Service     | `8085` | Authentication + JWT generation |
| Employee Service | `8081` | Employee business logic         |

The client should normally communicate with the Gateway:

```text
http://localhost:8080
```

rather than directly accessing individual microservices.

---

## 13. Final Responsibility Separation

The important design decision is the separation of responsibilities:

```text
+------------------+----------------------------------+
| Component        | Responsibility                   |
+------------------+----------------------------------+
| Eureka Server    | Service discovery                |
| Auth Service     | Authentication + JWT generation  |
| API Gateway      | JWT validation + routing         |
| Employee Service | Employee business functionality  |
+------------------+----------------------------------+
```

This gives WorkSphere a cleaner foundation for adding authentication to additional microservices.

In the next section, we will move from the **concept** to the actual implementation of the Auth Service and understand how the login request reaches the Auth Service and how the JWT is generated.

# 27. Authentication & JWT Security

# Part 2 — Auth Service Implementation

## 14. Auth Service

The Auth Service is a dedicated microservice responsible for handling user authentication in WorkSphere.

Its main responsibilities are:

* Accept login requests
* Find the user from the database
* Validate the supplied password
* Authenticate the user using Spring Security
* Generate a JWT after successful authentication
* Return the JWT to the client

The Auth Service runs independently from the Employee Service.

```text
Auth Service
Port: 8085
```

The basic flow is:

```text
Client
   |
   | username + password
   v
Auth Service
   |
   +-- Find user
   |
   +-- Validate password
   |
   +-- Authenticate
   |
   +-- Generate JWT
   |
   v
Client
```

---

## 15. Auth Service Module

A separate Maven module was created for authentication:

```text
auth-service/
```

The module follows the same multi-module Maven structure used by the rest of WorkSphere.

```text
worksphere-parent
│
├── common-library
├── employee-service
├── department-service
├── payroll-service
├── auth-service
└── api-gateway
```

The parent project manages the common dependency and version configuration.

---

## 16. Auth Service Package Structure

The authentication implementation is organized into separate layers.

```text
auth-service
└── src
    └── main
        └── java
            └── com.worksphere.auth
                │
                ├── AuthServiceApplication
                │
                ├── controller
                │   └── AuthenticationController
                │
                ├── dto
                │   ├── request
                │   │   └── AuthenticationRequest
                │   │
                │   └── response
                │       └── AuthenticationResponse
                │
                ├── entity
                │   └── AppUser
                │
                ├── repository
                │   └── AppUserRepository
                │
                ├── security
                │   ├── CustomUserDetailsService
                │   ├── JwtService
                │   └── JwtAuthenticationFilter
                │
                ├── service
                │   └── AuthenticationService
                │
                └── config
                    └── SecurityConfig
```

Each component has a specific responsibility.

---

# 17. AppUser Entity

The `AppUser` entity represents an authenticated WorkSphere user.

The entity is mapped to the database table:

```text
app_users
```

The main information stored for a user includes:

```text
id
username
password
role
enabled
```

Conceptually:

```text
AppUser
│
├── id
├── username
├── password
├── role
└── enabled
```

The password stored in the database is not the original plain-text password.

Instead, it is stored as a BCrypt hash.

For example:

```text
User enters:
Admin@123

Database:
$2a$10$................................
```

This prevents the application from storing user passwords in plain text.

---

# 18. AppUserRepository

The repository provides database access for users.

Conceptually:

```java
public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
```

The important operation is:

```text
findByUsername()
```

During login, the username supplied by the client is used to retrieve the corresponding user.

The flow is:

```text
username
   |
   v
AppUserRepository
   |
   v
app_users table
   |
   v
AppUser
```

Spring Data JPA generates the required query automatically.

---

# 19. Authentication Request DTO

The client should not directly send an entity object to the authentication API.

Instead, a request DTO is used.

The authentication request contains the credentials required for login.

```text
AuthenticationRequest
│
├── username
└── password
```

Example request:

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

This request is sent to the authentication endpoint.

---

# 20. Authentication Response DTO

After successful authentication, the Auth Service returns an authentication response.

The important information returned to the client is the JWT.

Conceptually:

```text
AuthenticationResponse
│
└── token
```

Example:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

The client then uses this token for subsequent protected API requests.

---

# 21. CustomUserDetailsService

Spring Security needs a way to load a user.

For WorkSphere, the application uses a custom implementation of:

```text
UserDetailsService
```

The responsibility of `CustomUserDetailsService` is:

```text
username
   |
   v
AppUserRepository
   |
   v
AppUser
   |
   v
Spring Security UserDetails
```

The important method is:

```java
loadUserByUsername(String username)
```

The method:

1. Receives the username.
2. Searches the database.
3. Retrieves the `AppUser`.
4. Converts it into Spring Security's `UserDetails`.
5. Throws an exception if the user does not exist.

This allows Spring Security to use our database users during authentication.

---

# 22. Password Validation

Passwords are validated using Spring Security together with BCrypt.

The application configures:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

The important point is that the application does not compare passwords manually.

Instead:

```text
Client Password
      |
      v
Spring Security
      |
      v
BCryptPasswordEncoder
      |
      v
Stored BCrypt Hash
```

If the password matches the stored hash, authentication succeeds.

Otherwise, authentication fails.

---

# 23. AuthenticationManager

The Auth Service uses Spring Security's:

```text
AuthenticationManager
```

to perform authentication.

It is obtained through:

```java
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration)
        throws Exception {

    return configuration.getAuthenticationManager();
}
```

The `AuthenticationManager` coordinates the authentication process.

Conceptually:

```text
AuthenticationManager
        |
        v
UserDetailsService
        |
        v
Load user
        |
        v
PasswordEncoder
        |
        v
Validate password
```

If the credentials are valid, authentication is considered successful.

---

# 24. AuthenticationService

The `AuthenticationService` contains the application's authentication logic.

The login process is conceptually:

```text
AuthenticationRequest
        |
        v
AuthenticationService
        |
        v
AuthenticationManager
        |
        +-- Load UserDetails
        |
        +-- Validate password
        |
        v
Authentication successful
        |
        v
JwtService
        |
        v
JWT
```

This keeps authentication logic out of the controller.

The controller is responsible for handling HTTP requests, while the service performs the actual authentication operation.

---

# 25. AuthenticationController

The `AuthenticationController` exposes the login API.

A typical endpoint is:

```http
POST /api/v1/auth/login
```

The client sends:

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

The controller passes the request to:

```text
AuthenticationService
```

The service authenticates the user and generates the JWT.

The controller then returns the authentication response.

---

# 26. Complete Login Flow

The complete login flow inside the Auth Service is:

```text
Client
  |
  | POST /api/v1/auth/login
  | username + password
  v
AuthenticationController
  |
  v
AuthenticationService
  |
  v
AuthenticationManager
  |
  v
CustomUserDetailsService
  |
  v
AppUserRepository
  |
  v
MySQL app_users
  |
  v
User found
  |
  v
BCrypt password validation
  |
  +---- Invalid ----> Authentication failure
  |
  +---- Valid
          |
          v
      JwtService
          |
          v
       JWT Token
          |
          v
AuthenticationResponse
          |
          v
        Client
```

This is the most important flow to understand before moving to JWT validation.

---

# 27. JWT Generation

After successful authentication, the Auth Service uses `JwtService` to generate the token.

The JWT contains information about the authenticated user.

Conceptually:

```text
Authenticated User
       |
       v
    JwtService
       |
       v
 JWT generated
```

The token is digitally signed using a secret key.

The signature allows the receiving component to verify that the token was generated by a trusted issuer and has not been modified.

---

# 28. Why JWT Is Generated by Auth Service

The Auth Service is responsible for proving the user's identity.

Therefore, it is the appropriate place to generate the JWT.

The responsibilities are separated:

```text
Auth Service
    |
    +-- Authenticate user
    +-- Generate JWT

Gateway
    |
    +-- Validate JWT
    +-- Route request

Employee Service
    |
    +-- Employee operations
```

The Employee Service does not need to authenticate the user's username and password again.

---

# 29. Auth Service Database

The authentication data is stored separately from the Employee Service data.

The Auth Service connects to its authentication database and maintains the `app_users` table.

During application startup, Hibernate/JPA initializes the entity mapping.

The login process then performs a query similar to:

```sql
SELECT
    id,
    enabled,
    password,
    role,
    username
FROM app_users
WHERE username = ?;
```

This was also visible in the Auth Service startup/log output during our implementation.

---

# 30. Authentication Service Responsibility Summary

At this stage, the Auth Service has the following responsibility:

```text
+---------------------------------------------+
|               AUTH SERVICE                  |
+---------------------------------------------+
| Receive login request                       |
| Find user                                   |
| Validate password                           |
| Authenticate using Spring Security          |
| Generate JWT                                |
| Return JWT                                  |
+---------------------------------------------+
```

The Auth Service does **not** contain employee business logic.

This separation makes the authentication component reusable for other WorkSphere services.

---

# 31. Local Auth Service Configuration

For local development, the Auth Service runs on:

```yaml
server:
  port: 8085
```

Its application name is:

```yaml
spring:
  application:
    name: auth-service
```

The service is also registered with Eureka:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

Therefore, when the Auth Service starts, it registers itself with Eureka.

The overall local environment becomes:

```text
Eureka Server
localhost:8761
       |
       +-------------------+
       |                   |
       v                   v
AUTH-SERVICE         EMPLOYEE-SERVICE
localhost:8085       localhost:8081
       |
       v
    MySQL
```

---

# 32. First Verification

Before integrating the Auth Service with the API Gateway, we verified that authentication worked independently.

The Auth Service was started using:

```bash
mvn -pl auth-service spring-boot:run
```

The service started on:

```text
http://localhost:8085
```

The login API was tested directly.

A successful login returned a JWT.

This confirmed that:

* Auth Service was running.
* MySQL connection was working.
* `app_users` data was available.
* Spring Security was loading the user.
* BCrypt password validation was working.
* JWT generation was working.

Only after this independent authentication flow was working did we integrate the Auth Service with the API Gateway.

---

# 33. Important Design Decision

The most important architectural change in this stage was:

```text
BEFORE

Employee Service
 ├── Employee APIs
 ├── User authentication
 ├── JWT generation
 └── JWT validation


AFTER

Auth Service
 ├── User authentication
 └── JWT generation

        +

API Gateway
 ├── JWT validation
 └── Request routing

        +

Employee Service
 └── Employee APIs
```

This separation prepares WorkSphere for a centralized authentication architecture where additional services can be protected without duplicating the complete login implementation.

The next part explains the **JWT itself**, how the token is structured, how the API Gateway validates it, and how the request flows from the client through the Gateway to Employee Service.

# 27. Authentication & JWT Security

# Part 3 — JWT Generation and Token Flow

## 34. What Is JWT?

JWT stands for **JSON Web Token**.

It is a compact, signed token commonly used to transfer authentication information between a client and backend services.

In WorkSphere, JWT is generated after successful authentication.

The basic flow is:

```text
Username + Password
        |
        v
   Auth Service
        |
        | Authentication successful
        v
      JWT
        |
        v
      Client
```

The client then uses this JWT when accessing protected APIs.

---

# 35. Why WorkSphere Uses JWT

In a microservices architecture, we do not want the client to send username and password to every service.

Instead, the user authenticates once.

```text
Client
   |
   | username + password
   v
Auth Service
   |
   | successful authentication
   v
JWT Token
   |
   v
Client
```

For subsequent requests:

```text
Client
   |
   | JWT
   v
API Gateway
   |
   | validate JWT
   v
Microservice
```

This provides a cleaner separation between authentication and business services.

---

# 36. JWT Structure

A JWT normally consists of three parts:

```text
Header.Payload.Signature
```

They are separated using periods:

```text
xxxxx.yyyyy.zzzzz
```

For example:

```text
eyJhbGciOiJIUzI1NiJ9
.
eyJzdWIiOiJhZG1pbiJ9
.
signature
```

The three components are:

```text
Header
   +
Payload
   +
Signature
```

---

# 37. JWT Header

The header contains information about the token.

One important field is the signing algorithm.

For example:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Where:

```text
alg = signing algorithm
typ = token type
```

In our implementation, the JWT is signed using a configured secret key and signing algorithm.

The header tells the JWT library how the token was signed.

---

# 38. JWT Payload

The payload contains claims.

Claims are pieces of information associated with the authenticated user.

A simplified example could be:

```json
{
  "sub": "admin",
  "role": "ADMIN",
  "iat": 1754540000,
  "exp": 1754543600
}
```

Common claims include:

| Claim  | Meaning                  |
| ------ | ------------------------ |
| `sub`  | Subject/user identity    |
| `iat`  | Issued-at time           |
| `exp`  | Expiration time          |
| `role` | User role, when included |

The exact claims depend on what is configured in `JwtService`.

---

# 39. JWT Signature

The signature is what allows the application to verify the integrity of the token.

Conceptually:

```text
Header
   +
Payload
   +
Secret Key
   |
   v
Signature
```

If someone modifies the payload after the token has been generated, the signature will no longer match.

Therefore, the Gateway can detect that the token is invalid.

---

# 40. JWT Is Encoded, Not Encrypted

An important concept is that a normal JWT payload is **encoded**, not encrypted.

For example, someone who obtains a JWT can decode the header and payload.

Therefore, sensitive information such as:

```text
password
credit card number
secret information
```

should never be placed inside the JWT payload.

The JWT should contain only information required for authentication and authorization.

---

# 41. JWT Generation in WorkSphere

After Spring Security successfully authenticates the user, the Auth Service calls:

```text
JwtService
```

The responsibility of `JwtService` is to create the JWT.

Conceptually:

```text
Authentication successful
        |
        v
    JwtService
        |
        +-- Create claims
        |
        +-- Set subject
        |
        +-- Set expiration
        |
        +-- Sign token
        |
        v
      JWT
```

The generated JWT is then returned through the `AuthenticationResponse`.

---

# 42. JWT Subject

The authenticated user is generally represented using the JWT subject claim:

```text
sub
```

For example:

```json
{
  "sub": "admin"
}
```

This allows the receiving component to identify which user the token belongs to.

The Gateway does not need to ask the client for the username again.

The identity can be extracted from the validated JWT.

---

# 43. JWT Expiration

JWTs should not remain valid forever.

An expiration time is therefore associated with the token.

Conceptually:

```text
Token Created
     |
     | valid
     |
     v
Expiration Time
     |
     | token expired
     v
Request rejected
```

If an expired token is sent to the Gateway, the request should not be forwarded to the protected service.

The client must authenticate again and obtain a new token.

---

# 44. Bearer Token

When calling a protected API, the JWT is normally sent using the HTTP `Authorization` header.

The format is:

```http
Authorization: Bearer <JWT>
```

For example:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

`Bearer` means that the client presenting the token is attempting to use that token as proof of authentication.

---

# 45. Login Request

The client first calls the login endpoint.

For our WorkSphere setup, the Gateway exposes the Auth Service through service discovery.

The request is:

```http
POST http://localhost:8080/auth-service/api/v1/auth/login
```

Request body:

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

The request flow is:

```text
Client
  |
  | POST /auth-service/api/v1/auth/login
  v
API Gateway
  |
  | Discover AUTH-SERVICE
  v
Eureka
  |
  v
Auth Service :8085
  |
  | Authenticate
  v
JWT generated
  |
  v
Client
```

The client receives the JWT in the response.

---

# 46. Why the Gateway Can Find Auth Service

The Gateway uses Eureka for service discovery.

Auth Service registers itself using:

```yaml
spring:
  application:
    name: auth-service
```

and:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

After registration, Eureka maintains information about the Auth Service instance.

Conceptually:

```text
Eureka
   |
   +-- API-GATEWAY :8080
   |
   +-- AUTH-SERVICE :8085
   |
   +-- EMPLOYEE-SERVICE :8081
```

The Gateway can therefore locate `AUTH-SERVICE` without hardcoding:

```text
localhost:8085
```

into every route.

---

# 47. Gateway Discovery Locator

The API Gateway was configured with:

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

The discovery locator allows Gateway to automatically create routes based on services registered with the discovery server.

Therefore, a service registered as:

```text
AUTH-SERVICE
```

can be accessed using:

```text
/auth-service/**
```

Similarly:

```text
EMPLOYEE-SERVICE
```

can be accessed using:

```text
/employee-service/**
```

This is why our requests can use:

```text
http://localhost:8080/auth-service/...
```

and:

```text
http://localhost:8080/employee-service/...
```

---

# 48. Protected Employee Request

After receiving the JWT, the client can call:

```http
GET http://localhost:8080/employee-service/api/v1/employees/1
```

with:

```http
Authorization: Bearer <JWT>
```

The request flow is:

```text
Client
  |
  | GET /employee-service/api/v1/employees/1
  | Authorization: Bearer JWT
  v
API Gateway
  |
  | Validate JWT
  |
  +-------- Invalid --------> 401 Unauthorized
  |
  |
  +-------- Valid
              |
              v
        EMPLOYEE-SERVICE
              |
              v
       EmployeeController
              |
              v
       EmployeeService
              |
              v
          Database
```

---

# 49. Request Without JWT

If the protected endpoint is accessed without a token:

```http
GET http://localhost:8080/employee-service/api/v1/employees/1
```

the Gateway should reject the request.

The expected response is:

```text
HTTP 401 Unauthorized
```

The request should not reach Employee Service.

This is an important security boundary.

```text
Client
  |
  | No JWT
  v
Gateway
  |
  X
  |
  401 Unauthorized
```

---

# 50. Request With Invalid JWT

If the client sends an invalid or modified token:

```http
Authorization: Bearer invalid-token
```

the Gateway should also reject the request.

```text
Client
   |
   | Invalid JWT
   v
Gateway
   |
   | JWT validation failed
   v
401 Unauthorized
```

Employee Service should not receive the request.

---

# 51. Request With Valid JWT

If the client sends a valid JWT:

```http
Authorization: Bearer <valid-token>
```

the Gateway validates it.

If validation succeeds:

```text
Client
   |
   | Valid JWT
   v
Gateway
   |
   | Authentication successful
   v
Employee Service
   |
   v
200 OK
```

This is the flow we verified during the WorkSphere implementation.

---

# 52. Authentication and Routing Are Different Responsibilities

One important concept from our implementation is that **authentication and routing are separate operations**.

The Gateway performs both, but they solve different problems.

### Authentication

Answers:

```text
Is this request authenticated?
```

JWT validation answers this question.

### Routing

Answers:

```text
Where should this request go?
```

Eureka and Spring Cloud Gateway answer this question.

Therefore:

```text
                    API Gateway
                         |
              +----------+----------+
              |                     |
              v                     v
       JWT Validation           Routing
              |                     |
              v                     v
       Is token valid?       Which service?
              |                     |
              +----------+----------+
                         |
                         v
                  Target Service
```

---

# 53. Why Auth Service Does Not Validate Every Employee Request

The Auth Service is responsible for **login and token generation**.

Once the JWT has been generated, the Gateway can validate the token for subsequent requests.

Therefore, a normal employee request does not need to go back to the Auth Service for every request.

Without this design:

```text
Every request
     |
     v
Auth Service
     |
     v
Employee Service
```

This would introduce unnecessary communication with the Auth Service.

With JWT:

```text
Every request
     |
     v
API Gateway
     |
     | Validate JWT locally
     v
Employee Service
```

This reduces unnecessary authentication calls.

---

# 54. Stateless JWT Flow

The final authentication model is therefore:

```text
                    +----------------+
                    |   Auth Service |
                    |     :8085      |
                    +-------+--------+
                            |
                       Generate JWT
                            |
                            v
                         Client
                            |
                    Authorization Header
                            |
                            v
                    +-------+--------+
                    |  API Gateway   |
                    |     :8080      |
                    +-------+--------+
                            |
                       Validate JWT
                            |
                  +---------+---------+
                  |                   |
                Invalid             Valid
                  |                   |
                  v                   v
             401 Response      Service Discovery
                                      |
                                      v
                              Employee Service
                                   :8081
```

This is the core JWT security architecture implemented in WorkSphere.

---

# 55. Important Security Boundary

The Gateway is now the main entry point for client requests.

Therefore, the intended production flow is:

```text
Client
   |
   v
API Gateway
   |
   +---- Authentication
   |
   +---- Routing
   |
   v
Microservices
```

The client should not normally bypass the Gateway and directly access:

```text
http://localhost:8081
```

or:

```text
http://localhost:8085
```

In a production deployment, the individual services can be placed on an internal network so that external clients cannot directly reach them.

---

# 56. JWT Flow Summary

The complete process can be summarized as:

```text
1. User submits username/password.

2. Request reaches Auth Service.

3. Auth Service loads the user from MySQL.

4. BCrypt validates the password.

5. Spring Security authenticates the user.

6. JwtService generates a signed JWT.

7. JWT is returned to the client.

8. Client stores/uses the JWT.

9. Client sends JWT in Authorization header.

10. Request reaches API Gateway.

11. Gateway validates JWT.

12. Invalid JWT -> 401 Unauthorized.

13. Valid JWT -> request is routed.

14. Eureka helps Gateway locate the target service.

15. Employee Service processes the business request.

16. Response is returned through the Gateway.
```

This completes the JWT generation and request-flow portion of the authentication implementation.

# 27. Authentication & JWT Security

# Part 4 — API Gateway Security Implementation

## 57. Why JWT Validation Was Moved to API Gateway

After creating the Auth Service, the next architectural step was to move JWT validation to the API Gateway.

Previously, Employee Service contained authentication-related code such as:

```text
SecurityConfig
JwtAuthenticationFilter
JwtService
CustomUserDetailsService
AuthenticationService
AuthenticationController
AppUser
AppUserRepository
```

This meant Employee Service was responsible for both:

```text
Employee Service
      |
      +-- Employee business logic
      |
      +-- Authentication
      |
      +-- JWT validation
```

After introducing the Auth Service and Gateway security, the responsibility became:

```text
Auth Service
      |
      +-- User authentication
      +-- Password validation
      +-- JWT generation

API Gateway
      |
      +-- JWT validation
      +-- Security filtering
      +-- Request routing

Employee Service
      |
      +-- Employee business logic
```

This gives each component a clearer responsibility.

---

# 58. Gateway Security Flow

The API Gateway is now the security entry point for protected requests.

The high-level flow is:

```text
Client
   |
   | Authorization: Bearer JWT
   v
API Gateway
   |
   v
JwtAuthenticationFilter
   |
   v
JwtService
   |
   | Validate JWT
   |
   +------ Invalid ------> 401 Unauthorized
   |
   +------ Valid
             |
             v
       SecurityContext
             |
             v
        Gateway Routing
             |
             v
      Employee Service
```

The important point is that **Employee Service does not need to authenticate the username/password again**.

---

# 59. Gateway Security Classes

Two main security classes were introduced into the Gateway.

```text
api-gateway
└── security
    ├── JwtAuthenticationFilter
    └── JwtService
```

Their responsibilities are different.

### `JwtAuthenticationFilter`

Responsible for:

* Intercepting incoming requests
* Reading the `Authorization` header
* Extracting the Bearer token
* Passing the token to `JwtService`
* Validating the token
* Rejecting invalid requests
* Allowing valid requests to continue

### `JwtService`

Responsible for:

* Understanding the JWT
* Parsing the JWT
* Verifying the signature
* Checking token validity
* Extracting claims such as username

Therefore:

```text
JwtAuthenticationFilter
        |
        | "I received a token"
        v
JwtService
        |
        | "Is this token valid?"
        v
true / false
```

---

# 60. Why We Need a Gateway Filter

Spring Cloud Gateway is built on the reactive stack.

The Gateway receives requests before they reach the downstream microservices.

Therefore, a filter is an ideal place to perform security checks.

Conceptually:

```text
Incoming Request
       |
       v
Gateway Filter
       |
       +---- Security validation
       |
       v
Routing
       |
       v
Downstream Service
```

The security filter acts as a checkpoint.

---

# 61. JwtAuthenticationFilter

The `JwtAuthenticationFilter` is responsible for intercepting requests and checking whether they contain a JWT.

The filter follows this basic process:

```text
1. Receive request

2. Read Authorization header

3. Check whether header exists

4. Check whether it starts with "Bearer "

5. Extract JWT

6. Validate JWT

7. If valid → continue

8. If invalid → reject request
```

Conceptually:

```text
Request
   |
   v
Authorization Header?
   |
   +---- No ----> Continue/Reject based on endpoint policy
   |
   +---- Yes
          |
          v
    Bearer token?
          |
          +---- No ----> Reject
          |
          +---- Yes
                 |
                 v
            Extract JWT
                 |
                 v
            Validate JWT
```

---

# 62. Authorization Header

The JWT is sent using the HTTP `Authorization` header.

The expected format is:

```http
Authorization: Bearer <JWT>
```

For example:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The filter first reads this header.

Conceptually:

```java
String authHeader =
        exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
```

The filter then checks whether the header starts with:

```text
Bearer
```

---

# 63. Extracting the JWT

The `Bearer` prefix is not part of the JWT itself.

Therefore, the filter removes it.

For example:

```text
Authorization Header

Bearer eyJhbGciOiJIUzI1NiJ9...
```

becomes:

```text
JWT

eyJhbGciOiJIUzI1NiJ9...
```

Conceptually:

```java
String token = authHeader.substring(7);
```

because:

```text
"Bearer " = 7 characters
```

The resulting value is passed to `JwtService`.

---

# 64. JwtService in Gateway

The `JwtService` contains the JWT-specific logic.

The Gateway must be able to verify the token using the same signing configuration used when the token was generated.

The basic concept is:

```text
Auth Service
     |
     | Secret Key
     v
Generate JWT
     |
     v
Client
     |
     | JWT
     v
Gateway
     |
     | Same Secret Key
     v
Validate JWT
```

The Gateway does not need the user's password.

It only needs the information required to verify the signed token.

---

# 65. JWT Signature Validation

When Auth Service generates the JWT, it signs it.

Later, the Gateway verifies the signature.

Conceptually:

```text
Auth Service
     |
     | Header + Payload + Secret
     v
   Signature
```

When the Gateway receives the token:

```text
Gateway
   |
   | Header + Payload + Secret
   v
Expected Signature
```

The Gateway compares the token's signature with the signature generated during validation.

If they do not match:

```text
Invalid JWT
```

and the request should be rejected.

---

# 66. Why the Same Secret Is Important

For the current WorkSphere implementation using a shared signing secret, Auth Service and Gateway must use compatible JWT signing configuration.

For example:

```text
Auth Service
    |
    | JWT_SECRET
    v
Generate JWT
```

and:

```text
API Gateway
    |
    | JWT_SECRET
    v
Validate JWT
```

If the secrets are different:

```text
Auth Service Secret
        !=
Gateway Secret
```

then the Gateway cannot validate the token generated by Auth Service.

The result is an authentication failure.

---

# 67. Token Expiration Validation

The Gateway should also validate the token's expiration.

Conceptually:

```text
JWT
 |
 +-- Signature valid?
 |
 +-- Token expired?
 |
 +-- Claims valid?
 |
 v
Valid / Invalid
```

If the token has expired:

```text
JWT
 |
 v
Expired
 |
 v
401 Unauthorized
```

The client must authenticate again and obtain a new token.

---

# 68. Security Context

After a JWT is successfully validated, the authenticated user's identity can be represented using Spring Security's security context.

Conceptually:

```text
JWT
 |
 | validate
 v
Username / Claims
 |
 v
Authentication
 |
 v
SecurityContext
```

The security context represents the authenticated request.

This allows security-related information to be associated with the current request processing.

---

# 69. Valid JWT Flow

When the client sends a valid JWT:

```text
Client
   |
   | Authorization: Bearer JWT
   v
Gateway
   |
   v
JwtAuthenticationFilter
   |
   v
JwtService
   |
   | valid
   v
SecurityContext
   |
   v
Gateway Routing
   |
   v
Employee Service
```

The downstream Employee API can then process the request.

---

# 70. Invalid JWT Flow

If the client sends an invalid token:

```text
Client
   |
   | Authorization: Bearer invalid-token
   v
Gateway
   |
   v
JwtAuthenticationFilter
   |
   v
JwtService
   |
   | invalid
   v
401 Unauthorized
```

The request does not continue to Employee Service.

This is important because the Gateway acts as the security boundary.

---

# 71. Missing JWT

If a protected API is called without the Authorization header:

```http
GET /employee-service/api/v1/employees/1
```

instead of:

```http
Authorization: Bearer <JWT>
```

the request should not be allowed to access the protected resource.

Expected result:

```text
401 Unauthorized
```

The flow becomes:

```text
Client
   |
   | No JWT
   v
Gateway
   |
   X
   |
401 Unauthorized
```

---

# 72. Public vs Protected Endpoints

Not every endpoint necessarily requires authentication.

For example, the login endpoint must be accessible without an existing JWT.

Otherwise, there would be a circular dependency:

```text
Need JWT to login
        |
        v
Cannot get JWT
        |
        v
Cannot login
```

Therefore, authentication endpoints such as:

```text
/api/v1/auth/login
```

are treated as public endpoints.

Protected business APIs require authentication.

Conceptually:

```text
/auth-service/api/v1/auth/login
              |
              v
           PUBLIC


/employee-service/api/v1/employees/**
              |
              v
          PROTECTED
```

---

# 73. Why Login Works Without JWT

When we tested:

```http
POST http://localhost:8080/auth-service/api/v1/auth/login
```

we did not already have a JWT.

That is expected.

The request is routed to Auth Service, where the username and password are validated.

If authentication succeeds:

```text
username + password
       |
       v
Auth Service
       |
       v
JWT
```

The JWT is returned to the client.

The JWT is then used for subsequent protected requests.

---

# 74. Protected Employee API

After obtaining the JWT, we tested:

```http
GET http://localhost:8080/employee-service/api/v1/employees/1
```

with:

```http
Authorization: Bearer <JWT>
```

The flow is:

```text
Client
   |
   | JWT
   v
API Gateway :8080
   |
   | Validate JWT
   v
EMPLOYEE-SERVICE
   |
   | Process request
   v
200 OK
```

This demonstrated that the Gateway security layer was working.

---

# 75. Why Direct Employee Access Is Different

During our testing, we also used:

```text
http://localhost:8081/api/v1/employees/1
```

This is a **direct request to Employee Service**.

It bypasses:

```text
API Gateway
JWT validation
Gateway routing
```

Therefore, there is an important distinction:

```text
Gateway URL

http://localhost:8080/employee-service/api/v1/employees/1

        |
        v
JWT validation
        |
        v
Employee Service
```

versus:

```text
Direct URL

http://localhost:8081/api/v1/employees/1

        |
        v
Employee Service directly
```

In the intended architecture, external clients should use the Gateway rather than directly accessing individual microservices.

---

# 76. Removing Authentication From Employee Service

Once Gateway authentication was working, the old authentication implementation was removed from Employee Service.

The authentication-related classes removed included components such as:

```text
DataInitializer
SecurityConfig
AuthenticationController
AuthenticationRequest
AuthenticationResponse
AppUser
AppUserRepository
CustomUserDetailsService
JwtAuthenticationFilter
JwtService
AuthenticationService
```

The purpose of this cleanup was to prevent duplicate authentication logic.

The Employee Service is now focused on:

```text
EmployeeController
       |
       v
EmployeeService
       |
       v
EmployeeServiceImpl
       |
       v
Repository / Database
```

while authentication is handled outside it.

---

# 77. What Employee Service Does Now

The Employee Service should primarily perform employee-related business operations.

For example:

```text
GET    /api/v1/employees
GET    /api/v1/employees/{id}
POST   /api/v1/employees
PUT    /api/v1/employees/{id}
DELETE /api/v1/employees/{id}
```

The security boundary is intended to be placed before these APIs at the Gateway.

Therefore:

```text
Client
   |
   v
Gateway
   |
   | Authentication
   |
   v
Employee Service
   |
   | Business logic
   |
   v
Database
```

---

# 78. Gateway Security Configuration

The Gateway security configuration defines how HTTP requests should be handled.

The important concepts are:

```text
CSRF
Session Management
Authorization Rules
JWT Filter
```

Unlike a traditional browser-based application, the WorkSphere REST APIs use stateless authentication.

Therefore, session-based authentication is not the main authentication mechanism.

---

# 79. Stateless Security

JWT authentication is stateless.

The Gateway does not need to maintain a server-side login session for every user.

Instead:

```text
Request 1
   |
   +-- JWT


Request 2
   |
   +-- JWT


Request 3
   |
   +-- JWT
```

Each request carries the authentication token.

The Gateway validates the token when processing the request.

This works well with horizontally scalable microservices.

---

# 80. Why CSRF Is Disabled

WorkSphere exposes REST APIs rather than relying on traditional browser form authentication.

For this API architecture, CSRF protection is not being used as the primary authentication mechanism.

Therefore, the Gateway's security configuration disables CSRF where applicable.

The important distinction is:

```text
CSRF protection
       !=
JWT authentication
```

JWT validation remains responsible for verifying the request's authentication.

---

# 81. Authentication Responsibility After Refactoring

After completing the refactoring, the responsibilities are:

```text
+------------------+--------------------------------------+
| Component        | Responsibility                       |
+------------------+--------------------------------------+
| Auth Service     | Authenticate user                    |
| Auth Service     | Generate JWT                         |
| API Gateway      | Validate JWT                         |
| API Gateway      | Apply security rules                 |
| API Gateway      | Route request                        |
| Eureka           | Discover service instances           |
| Employee Service | Employee business logic              |
+------------------+--------------------------------------+
```

This is the final responsibility separation for the current authentication implementation.

---

# 82. Complete Gateway Security Flow

The complete flow can now be visualized as:

```text
                         LOGIN
                           |
                           v
                    +-------------+
                    | Auth Service|
                    |    :8085    |
                    +------+------+
                           |
                    Generate JWT
                           |
                           v
                         Client
                           |
                           | Bearer JWT
                           v
                  +--------+--------+
                  |   API Gateway   |
                  |      :8080      |
                  +--------+--------+
                           |
                           v
                JwtAuthenticationFilter
                           |
                           v
                      JwtService
                           |
                 +---------+---------+
                 |                   |
              Invalid              Valid
                 |                   |
                 v                   v
          401 Unauthorized     SecurityContext
                                     |
                                     v
                               Gateway Routing
                                     |
                                     v
                              Eureka Discovery
                                     |
                                     v
                            Employee Service
                               :8081
                                     |
                                     v
                               Business Logic
                                     |
                                     v
                                Response
```

---

# 83. Key Takeaway

The most important architectural change is:

```text
Authentication is no longer the responsibility
of every individual business microservice.
```

Instead:

```text
Auth Service
    |
    | Authentication + JWT generation
    v
Client
    |
    | JWT
    v
API Gateway
    |
    | JWT validation + routing
    v
Business Microservices
```

This gives WorkSphere a centralized authentication boundary while keeping business services focused on their own responsibilities.

The next part will explain **Eureka + Gateway dynamic routing**, including why `/auth-service/**` and `/employee-service/**` work, how service instances are discovered, and the hostname problem we encountered with `DESKTOP-CAAHND3.mshome.net` and `UnknownHostException`.

# 28. Authentication & JWT Security

# Part 5 — Eureka + API Gateway Dynamic Routing

## 84. Why Service Discovery Is Required

In a microservices architecture, the API Gateway should not need to know the fixed IP address and port of every service.

For example, we currently have:

```text
Auth Service       → 8085
Employee Service   → 8081
API Gateway        → 8080
Eureka Server      → 8761
```

A simple approach would be to configure the Gateway like this:

```text
/auth-service/**

        ↓

http://localhost:8085/**


/employee-service/**

        ↓

http://localhost:8081/**
```

However, this becomes difficult to manage when the system grows.

For example:

```text
Employee Service
    |
    +-- Instance 1 → 8081
    +-- Instance 2 → 8082
    +-- Instance 3 → 8083
```

The Gateway should not have to maintain all these addresses manually.

This is where **Eureka Service Discovery** is used.

---

# 85. What Eureka Does

Eureka acts as a service registry.

Services register themselves with Eureka when they start.

Our architecture is:

```text
                    +----------------+
                    | Eureka Server  |
                    |     :8761      |
                    +-------+--------+
                            |
              +-------------+-------------+
              |             |             |
              v             v             v
         AUTH-SERVICE  EMPLOYEE-SERVICE API-GATEWAY
            :8085          :8081          :8080
```

Eureka keeps information about:

```text
Service name
Host
Port
Status
Instance information
```

The Gateway can then ask Eureka:

```text
"Where is EMPLOYEE-SERVICE?"
```

Eureka responds with the registered instance information.

---

# 86. Eureka Server

Our Eureka Server runs on:

```text
http://localhost:8761
```

Its responsibility is service registration and discovery.

The important point is that Eureka does not process the actual employee request.

It only helps services discover each other.

Therefore:

```text
Eureka
   |
   +-- Service Registry
   |
   +-- Service Discovery
```

It is not:

```text
Eureka
   |
   X-- API Gateway
   X-- Authentication
   X-- Employee business logic
```

---

# 87. Service Registration

When Auth Service starts, it registers itself with Eureka.

Conceptually:

```text
Auth Service
     |
     | Register
     v
Eureka Server
```

Similarly:

```text
Employee Service
     |
     | Register
     v
Eureka Server
```

And the Gateway also registers itself:

```text
API Gateway
     |
     | Register
     v
Eureka Server
```

Therefore, the Eureka dashboard can show entries such as:

```text
API-GATEWAY
AUTH-SERVICE
EMPLOYEE-SERVICE
```

---

# 88. Why API Gateway Registers With Eureka

It may initially seem unnecessary for the Gateway itself to register with Eureka.

However, in a service-discovery architecture, the Gateway is also treated as a service.

Therefore:

```text
API Gateway
    |
    +-- Discover services
    |
    +-- Register itself
```

This allows the complete application environment to be managed through service discovery.

---

# 89. Gateway Eureka Configuration

The Gateway contains the Eureka client configuration:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

This tells the Gateway where the Eureka Server is located.

The Gateway can then communicate with Eureka.

---

# 90. Gateway Discovery Locator

Our Gateway configuration contains:

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

The important property is:

```yaml
enabled: true
```

This enables Gateway routes based on service discovery.

Instead of manually defining every service route, the Gateway can create routes from services registered with Eureka.

---

# 91. What `lower-case-service-id` Does

Eureka service names are normally registered using uppercase names such as:

```text
AUTH-SERVICE
EMPLOYEE-SERVICE
API-GATEWAY
```

The Gateway configuration contains:

```yaml
lower-case-service-id: true
```

This allows the service ID to be used in lowercase URLs.

For example:

```text
AUTH-SERVICE
```

can be accessed through:

```text
/auth-service/**
```

and:

```text
EMPLOYEE-SERVICE
```

can be accessed through:

```text
/employee-service/**
```

This makes API URLs easier to use.

---

# 92. Dynamic Route Creation

With discovery locator enabled, the Gateway can dynamically create routes.

For example:

```text
Eureka
 |
 +-- AUTH-SERVICE
 |
 +-- EMPLOYEE-SERVICE
```

The Gateway discovers these services.

Therefore:

```text
/auth-service/**
```

can be routed to:

```text
AUTH-SERVICE
```

and:

```text
/employee-service/**
```

can be routed to:

```text
EMPLOYEE-SERVICE
```

The Gateway does not need a separate hardcoded URL such as:

```text
http://localhost:8085
```

for every service.

---

# 93. Auth Service Request Flow

When we send:

```http
POST http://localhost:8080/auth-service/api/v1/auth/login
```

the request first reaches the Gateway.

```text
Client
   |
   | POST /auth-service/api/v1/auth/login
   v
API Gateway :8080
```

The Gateway identifies:

```text
auth-service
```

as the service ID.

It then uses Eureka to discover:

```text
AUTH-SERVICE
```

The request is routed to the Auth Service instance.

```text
API Gateway
     |
     | Discovery
     v
Eureka
     |
     | AUTH-SERVICE
     v
Auth Service :8085
```

Auth Service validates the credentials and returns the JWT.

---

# 94. Employee Service Request Flow

After receiving the JWT, the client can call:

```http
GET http://localhost:8080/employee-service/api/v1/employees/1
```

The flow is:

```text
Client
   |
   | JWT
   v
API Gateway :8080
   |
   | Validate JWT
   v
Eureka
   |
   | Find EMPLOYEE-SERVICE
   v
Employee Service :8081
   |
   v
Employee Response
```

This combines the two concepts we implemented:

```text
JWT Security
     +
Eureka Service Discovery
```

---

# 95. Complete Authentication + Discovery Flow

The complete request lifecycle is now:

```text
                         LOGIN
                           |
                           v
                    +-------------+
                    | Auth Service|
                    |    :8085    |
                    +------+------+
                           |
                        JWT
                           |
                           v
                         Client
                           |
                           | Bearer JWT
                           v
                  +--------+--------+
                  |   API Gateway   |
                  |      :8080      |
                  +--------+--------+
                           |
                     JWT Validation
                           |
                           v
                       Eureka
                           |
                    Service Discovery
                           |
                           v
                  Employee Service
                     :8081
                           |
                           v
                     Business Logic
```

---

# 96. Why We Do Not Put `localhost:8081` in the Gateway URL

A common question is:

> If Employee Service is running on port 8081, why don't we simply call localhost:8081?

Because that bypasses the Gateway.

Direct request:

```text
Client
   |
   v
Employee Service :8081
```

Gateway request:

```text
Client
   |
   v
Gateway :8080
   |
   v
Eureka
   |
   v
Employee Service :8081
```

The second architecture gives us a central location for:

```text
Authentication
Routing
Logging
Rate Limiting
Circuit Breaking
Security
Monitoring
```

---

# 97. Why Gateway Should Be the External Entry Point

In a production microservices architecture, clients should normally communicate with the Gateway rather than directly accessing internal services.

Instead of:

```text
Client
 |
 +----> Auth Service
 |
 +----> Employee Service
 |
 +----> Department Service
 |
 +----> Payroll Service
```

we want:

```text
                 Client
                   |
                   v
             API Gateway
                   |
       +-----------+-----------+
       |           |           |
       v           v           v
     Auth      Employee    Department
    Service     Service      Service
```

The Gateway becomes the controlled entry point.

---

# 98. The Hostname Problem We Encountered

During our testing, Eureka displayed service instances such as:

```text
DESKTOP-CAAHND3.mshome.net:auth-service:8085
```

and:

```text
DESKTOP-CAAHND3.mshome.net:employee-service:8081
```

At first, this may look strange because we are running everything locally.

The important detail is that Eureka was registering the machine hostname instead of:

```text
localhost
```

The Gateway then tried to use the hostname returned by Eureka.

---

# 99. Why Login Could Work but Routing Could Fail

We encountered an important scenario.

The Auth Service was visible in Eureka and authentication could work, but Gateway routing produced:

```text
java.net.UnknownHostException:
Failed to resolve 'DESKTOP-CAAHND3.mshome.net'
```

This means:

```text
Gateway
   |
   | Ask Eureka for Auth/Employee Service
   v
Eureka
   |
   | Returns
   v
DESKTOP-CAAHND3.mshome.net
   |
   v
Gateway tries DNS resolution
   |
   X
NXDOMAIN
```

The problem was not JWT validation.

The problem was **service discovery resolving the registered hostname**.

This distinction is important.

---

# 100. What `NXDOMAIN` Means

The error contained:

```text
Query failed with NXDOMAIN
```

NXDOMAIN means that DNS could not resolve the requested hostname.

In our case:

```text
DESKTOP-CAAHND3.mshome.net
```

could not be resolved by the Gateway's Netty DNS resolver.

Therefore:

```text
UnknownHostException
```

was generated.

This happened before the actual downstream Employee Service request could be completed.

---

# 101. `prefer-ip-address`

To address hostname-related service discovery problems, we added:

```yaml
eureka:
  instance:
    prefer-ip-address: true
```

The intention is to tell Eureka clients to prefer registering the instance using its IP address instead of its hostname.

Conceptually:

Before:

```text
DESKTOP-CAAHND3.mshome.net
```

After:

```text
192.168.x.x
```

The exact IP depends on the machine and network.

This can avoid problems where the machine hostname cannot be resolved by the Gateway.

---

# 102. Why Eureka Must Be Restarted

Changing:

```yaml
prefer-ip-address: true
```

does not necessarily change an already registered instance immediately in the way we expect.

A service should be restarted so that it registers again with Eureka using the updated configuration.

Therefore, after changing Eureka instance configuration:

```text
Stop Service
     |
     v
Start Service
     |
     v
Register again with Eureka
```

Then check the Eureka dashboard again.

---

# 103. Gateway Configuration-Key Warning

During Gateway startup, we also encountered a warning saying that these configuration keys had been renamed:

```text
spring.cloud.gateway.discovery.locator.enabled
spring.cloud.gateway.discovery.locator.lower-case-service-id
```

The newer configuration namespace suggested by Spring Cloud Gateway was:

```text
spring.cloud.gateway.server.webflux.discovery.locator.enabled
```

and:

```text
spring.cloud.gateway.server.webflux.discovery.locator.lower-case-service-id
```

The application temporarily mapped the old keys for compatibility.

The important lesson is that this was a **configuration migration warning**, not the cause of our `UnknownHostException`.

The Gateway still started successfully.

---

# 104. Gateway Startup Confirmation

A successful Gateway startup contains messages similar to:

```text
Netty started on port 8080
```

and:

```text
Started ApiGatewayApplication
```

The Eureka registration also showed:

```text
registration status: 204
```

This indicates that the Gateway successfully registered with Eureka.

Therefore:

```text
Gateway
   |
   +-- Started
   |
   +-- Registered with Eureka
```

was working correctly.

---

# 105. Eureka Registration vs Routing

These are two different operations.

### Registration

A service tells Eureka:

```text
"I am EMPLOYEE-SERVICE and I am running on this host and port."
```

### Discovery

Gateway asks Eureka:

```text
"Where is EMPLOYEE-SERVICE?"
```

### Routing

Gateway then sends the actual HTTP request to the discovered instance.

Therefore:

```text
Registration
      ↓
Discovery
      ↓
Routing
```

A service appearing in Eureka does **not automatically mean** that every routing request will succeed.

The discovered host and port must also be reachable.

---

# 106. Why We Were Seeing `AUTH-SERVICE` and `API-GATEWAY`

The Eureka dashboard showed services such as:

```text
API-GATEWAY
AUTH-SERVICE
```

This means the services successfully registered.

Later, when Employee Service was also running, it appeared as:

```text
EMPLOYEE-SERVICE
```

Therefore, the expected local environment is:

```text
EUREKA-SERVER
     |
     +-- API-GATEWAY
     +-- AUTH-SERVICE
     +-- EMPLOYEE-SERVICE
```

---

# 107. Service Ports in WorkSphere

Our current local development setup uses:

```text
Eureka Server
    8761

API Gateway
    8080

Employee Service
    8081

Auth Service
    8085
```

The flow is therefore:

```text
localhost:8761
       |
       | Service Registry
       |
       +-------------------------+
       |                         |
localhost:8080             Services
       |                         |
       |                    +----+----+
       |                    |         |
       v                    v         v
 Gateway:8080          Auth:8085  Employee:8081
```

---

# 108. Gateway URLs

For external API testing, we should use the Gateway.

### Login

```text
POST
http://localhost:8080/auth-service/api/v1/auth/login
```

### Get Employee

```text
GET
http://localhost:8080/employee-service/api/v1/employees/1
```

with:

```http
Authorization: Bearer <JWT>
```

The direct Employee URL:

```text
http://localhost:8081/api/v1/employees/1
```

is useful for internal troubleshooting, but it bypasses the Gateway.

---

# 109. Why Eureka + Gateway Is Better Than Hardcoded URLs

Without service discovery:

```text
Gateway
 |
 +-- http://localhost:8085
 |
 +-- http://localhost:8081
 |
 +-- http://localhost:8082
```

With Eureka:

```text
Gateway
 |
 v
Eureka
 |
 +-- AUTH-SERVICE
 +-- EMPLOYEE-SERVICE
 +-- DEPARTMENT-SERVICE
```

The Gateway can discover the current service instances.

This becomes especially valuable when deploying multiple instances.

---

# 110. Multiple Instances

Suppose Employee Service is scaled:

```text
EMPLOYEE-SERVICE
   |
   +-- Instance 1 → 8081
   +-- Instance 2 → 8082
   +-- Instance 3 → 8083
```

Eureka can maintain these instances.

The Gateway can discover the available instances rather than relying on one hardcoded address.

This is one of the key benefits of service discovery in a microservices architecture.

---

# 111. Final Architecture

After implementing Authentication, JWT validation, Gateway and Eureka, our WorkSphere architecture is:

```text
                         +----------------+
                         |  Eureka Server |
                         |     :8761      |
                         +-------+--------+
                                 |
              +------------------+------------------+
              |                  |                  |
              v                  v                  v
       +-------------+    +-------------+    +-------------+
       | Auth Service|    |Employee     |    | API Gateway |
       |    :8085    |    |Service :8081|    |    :8080    |
       +-------------+    +-------------+    +------+------+
                                                   |
                                                   |
                                               Client
```

The logical request flow is:

```text
Client
   |
   v
API Gateway
   |
   +---- JWT Validation
   |
   +---- Eureka Discovery
   |
   v
Employee Service
   |
   v
Database
```

And login:

```text
Client
   |
   v
API Gateway
   |
   v
Eureka
   |
   v
Auth Service
   |
   v
JWT
   |
   v
Client
```

---

# 112. Key Takeaways

The major concepts implemented in this part are:

1. **Eureka is the service registry.**

2. **Microservices register themselves with Eureka.**

3. **API Gateway discovers services through Eureka.**

4. **Discovery Locator allows Gateway routes to be generated dynamically.**

5. **`lower-case-service-id` makes service IDs easier to use in URLs.**

6. **Gateway becomes the external entry point.**

7. **Gateway can validate JWT before routing protected requests.**

8. **Eureka registration and request routing are separate operations.**

9. **A service appearing in Eureka does not guarantee that its hostname is reachable.**

10. **`UnknownHostException` and `NXDOMAIN` indicated a hostname/DNS resolution problem, not a JWT problem.**

11. **`prefer-ip-address: true` can help avoid local hostname-resolution issues.**

12. **After changing Eureka instance configuration, services should be restarted and checked again in Eureka.**

The final architecture is therefore:

```text
Authentication
       +
JWT
       +
API Gateway
       +
Eureka Service Discovery
       +
Microservices
```

This forms the foundation for secure request routing in the WorkSphere microservices architecture.
