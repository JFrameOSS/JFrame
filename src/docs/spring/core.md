# jframe-spring-core

HTTP logging, exception handling, request-scoped caching, and auto-configuration for Spring Boot applications.

## Auto-configuration

Add the dependency and configure `jframe.application.*` properties — everything else activates automatically via `CoreAutoConfiguration`.

```yaml
jframe:
  application:
    name: my-service
    group: com.example
    version: 1.0.0
    environment: dev        # optional, default: dev
    url: http://localhost:8080  # optional
```

## HTTP logging

JFrame registers a filter chain that logs every HTTP request and response with structured fields in SLF4J MDC.

### Filter chain (execution order)

| Priority | Filter | Purpose |
|----------|--------|---------|
| -17500 | RequestDurationFilter | Measures and logs request duration |
| -1000 | TracingResponseFilter | Trace/span ID propagation (requires `spring-otlp`) |
| -950 | RequestResponseLogFilter | Logs full request/response (method, URI, status, headers, body) |
| -500 | TransactionIdFilter | Reads/generates transaction ID from header, stores in MDC (`transaction.id`) |
| -400 | RequestIdFilter | Generates UUID per request, stores in MDC (`request.id`) |

### Accessing request context

All filter orders are configurable via `jframe.logging.filters.<name>.order`.

```java
// In any thread handling the request
String requestId = RequestId.get();       // UUID string or null
String txId = TransactionId.get();        // UUID string or null
```

### Path exclusions

```yaml
jframe:
  logging:
    exclude-paths:
      - /actuator/*
      - /health
```

### Sensitive field masking

JSON fields are automatically masked in logged request/response bodies:

```yaml
jframe:
  logging:
    fields-to-mask:
      - password
      - client_secret
      - secret
```

### Custom logger

Replace the default request/response logger:

```java
@Bean
public RequestResponseLogger myLogger() {
    return new MyCustomRequestResponseLogger();
}
```

The `@ConditionalOnMissingBean` on the default ensures your bean takes precedence.

## Exception handling

`JFrameResponseEntityExceptionHandler` is a `@RestControllerAdvice` that converts exceptions to structured JSON error responses.

### Handled exceptions

| Exception | HTTP Status | Response type |
|-----------|-------------|---------------|
| `BadRequestException` | 400 | `ErrorResponseResource` |
| `UnauthorizedRequestException` | 401 | `ErrorResponseResource` |
| `ResourceNotFoundException` | 404 | `ErrorResponseResource` |
| `ApiException` | 400 | `ApiErrorResponseResource` (with error code + reason) |
| `ValidationException` | 400 | `ValidationErrorResponseResource` (with field errors) |
| `RateLimitExceededException` | 429 | `RateLimitErrorResponseResource` (with limit headers) |
| `MethodArgumentNotValidException` | 400 | Validation errors from `@Valid` |
| `AccessDeniedException` | 403 | `ErrorResponseResource` |
| `Throwable` (catch-all) | 500 | `ErrorResponseResource` |

### Error response format

```json
{
  "statusCode": 404,
  "statusText": "Not Found",
  "errorMessage": "User not found",
  "method": "GET",
  "uri": "/api/users/42",
  "transactionId": "a1b2c3d4-...",
  "traceId": "...",
  "spanId": "...",
  "timestamp": "2026-03-18T10:00:00Z"
}
```

### Custom error enricher

Add fields to every error response:

```java
@Component
public class TenantEnricher implements ErrorResponseEnricher {
    @Override
    public void doEnrich(ErrorResponseResource resource, Throwable t,
                         WebRequest req, HttpStatus status) {
        resource.putDetail("tenantId", TenantContext.current());
    }
}
```

### Throwing exceptions

```java
// Simple HTTP exceptions
throw new ResourceNotFoundException("User not found");
throw new BadRequestException("Invalid input");

// API errors with error codes
throw new ApiException(MyApiErrors.USER_DISABLED);

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

## Request-scoped caching

Prevent duplicate database queries within a single HTTP request:

```java
@Component
@RequestScope
public class UserCache extends RequestScopedCache<Long, User> {
    @Override
    protected Long getId(User user) {
        return user.getId();
    }
}

// In your service
@Service
public class UserService {
    private final UserCache cache;
    private final UserRepository repo;

    public User getUser(Long id) {
        return cache.getOrLoad(id, () -> repo.findById(id).orElseThrow());
    }
}
```

## Scheduled task context

`@Scheduled` methods automatically get request/transaction IDs via `ScheduledAspect`:

```java
@Scheduled(fixedRate = 60000)
public void processQueue() {
    // RequestId and TransactionId are set automatically
    // MDC contains req_id and tx_id for log correlation
    log.info("Processing queue");
}
```

## Outbound HTTP logging

Log outgoing RestTemplate calls:

```java
@Bean
public RestTemplate restTemplate(LoggingClientHttpRequestInterceptor interceptor) {
    RestTemplate rt = new RestTemplate();
    rt.getInterceptors().add(interceptor);
    return rt;
}
```
