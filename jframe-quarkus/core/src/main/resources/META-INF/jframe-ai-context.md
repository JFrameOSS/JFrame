# jframe-quarkus-core — AI Context

> Quarkus adapter for jframe. JAX-RS exception mappers, request/response filters, ECS logging, CDI producers, and OpenAPI integration.

## Exception Mappers

All extend `AbstractExceptionMapper<T>` (`@Provider @ApplicationScoped`).

| Mapper | Exception | Status |
|--------|-----------|--------|
| `ApiExceptionMapper` | `ApiException` | 400 |
| `HttpExceptionMapper` | `HttpException` | From `exception.getHttpStatus()` |
| `RateLimitExceededExceptionMapper` | `RateLimitExceededException` | 429 + X-RateLimit-* headers |
| `ValidationExceptionMapper` | `ValidationException` | 400 |
| `ThrowableMapper` | `Throwable` (catch-all) | 500 (generic message) |

**Response enrichment:** `ErrorResponseEntityBuilder` creates base response via `DefaultErrorResponseFactory`, then applies 8 `ErrorResponseEnricher` beans: StatusCode, ErrorMessage, RequestInfo, TransactionId, ApiError, RateLimit, ConstraintViolation, ValidationError.

## JAX-RS Filters

All `@Provider @ApplicationScoped`, implement `ContainerRequestFilter` + `ContainerResponseFilter`.

### Inbound Filters

| Filter | @Priority | MDC Fields | Purpose |
|--------|-----------|------------|---------|
| `TransactionIdFilter` | 100 | `transaction.id` | Reads/generates TX-ID from header |
| `RequestIdFilter` | 200 | `request.id` | Generates unique request ID |
| `RequestDurationFilter` | 300 | `event.duration`, `transaction.duration` | Measures request duration |
| `RequestResponseLogFilter` | 400 | request/response fields | Logs HTTP request/response |

### Outbound Filters (`ClientRequestFilter`)

| Filter | @Priority | Purpose |
|--------|-----------|---------|
| `OutboundCorrelationFilter` | 100 | Propagates txId, reqId, traceId to outbound headers |
| `OutboundLoggingFilter` | 300 | Logs outbound HTTP request/response |

Toggle all with `jframe.logging.filters.{name}.enabled` (default: `true`).

## Configuration

### `jframe.application` (ApplicationConfig, @ConfigMapping)

| Property | Default | Description |
|----------|---------|-------------|
| `name` | required | Service name |
| `group` | required | Service namespace |
| `version` | required | Service version |
| `environment` | `dev` | Environment |
| `url` | `http://localhost:8080` | Base URL |

### `jframe.logging` (LoggingConfig, @ConfigMapping)

| Property | Default | Description |
|----------|---------|-------------|
| `disabled` | `false` | Disable all logging |
| `responseLength` | `-1` | Max response body chars |
| `bodyExcludedContentTypes` | `multipart/form-data` | Exclude body for types |
| `excludePaths` | `/actuator/*` | Paths to exclude |
| `fieldsToMask` | `password, keyPassphrase, client_secret, secret` | Sensitive fields |
| `allowedContentTypes` | JSON, XML, text variants | Loggable types |

### `jframe.logging.filters` (FilterConfig, @ConfigMapping)

Each sub-config has `enabled()` (default: `true`): `transactionId`, `requestId`, `requestDuration`, `requestResponse`, `outboundCorrelation`, `outboundLogging`, `tracingResponse`.

## CDI Producers

| Producer | Bean | Purpose |
|----------|------|---------|
| `FilterVoterProducer` | `FilterVoter` | Media type + path voting |
| `PasswordMaskerProducer` | `PasswordMasker` | Sensitive field masking |
| `RequestResponseLoggerProducer` | `RequestResponseLogger` | HTTP logging facade |
| `JFrameJacksonCustomizer` | `ObjectMapper` | Jackson 3.x config (LOWER_CAMEL_CASE, no indent, ISO dates) |

## OpenAPI Integration

`JFrameErrorResponseFilter` (`OASFilter`) — auto-adds standard error responses (400, 429, 500) to all OpenAPI operations.

## Scheduled Task Support

`@ScheduledWithContext` — CDI interceptor binding. `ScheduledContextInterceptor` generates RequestId + TransactionId, tags MDC, clears in finally.

## Request-Scoped Cache

Same as jframe-core's `RequestScopedCache<K, V>`. Extend and annotate with `@RequestScoped`.

## Startup Logging

`CorePackageLogger` — logs application metadata and registered `JFrameFilter` instances on `StartupEvent`.
