# 24. Swagger / OpenAPI

## Objective

Learn how Swagger (OpenAPI) automatically generates interactive REST API documentation for Spring Boot applications.

In WorkSphere, Swagger is used to:

- Document REST APIs.
- Describe request and response models.
- Display HTTP status codes.
- Allow developers to test APIs directly from the browser.
- Improve collaboration between Backend, Frontend, QA, and API consumers.

---

# What is Swagger?

Swagger is a collection of tools used to design, document, and test REST APIs.

Today, the official API specification is called **OpenAPI Specification (OAS)**.

Spring Boot applications commonly use:

```
SpringDoc OpenAPI
```

to automatically generate Swagger UI.

---

# OpenAPI vs Swagger

Although developers often use these names interchangeably, they are different.

| OpenAPI | Swagger |
|----------|----------|
| API Specification | Toolset implementing the specification |
| Defines API structure | Displays interactive documentation |
| Standard maintained by OpenAPI Initiative | Originally developed by SmartBear |

In modern Spring Boot applications, we typically use **SpringDoc OpenAPI**, which generates **Swagger UI**.

---

# Why do we need Swagger?

Imagine a project with hundreds of REST APIs.

Without Swagger:

- Developers must read controller classes.
- API consumers depend on Postman collections.
- Documentation easily becomes outdated.
- Frontend teams constantly ask backend developers for API details.

With Swagger:

- Documentation is generated automatically.
- API changes are reflected immediately.
- Every endpoint is available through a web interface.
- APIs can be tested directly from the browser.

---

# Life Before Swagger

```
Developer

↓

Reads Controller Code

↓

Creates Postman Request

↓

Tries API
```

---

# Life With Swagger

```
Developer

↓

Open Browser

↓

Swagger UI

↓

Read Documentation

↓

Execute API
```

Swagger significantly improves developer productivity and reduces communication overhead between teams.

---

# Integrating Swagger in WorkSphere

Spring Boot does not provide Swagger UI by default.

To generate API documentation automatically, we added **SpringDoc OpenAPI**.

---

# SpringDoc OpenAPI

SpringDoc is the official library used with Spring Boot 3.x.

Dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.9</version>
</dependency>
```

After adding this dependency and starting the application, Swagger UI becomes available automatically.

No additional configuration is required for basic usage.

---

# Swagger UI URL

When the application is running:

```
http://localhost:8083/swagger-ui/index.html
```

The browser displays all REST APIs exposed by the application.

---

# OpenAPI JSON

Swagger UI is generated from an OpenAPI specification.

The specification is available as JSON.

Example:

```
http://localhost:8083/v3/api-docs
```

This endpoint returns the complete OpenAPI specification of the application.

Swagger UI simply reads this JSON and renders a user-friendly interface.

---

# How Swagger Works

```
Spring Boot Application
            │
            ▼
Controller Annotations
            │
            ▼
SpringDoc scans Controllers
            │
            ▼
Generates OpenAPI Specification
            │
            ▼
