# JWT Authentication in WorkSphere

---

# Phase 1 – Introduction

## Overview

WorkSphere uses **Spring Security** with **JSON Web Token (JWT)** to secure REST APIs.

JWT (JSON Web Token) is a compact, URL-safe token that allows users to authenticate once and then access protected APIs without maintaining a server-side session.

Unlike traditional Session Authentication, JWT is **stateless**, meaning the server does not store any session information. Every request contains the JWT, and the server validates it before processing the request.

This approach is widely used in enterprise applications because it is scalable, secure, and ideal for Microservices Architecture.

---

## Why Authentication?

Authentication answers the question:

> **"Who are you?"**

Before accessing any protected resource, the application must verify the identity of the user.

Example:

A user logs in using:

Username : admin

Password : admin123

If the credentials are correct, the application generates a JWT Token.

If the credentials are incorrect, access is denied.

---

## Authentication vs Authorization

Authentication and Authorization are different concepts.

Authentication

- Verifies user identity.
- Happens during login.
- Example:
    - Username = admin
    - Password = admin123

Authorization

- Determines what the authenticated user is allowed to access.
- Happens after authentication.
- Example:
    - ADMIN can create employees.
    - USER can only view employees.

WorkSphere currently implements **Authentication** using JWT.

Authorization (Role-Based Access Control) will be implemented in a later phase.

---

## Why JWT?

Traditional web applications use **HTTP Sessions**.

In Session Authentication:

1. User logs in.
2. Server verifies credentials.
3. Server creates a Session.
4. Session ID is stored on the server.
5. Client sends Session ID with every request.

Problems with Sessions:

- Server memory increases with every user.
- Difficult to scale.
- Not suitable for distributed Microservices.
- Requires Sticky Sessions behind Load Balancers.

JWT solves these problems by storing authentication information inside a digitally signed token.

The server only validates the token and does not store session information.

---

## Why JWT in WorkSphere?

WorkSphere follows a **Microservices Architecture**.

Current Architecture

Client

↓

API Gateway

↓

Employee Service

↓

Department Service

Instead of every service maintaining its own user session, all services trust the JWT issued after login.

Benefits:

- Stateless Authentication
- Better Performance
- Easy Horizontal Scaling
- Suitable for Cloud Deployment
- Industry Standard Authentication Mechanism

---

## Technologies Used

- Spring Boot 3.x
- Spring Security
- JSON Web Token (JWT)
- JJWT Library
- BCrypt Password Encoder
- Swagger/OpenAPI
- Maven

# Phase 2 – JWT Architecture and Authentication Flow

---

## JWT Architecture in WorkSphere

The following diagram represents the current authentication architecture implemented in WorkSphere.

                        +------------------+
                        |      Client      |
                        +------------------+
                                  |
                           POST /auth/login
                                  |
                                  ▼
                   +----------------------------+
                   | AuthenticationController   |
                   +----------------------------+
                                  |
                                  ▼
                    +---------------------------+
                    | AuthenticationService     |
                    +---------------------------+
                                  |
                                  ▼
                 +-------------------------------+
                 | AuthenticationManager         |
                 +-------------------------------+
                                  |
                                  ▼
                 +-------------------------------+
                 | CustomUserDetailsService      |
                 +-------------------------------+
                                  |
                                  ▼
                           Validate User
                                  |
                     Username & Password Correct?
                           /                 \
                         No                   Yes
                         |                     |
                         ▼                     ▼
                  Return 401          JwtService.generateToken()
                                             |
                                             ▼
                                     Generate JWT Token
                                             |
                                             ▼
                                    Return JWT to Client

The client stores this JWT and sends it with every protected API request.

---

## Authentication Request Flow

### Step 1

The client sends login credentials.

Request

POST /auth/login

```json
{
   "username":"admin",
   "password":"admin123"
}
```

---

### Step 2

AuthenticationController receives the request.

Responsibilities

- Accept Login Request
- Validate Request
- Call AuthenticationService

---

### Step 3

