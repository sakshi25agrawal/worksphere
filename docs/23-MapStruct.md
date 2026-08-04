# 23. MapStruct

## Objective

Learn how MapStruct eliminates manual object mapping in Spring Boot applications by generating mapping code at compile time.

In WorkSphere, MapStruct is used in the Payroll Service to convert between:

- Request DTO → Entity
- Entity → Response DTO
- Update DTO → Existing Entity

---

# Why do we need MapStruct?

In enterprise applications, Controllers should never expose Entity classes directly.

Instead, applications use **DTOs (Data Transfer Objects)** to communicate with clients.

A typical request flow looks like this:

```
Client
   │
   ▼
CreatePayrollRequest (DTO)
   │
   ▼
Payroll Entity
   │
   ▼
Database
   │
   ▼
Payroll Entity
   │
   ▼
PayrollResponse (DTO)
   │
   ▼
Client
```

Because DTOs and Entities serve different purposes, we need a mechanism to convert one into another.

This conversion process is called **Object Mapping**.

---

# What is Object Mapping?

Object Mapping is the process of copying data from one Java object to another.

Example:

```java
CreatePayrollRequest
        │
        ▼
Payroll Entity
```

and

```java
Payroll Entity
        │
        ▼
PayrollResponse
```

Without a mapper, developers must manually copy every field.

As projects grow larger, manual mapping becomes repetitive, difficult to maintain, and prone to mistakes.


---

# Manual Object Mapping

Before using MapStruct, developers had to manually copy values from one object to another.

Example:

```java
Payroll payroll = new Payroll();

payroll.setEmployeeId(request.employeeId());
payroll.setBasicSalary(request.basicSalary());
payroll.setBonus(request.bonus());
payroll.setTax(request.tax());
```

Similarly, converting an Entity back to a Response DTO required writing more repetitive code:

```java
PayrollResponse response = new PayrollResponse(
        payroll.getId(),
        payroll.getEmployeeId(),
        payroll.getBasicSalary(),
        payroll.getBonus(),
        payroll.getTax(),
        payroll.getNetSalary()
);
```

As the number of fields increases, the mapping code also grows.

---

# Problems with Manual Mapping

Manual mapping works, but it has several disadvantages:

- Repetitive boilerplate code.
- Easy to forget newly added fields.
- Difficult to maintain when DTOs or Entities change.
- Increases development time.
- Makes service classes unnecessarily lengthy.

For example, if a new field like:

```java
private BigDecimal hra;
```

is added to the Payroll entity, every manual mapping method must also be updated.

Missing even one field may introduce bugs.

---

# What is MapStruct?

MapStruct is a Java Annotation Processor that automatically generates mapping code during compilation.

Instead of writing mapping logic manually, developers only define mapping methods inside an interface.

Example:

```java
@Mapper(componentModel = "spring")
public interface PayrollMapper {

    Payroll toEntity(CreatePayrollRequest request);

    PayrollResponse toResponse(Payroll payroll);

}
```

During compilation, MapStruct generates the implementation automatically.

The generated implementation performs exactly the same field assignments that a developer would write manually.

---

# How MapStruct Works

The overall flow is:

```
Developer writes Mapper Interface
                │
                ▼
Maven Compilation
                │
                ▼
MapStruct Annotation Processor
                │
                ▼
PayrollMapperImpl.java (Generated)
                │
                ▼
Spring Boot injects the generated mapper
```

The developer only writes the interface.

MapStruct generates the implementation.

Spring Boot injects it as a Bean.

---

# Advantages of MapStruct

Compared to manual mapping, MapStruct provides several benefits:

- Eliminates repetitive boilerplate code.
- Generates code at compile time.
- Improves readability.
- Makes services cleaner.
- Provides compile-time validation.
- Produces high-performance mapping code.
- Easier to maintain in large applications.

For these reasons, MapStruct is widely used in enterprise Spring Boot applications.

---

# Setting Up MapStruct

MapStruct is not included in Spring Boot by default.

