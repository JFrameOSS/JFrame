# jframe-spring-core — AI Context

> Spring Boot 4.x adapter for jframe. Auto-configures filters, exception handling, logging, and Jackson.

## Auto-Configuration

`CoreAutoConfiguration` — registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

- Imports `FilterConfiguration`, `JacksonConfig`
- Enables `ApplicationProperties`, `LoggingProperties`
- Component-scans `io.github.jframe.*`
- Loads defaults from `classpath:jframe-properties.yml`

## Configuration Properties

### `jframe.application` (ApplicationProperties)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | String | `--- UNSET ---` | Service name (@NotBlank) |
| `group` | String | `--- UNSET ---` | Service namespace (@NotBlank) |
| `version` | String | `--- UNSET ---` | Service version (@NotBlank) |
| `environment` | String | `dev` | Environment (dev, test, staging, prod) |
| `url` | String | `http://localhost:8080` | Base URL |

### `jframe.logging` (LoggingProperties)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `disabled` | boolean | `false` | Disable all JFrame logging |
| `responseLength` | int | `-1` | Max response body chars (-1 = unlimited) |
| `bodyExcludedContentTypes` | List | `multipart/form-data` | Content types to exclude body |
| `excludePaths` | List | `/actuator/*` | Paths to exclude from logging |
| `fieldsToMask` | List | `password, keyPassphrase, client_secret, secret` | Sensitive field names |
| `allowedContentTypes` | List | JSON, XML, form-data, text variants | Loggable content types |

## Servlet Filters

All extend `AbstractGenericFilter` (extends `OncePerRequestFilter`). Toggle with `jframe.logging.filters.{name}.enabled`.

| Filter | Default Order | MDC Fields | Purpose |
|--------|--------------|------------|---------|
| `RequestDurationFilter` | -17500 | `event.duration`, `transaction.duration` | Measures request duration |
| `TracingResponseFilter` | -1000 | `trace.id`, `span.id` | Adds trace context to response (from OTLP module) |
| `RequestResponseLogFilter` | -950 | `event.category`, request/response fields | Logs HTTP request/response |
| `TransactionIdFilter` | -500 | `transaction.id` | Reads/generates TX-ID from header |
| `RequestIdFilter` | -400 | `request.id` | Generates unique request ID |

## Global Exception Handling

`JFrameResponseEntityExceptionHandler` — `@RestControllerAdvice`, `Ordered.HIGHEST_PRECEDENCE`.

| Exception | Status | Response Type |
|-----------|--------|---------------|
| `HttpException` | Varies | `ErrorResponseResource` |
| `RateLimitExceededException` | 429 | `RateLimitErrorResponseResource` + X-RateLimit-* headers |
| `ApiException` | 400 | `ApiErrorResponseResource` |
| `ValidationException` | 400 | `ValidationErrorResponseResource` |
| `BadCredentialsException` | 401 | `ErrorResponseResource` |
| `AccessDeniedException` | 403 | `ErrorResponseResource` |
| `MethodArgumentNotValidException` | 400 | `MethodArgumentNotValidResponseResource` |
| `NoResourceFoundException` | 404 | `ErrorResponseResource` |
| `Throwable` | 500 | `ErrorResponseResource` |

**Enrichers** — 8 `ErrorResponseEnricher` beans populate response fields: StatusCode, ErrorMessage, RequestInfo, TransactionId, ApiError, RateLimit, MethodArgumentNotValid, ValidationError.

## Outbound HTTP Logging

`LoggingClientHttpRequestInterceptor` — `ClientHttpRequestInterceptor` for RestTemplate/WebClient.

```java
@Bean
public RestTemplate restTemplate(LoggingClientHttpRequestInterceptor interceptor) {
    RestTemplate rt = new RestTemplate();
    rt.getInterceptors().add(interceptor);
    return rt;
}
```

Logs request/response bodies, headers. Tags MDC with `CALL_STATUS` (SUCCESS/TIMEOUT/FAILURE).

## Scheduled Task Support

`ScheduledAspect` — AOP aspect for `@Scheduled` methods. Auto-generates `RequestId` + `TransactionId`, tags MDC, clears in finally.

## Jackson Configuration

`JacksonConfig` — `JsonMapperBuilderCustomizer`:
- Include: ALWAYS (even nulls)
- Disable: FAIL_ON_UNKNOWN_PROPERTIES, INDENT_OUTPUT, WRITE_DATES_AS_TIMESTAMPS
- Naming: LOWER_CAMEL_CASE

## Request-Scoped Cache

```java
@Component @RequestScope
public class UserCache extends RequestScopedCache<Long, User> {
    @Override protected Long getId(User entity) { return entity.getId(); }
}
// cache.getOrLoad(id, repo::findById)
// cache.getAllOrLoad(ids, repo::findAllById)
```

## Utilities

- `ResourceLoaderUtil` — `getResourceFile(path)`, `getResourceAsString(path)`, `getResource(path)`
- `DateTimeMapper` — MapStruct mapper for ZonedDateTime/OffsetDateTime/LocalDateTime (UTC)
- `FilterVoter` — combines `MediaTypeVoter` + `RequestVoter` for filter decisions
- `CorePackageLogger` — logs app metadata + registered filters at startup
