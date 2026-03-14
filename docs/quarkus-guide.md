# Quarkus Adoption Guide

Guide for adopting JFrame in Quarkus applications via the `jframe-quarkus-*` modules.

## Overview

JFrame now supports Quarkus via a dedicated set of adapter modules. These modules integrate JFrame's exception handling, request logging, JPA search, and OpenTelemetry tracing into the Quarkus/CDI/JAX-RS runtime. All modules share framework-agnostic code from `jframe-core`, ensuring consistent behavior across Spring Boot and Quarkus projects.

**Available Quarkus modules:**

| Module | Purpose |
|--------|---------|
| `jframe-quarkus-core` | JAX-RS exception mappers, request logging filters, HTTP status |
| `jframe-quarkus-jpa` | Panache search integration and page mapping |
| `jframe-quarkus-otlp` | OpenTelemetry tracing with CDI interceptors |

---

## Module Overview

### jframe-quarkus-core

```
jframe-quarkus-core/src/main/java/io/github/jframe/
├── exception/mapper/
│   ├── ApiExceptionMapper.java            # Maps ApiException → JAX-RS Response
│   ├── HttpExceptionMapper.java           # Maps HttpException → JAX-RS Response
│   ├── ValidationExceptionMapper.java     # Maps ValidationException → 400 response
│   ├── RateLimitExceededExceptionMapper.java  # Maps rate limit → 429 response
│   └── ThrowableMapper.java              # Fallback mapper for unhandled throwables
├── logging/filter/
│   ├── RequestIdFilter.java              # Generates/extracts x-request-id header
│   ├── TransactionIdFilter.java          # Tracks x-transaction-id header
│   ├── RequestDurationFilter.java        # Measures and logs request duration
│   └── RequestResponseLogFilter.java     # Logs request/response bodies
└── http/
    └── QuarkusHttpStatus.java            # HTTP status mapping for Quarkus
```

### jframe-quarkus-jpa

```
jframe-quarkus-jpa/src/main/java/io/github/jframe/datasource/
├── PanacheSearchRepository.java          # Base repository with search support
├── PanacheSortAdapter.java               # Converts JFrame sort to Panache sort
├── QuarkusPageMapper.java                # Maps Panache results to PageResource
└── QuarkusPageAdapter.java               # Adapts Panache pagination output
```

### jframe-quarkus-otlp

```
jframe-quarkus-otlp/src/main/java/io/github/jframe/tracing/
├── TracingInterceptor.java               # CDI interceptor for @Traced methods
├── TimerInterceptor.java                 # CDI interceptor for @LogExecutionTime methods
├── QuarkusSpanManager.java               # OpenTelemetry span lifecycle management
├── TracingResponseFilter.java            # Adds trace/span IDs to error responses
├── OpenTelemetryConfig.java              # Quarkus OTEL configuration bean
└── util/
    └── QuarkusAuthenticationUtil.java    # CDI-compatible auth context utilities
```

