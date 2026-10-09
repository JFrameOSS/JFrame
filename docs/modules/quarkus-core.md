# jframe-quarkus-core

JAX-RS exception mappers, HTTP request/response logging filters, request-scoped caching, and CDI interceptors for Quarkus applications.

## Auto-discovery

Quarkus auto-discovers all CDI beans and JAX-RS providers via Jandex indexing — no `beans.xml` or configuration classes needed. Just add the dependency.

### Configuration (`application.properties`)

```properties
# Required
jframe.application.name=my-service
jframe.application.group=com.example
jframe.application.version=1.0.0

# Optional
jframe.application.environment=dev

# Logging (all optional — sensible defaults provided)
jframe.logging.disabled=false
jframe.logging.response-length=-1
jframe.logging.exclude-paths=/health,/actuator/*
jframe.logging.fields-to-mask=password,client_secret,secret
```

## HTTP logging filters

JAX-RS container filters log every HTTP request and response with structured MDC fields.

### Filter chain (execution order)

| Priority | Filter | Purpose |
|----------|--------|---------|
| 50 | UserIdentityFilter | Captures authenticated user identity (enabled by default) |
| 100 | TransactionIdFilter | Reads/generates transaction ID from header, stores in MDC (`transaction.id`) (opt-in) |
| 200 | RequestIdFilter | Generates UUID per request, stores in MDC (`request.id`) (opt-in) |
| 300 | RequestDurationFilter | Measures and logs request duration (enabled by default) |
| 400 | RequestResponseLogFilter | Logs full request/response with body masking (enabled by default) |

### Debug logging

The request/response and request-duration filters short-circuit when DEBUG logging is disabled, avoiding any overhead in production. Their log output is written at DEBUG level, so to see request/response bodies or duration logs, enable DEBUG on the filter loggers:

```properties
quarkus.log.category."io.github.jframe.logging.filter.type.RequestResponseLogFilter".level=DEBUG
quarkus.log.category."io.github.jframe.logging.filter.type.RequestDurationFilter".level=DEBUG
```

### Accessing request context

```java
String requestId = RequestId.get();       // UUID string or null
String txId = TransactionId.get();        // UUID string or null
```

### Outbound HTTP filters

Propagate correlation IDs to outbound JAX-RS client calls:

- **OutboundCorrelationFilter** — adds `X-Request-Id` and `X-Transaction-Id` headers
- **OutboundLoggingFilter** — logs outbound request/response details

Register on your JAX-RS client:

```java
@RegisterRestClient
@RegisterProvider(OutboundCorrelationFilter.class)
@RegisterProvider(OutboundLoggingFilter.class)
public interface UserClient {
    @GET @Path("/users/{id}")
    User getUser(@PathParam("id") Long id);
}
```

## Exception mappers

JAX-RS `@Provider` exception mappers convert JFrame exceptions to RFC 9457 Problem Details responses (`application/problem+json`).

### Handled exceptions

6 `@Provider` exception mappers handle the full exception hierarchy:

| Mapper | Exception | HTTP Status |
|--------|-----------|-------------|
| `HttpExceptionMapper` | `HttpException` (+ subclasses) | Dynamic |
| `WebApplicationExceptionMapper` | `WebApplicationException` (JAX-RS) | From exception; 5xx → 500 |
| `ValidationExceptionMapper` | `ValidationException` | 400 |
| `ConstraintViolationExceptionMapper` | `ConstraintViolationException` (Bean Validation) | 400 |
| `RateLimitExceptionMapper` | `RateLimitExceededException` | 429 |
| `ThrowableMapper` | `Throwable` (catch-all) | 500 |

### Error response format

