# Exception Handling Simplification Migration Guide

This guide covers migrating JFrame-based applications (Spring Boot and Quarkus) after the exception handling simplification in version 2.0.0.

## What changed

The exception hierarchy was simplified to eliminate redundant classes and clarify the contract between error codes and HTTP status codes.

**Breaking changes:**

1. [`ApiError` gained required method `getHttpStatus()`](#step-1-update-apierror-implementations)
2. [`ApiException` deleted — use `HttpException` instead](#step-2-replace-apiexception-with-httpexception)
3. [`ApiErrorResponseResource` deleted](#step-3-update-error-response-type-references)
4. [Three exception classes deleted](#step-4-replace-deleted-exception-classes)
5. [`ApiErrorResponseEnricher` deleted](#step-5-remove-apierrorresponseenricher-references)
6. [`ApiExceptionMapper` deleted (Quarkus only)](#step-6-remove-apiexceptionmapper-references-quarkus-only)
7. [`ObjectMappers.fromJson()` no longer wraps `JacksonException`](#step-7-update-objectmappers-catch-blocks)

**New additions:**

- `ErrorResponseWriter` utility for security filters (Spring only)
- `HttpExceptionMapper` handles all `HttpException` subclasses (Quarkus)

**What did NOT change:**

- Validation exception behavior or API
- Global error response format — same JSON structure
- `ResourceNotFoundException` and `BadRequestException` semantics
- Auto-configuration behavior

---

## Step 1: Update `ApiError` implementations

All `ApiError` interface implementations (typically enums) must add a `Response.Status` field and implement the new `getHttpStatus()` method.

### Before

```java
import java.io.Serializable;

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
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getReason() {
        return reason;
    }
}
```

### After

```java
import jakarta.ws.rs.core.Response;
import java.io.Serializable;

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
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public Response.Status getHttpStatus() {
        return httpStatus;
    }
}
```

**Steps:**

1. Add `Response.Status` field to enum (typically named `httpStatus`)
2. Import `jakarta.ws.rs.core.Response`
3. Update enum constructor to accept `Response.Status` parameter
4. Implement `getHttpStatus()` method returning the status field
5. Update all enum constant declarations with the appropriate HTTP status

**Common status mappings:**
- Not found errors → `Response.Status.NOT_FOUND` (404)
- Authorization errors → `Response.Status.UNAUTHORIZED` (401)
- Permission errors → `Response.Status.FORBIDDEN` (403)
- Validation/client errors → `Response.Status.BAD_REQUEST` (400)
- Server errors → `Response.Status.INTERNAL_SERVER_ERROR` (500)
- Rate limiting → `Response.Status.TOO_MANY_REQUESTS` (429)

---

## Step 2: Replace `ApiException` with `HttpException`

`ApiException` was deleted. Use `HttpException` instead, which now accepts `ApiError` directly.

### Before

```java
import io.github.jframe.exception.core.ApiException;
import static com.example.MyErrors.USER_NOT_FOUND;

// Throw with ApiError
throw new ApiException(USER_NOT_FOUND);

// Or specify status separately
throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
```

### After

```java
import io.github.jframe.exception.core.HttpException;
import jakarta.ws.rs.core.Response;
import static com.example.MyErrors.USER_NOT_FOUND;

// Throw with ApiError — status comes from apiError.getHttpStatus()
throw new HttpException(USER_NOT_FOUND);

// With cause chain
throw new HttpException(USER_NOT_FOUND, cause);

// Or specify status directly
throw new HttpException(Response.Status.NOT_FOUND, "Not found");
```

**Constructor signatures:**
- `new HttpException(ApiError apiError)` — status from `apiError.getHttpStatus()`
- `new HttpException(ApiError apiError, Throwable cause)` — with cause chain
- `new HttpException(Response.Status status, String message)` — direct status without error code

**In service classes:**

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findById(final Long id) {
        // Before: throw new ApiException(USER_NOT_FOUND);
        // After:
        return userRepository.findById(id)
            .orElseThrow(() -> new HttpException(USER_NOT_FOUND));
    }

    public void createUser(final CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            // Before: throw new ApiException(USER_ALREADY_EXISTS);
            // After:
            throw new HttpException(USER_ALREADY_EXISTS);
        }
        // ...
    }
}
```

---

## Step 3: Update error response type references

`ApiErrorResponseResource` was deleted. Update all type references to use `ErrorResponseResource` instead.

### Before

```java
import io.github.jframe.exception.ApiErrorResponseResource;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponseResource.class))
    )
    public ResponseEntity<UserResponse> getUser(@PathVariable final Long id) {
        // ...
    }

    // Check response type in code
    public ResponseEntity<?> handleCustomResponse() {
        if (/* something */ instanceof ApiErrorResponseResource) {
            // ...
        }
        return ResponseEntity.ok().build();
    }
}
```

### After

```java
import io.github.jframe.exception.ErrorResponseResource;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))
    )
    public ResponseEntity<UserResponse> getUser(@PathVariable final Long id) {
        // ...
    }

    // Check response type in code
    public ResponseEntity<?> handleCustomResponse() {
        if (/* something */ instanceof ErrorResponseResource) {
            // ...
        }
        return ResponseEntity.ok().build();
    }
}
```

**Changes:**
- Replace `ApiErrorResponseResource` import with `ErrorResponseResource`
- Replace `ApiErrorResponseResource` class references with `ErrorResponseResource`
- If you extended `ApiErrorResponseResource`, extend `ErrorResponseResource` instead

**Response JSON format unchanged:**
- `ErrorResponseResource` always includes `apiErrorCode` and `apiErrorReason` when an error has a code
- For generic HTTP errors without an `ApiError`, only `message` and `statusCode` are present
- No changes needed to frontend/client parsing

---

## Step 4: Replace deleted exception classes

Three exception classes were removed. Use the indicated replacements.

### `DataNotFoundException` → `ResourceNotFoundException`

```java
// Before
import io.github.jframe.exception.core.DataNotFoundException;
throw new DataNotFoundException(WIDGET_NOT_FOUND);