AuthenticationService authenticates the user.

It delegates authentication to Spring Security using AuthenticationManager.

```java
authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
                username,
                password
        )
);
```

At this stage Spring Security verifies

- Username
- Password
- User Exists

---

### Step 4

AuthenticationManager internally calls

CustomUserDetailsService

↓

loadUserByUsername()

↓

Database / Memory

↓

Returns UserDetails

If username does not exist

↓

Throws UsernameNotFoundException

If password is incorrect

↓

Throws BadCredentialsException

---

### Step 5

After successful authentication

AuthenticationService calls

JwtService.generateToken()

The JWT contains

- Username
- Issued Time
- Expiry Time

The token is digitally signed using the application's Secret Key.

---

### Step 6

AuthenticationService returns the JWT.

Example Response

```json
{
    "token":"eyJhbGciOiJIUzI1NiJ9....."
}
```

The client stores this token.

---

## Accessing Protected APIs

Every protected request must include

Authorization Header

```text
Authorization: Bearer <JWT Token>
```

Example

```text
Authorization:
Bearer eyJhbGciOiJIUzI1NiJ9....
```

Spring Security reads this header before executing the controller.

---

## Request Lifecycle

Client

↓

Protected API

↓

JWT Authentication Filter

↓

Extract Token

↓

Validate Token

↓

Extract Username

↓

Load User

↓

SecurityContextHolder

↓

Controller Executes

---

## Sequence Diagram

Client

│

├────────────── POST /auth/login ─────────────►

│

AuthenticationController

│

AuthenticationService

│

AuthenticationManager

│

CustomUserDetailsService

│

Validate Username & Password

│

JwtService.generateToken()

│

Return JWT

│

◄────────────── JWT Returned ───────────────

│

Client Stores JWT

│

GET /employees

Authorization: Bearer JWT

│

▼

JwtAuthenticationFilter

│

Validate JWT

│

SecurityContextHolder

│

EmployeeController

│

EmployeeService

│

Employee Response

---

## Advantages of this Flow

- Stateless Authentication
- No Session Storage
- Scalable
- Secure
- Easy to integrate with Microservices
- Spring Security handles authentication
- JWT handles identity verification


# Phase 3 – Spring Security Configuration

---

## What is SecurityConfig?

SecurityConfig is the central configuration class of Spring Security.

Its responsibility is to define:

- Which APIs are public
- Which APIs require authentication
- Which authentication mechanism will be used
- Which Password Encoder will be used
- Whether Session or JWT authentication is used
- Which Security Filters should be executed

Without this configuration, Spring Boot applies its default security settings.

---

## SecurityConfig Architecture

                Incoming Request
                        │
                        ▼
             Spring Security Filter Chain
                        │
                        ▼
               SecurityConfig Rules
                        │
         ┌──────────────┴───────────────┐
         │                              │
    Public API                     Protected API
         │                              │
         ▼                              ▼
Allow Request              JwtAuthenticationFilter
│
▼
Validate JWT
│
▼
Execute Controller

---

## Responsibilities of SecurityConfig

Our SecurityConfig performs the following tasks:

✔ Configures Spring Security

✔ Defines public endpoints

✔ Protects remaining APIs

✔ Registers JwtAuthenticationFilter

✔ Creates AuthenticationManager Bean

✔ Creates PasswordEncoder Bean

✔ Disables HTTP Session

---

## Why do we need SecurityConfig?

Without SecurityConfig

Spring Security

↓

Creates Default Login Page

↓

Creates Default User

↓

Uses Session Authentication

↓

Every request requires Session

This is not suitable for REST APIs.

We replace this default behavior with JWT Authentication.

---

## PasswordEncoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Purpose

Passwords should never be stored as plain text.

Instead

admin123

↓

BCrypt

↓

$2a$10$YwA3l....

Only encrypted passwords are stored.

Advantages

- Secure
- One-way encryption
- Salt generated automatically
- Industry Standard

---

## Why BCrypt?

BCrypt

✔ Slow hashing algorithm