RFC 9457 Problem Details with jFrame extension members:

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "User not found",
  "instance": "/api/users/42",
  "errorCode": "USER_001",
  "txId": "abc-123",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7"
}
```

Content type: `application/problem+json`. Absent extension members are omitted (never `null`). `type` is omitted unless `jframe.exception.type-base-uri` is configured.

### Built-in enrichers

6 enrichers run on every error response (plus `TracingEnricher` from `quarkus-otlp`):

| Enricher | Adds |
|----------|------|
| `ErrorCodeResponseEnricher` | `errorCode`, `detail` (from `ApiError` or HTTP status) |
| `ConstraintViolationResponseEnricher` | `errors` extension (Bean Validation violations) |
| `ValidationErrorResponseEnricher` | `errors` extension (field violations from `ValidationException`) |
| `RateLimitResponseEnricher` | `limit`, `remaining`, `resetDate` extensions (rate limit headers still set) |
| `TransactionIdResponseEnricher` | `txId` extension |
| `TracingEnricher` *(quarkus-otlp)* | `traceId`, `spanId` extensions |

Enrichers run in deterministic order: built-ins first (via `@Priority(ErrorResponseEnricher.BUILT_IN_PRIORITY)`), then application enrichers.

**JAX-RS exception handling:** `WebApplicationExceptionMapper` handles JAX-RS exceptions (404, 405, 415, etc.) with their own status codes and response headers (e.g. `Allow`). `ConstraintViolationExceptionMapper` handles Bean Validation violations (400 VALIDATION_ERROR). Return-value violations → 500.

### Custom error enricher

Add fields to every error response:

```java
@ApplicationScoped
@Priority(ErrorResponseEnricher.BUILT_IN_PRIORITY + 100)  // after built-ins
public class TenantEnricher implements ErrorResponseEnricher {
    @Override
    public void doEnrich(ErrorResponseResource resource, Throwable throwable,
                         ContainerRequestContext requestContext, int statusCode) {
        String tenantId = requestContext.getHeaderString("X-Tenant-ID");
        if (tenantId != null) {
            resource.addExtension("tenantId", tenantId);
        }
    }
}
```

**Signature:** `doEnrich(ErrorResponseResource, Throwable, ContainerRequestContext, int statusCode)` — `statusCode` is a primitive `int`.

**Available methods:**
- `resource.setErrorCode(String)` — override error code
- `resource.setDetail(String)` — override detail message
- `resource.addExtension(String key, Object value)` — add extension member

## Request-scoped caching

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

## Scheduled task context

`@ScheduledWithContext` CDI interceptor binding adds request/transaction IDs to scheduled methods:

```java
@ApplicationScoped
public class QueueProcessor {
    @Scheduled(every = "1m")
    @ScheduledWithContext
    public void processQueue() {
        // RequestId and TransactionId are set (same UUID)
        // MDC contains req_id and tx_id for log correlation
        log.info("Processing queue");
        // Cleanup happens automatically in finally block
    }
}
```

The interceptor generates a single UUID used for both `RequestId` and `TransactionId`, tags MDC, and clears everything in a `finally` block — even if the method throws.

## Jackson configuration

`JFrameJacksonCustomizer` produces a configured `ObjectMapper` (Jackson 3.x):
- Property naming: `lowerCamelCase`
- No pretty-print
- Always include values (no null exclusion)
- Ignore unknown properties on deserialization

## OpenAPI integration

`JFrameErrorResponseFilter` (`OASFilter`) auto-adds standard error responses to all OpenAPI operations:

| Status | Description | Schema |
|--------|-------------|--------|
| 400 | Bad Request | `ErrorResponseResource` |
| 401 | Unauthorized | `ErrorResponseResource` |
| 403 | Forbidden | `ErrorResponseResource` |
| 404 | Not Found | `ErrorResponseResource` |
| 429 | Too Many Requests | `RateLimitErrorResponseResource` |
| 500 | Internal Server Error | `ErrorResponseResource` |

No configuration needed — activates automatically with `quarkus-smallrye-openapi`.

### Known limitation: 401/403 from Quarkus Security

Quarkus Security (401/403) raised by authentication/authorisation filters keep Quarkus's native responses (no jFrame Problem Details body). Custom exception mappers would break authentication challenges (e.g. OIDC code flow redirect, `WWW-Authenticate` header).

## Startup logging

`CorePackageLogger` logs application metadata and registered JFrame filters at startup:

```
INFO  [CorePackageLogger] JFrame Quarkus Core initialized
  Application: my-service (com.example) v1.0.0 [dev]
  Registered filters: RequestIdFilter, TransactionIdFilter, ...
```
