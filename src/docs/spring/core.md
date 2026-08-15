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
| -17500 | RequestDurationFilter | Measures and logs request duration (enabled by default) |
| -950 | RequestResponseLogFilter | Logs full request/response (method, URI, status, headers, body) (enabled by default) |
| -500 | TransactionIdFilter | Reads/generates transaction ID from header, stores in MDC (`transaction.id`) (opt-in) |
| -400 | RequestIdFilter | Generates UUID per request, stores in MDC (`request.id`) (opt-in) |
| -100 | UserIdentityFilter | Captures authenticated user identity (enabled by default) |

### Accessing request context

All filter orders are configurable via `jframe.logging.filters.<name>.order`.

```java
// In any thread handling the request
String requestId = RequestId.get();       // UUID string or null
String txId = TransactionId.get();        // UUID string or null
```

### Debug logging

The request/response and request-duration filters short-circuit when DEBUG logging is disabled, avoiding any overhead in production. Their log output is written at DEBUG level, so to see request/response bodies or duration logs, enable DEBUG on the filter loggers:

```yaml
logging:
  level:
    io.github.jframe.logging.filter.type.RequestResponseLogFilter: DEBUG
    io.github.jframe.logging.filter.type.RequestDurationFilter: DEBUG
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
| `HttpException` (Dynamic) | Dynamic | `ErrorResponseResource` (error code + reason from `ApiError`) |
| `ValidationException` | 400 | `ValidationErrorResponseResource` (with field errors) |
| `RateLimitExceededException` | 429 | `RateLimitErrorResponseResource` (with limit headers) |
| `MethodArgumentNotValidException` | 400 | Validation errors from `@Valid` |
| `Throwable` (catch-all) | 500 | `ErrorResponseResource` |

### Error response format

```json
{
  "statusCode": 404,
  "errorCode": "USER_001",
  "errorReason": "User not found",
  "cause": null,
  "method": "GET",
  "uri": "/api/users/42",
  "query": null,
  "contentType": "application/json",
  "txId": "abc-123",
  "traceId": "...",
  "spanId": "..."
}
```

### Built-in enrichers

8 enrichers run on every error response (plus `TracingResponseEnricher` from `spring-otlp`):

| Enricher | Adds |
|----------|------|
| `StatusCodeEnricher` | `statusCode` |
| `ErrorCodeEnricher` | `errorCode`, `errorReason` |
| `RequestInfoEnricher` | `method`, `uri`, `query`, `contentType` |
| `TransactionIdEnricher` | `txId` |
| `ValidationEnricher` | field error details (ValidationException) |
| `MethodArgumentNotValidEnricher` | field error details (@Valid failures) |
| `RateLimitEnricher` | limit headers (RateLimitExceededException) |
| `TracingResponseEnricher` *(spring-otlp)* | `traceId`, `spanId` |

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
// Simple HTTP exceptions (no-arg or cause-only)
throw new BadRequestException();
throw new ResourceNotFoundException();

// API errors with error codes
throw new HttpException(MyErrors.USER_NOT_FOUND);
throw new HttpException(MyErrors.USER_NOT_FOUND, cause);

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

## Mappers

`SharedMapperConfig` declares `uses = { UuidMapper.class, AmountMapper.class }`. Any mapper annotated `@Mapper(config = SharedMapperConfig.class)` inherits these implicit conversions.

**This means `UUID` to `String` now converts automatically in your mappers.** If you already declare your own `UUID` to `String` mapping method, MapStruct will report an ambiguous mapping at compile time. Resolve it by qualifying the mapping, or by removing your own method in favour of the shared one.

`DateTimeMapper` is deliberately NOT in the registry. Three of its four methods assume UTC, and auto-applying `LocalDateTime` to `ZonedDateTime` across a non-UTC estate would silently shift timestamps. Opt in explicitly where you want it:

```java
@Mapper(config = SharedMapperConfig.class, uses = DateTimeMapper.class)
public interface OrderMapper {
    OrderDto toDto(Order order);
}
```

Consumer-level `uses` merges with the config-level registry rather than replacing it, so both sets of conversions are available.
