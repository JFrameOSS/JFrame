# Quarkus Integration Guide

Comprehensive guide to integrating JFrame into Quarkus applications. Covers all three modules: `jframe-quarkus-core`, `jframe-quarkus-jpa`, and `jframe-quarkus-otlp`.

## Quick start

### 1. Add dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.jframeoss:jframe-quarkus-core:1.0.0")
    implementation("io.github.jframeoss:jframe-quarkus-jpa:1.0.0")    // optional
    implementation("io.github.jframeoss:jframe-quarkus-otlp:1.0.0")   // optional
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-quarkus-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-quarkus-jpa</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-quarkus-otlp</artifactId>
    <version>1.0.0</version>
</dependency>
```

> **Note:** `jframe-core` is pulled in transitively — do not add it as an explicit dependency.

### 2. Configure

```properties
# application.properties — required
jframe.application.name=my-service
jframe.application.group=com.example
jframe.application.version=1.0.0

# Optional
jframe.application.environment=dev
```

### 3. Done

All HTTP filters, exception handlers, and logging are auto-configured via CDI/Jandex — no `beans.xml` or configuration classes needed.

---

## Module overview

| Module | Provides |
|--------|----------|
| `jframe-quarkus-core` | HTTP logging, exception handling, request correlation, caching, Jackson config |
| `jframe-quarkus-jpa` | Panache search specifications, pagination, SQL query logging |
| `jframe-quarkus-otlp` | OpenTelemetry tracing, `@Traced` interceptor, outbound HTTP tracing |

### Dependencies between modules

```
jframe-quarkus-core  ← required by all
jframe-quarkus-jpa   ← requires: quarkus-hibernate-orm-panache
jframe-quarkus-otlp  ← requires: quarkus-opentelemetry
```

### Required Quarkus extensions

Add these to your project depending on which JFrame modules you use:

```kotlin
// For jframe-quarkus-jpa
implementation("io.quarkus:quarkus-hibernate-orm-panache")
implementation("io.quarkus:quarkus-jdbc-postgresql")  // or your DB driver