We must explicitly add the required dependencies and configure the annotation processor.

---

# Step 1: Add MapStruct Dependency

In the parent `pom.xml`, add the MapStruct dependency.

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

This provides all the annotations required to define mapper interfaces.

For example:

- `@Mapper`
- `@Mapping`
- `@MappingTarget`

However, this dependency alone is **not enough**.

---

# Step 2: Add MapStruct Processor

MapStruct generates Java code during compilation.

For that, Maven needs the MapStruct Annotation Processor.

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>${mapstruct.version}</version>
    <scope>provided</scope>
</dependency>
```

The processor is used only during compilation.

It is **not required at runtime**, therefore it is added with:

```xml
<scope>provided</scope>
```

---

# Step 3: Configure Maven Compiler Plugin

Inside the parent `pom.xml`, configure the annotation processor.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>

    <configuration>

        <source>${java.version}</source>
        <target>${java.version}</target>

        <annotationProcessorPaths>

            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>

        </annotationProcessorPaths>

    </configuration>

</plugin>
```

During Maven compilation:

```text
mvn clean install
```

MapStruct scans every interface annotated with `@Mapper`.

Then it generates the implementation automatically.

---

# PayrollMapper in WorkSphere

Our Payroll Service contains the following mapper:

```java
@Mapper(componentModel = "spring")
public interface PayrollMapper {

    PayrollResponse toResponse(Payroll payroll);

    Payroll toEntity(CreatePayrollRequest request);

    void updateEntity(
            UpdatePayrollRequest request,
            @MappingTarget Payroll payroll);

}
```

Notice that **only method declarations** are written.

No implementation is provided.

---

# What does componentModel = "spring" mean?

```java
@Mapper(componentModel = "spring")
```

This tells MapStruct to generate the mapper as a Spring Bean.

Because of this, Spring Boot can inject it like any other component.

Example:

```java
@RequiredArgsConstructor
@Service
public class PayrollServiceImpl {

    private final PayrollMapper payrollMapper;

}
```

No object creation is required.

Spring automatically injects the generated implementation.

---

# What happens during compilation?

When Maven executes:

```text
mvn clean install
```

MapStruct generates:

```text
PayrollMapperImpl.java
```

inside:

```text
target/
└── generated-sources/
    └── annotations/
        └── PayrollMapperImpl.java
```

Developers never create this file manually.

It is generated automatically every time the project is compiled.

---

# WorkSphere Implementation

In the Payroll Service, MapStruct is responsible for:

- Converting `CreatePayrollRequest` into `Payroll`
- Converting `Payroll` into `PayrollResponse`
- Updating an existing `Payroll` using `UpdatePayrollRequest`

This completely removes the need for manual field copying inside the service layer.

---

# Understanding MapStruct Annotations

MapStruct provides several annotations to control how objects are mapped.

In the Payroll Service, we primarily used:

- `@Mapper`
- `@Mapping`
- `@MappingTarget`

Each annotation serves a different purpose.

---

# @Mapper

The `@Mapper` annotation tells MapStruct that the interface is a mapper.

Example:

```java
@Mapper(componentModel = "spring")
public interface PayrollMapper {
}
```

Without this annotation:

- MapStruct ignores the interface.
- No implementation class is generated.
- Spring cannot inject the mapper.

---

# componentModel = "spring"

```java
@Mapper(componentModel = "spring")
```

This instructs MapStruct to generate the implementation as a Spring Bean.

Generated class:

```
PayrollMapperImpl
```

Spring Boot automatically registers it inside the Application Context.

Because of this, we can inject it like any other Spring component.

```java
@RequiredArgsConstructor
@Service
public class PayrollServiceImpl {

    private final PayrollMapper payrollMapper;

}
```

Without:

```java
componentModel = "spring"
```

we would have to manually obtain the mapper:

```java
PayrollMapper mapper = Mappers.getMapper(PayrollMapper.class);
```

Since WorkSphere uses Spring Boot, `componentModel = "spring"` is the preferred approach.

---

# @Mapping