Swagger UI displays documentation
```

Developers never write the OpenAPI JSON manually.

SpringDoc generates it automatically from controller annotations.

---

# What Swagger Displays

For every endpoint, Swagger automatically shows:

- HTTP Method
- URL
- Request Parameters
- Request Body
- Response Body
- HTTP Status Codes
- Validation Rules
- Example Values (if provided)
- Response Schema

This makes API documentation self-maintaining.

---

# WorkSphere Implementation

In WorkSphere, after adding the SpringDoc dependency, Swagger automatically documented:

- Employee APIs
- Department APIs
- Payroll APIs

Whenever a new controller is added, Swagger includes it automatically without additional configuration.

This ensures that the documentation always remains synchronized with the source code.

---

# Advantages

Using SpringDoc OpenAPI provides several benefits:

- Zero manual API documentation.
- Interactive API testing.
- Always synchronized with source code.
- Easy onboarding for new developers.
- Simplifies frontend-backend integration.
- Reduces dependency on Postman collections.

For these reasons, SpringDoc OpenAPI has become the standard choice for Spring Boot 3 applications.

---

# Understanding Swagger Annotations

Swagger (SpringDoc OpenAPI) uses annotations to generate detailed API documentation.

In the Payroll Service, we used the following annotations:

- `@Tag`
- `@Operation`
- `@ApiResponses`
- `@ApiResponse`

Each annotation serves a different purpose.

---

# @Tag

The `@Tag` annotation groups related APIs together inside Swagger UI.

Example:

```java
@Tag(
    name = "Payroll Management",
    description = "APIs for managing employee payroll"
)
@RestController
@RequestMapping("/api/payroll")
public class PayrollController {
}
```

Without `@Tag`, Swagger groups APIs using the controller name.

Using meaningful tags makes the documentation easier to navigate.

---

# @Operation

The `@Operation` annotation describes a single REST endpoint.

Example:

```java
@Operation(
    summary = "Create Payroll",
    description = "Creates payroll details for an employee."
)
```

Swagger displays:

- Endpoint name
- Short summary
- Detailed description

This helps API consumers understand the purpose of the endpoint.

---

# @ApiResponses

An API can return multiple HTTP status codes.

The `@ApiResponses` annotation documents all possible responses.

Example:

```java
@ApiResponses({
    @ApiResponse(
            responseCode = "201",
            description = "Payroll created successfully"
    ),
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed"
    ),
    @ApiResponse(
            responseCode = "409",
            description = "Payroll already exists"
    )
})
```

Swagger displays these responses in the documentation.

API consumers immediately know what responses to expect.

---

# @ApiResponse

Each possible response is documented using:

```java
@ApiResponse(...)
```

Example:

```java
@ApiResponse(
        responseCode = "404",
        description = "Payroll not found"
)
```

This improves API readability and reduces ambiguity.

---

# Response Codes Used in WorkSphere

During Payroll implementation, the following response codes were documented.

| HTTP Code | Meaning |
|------------|----------|
| 200 | Request successful |
| 201 | Resource created |
| 204 | Resource deleted successfully |
| 400 | Validation failed |
| 404 | Resource not found |
| 409 | Duplicate payroll exists |

Using meaningful status codes makes APIs easier to consume.

---

# Example – Create Payroll

Our implementation:

```java
@Operation(
        summary = "Create Payroll",
        description = "Creates payroll details for an employee."
)
@ApiResponses({
    @ApiResponse(
            responseCode = "201",
            description = "Payroll created successfully"
    ),
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed"
    ),
    @ApiResponse(
            responseCode = "409",
            description = "Payroll already exists"
    )
})
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public PayrollResponse createPayroll(
        @Valid @RequestBody CreatePayrollRequest request) {

    return payrollService.createPayroll(request);
}
```

Swagger automatically displays:

- POST endpoint
- Request body
- Response model
- HTTP status codes
- Description

---

# Example – Update Payroll

```java
@Operation(
        summary = "Update Payroll",
        description = "Updates payroll information and recalculates net salary."
)
```

Swagger immediately shows that:

- Net salary is recalculated.
- Employee ID is not updated.
- The endpoint updates an existing payroll record.

Good descriptions reduce misunderstandings for API consumers.

---

# Example – Delete Payroll

```java
@Operation(
        summary = "Delete Payroll",
        description = "Deletes payroll details."
)
```

Combined with:

```java
@ResponseStatus(HttpStatus.NO_CONTENT)
```

Swagger shows:

```
204 No Content
```

indicating that deletion is successful without returning a response body.

---

# WorkSphere Implementation

In the Payroll Service:

- Every endpoint has an `@Operation`.
- Important endpoints include documented `@ApiResponses`.
- HTTP status codes match the actual controller implementation.

This keeps API documentation synchronized with the code.

---

# Best Practices

✔ Use meaningful summaries.

✔ Keep descriptions concise.

✔ Document important HTTP responses.

✔ Use correct HTTP status codes.

✔ Update Swagger whenever the API changes.

Well-maintained API documentation improves collaboration between Backend, Frontend, QA, and external API consumers.

---

# Validation Integration

One of the biggest advantages of SpringDoc OpenAPI is its integration with **Jakarta Bean Validation**.

Consider the following DTO used in the Payroll Service:

```java
public record CreatePayrollRequest(

    @NotNull(message = "Employee Id is mandatory")
    Long employeeId,

    @NotNull(message = "Basic salary is mandatory")
    @DecimalMin(value = "0.00", inclusive = true)
    BigDecimal basicSalary,

    @NotNull(message = "Bonus is mandatory")
    @DecimalMin(value = "0.00", inclusive = true)
    BigDecimal bonus,

    @NotNull(message = "Tax is mandatory")
    @DecimalMin(value = "0.00", inclusive = true)
    BigDecimal tax

) {
}
```

When this DTO is used in the controller:

```java
@PostMapping
public PayrollResponse createPayroll(
        @Valid @RequestBody CreatePayrollRequest request) {

    return payrollService.createPayroll(request);
}
```

Swagger automatically identifies:

- Required fields
- Request Body Structure
- Data Types

This gives API consumers a clear understanding of what the endpoint expects.

---

# How Swagger Reads Our Application

The complete workflow is:

```
Controller
     │
     ▼