// For jframe-quarkus-otlp
implementation("io.quarkus:quarkus-opentelemetry")
```

---

## Core module — `jframe-quarkus-core`

### HTTP logging filters

JAX-RS container filters log every HTTP request and response with structured MDC fields following [Elastic Common Schema (ECS)](https://www.elastic.co/guide/en/ecs/current/index.html).

#### Filter chain (execution order)

| Priority | Filter | Purpose |
|----------|--------|---------|
| 50 | `TracingResponseFilter` | Trace/span ID propagation (requires `quarkus-otlp`) |
| 100 | `TransactionIdFilter` | Reads/generates transaction ID from header, stores in MDC (`transaction.id`) |
| 200 | `RequestIdFilter` | Generates UUID per request, stores in MDC (`request.id`) |
| 300 | `RequestDurationFilter` | Measures and logs request duration |
| 400 | `RequestResponseLogFilter` | Logs full request/response with body masking |

#### Accessing request context

```java
String requestId = RequestId.get();       // UUID string or null
String txId = TransactionId.get();        // UUID string or null
```

Both use `InheritableThreadLocal` — child threads inherit the parent's values.

#### Configuration

```properties
# Logging (all optional — sensible defaults provided)
jframe.logging.disabled=false
jframe.logging.response-length=-1
jframe.logging.exclude-paths=/health,/actuator/*
jframe.logging.fields-to-mask=password,client_secret,secret
```

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.logging.disabled` | `false` | Disable all HTTP logging |
| `jframe.logging.response-length` | `-1` | Max response body chars to log (-1 = unlimited) |
| `jframe.logging.exclude-paths` | `/actuator/*` | Path patterns to skip |
| `jframe.logging.fields-to-mask` | `password, keyPassphrase, client_secret, secret` | Sensitive JSON fields to mask |
| `jframe.logging.allowed-content-types` | `application/json, application/xml, text/plain, ...` | Content types eligible for body logging |
| `jframe.logging.body-excluded-content-types` | `multipart/form-data` | Content types excluded from body logging |

---

### Exception mappers

JAX-RS `@Provider` exception mappers convert JFrame exceptions to structured JSON error responses.

#### Handled exceptions

| Exception | HTTP Status | Response type |
|-----------|-------------|---------------|
| `BadRequestException` | 400 | `ErrorResponseResource` |
| `UnauthorizedRequestException` | 401 | `ErrorResponseResource` |
| `ResourceNotFoundException` | 404 | `ErrorResponseResource` |
| `ApiException` | 400 | `ApiErrorResponseResource` (with error code + reason) |
| `ValidationException` | 400 | `ValidationErrorResponseResource` (with field errors) |
| `RateLimitExceededException` | 429 | `RateLimitErrorResponseResource` (with limit headers) |
| `Throwable` (catch-all) | 500 | `ErrorResponseResource` |

#### Error response format

```json
{
  "statusCode": 404,
  "statusText": "Not Found",
  "errorMessage": "User not found",
  "method": "GET",
  "uri": "/api/users/42",
  "transactionId": "a1b2c3d4-..."
}
```

When `quarkus-otlp` is active, `traceId` and `spanId` are added automatically.

#### Throwing exceptions

```java
// Simple HTTP exceptions
throw new ResourceNotFoundException("User not found");
throw new BadRequestException("Invalid input");

// API errors with application-specific error codes
throw new ApiException(UserErrors.USER_DISABLED);

// Validation errors
Validator<CreateUserRequest> validator = (obj, result) -> {
    result.rejectField("email", obj.getEmail())
        .whenNull()
        .orWhen(e -> !e.contains("@"), "invalid_email");
};
validator.validateAndThrow(request);

// Rate limiting
throw new RateLimitExceededException(100, 0, resetDate);
```

#### Custom error enrichers

Add fields to every error response by implementing `ErrorResponseEnricher`:

```java
@ApplicationScoped
public class TenantEnricher implements ErrorResponseEnricher {
    @Override
    public void enrich(ErrorResponseResource resource, Throwable t,
                       ContainerRequestContext request, int statusCode) {
        resource.putDetail("tenantId", TenantContext.current());
    }
}
```

9 built-in enrichers add method, URI, status text, transaction ID, trace ID, and more.

---

### Outbound HTTP filters

Propagate correlation IDs and log outbound JAX-RS client calls.

#### Correlation ID propagation

`OutboundCorrelationFilter` adds `X-Request-Id` and `X-Transaction-Id` headers to outbound requests:

```java
@RegisterRestClient
@RegisterProvider(OutboundCorrelationFilter.class)
public interface UserClient {
    @GET @Path("/users/{id}")
    User getUser(@PathParam("id") Long id);
}
```

#### Outbound request/response logging

`OutboundLoggingFilter` logs request/response details with ECS-compliant MDC fields:

```java
@RegisterRestClient
@RegisterProvider(OutboundCorrelationFilter.class)
@RegisterProvider(OutboundLoggingFilter.class)
public interface PaymentClient {
    @POST @Path("/charges")
    ChargeResult charge(ChargeRequest req);
}
```

---

### Request-scoped caching

Prevent duplicate database queries within a single HTTP request:

```java
@RequestScoped
public class UserCache extends RequestScopedCache<Long, User> {
    @Override
    protected Long getId(User user) {
        return user.getId();
    }
}

@ApplicationScoped
public class UserService {
    @Inject UserCache cache;
    @Inject UserRepository repo;

    public User getUser(Long id) {
        return cache.getOrLoad(id, () -> repo.findById(id));
    }
}
```

The cache is scoped to the current HTTP request and cleared automatically.

---

### Scheduled task context

`@ScheduledWithContext` CDI interceptor binding adds request/transaction IDs to scheduled methods:

```java
@ApplicationScoped
public class QueueProcessor {
    @Scheduled(every = "1m")
    @ScheduledWithContext
    public void processQueue() {
        // RequestId and TransactionId are set (same UUID)
        // MDC contains request.id and transaction.id
        log.info("Processing queue");
        // Cleanup happens automatically
    }
}
```

---

### Jackson configuration

`JFrameJacksonCustomizer` produces a configured `ObjectMapper`:

- Property naming: `lowerCamelCase`
- No pretty-print
- Always include values (no null exclusion)
- Ignore unknown properties on deserialization

---

### Startup logging

`CorePackageLogger` logs application metadata and registered JFrame filters at startup:

```
INFO  [CorePackageLogger] JFrame Quarkus Core initialized
  Application: my-service (com.example) v1.0.0 [dev]
  Registered filters: RequestIdFilter, TransactionIdFilter, ...
```

---

## JPA module — `jframe-quarkus-jpa`

### SQL query logging

`DatasourceProxyProducer` wraps your `DataSource` with a logging proxy that pretty-prints all SQL queries at `DEBUG` level. Activates automatically — no configuration needed.

```
DEBUG [SLF4JQueryLoggingListener] —
    select u.id, u.name, u.email
    from users u
    where u.status = ?
```

#### Suppress logging for batch operations

```java
import io.github.jframe.datasource.logging.SqlStatementLogging;

try (var state = SqlStatementLogging.suppress()) {
    repository.saveAll(largeList);
    // SQL queries won't be logged
}
```

---

### Search specifications

Build type-safe JPA Criteria API queries from frontend search inputs using Panache.

#### Step 1: Define search metadata

Map frontend field names to database columns:

```java
@ApplicationScoped
public class UserSearchMetaData extends AbstractPanacheSearchMetaData {
    public UserSearchMetaData() {
        // addField(frontendName, dbColumn, searchType, sortable)
        addField("name", "name", SearchType.FUZZY_TEXT, true);
        addField("email", "email", SearchType.TEXT, true);
        addField("status", "status", SearchType.ENUM, UserStatus.class, true);
        addField("role", "role", SearchType.MULTI_ENUM, Role.class, false);
        addField("createdAt", "createdAt", SearchType.DATE, true);
        addField("age", "age", SearchType.NUMERIC, true);
        addField("active", "active", SearchType.BOOLEAN, false);

        // Multi-column fuzzy: search across multiple columns with one term
        addField("fullName",
            List.of("firstName", "lastName"),
            SearchType.MULTI_COLUMN_FUZZY, false);
    }
}
```

#### Step 2: Implement search repository

```java
@ApplicationScoped
public class UserRepository extends PanacheSearchRepository<User> {
    @Inject EntityManager em;

    @Override
    protected Class<User> entityClass() { return User.class; }

    @Override
    protected EntityManager entityManager() { return em; }
}
```

#### Step 3: Execute a search

```java
@ApplicationScoped
public class UserSearchService {
    @Inject UserRepository repository;
    @Inject UserSearchMetaData metaData;

    public PageResource<UserDto> search(SortablePageInput input) {
        PageResource<User> page = repository.search(input, metaData);
        return new UserPageMapper().map(page);
    }
}
```

#### Step 4: Map pages to DTOs

```java
public class UserPageMapper extends QuarkusPageMapper<User, UserDto> {
    @Override
    protected UserDto mapItem(User entity) {
        return new UserDto(entity.getId(), entity.getName());
    }
}
```

#### Search types

| SearchType | SQL operator | Example input |
|-----------|-------------|---------------|
| `TEXT` | `= ?` | `"john@example.com"` |
| `FUZZY_TEXT` | `LIKE %?%` | `"john"` |
| `MULTI_TEXT` | `IN (?, ?, ...)` | `["admin", "user"]` |
| `MULTI_FUZZY` | `LIKE %?% AND/OR LIKE %?%` | `["john", "doe"]` |
| `MULTI_COLUMN_FUZZY` | `col1 LIKE %?% OR col2 LIKE %?%` | `"john"` |
| `NUMERIC` | `= ?` | `42` |
| `BOOLEAN` | `= ?` | `true` |
| `DATE` | `BETWEEN ? AND ?` | from/to dates |
| `ENUM` | `= ?` | `"ACTIVE"` |
| `MULTI_ENUM` | `IN (?, ?, ...)` | `["ACTIVE", "PENDING"]` |

#### Inverse search

Prefix any search value with `!` to negate the predicate:

```json
{ "fieldName": "status", "textValue": "!DISABLED" }
```

Generates `status != 'DISABLED'` instead of `status = 'DISABLED'`.

#### Frontend request format

```json
{
  "pageNumber": 0,
  "pageSize": 20,
  "sortOrder": [
    { "column": "name", "direction": "ASC" }
  ],
  "searchInputs": [
    { "fieldName": "name", "textValue": "john" },
    { "fieldName": "status", "textValue": "ACTIVE" }
  ]
}
```

#### Response format

```json
{
  "totalElements": 142,
  "totalPages": 8,
  "pageSize": 20,
  "pageNumber": 0,
  "content": [ ... ]
}
```

#### Sort adapter

`PanacheSortAdapter` converts `List<SortableColumn>` to Panache `Sort`:

```java
Sort sort = PanacheSortAdapter.toSort(input.getSortOrder(), metaData);
```

This is handled automatically by `PanacheSearchRepository` — you only need it for custom queries.

---

## OTLP module — `jframe-quarkus-otlp`

### Setup

```properties
# application.properties
jframe.otlp.disabled=false
jframe.otlp.url=http://localhost:4318
jframe.otlp.exporter=otlp
jframe.otlp.sampling-rate=1.0
jframe.otlp.timeout=10s
jframe.otlp.excluded-methods=health,actuator,ping,status,info,metrics
```

Requires `quarkus-opentelemetry` extension on your classpath:

```kotlin
implementation("io.quarkus:quarkus-opentelemetry")
```

#### OTLP configuration properties

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.otlp.disabled` | `false` | Disable tracing entirely |
| `jframe.otlp.url` | `http://localhost:4318` | OTLP collector endpoint |
| `jframe.otlp.exporter` | `otlp` | Exporter: `otlp`, `jaeger`, `zipkin` |
| `jframe.otlp.sampling-rate` | `1.0` | Sampling rate (0.0–1.0) |
| `jframe.otlp.timeout` | `10s` | Export timeout |
| `jframe.otlp.excluded-methods` | `health, actuator, ping, status, info, metrics` | Method names excluded from tracing |
| `jframe.otlp.propagators` | `tracecontext,baggage` | W3C trace context propagators |

#### Auto-instrumentation defaults

JFrame configures Quarkus OpenTelemetry auto-instrumentation via `microprofile-config.properties`:

| Instrumentation | Enabled | Property |
|----------------|---------|----------|
| JDBC | ✅ | `quarkus.datasource.jdbc.telemetry=true` |
| RESTEasy (server) | ✅ | `quarkus.otel.instrument.resteasy=true` |
| RESTEasy (client) | ✅ | `quarkus.otel.instrument.resteasy-client=true` |
| gRPC | ✅ | `quarkus.otel.instrument.grpc=true` |
| Messaging (Kafka/RabbitMQ) | ✅ | `quarkus.otel.instrument.messaging=true` |
| Vert.x HTTP | ✅ | `quarkus.otel.instrument.rest=true` |

Override any of these in your `application.properties`:

```properties
# Disable gRPC tracing
quarkus.otel.instrument.grpc=false
```

---

### Method tracing with `@Traced`

CDI interceptor that creates an OTEL span around each intercepted method.

#### Explicit annotation

```java
@ApplicationScoped
public class OrderService {
    @Traced
    public Order createOrder(CreateOrderRequest req) {
        // Span "OrderService.createOrder" created automatically
    }
}
```

Apply at class level to trace all public methods:

```java
@ApplicationScoped
@Traced
public class PaymentService {
    public void charge(Payment p) { /* traced */ }
    public void refund(Payment p) { /* traced */ }
}
```

#### Automatic tracing via build-time processor

The `jframe-quarkus-otlp-deployment` module includes a Quarkus build-time processor that automatically adds `@Traced` to all `@ApplicationScoped` beans via Jandex bytecode transformation. This means your services get tracing without manual annotation.

> **Note:** Add the deployment module as a dependency if you want automatic tracing:
> ```kotlin
> implementation("io.github.jframeoss:jframe-quarkus-otlp-deployment:1.0.0")
> ```

#### Custom span names

```java
@Traced("process-payment")
public void charge(Payment p) {
    // Span name: "process-payment"
}
```

#### Record parameters

```java
@Traced(recordParameters = true)
public void charge(Payment p) {
    // Method parameters recorded as span attributes
}
```

#### Excluded methods

The interceptor skips:

- Getters/setters: `get*()`, `set*()`, `is*()`
- Common methods: `toString()`, `hashCode()`, `equals()`, `clone()`
- Methods matching `jframe.otlp.excluded-methods` config

#### Span attributes

Each span includes:

| Attribute | Value |
|-----------|-------|
| `service.name` | Class name |
| `service.method` | Method name |
| `http.remote_user` | Authenticated user (if available) |
| `http.transaction_id` | Transaction ID from MDC |
| `http.request_id` | Request ID from MDC |

Errors are recorded via `span.recordException()` and `span.setStatus(StatusCode.ERROR)`.

---

### Execution timing with `@LogExecutionTime`

CDI interceptor that logs method duration at `DEBUG` level without creating OTEL spans:

```java
@ApplicationScoped
public class ReportService {
    @LogExecutionTime
    public Report generate() {
        // Logs: Method 'ReportService.generate' executed in 1234 ms
    }
}
```

Combine both annotations:

```java
@Traced
@LogExecutionTime
public Result compute() {
    // Creates span AND logs duration
}
```

---

### Outbound HTTP tracing

`OutboundTracingFilter` creates CLIENT spans and injects W3C `traceparent` headers for outbound JAX-RS client calls:

```java
@RegisterRestClient
@RegisterProvider(OutboundTracingFilter.class)
public interface PaymentClient {
    @POST @Path("/charges")
    ChargeResult charge(ChargeRequest req);
}
```

Span attributes: `peer.service`, `ext.response.status_code`. Spans marked as ERROR for status >= 400.

#### W3C Trace Context format

```
traceparent: 00-{traceId}-{spanId}-01
```

Quarkus OTEL extension auto-extracts inbound `traceparent` headers.

---

### Trace ID in error responses

`TracingEnricher` adds `traceId` and `spanId` to error response JSON and enriches the OTEL span with error details:

```json
{
  "statusCode": 500,
  "errorMessage": "Internal error",
  "traceId": "abc123...",
  "spanId": "def456..."
}
```

Error span attributes: `error.type`, `error.message`, `http.status_code`, `http.method`, `http.uri`.

---

### Response headers

`TracingResponseFilter` adds trace context to HTTP response headers and populates SLF4J MDC:

```
X-Trace-Id: abc123...
X-Span-Id: def456...
```

MDC fields: `trace.id`, `span.id` — available for log correlation in your logging framework configuration.

---

### Span management

`SpanManager` provides utilities for custom span creation:

```java
@Inject SpanManager spanManager;

