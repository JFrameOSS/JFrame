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

`JFrameResponseEntityExceptionHandler` is a `@RestControllerAdvice` that converts exceptions to RFC 9457 Problem Details error responses (`application/problem+json`). Registered with `@Order(Ordered.LOWEST_PRECEDENCE)` so application `@RestControllerAdvice` / `@ExceptionHandler` beans take precedence for exceptions they handle (including jFrame `HttpException` subtypes); jFrame handles the rest. Disabled via `jframe.exception.enabled=false`; backs off if the application defines its own `ErrorController`.

### Handled exceptions

| Exception | HTTP Status | Response type |
|-----------|-------------|---------------|
| `HttpException` (Dynamic) | Dynamic | `ErrorResponseResource` (error code + reason from `ApiError`) |
| `ValidationException` | 400 | `ValidationErrorResponseResource` (with field errors) |
| `RateLimitExceededException` | 429 | `RateLimitErrorResponseResource` (with limit headers) |
| `MethodArgumentNotValidException` | 400 | Validation errors from `@Valid` |
| Spring MVC exceptions (404, 405, 415, etc.) | Dynamic | `ErrorResponseResource` with jFrame enrichment |
| `AuthenticationException` | 401 | `ErrorResponseResource` |
| `AccessDeniedException` | 403 | `ErrorResponseResource` |
| `Throwable` (catch-all) | 500 | `ErrorResponseResource` |

### Fallback `/error` controller

`JFrameErrorController` handles errors outside MVC (exceptions thrown in filters, `response.sendError`) and returns jFrame Problem Details instead of Spring Boot's default error body. The `instance` field is set to the original request path. `txId` is included only when the transaction-ID filter is on, like the MVC handler. Disabled via `jframe.exception.enabled=false`; backs off if the application defines its own `ErrorController`.

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

6 enrichers run on every error response (plus `TracingResponseEnricher` from `spring-otlp`):

| Enricher | Adds |
|----------|------|
| `ErrorCodeResponseEnricher` | `errorCode`, `detail` (from `ApiError` or HTTP status) |
| `MethodArgumentNotValidResponseEnricher` | `errors` extension (field violations from `@Valid`) |
| `ValidationErrorResponseEnricher` | `errors` extension (field violations from `ValidationException`) |
| `RateLimitResponseEnricher` | `limit`, `remaining`, `resetDate` extensions (rate limit headers still set) |
| `TransactionIdResponseEnricher` | `txId` extension |
| `TracingResponseEnricher` *(spring-otlp)* | `traceId`, `spanId` extensions |

Enrichers run in deterministic order: built-ins first (via `@Order(ErrorResponseEnricher.BUILT_IN_ORDER)`), then application enrichers.

### Custom error enricher

Add fields to every error response:

```java
@Configuration
public class ErrorHandlingConfig {
    @Bean
    @Order(ErrorResponseEnricher.BUILT_IN_ORDER + 100)  // after built-ins
    public ErrorResponseEnricher tenantEnricher() {
        return (resource, throwable, request, httpStatus) -> {
            String tenantId = request.getHeader("X-Tenant-ID");
            if (tenantId != null) {
                resource.addExtension("tenantId", tenantId);
            }
        };
    }
}
```

**Signature:** `doEnrich(ErrorResponseResource, Throwable, WebRequest, HttpStatus)` — `httpStatus` is an `HttpStatus` enum.

**Available methods:**
- `resource.setErrorCode(String)` — override error code
- `resource.setDetail(String)` — override detail message
- `resource.addExtension(String key, Object value)` — add extension member

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
