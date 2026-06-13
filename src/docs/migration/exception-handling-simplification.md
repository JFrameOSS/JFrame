# Exception Handling Simplification Migration Guide

This guide covers migrating JFrame-based applications (Spring Boot and Quarkus) after two rounds of exception handling simplification. Both rounds introduced breaking changes — this guide reflects the **final API state**.

## What changed

### Round 1 — Hierarchy simplification

1. `ApiError` gained required method `getHttpStatus()`
2. `ApiException` deleted — merged into `HttpException`
3. `ApiErrorResponseResource` deleted — merged into `ErrorResponseResource`
4. Three exception classes deleted: `DataNotFoundException`, `InternalServerErrorException`, `UnauthorizedRequestException`
5. `ApiErrorResponseEnricher` deleted — use `ErrorCodeResponseEnricher`
6. `ApiExceptionMapper` deleted (Quarkus) — `HttpExceptionMapper` handles all `HttpException` subclasses

### Round 2 — API strictness

7. [`HttpException` now only accepts `ApiError`](#step-5-update-httpexception-direct-usage) — all status-only constructors removed
8. [Subclasses (`BadRequestException`, `ResourceNotFoundException`, `RateLimitExceededException`) lost string message constructors](#step-4-update-exception-subclass-usage) — no-arg and Throwable constructors only
9. [`ErrorResponseResource` fields changed](#step-6-update-error-response-consumers) — `statusMessage`, `errorMessage`, `apiErrorCode`, `apiErrorReason` deleted; `errorCode`, `errorReason`, `cause` added
10. [New `JFrameErrorCode` enum](#step-9-jframeerrorcode-defaults) — 6 framework defaults, used by all built-in exceptions
11. [`ErrorCodeResponseEnricher` replaces `ErrorMessageResponseEnricher` and `ApiErrorResponseEnricher`](#step-7-update-custom-enrichers)

**What did NOT change:**

- Validation exception behavior or API
- `ResourceNotFoundException` and `BadRequestException` semantics
- Auto-configuration behavior
- `ErrorResponseWriter` signatures (Spring Boot)

---

## Step 1: Update `ApiError` implementations

All `ApiError` implementations must add a `Response.Status` field and implement `getHttpStatus()`.

### Before

```java
public enum MyErrors implements ApiError {
    USER_NOT_FOUND("USER_001", "User not found"),
    INVALID_TOKEN("AUTH_001", "Invalid or expired token"),
    QUOTA_EXCEEDED("QUOTA_001", "API quota exceeded");

    private final String errorCode;
    private final String reason;

    MyErrors(final String errorCode, final String reason) {
        this.errorCode = errorCode;
        this.reason = reason;
    }

    @Override
    public String getErrorCode() { return errorCode; }

    @Override
    public String getReason() { return reason; }
}
```

### After

```java
import jakarta.ws.rs.core.Response;

public enum MyErrors implements ApiError {
    USER_NOT_FOUND("USER_001", "User not found", Response.Status.NOT_FOUND),
    INVALID_TOKEN("AUTH_001", "Invalid or expired token", Response.Status.UNAUTHORIZED),
    QUOTA_EXCEEDED("QUOTA_001", "API quota exceeded", Response.Status.TOO_MANY_REQUESTS);

    private final String errorCode;
    private final String reason;
    private final Response.Status httpStatus;

    MyErrors(final String errorCode, final String reason, final Response.Status httpStatus) {
        this.errorCode = errorCode;
        this.reason = reason;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getErrorCode() { return errorCode; }

    @Override
    public String getReason() { return reason; }

    @Override
    public Response.Status getHttpStatus() { return httpStatus; }
}
```

**Common status mappings:**
- Not found → `Response.Status.NOT_FOUND` (404)
- Auth errors → `Response.Status.UNAUTHORIZED` (401)
- Permission errors → `Response.Status.FORBIDDEN` (403)
- Client errors → `Response.Status.BAD_REQUEST` (400)
- Server errors → `Response.Status.INTERNAL_SERVER_ERROR` (500)
- Rate limiting → `Response.Status.TOO_MANY_REQUESTS` (429)

---

## Step 2: Replace `ApiException` with `HttpException`

`ApiException` was deleted. Use `HttpException` with an `ApiError`.

### Before

```java
import io.github.jframe.exception.core.ApiException;

throw new ApiException(MyErrors.USER_NOT_FOUND);
throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
```

### After

```java
import io.github.jframe.exception.HttpException;

throw new HttpException(MyErrors.USER_NOT_FOUND);
throw new HttpException(MyErrors.USER_NOT_FOUND, cause); // with cause chain
```

**HttpException constructors (final):**
- `new HttpException(ApiError)` — status from `apiError.getHttpStatus()`
- `new HttpException(ApiError, Throwable)` — with cause

> No status-only constructors exist. Every `HttpException` must carry an `ApiError`.

---

## Step 3: Replace deleted exception classes

### `DataNotFoundException` → `ResourceNotFoundException`

```java
// Before
import io.github.jframe.exception.core.DataNotFoundException;
throw new DataNotFoundException(WIDGET_NOT_FOUND);

// After
import io.github.jframe.exception.core.ResourceNotFoundException;
throw new ResourceNotFoundException();                    // uses JFRAME_NOT_FOUND defaults
throw new HttpException(MyErrors.WIDGET_NOT_FOUND);      // custom error code
```

### `InternalServerErrorException` → let exceptions propagate

```java
// Before
throw new InternalServerErrorException("Operation failed", e);

// After — let it propagate; global handler maps to JFRAME_INTERNAL_ERROR (500)
throw e;

// Or wrap with a cause for explicit framework handling
throw new HttpException(JFrameErrorCode.INTERNAL_ERROR, e);
```

### `UnauthorizedRequestException` → `HttpException` with custom `ApiError`

```java
// Before
throw new UnauthorizedRequestException("Missing token");

// After — define an ApiError for the specific scenario
public enum AuthErrors implements ApiError {
    MISSING_TOKEN("AUTH_002", "Missing authentication token", Response.Status.UNAUTHORIZED);
    // ...
}
throw new HttpException(AuthErrors.MISSING_TOKEN);
throw new HttpException(AuthErrors.MISSING_TOKEN, cause);
```

---

## Step 4: Update exception subclass usage

`BadRequestException`, `ResourceNotFoundException`, and `RateLimitExceededException` no longer accept string messages. Use no-arg or Throwable constructors only.

### Before

```java
throw new BadRequestException("Invalid input");
throw new ResourceNotFoundException("User not found");
throw new RateLimitExceededException("Rate limited", 100, 0, resetDate);
```

### After

```java
// BadRequestException
throw new BadRequestException();
throw new BadRequestException(cause);

// ResourceNotFoundException
throw new ResourceNotFoundException();
throw new ResourceNotFoundException(cause);

// RateLimitExceededException — note: cause is first parameter
throw new RateLimitExceededException(100, 0, resetDate);
throw new RateLimitExceededException(cause, 100, 0, resetDate);
```

**Need a custom message?** Define an app-level `ApiError` enum and use `HttpException` directly:

```java
public enum MyErrors implements ApiError {
    INVALID_USER_INPUT("APP_400", "The submitted data is invalid", Response.Status.BAD_REQUEST);
    // ...
}
throw new HttpException(MyErrors.INVALID_USER_INPUT);
```

---

## Step 5: Update `HttpException` direct usage

Status-only constructors were removed. Every throw must supply an `ApiError`.

### Before

```java
// Status-only — no longer exists
throw new HttpException(Response.Status.CONFLICT);
throw new HttpException("Conflict occurred", Response.Status.CONFLICT);
```

### After

```java
// Option 1: app-level ApiError (preferred)
throw new HttpException(MyErrors.CONFLICT);

// Option 2: framework default for quick one-offs
throw new HttpException(JFrameErrorCode.BAD_REQUEST);
throw new HttpException(JFrameErrorCode.INTERNAL_ERROR, cause);
```

See [Step 9](#step-9-jframeerrorcode-defaults) for all `JFrameErrorCode` values.

---

## Step 6: Update error response consumers

`ErrorResponseResource` field names changed. Update any client code, contract tests, or documentation that parses error JSON.

### Before

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "errorMessage": "User not found",
  "apiErrorCode": "USER_001",
  "apiErrorReason": "User not found"
}
```

### After

```json
{
  "statusCode": 404,
  "errorCode": "USER_001",
  "errorReason": "User not found",
  "cause": null
}
```

**Field mapping:**

| Old field | New field | Notes |
|-----------|-----------|-------|
| `apiErrorCode` | `errorCode` | Renamed |
| `apiErrorReason` | `errorReason` | Renamed |
| `statusMessage` | — | Deleted |
| `errorMessage` | — | Deleted |
| — | `cause` | New — nullable String, wraps exception message |

`cause` is only present when the `HttpException` was constructed with a `Throwable` cause. It exposes `cause.getMessage()` — never a stack trace.

Fields `method`, `uri`, `query`, `contentType`, `txId`, `traceId`, `spanId` are unchanged.

---

## Step 7: Update custom enrichers

`ErrorMessageResponseEnricher` and `ApiErrorResponseEnricher` were both deleted and replaced by `ErrorCodeResponseEnricher` (auto-registered). If you extended or referenced either, update as follows.

### Spring Boot

```java
// Before
import io.github.jframe.spring.exception.enricher.ApiErrorResponseEnricher;

@Component
public class MyEnricher extends ApiErrorResponseEnricher {
    @Override
    public void doEnrich(ErrorResponseResource resource, Throwable throwable,
                         WebRequest request, HttpStatus httpStatus) {
        super.doEnrich(resource, throwable, request, httpStatus);
        resource.setErrorMessage("extra context");   // no longer exists
        resource.setApiErrorCode("APP_001");          // no longer exists
    }
}

// After — implement ErrorResponseEnricher directly
import io.github.jframe.exception.handler.enricher.ErrorResponseEnricher;

@Component
public class MyEnricher implements ErrorResponseEnricher {
    @Override
    public void doEnrich(ErrorResponseResource resource, Throwable throwable,
                         WebRequest request, HttpStatus httpStatus) {
        resource.setErrorReason("extra context");    // use setErrorReason()
        resource.setErrorCode("APP_001");            // use setErrorCode()
    }
}
```

### Quarkus

```java
// Before
import io.github.jframe.quarkus.exception.enricher.ApiErrorResponseEnricher;

@Singleton
public class MyEnricher extends ApiErrorResponseEnricher { ... }

// After
import io.github.jframe.exception.enricher.ErrorResponseEnricher;

@ApplicationScoped
public class MyEnricher implements ErrorResponseEnricher {
    @Override
    public void doEnrich(ErrorResponseResource resource, Throwable throwable,
                         ContainerRequestContext requestContext, int statusCode) {
        resource.setErrorCode("APP_001");
        resource.setErrorReason("extra context");
    }
}
```

**Removed setters:** `setStatusMessage()`, `setErrorMessage()`, `setApiErrorCode()`, `setApiErrorReason()`

**Replacement setters:** `setErrorCode()`, `setErrorReason()`, `setCause()`

---

## Step 8: Update `ErrorResponseWriter` usage (Spring Boot only)

Signatures are unchanged. The output JSON now uses `errorCode`/`errorReason` instead of the old field names.

```java
import io.github.jframe.exception.handler.ErrorResponseWriter;
import jakarta.ws.rs.core.Response;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!isValid(extractToken(request))) {
            // Option 1: ApiError enum — preferred
            ErrorResponseWriter.write(request, response, MyErrors.INVALID_TOKEN);
            return;
        }

        if (isExpired(extractToken(request))) {
            // Option 2: explicit values
            ErrorResponseWriter.write(request, response,
                Response.Status.UNAUTHORIZED, "AUTH_002", "Token expired");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

**Signatures:**
- `ErrorResponseWriter.write(request, response, ApiError)` — status, code, reason from enum
- `ErrorResponseWriter.write(request, response, Response.Status, String errorCode, String errorReason)` — explicit values

---

## Step 9: `JFrameErrorCode` defaults

All built-in exceptions now use `JFrameErrorCode` as their `ApiError`. These codes appear in the `errorCode` field of every error response produced by a framework exception.

| Exception | `errorCode` | `errorReason` | HTTP Status |
|-----------|-------------|---------------|-------------|
| `BadRequestException` | `JFRAME_BAD_REQUEST` | Bad request | 400 |
| `ResourceNotFoundException` | `JFRAME_NOT_FOUND` | Resource not found | 404 |
| `RateLimitExceededException` | `JFRAME_RATE_LIMITED` | Rate limit exceeded | 429 |
| `ValidationException` / constraint violation | `JFRAME_VALIDATION_ERROR` | Validation failed | 400 |
| Unhandled `Throwable` | `JFRAME_INTERNAL_ERROR` | Internal server error | 500 |
| Generic HTTP errors | `JFRAME_HTTP_ERROR` | HTTP error | varies |

Use `JFrameErrorCode` in application code when you need a quick one-off throw without defining a custom `ApiError`:

```java
import io.github.jframe.exception.JFrameErrorCode;

throw new HttpException(JFrameErrorCode.INTERNAL_ERROR, cause);
throw new HttpException(JFrameErrorCode.BAD_REQUEST);
```

---

## Step 10: Quarkus-specific mapper changes

```java
// Before — both were registered
import io.github.jframe.quarkus.exception.mapper.ApiExceptionMapper;
import io.github.jframe.quarkus.exception.mapper.HttpExceptionMapper;

// After — only HttpExceptionMapper exists
import io.github.jframe.quarkus.exception.mapper.HttpExceptionMapper;
// Auto-registered; handles all HttpException subclasses
```

Remove any bean references to `ApiExceptionMapper`. No Spring changes needed — `HttpExceptionHandler` handled both cases already.

---

## Step 11: Update `ObjectMappers` catch blocks

`ObjectMappers.fromJson()` no longer wraps exceptions. `JacksonException` propagates directly.

### Before

```java
import io.github.jframe.exception.core.InternalServerErrorException;

try {
    final MyObject obj = ObjectMappers.fromJson(jsonString, MyObject.class);
} catch (InternalServerErrorException e) {
    log.error("Parse error", e);
}
```

### After

```java
import com.fasterxml.jackson.databind.exc.JacksonException;

try {
    final MyObject obj = ObjectMappers.fromJson(jsonString, MyObject.class);
} catch (JacksonException e) {
    log.error("Parse error", e);
}
```

---

## Verify

```bash
./gradlew clean build test

# Compilation errors to look for:
# - ApiException, DataNotFoundException, InternalServerErrorException, UnauthorizedRequestException imports
# - ApiErrorResponseResource type references
# - HttpException(Response.Status, ...) or HttpException(String, Response.Status) calls
# - BadRequestException("...") / ResourceNotFoundException("...") calls
# - setErrorMessage(), setStatusMessage(), setApiErrorCode(), setApiErrorReason() calls
# - JSON consumers reading statusMessage, errorMessage, apiErrorCode, apiErrorReason
```

---

## Checklist

### ApiError enum updates
- [ ] All `ApiError` implementations have `Response.Status httpStatus` field
- [ ] All `ApiError` implementations implement `getHttpStatus()`
- [ ] All enum constants include appropriate `Response.Status`
- [ ] Imported `jakarta.ws.rs.core.Response`

### Exception replacements
- [ ] `ApiException` → `HttpException(ApiError)`
- [ ] `DataNotFoundException` → `ResourceNotFoundException()` or `HttpException(MyErrors.X)`
- [ ] `UnauthorizedRequestException` → `HttpException(MyErrors.X)` with custom `ApiError`
- [ ] `InternalServerErrorException` → let propagate, or `HttpException(JFrameErrorCode.INTERNAL_ERROR, cause)`
- [ ] All `HttpException(Response.Status, ...)` throws updated to use `ApiError`

### Subclass constructor updates
- [ ] `BadRequestException("...")` → `BadRequestException()` or custom `ApiError`
- [ ] `ResourceNotFoundException("...")` → `ResourceNotFoundException()` or custom `ApiError`
- [ ] `RateLimitExceededException("...", limit, remaining, reset)` → `RateLimitExceededException(limit, remaining, reset)`

### Error response field changes
- [ ] JSON consumers updated: `apiErrorCode` → `errorCode`, `apiErrorReason` → `errorReason`
- [ ] `statusMessage` and `errorMessage` references removed from consumers
- [ ] `cause` field handling added where needed (nullable String)
- [ ] OpenAPI `@ApiResponse` annotations updated to reference `ErrorResponseResource`

### Enricher updates
- [ ] `ApiErrorResponseEnricher` / `ErrorMessageResponseEnricher` references removed
- [ ] Custom enrichers implement `ErrorResponseEnricher` directly
- [ ] `setErrorMessage()` / `setStatusMessage()` / `setApiErrorCode()` / `setApiErrorReason()` calls replaced

### Quarkus
- [ ] `ApiExceptionMapper` imports and bean references removed
- [ ] `HttpExceptionMapper` handles all `HttpException` variants

### ObjectMappers
- [ ] `fromJson()` catch blocks updated: `InternalServerErrorException` → `JacksonException`

### Verification
- [ ] Full build passes: `./gradlew clean build test`
- [ ] HTTP error responses include `errorCode` and `errorReason` fields
- [ ] `cause` field present when exception wraps a throwable, absent otherwise
- [ ] Validation exceptions still produce `JFRAME_VALIDATION_ERROR` responses
- [ ] Rate limit responses include `X-RateLimit-*` headers
- [ ] Auth/permission errors use correct status codes (401, 403)