Sometimes source and destination objects should not be copied exactly.

MapStruct allows customization using:

```java
@Mapping(...)
```

Example:

```java
@Mapping(target = "id", ignore = true)
```

Meaning:

> Ignore the destination field `id`.

---

# Why did we ignore ID?

During Payroll creation:

```java
Payroll toEntity(CreatePayrollRequest request);
```

The request does not contain an ID.

Even if it did, we should never trust the client.

The database generates the ID automatically using:

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

Therefore:

```java
@Mapping(target = "id", ignore = true)
```

prevents accidental overwriting.

---

# Why did we ignore Net Salary?

```java
@Mapping(target = "netSalary", ignore = true)
```

Reason:

Net salary is **not user input**.

It is calculated using business logic.

Inside the service:

```java
payroll.setNetSalary(
        calculateNetSalary(
                payroll.getBasicSalary(),
                payroll.getBonus(),
                payroll.getTax()
        )
);
```

If MapStruct copied this field from the request, users could manipulate payroll values.

Therefore, business-calculated fields should always remain under service-layer control.

---

# Why did we ignore Employee ID during Update?

```java
@Mapping(target = "employeeId", ignore = true)
```

Reason:

Employee ID identifies the payroll owner.

Once a payroll record is created, its employee cannot change.

If this field were mapped during updates, one employee's payroll could accidentally become another employee's payroll.

Ignoring the field preserves data integrity.

---

# @MappingTarget

Normally, MapStruct creates a new object.

Example:

```java
Payroll payroll = mapper.toEntity(request);
```

However, update operations are different.

The existing entity must be modified instead of creating a new one.

For this purpose, MapStruct provides:

```java
@MappingTarget
```

Example:

```java
void updateEntity(
        UpdatePayrollRequest request,
        @MappingTarget Payroll payroll);
```

Instead of creating another Payroll object, MapStruct updates the existing instance.

Generated code looks similar to:

```java
payroll.setBasicSalary(request.basicSalary());
payroll.setBonus(request.bonus());
payroll.setTax(request.tax());
```

The object reference remains the same.

Only selected fields are updated.

---

# Why is @MappingTarget important?

Suppose Payroll already exists:

```
Payroll (ID = 10)
```

If we created a new object during update:

```
Payroll (new object)
```

Hibernate could treat it as another entity.

Using `@MappingTarget` ensures:

- Existing entity is reused.
- Entity ID remains unchanged.
- Hibernate performs an UPDATE instead of creating another object.

This is the recommended approach for update APIs in enterprise applications.

---

# WorkSphere Implementation

The Payroll Service uses these annotations as follows:

| Annotation | Purpose |
|------------|---------|
| `@Mapper` | Declares the mapper interface |
| `componentModel="spring"` | Registers mapper as Spring Bean |
| `@Mapping(target="id", ignore=true)` | Prevents client from modifying primary key |
| `@Mapping(target="employeeId", ignore=true)` | Keeps employee relationship immutable |
| `@Mapping(target="netSalary", ignore=true)` | Allows service layer to calculate net salary |
| `@MappingTarget` | Updates existing entity instead of creating a new one |

---

# Summary

MapStruct annotations allow developers to control exactly how objects are converted.

In WorkSphere, they help us:

- Protect immutable fields.
- Keep business calculations inside the service layer.
- Reuse existing entities during updates.
- Reduce boilerplate code.
- Improve maintainability.

---

# Generated Code (PayrollMapperImpl)

One of the biggest advantages of MapStruct is that it generates **normal Java code**.

Unlike reflection-based libraries, MapStruct creates an implementation class during compilation.

Developers only write:

```java
@Mapper(componentModel = "spring")
public interface PayrollMapper {

    PayrollResponse toResponse(Payroll payroll);

    Payroll toEntity(CreatePayrollRequest request);

    void updateEntity(
            UpdatePayrollRequest request,
            @MappingTarget Payroll payroll);

}
```

No implementation is written manually.

---

# Where is the Generated Class?

After running:

```text
mvn clean install
```

