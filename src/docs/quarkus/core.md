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
| 50 | TracingResponseFilter | Trace/span ID propagation (requires `quarkus-otlp`) |
| 100 | TransactionIdFilter | Reads/generates transaction ID from header, stores in MDC (`tx_id`) |
| 200 | RequestIdFilter | Generates UUID per request, stores in MDC (`req_id`) |
| 300 | RequestDurationFilter | Measures and logs request duration |
| 400 | RequestResponseLogFilter | Logs full request/response with body masking |

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

JAX-RS `@Provider` exception mappers convert JFrame exceptions to structured JSON responses.

### Handled exceptions

| Exception | HTTP Status | Response type |
|-----------|-------------|---------------|
| `BadRequestException` | 400 | `ErrorResponseResource` |
| `UnauthorizedRequestException` | 401 | `ErrorResponseResource` |
| `ResourceNotFoundException` | 404 | `ErrorResponseResource` |
| `ApiException` | 400 | `ApiErrorResponseResource` (with error code + reason) |
| `ValidationException` | 400 | `ValidationErrorResponseResource` (with field errors) |
| `RateLimitExceededException` | 429 | `RateLimitErrorResponseResource` (with limit headers) |
| `Throwable` (catch-all) | 500 | `ErrorResponseResource` |

### Error response format

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

### Error response enrichers

9 enricher components customize error responses. Create your own:

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

`JFrameJacksonCustomizer` produces a configured `ObjectMapper`:
- Property naming: `lowerCamelCase`
- No pretty-print
- Always include values (no null exclusion)
- Ignore unknown properties on deserialization

## Startup logging

`CorePackageLogger` logs application metadata and registered JFrame filters at startup:

```
INFO  [CorePackageLogger] JFrame Quarkus Core initialized
  Application: my-service (com.example) v1.0.0 [dev]
  Registered filters: RequestIdFilter, TransactionIdFilter, ...
```