**Annotations:**
- `@Traced` — Marks a CDI bean method for automatic span creation
- `@LogExecutionTime` — Logs method execution time via CDI interceptor

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // Exception mappers and request logging (required)
    implementation("io.github.jframeoss:jframe-quarkus-core:0.10.0-SNAPSHOT")

    // Panache search integration (optional)
    implementation("io.github.jframeoss:jframe-quarkus-jpa:0.10.0-SNAPSHOT")

    // OpenTelemetry tracing (optional)
    implementation("io.github.jframeoss:jframe-quarkus-otlp:0.10.0-SNAPSHOT")
}
```

### Maven

```xml
<dependencies>
    <!-- Exception mappers and request logging (required) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-quarkus-core</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>

    <!-- Panache search integration (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-quarkus-jpa</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>

    <!-- OpenTelemetry tracing (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-quarkus-otlp</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

---

## Exception Handling

JFrame's exception hierarchy maps automatically to JAX-RS responses via the exception mappers in `jframe-quarkus-core`. No additional configuration is required — the mappers are discovered via CDI.

**Exception → HTTP Status Mapping:**

| Exception | HTTP Status |
|-----------|-------------|
| `BadRequestException` | 400 Bad Request |
| `UnauthorizedRequestException` | 401 Unauthorized |
| `ResourceNotFoundException` | 404 Not Found |
| `ValidationException` | 400 Bad Request (with field errors) |
| `ApiException` | Configurable (custom error codes) |
| `RateLimitExceededException` | 429 Too Many Requests |
| `Throwable` (fallback) | 500 Internal Server Error |

**Example — throwing a JFrame exception in a JAX-RS resource:**

```java
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.ResourceNotFoundException;
import io.github.jframe.validation.ValidationResult;
import io.github.jframe.exception.core.ValidationException;

@Path("/users")
@ApplicationScoped
public class UserResource {

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") Long id) {
        User user = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return Response.ok(user).build();
    }

    @POST
    public Response createUser(CreateUserRequest request) {
        ValidationResult result = new ValidationResult();

        result.rejectField("email", request.getEmail())
            .whenNull()
            .orWhen(e -> !e.contains("@"), "email.invalid");

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }

        // → Automatically returns 400 with field-level error details
        User created = userService.create(request);
        return Response.status(201).entity(created).build();
    }
}
```

---

## Search / JPA

Use `PanacheSearchRepository` as the base repository for entities that require dynamic search and pagination. It integrates JFrame's search criteria system with Quarkus Panache.

```java
import io.github.jframe.datasource.PanacheSearchRepository;
import io.github.jframe.datasource.search.model.SearchInput;
import io.github.jframe.datasource.search.model.resource.PageResource;

@ApplicationScoped
public class UserSearchService {

    @Inject
    PanacheSearchRepository<User> userRepository;

    public PageResource<UserDto> search(UserSearchInput input) {
        List<SearchCriterium> criteria = new ArrayList<>();

        if (input.getFirstName() != null) {
            criteria.add(new FuzzyTextField("firstName", input.getFirstName()));
        }
        if (input.getStatus() != null) {
            criteria.add(new EnumField("status", UserStatus.class, input.getStatus()));
        }

        return userRepository.search(criteria, input, UserDto::from);
    }
}
```

**Panache sort integration** — `PanacheSortAdapter` converts JFrame's `SortableColumn` model to Panache's `Sort` type automatically. `QuarkusPageMapper` converts Panache `PanacheQuery` results to JFrame's `PageResource` response format.

---

## Tracing

`jframe-quarkus-otlp` provides two CDI interceptor annotations for tracing and execution time logging.

### @Traced — Automatic Span Creation

Annotate any CDI bean method to create an OpenTelemetry span automatically:

```java
import io.github.jframe.tracing.Traced;

@ApplicationScoped
public class OrderService {

    @Traced
    public Order processOrder(Long orderId) {
        // A span named "OrderService.processOrder" is created automatically
        // Span is closed when the method returns or throws
        return orderRepository.findById(orderId);
    }
}
```

### @LogExecutionTime — Execution Timer

Annotate methods to log execution time:

```java
import io.github.jframe.tracing.LogExecutionTime;

@ApplicationScoped
public class ReportService {

    @LogExecutionTime
    public Report generateReport(ReportRequest request) {
        // Execution time is logged automatically at INFO level
        return buildReport(request);
    }
}
```

### QuarkusSpanManager

For manual span management:

```java
import io.github.jframe.tracing.QuarkusSpanManager;

@ApplicationScoped
public class AuditService {

    @Inject
    QuarkusSpanManager spanManager;

    public void auditAction(String action) {
        spanManager.startSpan("audit." + action);
        try {
            performAudit(action);
        } finally {
            spanManager.endSpan();
        }
    }
}
```

---

## Configuration

JFrame uses the same `jframe.*` property namespace in both Spring Boot and Quarkus. In Quarkus, configure via `application.properties` using [Smallrye Config](https://smallrye.io/smallrye-config/) (Quarkus's configuration system).

### application.properties (Quarkus)

```properties
# Application metadata
jframe.application.name=my-quarkus-app
jframe.application.group=com.example
jframe.application.version=1.0.0
jframe.application.environment=production

# Request logging
jframe.logging.disabled=false
jframe.logging.response-length=10000
jframe.logging.fields-to-mask=password,secret,token

# Logging filter toggles
jframe.logging.filters.request-id.enabled=true
jframe.logging.filters.transaction-id.enabled=true
jframe.logging.filters.request-duration.enabled=true
jframe.logging.filters.request-response.enabled=true

# OpenTelemetry
jframe.otlp.disabled=false
jframe.otlp.url=http://localhost:4317
jframe.otlp.sampling-rate=1.0
```

### Equivalent Spring Boot application.yml

```yaml
jframe:
  application:
    name: my-spring-app
    group: com.example
    version: 1.0.0
    environment: production

  logging:
    disabled: false
    response-length: 10000
    fields-to-mask: [password, secret, token]
    filters:
      request-id:
        enabled: true
      transaction-id:
        enabled: true
      request-duration:
        enabled: true
      request-response:
        enabled: true

  otlp:
    disabled: false
    url: http://localhost:4317
    sampling-rate: 1.0
```

> **Note:** Quarkus uses Smallrye Config (flat `application.properties` format). Spring Boot uses YAML hierarchical format. The effective `jframe.*` keys are identical.

---

## Spring Boot vs Quarkus Comparison

| Concern | Spring Boot | Quarkus |
|---------|-------------|---------|
| **Dependency Injection** | Spring IoC (`@Autowired`, `@Component`) | CDI (`@Inject`, `@ApplicationScoped`) |
| **REST Controllers** | `@RestController`, `@RequestMapping` | `@Path`, `@GET`/`@POST` (JAX-RS) |
| **Auto-configuration** | `@AutoConfiguration` + `AutoConfiguration.imports` | CDI bean discovery (automatic) |
| **JPA / Persistence** | Spring Data JPA + `JpaRepository` | Quarkus Panache + `PanacheRepository` |
| **Configuration** | `@ConfigurationProperties`, `application.yml` | Smallrye Config, `application.properties` |
| **Exception Handling** | `@ControllerAdvice` / `ResponseEntityExceptionHandler` | `ExceptionMapper<T>` (JAX-RS) |
| **AOP / Interceptors** | Spring AOP (`@Aspect`) | CDI Interceptors (`@Interceptor`) |
| **JFrame module** | `jframe-spring-*` | `jframe-quarkus-*` |
| **Shared core** | `jframe-core` (transitive) | `jframe-core` (transitive) |

---

## Shared Core

`jframe-core` is a framework-agnostic module shared between both the Spring Boot and Quarkus adapters. It provides:

- **Exception hierarchy** — `JFrameException`, `HttpException`, `ApiException`, `ValidationException`, `BadRequestException`, `UnauthorizedRequestException`, `ResourceNotFoundException`, `InternalServerErrorException`
- **Validation framework** — `ValidationResult`, `FieldRejection`, `Validator<T>`, `ValidationError`
- **HTTP status abstraction** — Framework-neutral status code mapping
- **Search specifications** — `SearchInput`, `SearchCriterium`, field types (`TextField`, `FuzzyTextField`, `EnumField`, `NumericField`, `DateField`, `BooleanField`, `MultiTextField`, `MultiEnumField`)

`jframe-core` is a **transitive dependency** — it is automatically included when you add `jframe-quarkus-core` or `jframe-spring-core`. No direct dependency declaration is needed.

---

## See Also

- [Migration Guide](./migration-guide.md) — Migrating from `jframe-starter-*` to `jframe-spring-*`
- [Architecture Overview](../src/docs/architecture.md)
- [jframe-spring-core](../src/docs/jframe-spring-core.md)
- [jframe-spring-jpa](../src/docs/jframe-spring-jpa.md)
- [jframe-spring-otlp](../src/docs/jframe-spring-otlp.md)