MapStruct generates:

```text
PayrollMapperImpl.java
```

Location:

```text
target/
└── generated-sources/
    └── annotations/
        └── PayrollMapperImpl.java
```

This folder is automatically regenerated every time the project is compiled.

Developers should never edit these files manually.

---

# Generated Method – Request → Entity

Our mapper contains:

```java
Payroll toEntity(CreatePayrollRequest request);
```

MapStruct generated code similar to:

```java
@Override
public Payroll toEntity(CreatePayrollRequest request) {

    if (request == null) {
        return null;
    }

    Payroll.PayrollBuilder payroll = Payroll.builder();

    payroll.employeeId(request.employeeId());
    payroll.basicSalary(request.basicSalary());
    payroll.bonus(request.bonus());
    payroll.tax(request.tax());

    return payroll.build();
}
```

Notice that this is ordinary Java code.

No reflection is used.

---

# Generated Method – Entity → Response

Mapper:

```java
PayrollResponse toResponse(Payroll payroll);
```

Generated implementation:

```java
@Override
public PayrollResponse toResponse(Payroll payroll) {

    if (payroll == null) {
        return null;
    }

    return new PayrollResponse(
            payroll.getId(),
            payroll.getEmployeeId(),
            payroll.getBasicSalary(),
            payroll.getBonus(),
            payroll.getTax(),
            payroll.getNetSalary()
    );
}
```

Again, the generated code looks exactly like code written by a developer.

---

# Generated Method – Update Existing Entity

Mapper:

```java
void updateEntity(
        UpdatePayrollRequest request,
        @MappingTarget Payroll payroll);
```

Generated implementation:

```java
@Override
public void updateEntity(
        UpdatePayrollRequest request,
        Payroll payroll) {

    if (request == null) {
        return;
    }

    payroll.setBasicSalary(request.basicSalary());
    payroll.setBonus(request.bonus());
    payroll.setTax(request.tax());

}
```

Notice:

- No new Payroll object is created.
- Existing entity is modified.
- Ignored fields remain unchanged.

---

# Compile-Time Generation

The important point is:

The compiler generates this class **before the application starts**.

Runtime execution simply calls:

```java
payrollMapper.toEntity(request);
```

which internally executes:

```java
PayrollMapperImpl.toEntity(request);
```

Since it is ordinary Java code:

- No reflection
- No runtime object inspection
- No performance penalty

---

# Why is this Faster?

Reflection-based frameworks inspect classes during runtime.

Typical runtime process:

```
Read Object

↓

Inspect Fields

↓

Match Names

↓

Copy Values
```

This happens every time mapping occurs.

MapStruct avoids this completely.

Instead, it executes pre-generated Java code:

```
Method Call

↓

Setter Invocation

↓

Completed
```

Therefore, MapStruct performs almost identically to manually written mapping code.

---

# Why Developers Should Read Generated Code

Generated classes help developers understand:

- What MapStruct actually generates.
- Why a field is or isn't mapped.
- Whether an `ignore=true` annotation is working.
- How update mappings are implemented.

During debugging, opening `PayrollMapperImpl` often answers mapping-related questions immediately.

---

# WorkSphere Example

During the Payroll Service implementation, we inspected:

```
PayrollMapperImpl.java
```

We confirmed that:

- `id` was ignored.
- `employeeId` remained unchanged during updates.
- `netSalary` was not copied from the request.
- `@MappingTarget` updated the existing entity.

This verified that our mapper behaved exactly as expected.

---

# Summary

MapStruct generates normal Java classes during compilation.

Developers write only the mapper interface, while the framework generates the implementation automatically.

Because the generated code is ordinary Java code:

- It is easy to debug.
- It is type-safe.
- It performs as fast as manual mapping.
- It avoids runtime reflection.

This compile-time generation is the primary reason why MapStruct is preferred in enterprise Spring Boot applications.

---

# MapStruct vs ModelMapper

Both MapStruct and ModelMapper are used for object mapping, but they work differently.