Span span = spanManager.startSpan("custom-operation");
try {
    // ... your code
} catch (Exception e) {
    spanManager.recordError(span, e);
    throw e;
} finally {
    span.end();
}
```

---

### Authentication utility

```java
String user = QuarkusAuthenticationUtil.getPrincipal(securityIdentity);
// Returns username or null (null-safe)
```

---

## Complete configuration reference

### Application properties (required)

```properties
jframe.application.name=my-service
jframe.application.group=com.example
jframe.application.version=1.0.0
jframe.application.environment=dev
```

### Logging properties

```properties
jframe.logging.disabled=false
jframe.logging.response-length=-1
jframe.logging.exclude-paths=/health,/actuator/*
jframe.logging.fields-to-mask=password,client_secret,secret
```

### OTLP properties

```properties
jframe.otlp.disabled=false
jframe.otlp.url=http://jaeger:4318
jframe.otlp.exporter=otlp
jframe.otlp.sampling-rate=0.1
jframe.otlp.timeout=10s
```

### Full example (`application.properties`)

```properties
# Application identity
jframe.application.name=order-service
jframe.application.group=com.example
jframe.application.version=1.0.0
jframe.application.environment=production

# HTTP logging
jframe.logging.fields-to-mask=password,creditCard,ssn
jframe.logging.exclude-paths=/health,/ready

# OpenTelemetry
jframe.otlp.disabled=false
jframe.otlp.url=http://otel-collector:4318
jframe.otlp.sampling-rate=0.1
jframe.otlp.timeout=30s
```

---

## Comparison with Spring modules

| Feature | Spring | Quarkus |
|---------|--------|---------|
| HTTP stack | Servlet filters | JAX-RS container filters |
| DI annotations | `@Component`, `@Service` | `@ApplicationScoped`, `@Inject` |
| Exception handling | `@RestControllerAdvice` | JAX-RS `@Provider` exception mappers |
| Tracing mechanism | Spring AOP (`@Aspect`) | CDI interceptors (`@InterceptorBinding`) |
| Auto-tracing | `@Service`, `@Controller`, `@RestController` | `@ApplicationScoped` (via deployment processor) |
| `@Traced` annotation | Optional (auto-traced classes) | Explicit or via deployment processor |
| Data layer | Spring Data JPA | Panache |
| Search metadata | `AbstractSortSearchMetaData` | `AbstractPanacheSearchMetaData` |
| Page mapping | `SpringPageAdapter` / `PageMapper` | `QuarkusPageMapper` |
| OTLP config format | YAML (`application.yml`) | Properties (`application.properties`) |
| OTLP SDK mapping | `otel.*` properties | `quarkus.otel.*` properties |
| OTLP default state | Disabled (`jframe.otlp.disabled: true`) | Enabled (`jframe.otlp.disabled=false`) |

### Shared across both frameworks

- Exception class hierarchy (`BadRequestException`, `ApiException`, `ValidationException`, etc.)
- Validation API (`Validator<T>`, `ValidationResult`)
- Search specifications and field types
- ECS/MDC logging (`EcsFields`, `EcsFieldNames`)
- Request/transaction ID context (`RequestId`, `TransactionId`)
- JSON utilities (`ObjectMappers`)
- SQL logging suppression (`SqlStatementLogging`)

---

## Setup checklist

- [ ] Added `jframe-quarkus-core` dependency
- [ ] Added `jframe-quarkus-jpa` dependency (if using JPA)
- [ ] Added `jframe-quarkus-otlp` dependency (if using tracing)
- [ ] Added required Quarkus extensions (`quarkus-hibernate-orm-panache`, `quarkus-opentelemetry`)
- [ ] Set `jframe.application.name`, `.group`, `.version`
- [ ] Configured `jframe.logging.fields-to-mask` for your sensitive fields
- [ ] Set `jframe.otlp.url` to your OTLP collector (if using tracing)
- [ ] Registered `OutboundCorrelationFilter` on JAX-RS REST clients
- [ ] Registered `OutboundTracingFilter` on JAX-RS REST clients (if using tracing)
- [ ] Verified startup logs show JFrame filters registered
- [ ] Tested exception handling returns structured JSON responses
- [ ] Verified traces appear in your observability backend (Jaeger, Grafana, etc.)