✔ Prevents Rainbow Table attacks

✔ Generates different hash for same password

Example

Password

admin123

Hash 1

$2a$10$abc...

Hash 2

$2a$10$xyz...

Same password

Different hashes

This improves security.

---

## AuthenticationManager Bean

```java
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration)
        throws Exception {

    return configuration.getAuthenticationManager();
}
```

### Purpose

AuthenticationManager is responsible for authenticating users.

AuthenticationService

↓

AuthenticationManager

↓

CustomUserDetailsService

↓

PasswordEncoder

↓

Authentication Success

It is the core authentication component of Spring Security.

---

## SecurityFilterChain Bean

This bean defines security rules.

Example

```java
http
.authorizeHttpRequests(...)
```

Responsibilities

- Public APIs
- Protected APIs
- Register JWT Filter
- Stateless Authentication
- Disable CSRF

---

## Public APIs

Public APIs are accessible without JWT.

Example

/auth/login

/swagger-ui/**

/v3/api-docs/**

Reason

A user must be able to log in before obtaining a JWT.

Swagger should also remain accessible.

---

## Protected APIs

All remaining APIs require authentication.

Example

/api/v1/employees

/api/v1/departments

Spring Security automatically checks for a valid JWT before allowing access.

---

## Disabling CSRF

```java
csrf(csrf -> csrf.disable())
```

### Why?

CSRF protection is mainly required for applications that use browser sessions and cookies.

WorkSphere uses JWT instead of Sessions.

Therefore

CSRF protection is unnecessary.

Disabling it avoids unnecessary validation.

---

## Stateless Session

```java
sessionManagement(
    session ->
        session.sessionCreationPolicy(
            SessionCreationPolicy.STATELESS
        )
)
```

### Purpose

Tell Spring Security

"Do not create HTTP Sessions."

Every request must contain a JWT.

Advantages

- No Session Storage
- Better Performance
- Easy Horizontal Scaling
- Suitable for Microservices

---

## Registering JwtAuthenticationFilter

```java
.addFilterBefore(
    jwtAuthenticationFilter,
    UsernamePasswordAuthenticationFilter.class
)
```

### Why Before?

Request

↓

JwtAuthenticationFilter

↓

JWT Valid?

↓

UsernamePasswordAuthenticationFilter

↓

Controller

If JWT is invalid

↓

401 Unauthorized

If valid

↓

Controller executes

The JWT must be validated before Spring Security attempts authentication.

---

## Complete Security Flow

Client

↓

Request

↓

SecurityFilterChain

↓

JwtAuthenticationFilter

↓

Validate JWT

↓

AuthenticationManager

↓

SecurityContextHolder

↓

Controller

↓

Response


# Phase 4 – JWT Service

---

## What is JwtService?

JwtService is responsible for all JWT-related operations in WorkSphere.

Responsibilities

- Generate JWT
- Extract Claims
- Extract Username
- Extract Expiration Time
- Validate JWT
- Verify JWT Signature

It is the only class responsible for creating and validating tokens.

---

## JwtService Architecture

                    AuthenticationService
                            │
                            ▼
                     JwtService
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
Generate Token      Extract Claims      Validate Token
│                   │                   │
└───────────────────┼───────────────────┘
│
▼
JWT Token

---

## JWT Generation Flow

User Login

↓

Authentication Successful

↓

JwtService.generateToken()

↓

Header Created

↓

Payload Created

↓

Signature Generated

↓

JWT Returned

---

## Secret Key

Every JWT is signed using a Secret Key.

Example

```
mySecretKey123456789....
```

The Secret Key ensures

- Token cannot be modified.
- Token authenticity can be verified.
- Anyone without the Secret Key cannot generate a valid JWT.

In production, the Secret Key should never be hardcoded.

It should be stored in:

- Environment Variables
- Spring Config Server
- Vault
- Kubernetes Secret

---

## Signing Algorithm

WorkSphere uses

HS256

HS256 stands for

HMAC SHA-256

During token generation

Header

+

Payload

+

Secret Key

↓

SHA-256

↓

Signature

Generated

---

## JWT Structure

Generated JWT

Header

.

Payload

.

Signature

Example

xxxxx.yyyyy.zzzzz

---

## generateToken()

Purpose

Creates a JWT after successful authentication.

Flow

Username

↓

Create Claims

↓

Set Issued Time

↓

Set Expiration Time

↓

Sign Token

↓

Return JWT

Typical Steps

1. Create Claims
2. Set Subject (Username)
3. Set Issue Time
4. Set Expiration Time
5. Sign using Secret Key
6. Return Compact JWT

---

## Claims

Claims are pieces of information stored inside JWT.

Example

{
"sub":"admin",
"iat":1785226691,
"exp":1785230291
}

Important Claims

sub

Username

iat

Issued Time

exp

Expiration Time

Custom Claims can also be added.

Example

Role

Department

Email

EmployeeId

---

## extractUsername()

Purpose

Extracts username from JWT.

Flow

JWT

↓

Read Payload

↓

Return "sub"

↓

Username

Example

JWT

↓

sub = admin

↓

Return admin

---

## extractClaim()

Purpose

Extracts any claim from JWT.

Examples

Username

Expiration

Issue Time

Role

Department

Instead of writing separate methods for every claim, a generic method is used.

---

## extractExpiration()

Purpose

Returns JWT expiry time.

Flow

JWT

↓

Read Payload

↓

Read exp

↓

Return Expiration Date

---

## isTokenValid()

Purpose

Checks whether the JWT is valid.

Validation Steps

JWT

↓

Extract Username

↓

Compare Username

↓

Check Expiry

↓

Verify Signature

↓

Token Valid

Otherwise

↓

Return False

---

## Expiration

Every JWT has an expiry time.

Example

Issued

10:00 AM

Expires

11:00 AM

After 11:00 AM

↓

401 Unauthorized

This prevents old tokens from being reused indefinitely.

---

## Signature Verification

Every request

↓

JWT Received

↓

Recalculate Signature

↓

Compare Signature

If Signatures Match

↓

Token is Genuine

Otherwise

↓

Reject Request

This prevents Token Tampering.

---

## Complete JwtService Flow

Authentication Successful

↓

generateToken()

↓

Client Receives JWT

↓

Client Calls Protected API

↓

extractUsername()

↓

extractClaim()

↓

isTokenValid()

↓

Request Allowed


# Phase 5 – Authentication Controller & Authentication Service

---

## Overview

Authentication in WorkSphere begins when the user sends login credentials.

The login request follows this path:

Client

↓

AuthenticationController

↓

AuthenticationService

↓

AuthenticationManager

↓

CustomUserDetailsService

↓

JwtService

↓

JWT Returned

---

## AuthenticationController

### Purpose

AuthenticationController exposes REST APIs related to authentication.

Responsibilities

- Accept Login Request
- Validate Request
- Call AuthenticationService
- Return JWT Response

The controller does not contain authentication logic.

It simply delegates the request to AuthenticationService.

---

## Login Endpoint

Request

POST

/auth/login

Request Body

```json
{
    "username": "admin",
    "password": "admin123"
}
```

Response

```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...."
}
```

---

## Why do we keep Controller thin?

Controllers should only handle

- Request Mapping
- Request Validation
- Calling Service Layer
- Returning Response

Business logic should always remain inside the Service Layer.

This makes the code

- Cleaner
- Easier to Test
- Easier to Maintain

---

## AuthenticationService

### Purpose

AuthenticationService contains the complete business logic for user authentication.

Responsibilities

✔ Authenticate User

✔ Generate JWT

✔ Return Authentication Response

---

## Authentication Flow

AuthenticationService

↓

AuthenticationManager.authenticate()

↓

CustomUserDetailsService

↓

Password Validation

↓

JwtService.generateToken()

↓

Return JWT

---

## Step 1 – Authenticate User

AuthenticationService calls

AuthenticationManager

Example

```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        request.username(),
        request.password()
    )
);
```

Purpose

Verify that the username and password are correct.

---

## What happens internally?

AuthenticationManager

↓

AuthenticationProvider

↓

CustomUserDetailsService

↓

Load User

↓

PasswordEncoder

↓

Password Match?

↓

Authentication Success

---

## Step 2 – Generate JWT

If authentication succeeds

↓

JwtService.generateToken(user)

↓

JWT Generated

↓

Return JWT

---

## Step 3 – Return Response

AuthenticationService returns

AuthenticationResponse

Example

```json
{
    "token":"eyJhbGciOiJIUzI1NiJ9..."
}
```

The controller sends this response back to the client.

---

## Authentication Sequence Diagram

Client

│

POST /auth/login

│

▼

AuthenticationController

│

▼

AuthenticationService

│

▼

AuthenticationManager

│

▼

CustomUserDetailsService

│

▼

PasswordEncoder

│

Password Valid?

│

Yes

│

▼

JwtService.generateToken()

│

▼

Return JWT

│

▼

AuthenticationController

│

▼

Client

---

## Error Scenarios

### Invalid Username

CustomUserDetailsService

↓

UsernameNotFoundException

↓

401 Unauthorized

---

### Incorrect Password

PasswordEncoder

↓

Password Mismatch

↓

BadCredentialsException

↓

401 Unauthorized

---

### Valid Credentials

Authentication Successful

↓

JWT Generated

↓

200 OK

---

## Layer Responsibilities

AuthenticationController

- Receives HTTP Request
- Returns HTTP Response

AuthenticationService

- Business Logic
- Authentication
- Token Generation

AuthenticationManager

- Validates Username & Password

JwtService

- Creates JWT
- Validates JWT

CustomUserDetailsService

- Loads User Details

PasswordEncoder

- Verifies Password

# Phase 6 – JwtAuthenticationFilter

---

## Overview

JwtAuthenticationFilter is responsible for validating the JWT token before every protected API request.

Every incoming request passes through this filter before reaching the controller.

If the JWT is valid, the request is authenticated.

If the JWT is invalid or missing, Spring Security returns **401 Unauthorized**.

---

## Why JwtAuthenticationFilter?

Suppose a client calls

GET /api/v1/employees

How does Spring Security know

- Who is calling?
- Is the user authenticated?
- Is the JWT valid?

The answer is

**JwtAuthenticationFilter**

---

## Why extend OncePerRequestFilter?

```java
public class JwtAuthenticationFilter
        extends OncePerRequestFilter
```

### Purpose

Spring executes this filter exactly **once** for every HTTP request.

Example

Client

↓

GET /employees

↓

JwtAuthenticationFilter (Executed Once)

↓

Controller

Without OncePerRequestFilter

- Filter may execute multiple times
- Duplicate Authentication
- Performance Issues

---

## Request Flow

Incoming Request

↓

JwtAuthenticationFilter

↓

Read Authorization Header

↓

Extract JWT

↓

Validate JWT

↓

Load User

↓

Set Authentication

↓

Continue Filter Chain

↓

Controller

---

## Step 1 – Read Authorization Header

```java
final String authHeader =
        request.getHeader("Authorization");
```

Purpose

Read the Authorization header.

Example

```
Authorization:
Bearer eyJhbGciOiJIUzI1NiJ9....
```

---

## Step 2 – Check Header

```java
if(authHeader == null ||
   !authHeader.startsWith("Bearer "))
{
    filterChain.doFilter(request,response);
    return;
}
```

Purpose

If the Authorization header

- does not exist
- does not start with Bearer

then

Skip JWT validation.

Continue processing the request.

---

## Why "Bearer " ?

Example

```
Bearer eyJhbGciOiJIUzI1NiJ9....
```

Bearer

↓

7 Characters

```
Bearer_
0123456
```

Therefore

```java
authHeader.substring(7)
```

removes

```
Bearer_
```

and returns only

```
eyJhbGciOiJIUzI1NiJ9....
```

This JWT is passed to JwtService.

---

## Step 3 – Extract JWT

```java
final String jwt =
        authHeader.substring(7);
```

Purpose

Remove "Bearer " and extract the JWT.

---

## Step 4 – Extract Username

```java
String username =
        jwtService.extractUsername(jwt);
```

JwtService

↓

Read Payload

↓

Extract

sub

↓

Return Username

Example

```
sub = admin
```

↓

Return

```
admin
```

---

## Step 5 – Check Authentication

```java
SecurityContextHolder
        .getContext()
        .getAuthentication()
```

Purpose

Checks whether the current request is already authenticated.

If authentication already exists

↓

Skip Authentication

Otherwise

↓

Authenticate User

---

## Step 6 – Load User

```java
UserDetails userDetails =
customUserDetailsService
.loadUserByUsername(username);
```

Purpose

Load user details from the configured UserDetailsService.

Returns

- Username
- Password
- Roles
- Authorities

---

## Step 7 – Validate JWT

```java
jwtService.isTokenValid(
        jwt,
        userDetails.getUsername()
);
```

Validation

✔ Username Match

✔ Signature Valid

✔ Not Expired

If all checks pass

↓

JWT is trusted

---

## Step 8 – Create Authentication Object

```java
UsernamePasswordAuthenticationToken
```

Purpose

Represents an authenticated user inside Spring Security.

It contains

- UserDetails
- Credentials
- Authorities

---

## Step 9 – Store Authentication

```java
SecurityContextHolder
    .getContext()
    .setAuthentication(authentication);
```

Purpose

Store authenticated user for the current request.

After this line

Spring Security considers

"The user is authenticated."

Now

Controllers

Services

Method Security

can access the authenticated user.

---

## Step 10 – Continue Filter Chain

```java
filterChain.doFilter(request,response);
```

Purpose

Pass the request to the next filter.

Flow

JwtAuthenticationFilter

↓

Next Spring Filter

↓

Controller

Without this line

↓

Request stops here

↓

Controller never executes

---

## Complete Flow

Request

↓

Authorization Header

↓

Bearer Present?

↓

No

↓

Continue Filter Chain

↓

Yes

↓

Extract JWT

↓

Extract Username

↓

Load User

↓

Validate JWT

↓

Create Authentication

↓

SecurityContextHolder

↓

Continue Filter Chain

↓

Controller

---

## SecurityContextHolder

SecurityContextHolder stores authentication information for the current request.

Example

```
Authenticated User

↓

Username

↓

Authorities

↓

Roles
```

Controllers can access

```
SecurityContextHolder
```

to retrieve the logged-in user.

---

## Real Example

Client

↓

GET /employees

↓

Authorization

Bearer eyJhbGc....

↓

JwtAuthenticationFilter

↓

JWT Valid

↓

SecurityContextHolder

↓

EmployeeController

↓

EmployeeService

↓

Employee List Returned


## Common Mistake

During development, the following issue occurred.

Problem

Employee APIs returned HTTP 200.

However

The controller methods were never executed.

Cause

The JwtAuthenticationFilter did not call

```java
filterChain.doFilter(request,response);
```

As a result

The request stopped inside the filter.

Controller

↓

Never Executed

After adding

```java
filterChain.doFilter(request,response);
```

The request successfully reached the controller and CRUD operations started working correctly.

# Phase 7 – CustomUserDetailsService & Spring Security Authentication

---

## Overview

Spring Security does not know how to retrieve users from a database or any other source.

Instead, it delegates this responsibility to an implementation of the `UserDetailsService` interface.

In WorkSphere, this implementation is `CustomUserDetailsService`.

Its primary responsibility is to load user details during authentication.

---

## Authentication Architecture

                    Login Request
                           │
                           ▼
                AuthenticationManager
                           │
                           ▼
                AuthenticationProvider
                           │
                           ▼
             CustomUserDetailsService
                           │
                           ▼
                  Load User Details
                           │
                           ▼
                 Password Validation
                           │
                           ▼
                 Authentication Success

---

## What is UserDetailsService?

`UserDetailsService` is a Spring Security interface.

It contains one important method.

```java
UserDetails loadUserByUsername(String username)
```

Purpose

- Find the user
- Load user information
- Return UserDetails

If the user is not found

↓

Throw UsernameNotFoundException

---

## Why do we need CustomUserDetailsService?

Spring Security does not know

- Where users are stored
- How users are retrieved
- What roles users have

We provide this information using CustomUserDetailsService.

Responsibilities

✔ Find User

✔ Return UserDetails

✔ Throw Exception if User does not exist

---

## loadUserByUsername()

Flow

Username

↓

Search User

↓

User Found?

      / \
    Yes  No
     |    |
     ▼    ▼
Return   Throw
UserDetails
UsernameNotFoundException

---

## UserDetails

Spring Security authenticates users using the `UserDetails` interface.

It contains

- Username
- Password
- Authorities (Roles)
- Account Status

Typical information

```text
Username

Password

Authorities

Account Enabled

Account Locked

Credentials Expired

Account Expired
```

---

## AuthenticationManager

AuthenticationManager is the central authentication component of Spring Security.

Purpose

Authenticate the user's credentials.

Flow

AuthenticationManager

↓

AuthenticationProvider

↓

CustomUserDetailsService

↓

PasswordEncoder

↓

Authentication Result

---

## AuthenticationProvider

AuthenticationProvider performs the actual authentication.

Responsibilities

- Load user
- Compare passwords
- Return Authentication object

It works internally with

- UserDetailsService
- PasswordEncoder

---

## Password Validation

Spring Security never compares plain text passwords.

Instead

User Password

↓

BCryptPasswordEncoder.matches()

↓

Stored Password Hash

↓

Password Match?

If Yes

↓

Authentication Successful

Otherwise

↓

BadCredentialsException

---

## Complete Login Flow

Client

↓

POST /auth/login

↓

AuthenticationController

↓

AuthenticationService

↓

AuthenticationManager

↓

AuthenticationProvider

↓

CustomUserDetailsService

↓

Load User

↓

PasswordEncoder

↓

Password Match

↓

Authentication Success

↓

JwtService.generateToken()

↓

JWT Returned

---

## Error Handling

### User Does Not Exist

CustomUserDetailsService

↓

UsernameNotFoundException

↓

401 Unauthorized

---

### Wrong Password

PasswordEncoder

↓

Password Mismatch

↓

BadCredentialsException

↓

401 Unauthorized

---

### Correct Credentials

Authentication Successful

↓

JWT Generated

↓

200 OK

---

## Responsibilities

AuthenticationController

- Accept Request
- Return Response

AuthenticationService

- Business Logic
- Generate JWT

AuthenticationManager

- Coordinate Authentication

AuthenticationProvider

- Verify Credentials

CustomUserDetailsService

- Load User

PasswordEncoder

- Compare Passwords

JwtService

- Generate Token
- Validate Token


# Phase 8 – API Testing

---

## Testing JWT Authentication

WorkSphere JWT authentication can be tested using both:

- Swagger UI
- Postman

---

## Step 1 – Login

Request

POST

/auth/login

Request Body

```json
{
    "username": "admin",
    "password": "admin123"
}
```

Expected Response

```json
{
    "token":"eyJhbGciOiJIUzI1NiJ9..."
}
```

HTTP Status

```
200 OK
```

---

## Step 2 – Copy JWT

Copy the JWT returned by the login API.

Example

```
eyJhbGciOiJIUzI1NiJ9...
```

---

## Step 3 – Access Protected API

Request

GET

/api/v1/employees

Headers

```
Authorization:
Bearer eyJhbGciOiJIUzI1NiJ9...
```

Expected Response

```
200 OK
```

Employee data is returned.

---

## Step 4 – Test Without JWT

Request

GET

/api/v1/employees

(No Authorization Header)

Expected Response

```
401 Unauthorized
```

Reason

JwtAuthenticationFilter could not find a valid JWT.

---

## Step 5 – Test Invalid JWT

Example

```
Bearer abc123xyz
```

Expected Response

```
401 Unauthorized
```

Reason

JWT signature validation fails.

---

## Step 6 – Test Expired JWT

If the JWT expiration time has passed,

Expected Response

```
401 Unauthorized
```

Reason

JwtService detects that the token has expired.

---

## Testing Using Swagger

Steps

1. Open Swagger UI

```
http://localhost:8081/swagger-ui/index.html
```

2. Click **Authorize**

3. Enter

```
Bearer <JWT>
```

4. Click **Authorize**

5. Execute protected APIs.

---

## Expected HTTP Responses

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Request Successful |
| 201 | Resource Created |
| 400 | Validation Failed |
| 401 | Unauthorized (Invalid/Missing JWT) |
| 403 | Forbidden (No Permission) |
| 404 | Resource Not Found |
| 500 | Internal Server Error |

---

## JWT Testing Checklist

✔ Login API returns JWT

✔ JWT copied successfully

✔ Authorization header added

✔ Protected API returns 200

✔ Missing JWT returns 401

✔ Invalid JWT returns 401

✔ Expired JWT returns 401


# Phase 9 – Best Practices & Production Considerations

---

## Best Practices

### 1. Never Hardcode Secret Keys

Avoid

```java
private String SECRET =
"my-secret-key";
```

Preferred

- Environment Variables
- Spring Config Server
- Vault
- Kubernetes Secret

---

### 2. Always Use HTTPS

JWT should always be transmitted over HTTPS.

Reason

Prevents token interception.

---

### 3. Keep Access Tokens Short-Lived

Recommended

15–30 minutes

Benefits

- Better Security
- Reduced Risk if Token is Stolen

---

### 4. Use Refresh Tokens

Instead of creating long-lived JWTs,

Use

Access Token

+

Refresh Token

Benefits

- Better User Experience
- Improved Security

---

### 5. Never Store Passwords in Plain Text

Always use

BCryptPasswordEncoder

Passwords must be stored as hashed values.

---

### 6. Do Not Log JWT Tokens

Avoid

```java
log.info(jwt);
```

Reason

Anyone with the JWT can access protected APIs.

---

### 7. Validate Every JWT

Always verify

✔ Signature

✔ Username

✔ Expiration

Never trust JWT without validation.

---

### 8. Keep Business Logic Outside Controllers

Controllers

↓

Receive Request

↓

Call Service

↓

Return Response

Business logic belongs in the Service Layer.

---

## Common Mistakes

### Missing filterChain.doFilter()

Problem

Controller methods never executed.

Solution

```java
filterChain.doFilter(request, response);
```

---

### Forgetting "Bearer "

Incorrect

```
Authorization:
eyJhbGc...
```

Correct

```
Authorization:
Bearer eyJhbGc...
```

---

### Hardcoding Secret Key

Avoid storing secret keys in source code.

Use secure configuration.

---

### Using Plain Text Passwords

Always use BCrypt.

---

### Forgetting SessionCreationPolicy.STATELESS

JWT applications should never create HTTP Sessions.

Always configure

```java
SessionCreationPolicy.STATELESS
```

---

## Future Improvements

The current implementation can be enhanced by adding:

- Refresh Token Support
- Role-Based Authorization (ADMIN, USER)
- Auth Service Microservice
- Spring Cloud Config
- Redis Token Blacklisting
- OAuth2 Integration
- Key Rotation
- Multi-Factor Authentication (MFA)

---

## Summary

Current JWT Features Implemented

✔ Spring Security

✔ JWT Authentication

✔ JWT Validation

✔ JWT Filter

✔ AuthenticationManager

✔ BCrypt Password Encoder

✔ Stateless Authentication

✔ Swagger Authentication

✔ Protected REST APIs

✔ Duplicate Email Validation

This implementation provides a secure and scalable authentication mechanism suitable for enterprise microservices applications.