| Feature | MapStruct | ModelMapper |
|----------|------------|-------------|
| Mapping Time | Compile Time | Runtime |
| Implementation | Generated Java Code | Reflection |
| Performance | Very Fast | Slower |
| Type Safety | Compile-time Validation | Runtime Validation |
| Debugging | Easy (Generated Code) | Difficult |
| Enterprise Usage | Highly Preferred | Mostly Small Projects / Prototypes |

---

# Why did we choose MapStruct?

For the WorkSphere project, MapStruct was chosen because:

- It generates Java code during compilation.
- It provides excellent performance.
- It is easy to debug.
- Mapping errors are detected during compilation.
- It keeps the service layer clean.

In enterprise Spring Boot applications, MapStruct is generally preferred over reflection-based mapping libraries.

---

# Best Practices

During WorkSphere development, the following best practices were followed:

### 1. One Mapper Per Microservice

Each microservice should have its own mapper.

Example:

```
EmployeeMapper

DepartmentMapper

PayrollMapper
```

This keeps responsibilities separated.

---

### 2. Keep Business Logic Out of Mapper

A mapper should only copy data.

Incorrect:

```java
netSalary = basicSalary + bonus - tax;
```

Correct:

```java
payroll.setNetSalary(
        calculateNetSalary(...)
);
```

Business logic belongs in the Service layer.

---

### 3. Ignore Immutable Fields

During updates:

```java
@Mapping(target = "id", ignore = true)

@Mapping(target = "employeeId", ignore = true)
```

Immutable fields should never be modified by clients.

---

### 4. Calculate Derived Fields in Service Layer

Fields like:

```
netSalary
```

should always be calculated in the service.

Never trust values received from clients.

---

### 5. Never Modify Generated Classes

Files inside:

```
target/generated-sources/annotations/
```

are regenerated every compilation.

Changes made there will be lost.

Always modify only:

```
PayrollMapper.java
```

---

### 6. Keep Mapper Focused

A mapper should only convert objects.

Avoid adding:

- Database access
- API calls
- Validation
- Business calculations

These belong in other layers.

---

# Common Interview Questions

### Q1. What is MapStruct?

MapStruct is a compile-time object mapping framework that generates Java mapping code using annotation processing.

---

### Q2. Why is MapStruct faster than ModelMapper?

Because MapStruct generates normal Java code during compilation.

It does not use reflection at runtime.

---

### Q3. What does componentModel = "spring" do?

It registers the generated mapper implementation as a Spring Bean.

This allows dependency injection.

---

### Q4. What is @MappingTarget?

It updates an existing object instead of creating a new one.

Useful for update APIs.

---

### Q5. Where is PayrollMapperImpl created?

```
target/generated-sources/annotations/
```

Generated automatically during Maven compilation.

---

### Q6. Why ignore fields like ID?

Primary keys and immutable fields should never be modified by clients.

Ignoring them protects data integrity.

---

### Q7. Why didn't we map netSalary?

Because it is derived using business logic.

Business calculations should remain inside the Service layer.

---

# WorkSphere Implementation Summary

In the Payroll Service, MapStruct is responsible for:

✔ Converting CreatePayrollRequest → Payroll

✔ Converting Payroll → PayrollResponse

✔ Updating Payroll using UpdatePayrollRequest

✔ Ignoring immutable fields

✔ Allowing the Service layer to calculate netSalary

✔ Eliminating repetitive mapping code

---

# Key Takeaways

After completing this implementation, we learned:

- Why DTOs are important.
- Why object mapping is required.
- The limitations of manual mapping.
- How MapStruct generates code.
- How Spring integrates with MapStruct.
- How @MappingTarget updates existing entities.
- Why compile-time mapping is preferred in enterprise applications.

---

# Conclusion

MapStruct significantly reduces boilerplate code while improving readability, maintainability, and performance.

By generating Java code during compilation, it provides the speed of manual mapping with the simplicity of declarative annotations.

For these reasons, MapStruct has become the preferred object mapping framework in modern Spring Boot microservice architectures.