// After
import io.github.jframe.exception.core.ResourceNotFoundException;
throw new ResourceNotFoundException(WIDGET_NOT_FOUND);
```

> Note: `ResourceNotFoundException` extends `HttpException` — all behavior is identical.

### `InternalServerErrorException` → let exceptions propagate

```java
// Before
import io.github.jframe.exception.core.InternalServerErrorException;

try {
    riskyOperation();
} catch (Exception e) {
    throw new InternalServerErrorException("Operation failed", e);
}

// After — let the exception propagate (caught by global handler as 500)
try {
    riskyOperation();
} catch (RuntimeException e) {
    // Log if needed
    throw e;  // Global handler catches this as 500 Internal Server Error
}
```

> The global exception handler catches all unhandled exceptions and returns a 500 response. No explicit `InternalServerErrorException` needed.

### `UnauthorizedRequestException` → use `HttpException` directly

```java
// Before
import io.github.jframe.exception.core.UnauthorizedRequestException;
throw new UnauthorizedRequestException("Missing token");

// After
import io.github.jframe.exception.core.HttpException;
import jakarta.ws.rs.core.Response;

// Option 1: Use existing ApiError enum value
throw new HttpException(INVALID_TOKEN);

// Option 2: Use HttpException directly
throw new HttpException(Response.Status.UNAUTHORIZED, "Missing token");

// Option 3: Define ApiError enum value for this scenario
public enum AuthErrors implements ApiError {
    MISSING_TOKEN("AUTH_002", "Missing authentication token", Response.Status.UNAUTHORIZED);
    // ...
}
throw new HttpException(AuthErrors.MISSING_TOKEN);
```

---

## Step 5: Remove `ApiErrorResponseEnricher` references

The `ApiErrorResponseEnricher` class no longer exists. Error codes and reasons are now set via the base enricher pipeline.

### Spring Boot

If you had a custom enricher extending `ApiErrorResponseEnricher`:

```java
// Before
import io.github.jframe.spring.exception.enricher.ApiErrorResponseEnricher;

@Component
public class MyCustomEnricher extends ApiErrorResponseEnricher {
    @Override
    public void enrich(final ErrorResponseResource response, final Exception exception) {
        super.enrich(response, exception);
        // Custom enrichment
    }
}

// After — extend the base enricher instead
import io.github.jframe.spring.exception.enricher.ErrorResponseEnricher;

@Component
public class MyCustomEnricher extends ErrorResponseEnricher {
    @Override
    public void enrich(final ErrorResponseResource response, final Exception exception) {
        super.enrich(response, exception);
        // Custom enrichment
    }
}
```

### Quarkus

```java
// Before
import io.github.jframe.quarkus.exception.enricher.ApiErrorResponseEnricher;

@Singleton
public class MyCustomEnricher extends ApiErrorResponseEnricher {
    @Override
    public void enrich(final ErrorResponseResource response, final Exception exception) {
        super.enrich(response, exception);
    }
}

// After
import io.github.jframe.quarkus.exception.enricher.ErrorResponseEnricher;

@Singleton
public class MyCustomEnricher extends ErrorResponseEnricher {
    @Override
    public void enrich(final ErrorResponseResource response, final Exception exception) {
        super.enrich(response, exception);
    }
}
```

**Action:** If you have a custom enricher, rename the parent class to `ErrorResponseEnricher`. The base implementation now handles all error code/reason setting.

---

## Step 6: Remove `ApiExceptionMapper` references (Quarkus only)

`ApiExceptionMapper` was deleted. `HttpExceptionMapper` now handles all `HttpException` subclasses including those with error codes.

### Before

```java
// Quarkus project
import io.github.jframe.quarkus.exception.mapper.ApiExceptionMapper;
import io.github.jframe.quarkus.exception.mapper.HttpExceptionMapper;

// Both were registered automatically
```

### After

```java
// Quarkus project
import io.github.jframe.quarkus.exception.mapper.HttpExceptionMapper;