Swagger Annotations
     │
     ▼
SpringDoc Scanner
     │
     ▼
OpenAPI Specification
     │
     ▼
Swagger UI
```

Whenever the application starts, SpringDoc scans all Spring MVC controllers.

Every endpoint is converted into an OpenAPI specification.

Swagger UI then renders that specification into an interactive webpage.

---

# WorkSphere Implementation

In WorkSphere, Swagger documents APIs from:

- Employee Service
- Department Service
- Payroll Service

For each endpoint it automatically displays:

- HTTP Method
- URL
- Path Variables
- Request Body
- Response Body
- Validation Rules
- HTTP Status Codes
- Description

No manual documentation is required.

---

# Best Practices

While implementing Swagger in WorkSphere, the following best practices were followed:

✔ Add meaningful `@Operation` summaries.

✔ Use concise endpoint descriptions.

✔ Document important HTTP response codes.

✔ Keep controller annotations synchronized with implementation.

✔ Use Bean Validation (`@Valid`) for request validation.

✔ Return appropriate HTTP status codes.

✔ Keep API documentation updated whenever endpoints change.

---

# Common Interview Questions

## Q1. What is Swagger?

Swagger is a tool used to generate interactive REST API documentation.

---

## Q2. What is OpenAPI?

OpenAPI is the specification that describes REST APIs.

Swagger UI is one implementation that displays the specification.

---

## Q3. What is SpringDoc OpenAPI?

SpringDoc is the library used in Spring Boot 3 applications to generate OpenAPI documentation automatically.

---

## Q4. How does Swagger generate documentation?

SpringDoc scans Spring MVC controllers and reads annotations such as:

- `@RestController`
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@Operation`
- `@ApiResponses`

It then generates an OpenAPI specification.

Swagger UI displays that specification.

---

## Q5. Why use Swagger?

Swagger provides:

- Automatic documentation
- Interactive API testing
- Up-to-date API specification
- Better collaboration between teams

---

## Q6. Does Swagger affect API execution?

No.

Swagger only documents and tests APIs.

The actual execution is still handled by Spring MVC.

---

## Q7. Why is Swagger useful in Microservices?

In a microservices architecture, multiple services expose REST APIs.

Swagger provides centralized, standardized documentation for each service.

This makes integration significantly easier.

---

# Key Takeaways

After implementing Swagger in WorkSphere, we learned:

- What Swagger is.
- What OpenAPI is.
- How SpringDoc generates API documentation.
- How controller annotations are converted into OpenAPI.
- How Bean Validation integrates with Swagger.
- How Swagger improves API discoverability and testing.

---

# Conclusion

Swagger (SpringDoc OpenAPI) simplifies REST API documentation by automatically generating an interactive interface directly from Spring Boot controllers.

Instead of maintaining documentation manually, developers only need to annotate their controllers.

This ensures that API documentation always remains synchronized with the source code.

For enterprise Spring Boot applications, SpringDoc OpenAPI has become the standard solution for documenting and testing REST APIs.