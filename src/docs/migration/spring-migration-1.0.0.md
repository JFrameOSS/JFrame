# Spring Migration Guide: 0.9.x → 1.0.0

This guide covers migrating Spring Boot applications from `jframe-starter-*` (0.9.x and earlier) to `jframe-spring-*` (1.0.0).

## What changed

JFrame 1.0.0 introduces multi-framework support (Spring Boot + Quarkus). Framework-agnostic code was extracted into a shared `jframe-core` module, and Spring modules were renamed for clarity.

**Breaking changes:**

1. [Module artifact renames](#step-1-update-dependencies) — `starter-*` → `jframe-spring-*`
2. [Kibana → ECS logging migration](#step-2-migrate-kibana-logging-to-ecs) — classes, packages, field keys all changed
3. [HttpStatus → Jakarta WS-RS](#step-3-fix-httpstatus-references) — `org.springframework.http.HttpStatus` → `jakarta.ws.rs.core.Response.Status`
4. [JpaSearchSpecification package change](#step-4-update-jpa-search-imports)
5. [OpenTelemetryConstants relocated](#step-5-update-opentelemetry-constants)

**What did NOT change:**

- GroupId (`io.github.jframeoss`)
- Configuration property namespace (`jframe.*`)
- Auto-configuration behavior
- Exception class names and semantics

**New capabilities:**

- SQL logging suppression (`SqlStatementLogging.suppress()`)
- Improved OTEL span error handling (`span.recordException()`)
- Framework-agnostic search specifications (same API, now in `jframe-core`)
- Enhanced auto-instrumentation defaults (JDBC, Spring Web, Kafka, MongoDB, R2DBC)
- W3C trace context propagator configuration

---

## Step 1: Update dependencies

### Gradle (Kotlin DSL)

```kotlin
// Before (0.9.x)
implementation("io.github.jframeoss:starter-core:0.9.0")
implementation("io.github.jframeoss:starter-jpa:0.9.0")
implementation("io.github.jframeoss:starter-otlp:0.9.0")

// After (1.0.0)
implementation("io.github.jframeoss:jframe-spring-core:1.0.0")
implementation("io.github.jframeoss:jframe-spring-jpa:1.0.0")
implementation("io.github.jframeoss:jframe-spring-otlp:1.0.0")
```

### Maven

```xml
<!-- Before (0.9.x) -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>starter-core</artifactId>
    <version>0.9.0</version>
</dependency>

<!-- After (1.0.0) -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-spring-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

| Old artifactId | New artifactId |
|----------------|----------------|
| `starter-core` | `jframe-spring-core` |
| `starter-jpa` | `jframe-spring-jpa` |
| `starter-otlp` | `jframe-spring-otlp` |

> **Note:** `jframe-core` is pulled in transitively — do not add it as an explicit dependency.

---

## Step 2: Migrate Kibana logging to ECS

All logging classes moved from `io.github.jframe.logging.kibana` to `io.github.jframe.logging.ecs` and were renamed to follow [Elastic Common Schema](https://www.elastic.co/guide/en/ecs/current/index.html) conventions.

### Class renames

| Old class | New class |
|-----------|-----------|
| `KibanaLogField` | `EcsField` |
| `KibanaLogFields` | `EcsFields` |
| `KibanaLogFieldNames` | `EcsFieldNames` |
| `KibanaLogContext` | `EcsLogContext` |
| `AutoCloseableKibanaLogField` | `AutoCloseableEcsField` |
| `AutoCloseableKibanaLogFieldImpl` | `AutoCloseableEcsFieldImpl` |
| `CompoundAutocloseableKibanaLogField` | `CompoundAutoCloseableEcsField` |
| `KibanaLogTypeNames` | `LogTypeNames` |
| `KibanaLogCallResultTypes` | `CallResultTypes` |

### Package change

```java
// Before
import io.github.jframe.logging.kibana.KibanaLogFieldNames;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.kibana.KibanaLogField;
import io.github.jframe.logging.kibana.AutoCloseableKibanaLogField;

// After
import io.github.jframe.logging.ecs.EcsFieldNames;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.ecs.EcsField;
import io.github.jframe.logging.ecs.AutoCloseableEcsField;
```

### Interface method rename

```java
// Before
public class MyTag implements KibanaLogField {
    @Override
    public String getLogName() { return "my.tag"; }
}

// After
public class MyTag implements EcsField {
    @Override
    public String getKey() { return "my.tag"; }
}
```

### Usage migration

```java
// Before
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;
KibanaLogFields.tag(TX_ID, transactionId);
KibanaLogFields.clear(REQUEST_ID);
String value = KibanaLogFields.get(TRACE_ID);

// After
import static io.github.jframe.logging.ecs.EcsFieldNames.*;
EcsFields.tag(TX_ID, transactionId);
EcsFields.clear(REQUEST_ID);
String value = EcsFields.get(TRACE_ID);
```

### Auto-closeable tagging

```java
// Before
try (var fields = KibanaLogFields.tagCloseable(REQUEST_ID, reqId)
        .and(TX_ID, txId)) {
    // MDC populated
}

// After
try (var fields = EcsFields.tagCloseable(REQUEST_ID, reqId)
        .and(TX_ID, txId)) {
    // MDC populated
}
```

### Context handling

```java
// Before
KibanaLogContext context = KibanaLogFields.getContext();
context.registerKibanaLogFieldsInThisThread();

// After
EcsLogContext context = EcsFields.getContext();
context.registerEcsFieldsInThisThread();
```

### MDC key value changes

All MDC field keys changed to ECS-compliant names. Update any Elasticsearch index patterns, Kibana dashboards, saved searches, and log parsing rules.

| Java constant | Old MDC key | New MDC key |
|---|---|---|
| `TX_ID` | `tx_id` | `transaction.id` |
| `REQUEST_ID` | `req_id` | `request.id` |
| `TRACE_ID` | `traceId` | `trace.id` |
| `SPAN_ID` | `spanId` | `span.id` |
| `HTTP_STATUS` | `http_status` | `http.response.status_code` |
| `TX_REQUEST_IP` | `tx_request_ip` | `client.ip` |
| `TX_REQUEST_METHOD` | `tx_request_method` | `http.request.method` |
| `TX_REQUEST_URI` | `tx_request_uri` | `url.path` |
| `USER_NAME` | `user_name` | `user.name` |
| `SESSION_ID` | `session_id` | `session.id` |
| `HOST_NAME` | `host_name` | `host.name` |
| `SOFTWARE_VERSION` | `software_version` | `service.version` |
| `REQUEST_DURATION` | `req_duration` | `event.duration` |
| `TX_DURATION` | `tx_duration` | `transaction.duration.ms` |
| `TX_STATUS` | `tx_status` | `transaction.result` |
| `CALL_REQUEST_METHOD` | `call_request_method` | `http.client.request.method` |
| `CALL_REQUEST_URI` | `call_request_uri` | `url.full` |
| `CALL_STATUS` | `call_status` | `event.outcome` |
| `CALL_DURATION` | `call_duration` | `event.duration.ms` |
| `LOG_TYPE` | `log_type` | `event.category` |

See [ECS naming convention migration](ecs-naming-convention-migration.md) for the complete field mapping table.

### Update logback-spring.xml

If your `logback-spring.xml` pattern references MDC keys directly, update them to the new ECS names:

```xml
<!-- Before -->
<pattern>%d{HH:mm:ss.SSS} %level [TX:%X{tx_id}/TRACE:%X{traceId}] [%thread] [%logger:%line]: %msg%n</pattern>

<!-- After -->
<pattern>%d{HH:mm:ss.SSS} %level [TX:%X{transaction.id}/TRACE:%X{trace.id}] [%thread] [%logger:%line]: %msg%n</pattern>
```

Common substitutions in log patterns:

| Old MDC key | New MDC key |
|---|---|
| `%X{tx_id}` | `%X{transaction.id}` |
| `%X{req_id}` | `%X{request.id}` |
| `%X{traceId}` | `%X{trace.id}` |
| `%X{spanId}` | `%X{span.id}` |
| `%X{http_status}` | `%X{http.response.status_code}` |

> **Note:** If you use a JSON encoder like `LogstashEncoder`, the field names are handled automatically by MDC — no pattern changes needed, but downstream log consumers (Elasticsearch, Kibana dashboards, alerts) must be updated to query the new field names.

---

## Step 3: Fix HttpStatus references

`HttpException` and its subclasses now use `jakarta.ws.rs.core.Response.Status` instead of Spring's `org.springframework.http.HttpStatus`. This makes the exception hierarchy framework-agnostic.

```java
// Before
import org.springframework.http.HttpStatus;
throw new HttpException(HttpStatus.NOT_FOUND, "Not found");

// After
import jakarta.ws.rs.core.Response;
throw new HttpException(Response.Status.NOT_FOUND, "Not found");
```

Custom exceptions:

```java
// Before
import org.springframework.http.HttpStatus;

public class PaymentFailedException extends HttpException {
    public PaymentFailedException() {
        super(HttpStatus.PAYMENT_REQUIRED, "Payment required");
    }
}

// After
import jakarta.ws.rs.core.Response;

public class PaymentFailedException extends HttpException {
    public PaymentFailedException() {
        super(Response.Status.PAYMENT_REQUIRED, "Payment required");
    }
}
```

Status code comparisons:

```java
// Before
if (exception.getHttpStatus() == HttpStatus.NOT_FOUND) { ... }

// After
if (exception.getHttpStatus() == Response.Status.NOT_FOUND) { ... }
```

> **Note:** Enum constant names are identical (`NOT_FOUND`, `BAD_REQUEST`, etc.) — only the package changes. The built-in exception subclasses (`BadRequestException`, `ResourceNotFoundException`, etc.) already use the correct status codes internally.

---

## Step 4: Update JPA search imports

`JpaSearchSpecification` was extracted to `jframe-core` with a simplified package path:

```java
// Before
import io.github.jframe.datasource.search.model.JpaSearchSpecification;

// After
import io.github.jframe.datasource.search.JpaSearchSpecification;
```

Usage is unchanged. The class still implements Spring's `Specification<T>`.

---

## Step 5: Update OpenTelemetry constants

`OpenTelemetryConstants.Attributes` was merged into `EcsFieldNames` as `SPAN_*` enum constants. The `OpenTelemetryConstants` class itself moved to `jframe-core`.

```java
// Before
import static io.github.jframe.OpenTelemetryConstants.Attributes.SERVICE_NAME;
import static io.github.jframe.OpenTelemetryConstants.Attributes.ERROR;
span.setAttribute(SERVICE_NAME, className);
span.setAttribute(ERROR, true);

// After
import static io.github.jframe.logging.ecs.EcsFieldNames.SPAN_SERVICE_NAME;
span.setAttribute(SPAN_SERVICE_NAME.getKey(), className);
span.setStatus(StatusCode.ERROR);  // replaces ERROR boolean attribute
```

| Old constant | New constant |
|---|---|
| `Attributes.SERVICE_NAME` | `SPAN_SERVICE_NAME` |
| `Attributes.SERVICE_METHOD` | `SPAN_SERVICE_METHOD` |
| `Attributes.HTTP_REMOTE_USER` | `SPAN_HTTP_REMOTE_USER` |
| `Attributes.HTTP_TRANSACTION_ID` | `SPAN_HTTP_TRANSACTION_ID` |
| `Attributes.HTTP_REQUEST_ID` | `SPAN_HTTP_REQUEST_ID` |
| `Attributes.HTTP_URI` | `SPAN_HTTP_URI` |
| `Attributes.HTTP_METHOD` | `SPAN_HTTP_METHOD` |
| `Attributes.HTTP_STATUS_CODE` | `SPAN_HTTP_STATUS_CODE` |
| `Attributes.PEER_SERVICE` | `SPAN_PEER_SERVICE` |
| `Attributes.ERROR` | *Removed — use `span.setStatus(StatusCode.ERROR)`* |

See [ECS naming convention migration](ecs-naming-convention-migration.md) for the complete mapping.

---

## Step 6: Check transitive dependencies

`jframe-core` adds these transitive API dependencies:

| Dependency | Notes |
|-----------|-------|
| `commons-lang3` | Was likely already on your classpath |
| `commons-collections4` | Was likely already on your classpath |
| `commons-io` | Was likely already on your classpath |
| `jakarta.ws.rs-api` | **New** — required for framework-agnostic `Response.Status` |

**Action:** If you declared any of these explicitly, check for version conflicts. Remove duplicate declarations if versions match.

---

## Step 7: Review OTLP configuration

### New default excluded methods

`info` and `metrics` are now excluded from tracing by default:

```yaml
jframe:
  otlp:
    excluded-methods:
      - health
      - actuator
      - ping
      - status
      - info      # NEW
      - metrics   # NEW
```

### Auto-instrumentation defaults

The bundled `jframe-properties.yml` now configures OpenTelemetry auto-instrumentation:

| Instrumentation | Enabled | Property |
|----------------|---------|----------|
| JDBC | ✅ | `otel.instrumentation.jdbc.enabled: true` |
| Spring Web (RestTemplate/WebClient) | ✅ | `otel.instrumentation.spring-web.enabled: true` |
| Spring WebMVC | ✅ | `otel.instrumentation.spring-webmvc.enabled: true` |
| Kafka | ✅ | `otel.instrumentation.kafka.enabled: true` |
| MongoDB | ✅ | `otel.instrumentation.mongo.enabled: true` |
| R2DBC | ✅ | `otel.instrumentation.r2dbc.enabled: true` |
| Logback MDC | ✅ | `otel.instrumentation.logback-mdc.enabled: true` |
| Spring WebFlux | ❌ | `otel.instrumentation.spring-webflux.enabled: false` |
| Logback Appender | ❌ | `otel.instrumentation.logback-appender.enabled: false` |
| Micrometer | ❌ | `otel.instrumentation.micrometer.enabled: false` |

Override any of these in your `application.yml` if needed.

### New propagators

W3C Trace Context propagation is now configured by default alongside B3 and Jaeger:

```yaml
otel:
  propagators: [b3, jaeger, tracecontext]
```

---

## Step 8: New features (optional adoption)

### SQL logging suppression

Temporarily suppress SQL query logging during batch operations:

```java
import io.github.jframe.datasource.logging.SqlStatementLogging;

try (var state = SqlStatementLogging.suppress()) {
    repository.saveAll(largeList);
    // SQL queries won't be logged
}
```

### Improved span error handling

Spans now use `span.recordException()` instead of manual attribute assignment:

```java
// Old approach
span.setAttribute("error", true);
span.setAttribute("error.message", e.getMessage());

// New approach (automatic in TracingAspect)
span.recordException(e);
span.setStatus(StatusCode.ERROR);
```

---

## Dependency version changes

| Dependency | 0.9.0 | 1.0.0 |
|-----------|-------|-------|
| Spring Boot | 4.0.1 | 4.1.0-M1 |
| OpenTelemetry | 2.23.0 | 2.25.0 |
| MapStruct | 1.6.3 | 1.6.3 |
| Springdoc | 3.0.0 | 3.0.1 |
| Datasource Proxy | 1.11.0 | 1.11.0 |

---

## Verify

```bash
# Rebuild and run tests
./gradlew clean build test

# Check for compilation errors related to:
# - org.springframework.http.HttpStatus in exception constructors
# - io.github.jframe.logging.kibana.* imports
# - OpenTelemetryConstants.Attributes.* references
# - JpaSearchSpecification import paths
```

---

## Checklist

### Dependencies
- [ ] Updated all `starter-*` → `jframe-spring-*` artifact IDs
- [ ] Updated version to `1.0.0`
- [ ] Checked for transitive dependency conflicts

### Kibana → ECS migration
- [ ] Replaced all `io.github.jframe.logging.kibana.*` imports with `io.github.jframe.logging.ecs.*`
- [ ] Renamed `KibanaLogField` → `EcsField`, `KibanaLogFields` → `EcsFields`, etc.
- [ ] Replaced `.getLogName()` → `.getKey()` on custom `EcsField` implementations
- [ ] Replaced `registerKibanaLogFieldsInThisThread()` → `registerEcsFieldsInThisThread()`
- [ ] Updated Elasticsearch index patterns for ECS field names
- [ ] Updated Kibana dashboards and saved searches
- [ ] Updated `logback-spring.xml` MDC keys (`tx_id` → `transaction.id`, `traceId` → `trace.id`, etc.)

### HttpStatus migration
- [ ] Replaced `org.springframework.http.HttpStatus` → `jakarta.ws.rs.core.Response.Status`
- [ ] Updated custom exception classes
- [ ] Updated status code comparisons

### OpenTelemetry
- [ ] Replaced `OpenTelemetryConstants.Attributes.*` → `EcsFieldNames.SPAN_*`
- [ ] Replaced `Attributes.ERROR` usage with `span.setStatus(StatusCode.ERROR)`
- [ ] Added `.getKey()` to SPAN_* constants in `span.setAttribute()` calls

### JPA
- [ ] Updated `JpaSearchSpecification` import path

### Verification
- [ ] Full build passes: `./gradlew clean build test`
- [ ] HTTP exception handling works correctly
- [ ] JPA search queries execute correctly
- [ ] OTLP tracing exports spans to collector
- [ ] Structured logging output uses ECS field names