// Only HttpExceptionMapper exists — handles all HttpException subclasses
```

**Action (Quarkus):**
- Remove any `ApiExceptionMapper` import statements
- Remove any bean references to `ApiExceptionMapper`
- `HttpExceptionMapper` is auto-registered and handles `HttpException` with or without error codes

**Action (Spring):**
- No changes needed — Spring only has `HttpExceptionHandler` which handles both

---

## Step 7: Update `ObjectMappers` catch blocks

`ObjectMappers.fromJson()` no longer wraps checked exceptions. `JacksonException` now propagates directly as a `RuntimeException`.

### Before

```java
import io.github.jframe.exception.core.InternalServerErrorException;
import io.github.jframe.util.mapper.ObjectMappers;

try {
    final MyObject obj = ObjectMappers.fromJson(jsonString, MyObject.class);
} catch (InternalServerErrorException e) {
    // Handle JSON parse error
    log.error("Parse error", e);
}
```

### After

```java
import com.fasterxml.jackson.databind.exc.JacksonException;
import io.github.jframe.util.mapper.ObjectMappers;

try {
    final MyObject obj = ObjectMappers.fromJson(jsonString, MyObject.class);
} catch (JacksonException e) {
    // Handle JSON parse error
    log.error("Parse error", e);
}
```

**Background:**
- Jackson 3.x changed `JacksonException` to be a `RuntimeException`
- `ObjectMappers.fromJson()` no longer catches and wraps this exception
- If you caught `InternalServerErrorException` from `fromJson()`, catch `JacksonException` instead
- Other uses of `ObjectMappers` are unaffected

---

## Step 8: Use `ErrorResponseWriter` in security filters (Spring Boot only)

Spring Boot includes a new utility for writing JSON error responses in security filters without direct exception propagation.

### Usage

```java
import io.github.jframe.spring.exception.ErrorResponseWriter;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final String token = extractToken(request);

        if (token == null || !isValid(token)) {
            // Option 1: Use ApiError enum
            ErrorResponseWriter.write(request, response, MyErrors.INVALID_TOKEN);
            return;
        }

        // Option 2: Use status + message directly
        if (isExpired(token)) {
            ErrorResponseWriter.write(
                request, 
                response, 
                Response.Status.UNAUTHORIZED, 
                "AUTH_002", 
                "Token expired"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

**Signatures:**
- `ErrorResponseWriter.write(request, response, ApiError apiError)` — uses error code + reason + status from enum
- `ErrorResponseWriter.write(request, response, Response.Status status, String errorCode, String reason)` — explicit values
- Response is automatically set to JSON and response status

**Benefits:**
- Avoid throwing exceptions in filter chains
- Consistent error response format (uses enricher pipeline)
- No try-catch needed for authentication logic

---

## Verify

```bash
# Rebuild and run tests
./gradlew clean build test

# Check for compilation errors related to:
# - io.github.jframe.exception.core.ApiException imports
# - io.github.jframe.exception.ApiErrorResponseResource type references
# - Catch blocks for InternalServerErrorException
# - ApiError implementations missing getHttpStatus()
# - DataNotFoundException, InternalServerErrorException, UnauthorizedRequestException usage
```

---

## Checklist

### ApiError enum updates
- [ ] All `ApiError` implementations have `Response.Status httpStatus` field
- [ ] All `ApiError` implementations implement `getHttpStatus()`
- [ ] All enum constants include appropriate `Response.Status` in constructor
- [ ] Imported `jakarta.ws.rs.core.Response`

### Exception replacements
- [ ] Replaced `ApiException` → `HttpException`
- [ ] Replaced `DataNotFoundException` → `ResourceNotFoundException` (or use `HttpException` with `NOT_FOUND`)
- [ ] Replaced `UnauthorizedRequestException` → `HttpException(Response.Status.UNAUTHORIZED, ...)`
- [ ] Removed `InternalServerErrorException` usage — let exceptions propagate
- [ ] Verified `HttpException` constructors accept `ApiError` or `Response.Status`

### Type references
- [ ] Replaced `ApiErrorResponseResource` → `ErrorResponseResource`
- [ ] Updated OpenAPI `@ApiResponse` annotations
- [ ] Updated any instanceof checks
- [ ] Updated custom enricher parent class → `ErrorResponseEnricher`

### Exception mapper references
- [ ] (Quarkus only) Removed `ApiExceptionMapper` imports
- [ ] (Spring only) No changes needed — `HttpExceptionHandler` handles all cases

### ObjectMappers
- [ ] Updated catch blocks for `fromJson()` — catch `JacksonException` instead of `InternalServerErrorException`

### Security filters (Spring Boot)
- [ ] (Spring Boot only) Updated authentication filters to use `ErrorResponseWriter` where appropriate
- [ ] No more exception propagation in filter chains

### Verification
- [ ] Full build passes: `./gradlew clean build test`
- [ ] HTTP error responses include correct status codes and error codes
- [ ] Validation exceptions still work as expected
- [ ] Authentication/authorization errors use correct status codes (401, 403)
- [ ] JSON error responses match expected format
- [ ] (Quarkus) Exception mappers correctly handle all `HttpException` variants
