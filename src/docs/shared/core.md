# jframe-core — Shared Library

Framework-agnostic infrastructure shared by Spring and Quarkus adapter modules. Pulled in transitively — you never need to depend on it directly.

## Exception hierarchy

```
RuntimeException
└── JFrameException (base)
    ├── HttpException (+ Response.Status, + errorCode, + errorReason)
    │   ├── BadRequestException           (400)
    │   ├── ResourceNotFoundException     (404)
    │   ├── RateLimitExceededException    (429, + limit/remaining/resetDate)
    │   └── SearchCriteriumException      (400, in datasource pkg)
    └── ValidationException (+ ValidationResult)
```

### ApiError interface

Three methods every error code must provide:

```java
public interface ApiError {
    String getErrorCode();   // e.g. "USER_001"
    String getReason();      // e.g. "User not found"
    Response.Status getHttpStatus();
}
```

Define application-specific error codes by implementing `ApiError`:

```java
public enum UserErrors implements ApiError {
    USER_NOT_FOUND("USER_001", "User does not exist", Response.Status.NOT_FOUND),
    USER_DISABLED("USER_002", "User account is disabled", Response.Status.FORBIDDEN);

    private final String errorCode;
    private final String reason;
    private final Response.Status httpStatus;
    // constructor + getters
}

// Throw it
throw new HttpException(UserErrors.USER_NOT_FOUND);
throw new HttpException(UserErrors.USER_NOT_FOUND, cause);
```

### JFrameErrorCode enum

Built-in `ApiError` constants for common cases:

```java
public enum JFrameErrorCode implements ApiError {
    BAD_REQUEST("JFRAME_BAD_REQUEST", "Bad request", Response.Status.BAD_REQUEST),
    NOT_FOUND("JFRAME_NOT_FOUND", "Resource not found", Response.Status.NOT_FOUND),
    RATE_LIMITED("JFRAME_RATE_LIMITED", "Rate limit exceeded", Response.Status.TOO_MANY_REQUESTS),
    VALIDATION_ERROR("JFRAME_VALIDATION_ERROR", "Validation failed", Response.Status.BAD_REQUEST),
    INTERNAL_ERROR("JFRAME_INTERNAL_ERROR", "Internal server error", Response.Status.INTERNAL_SERVER_ERROR),
    HTTP_ERROR("JFRAME_HTTP_ERROR", "HTTP error", Response.Status.BAD_REQUEST);
}
```

### HttpException constructors

```java
new HttpException(ApiError apiError)
new HttpException(ApiError apiError, Throwable cause)
```

Subclass constructors are no-arg or cause-only — the error code and HTTP status are fixed per class:

```java
new BadRequestException()
new BadRequestException(Throwable cause)

new ResourceNotFoundException()
new ResourceNotFoundException(Throwable cause)

new RateLimitExceededException(int limit, int remaining, ZonedDateTime resetDate)
```

### HttpStatusCode enum

Framework-independent HTTP status codes with utility methods:

```java
HttpStatusCode.NOT_FOUND.getCode();        // 404
HttpStatusCode.NOT_FOUND.getReason();      // "Not Found"
HttpStatusCode.NOT_FOUND.is4xxClientError(); // true
HttpStatusCode.valueOf(429);               // TOO_MANY_REQUESTS
```

### ErrorResponseResource fields

The structured error response DTO returned by both Spring and Quarkus exception handlers:

| Field | Type | Description |
|-------|------|-------------|
| `method` | String | HTTP method (e.g. `GET`) |
| `uri` | String | Request path |
| `query` | String | Query string, or `null` |
| `contentType` | String | Request content type |
| `statusCode` | int | HTTP status code |
| `errorCode` | String | Error code from `ApiError.getErrorCode()` |
| `errorReason` | String | Human-readable reason from `ApiError.getReason()` |
| `cause` | String | Wrapped exception message, or `null` when no cause |
| `txId` | String | Transaction ID |
| `traceId` | String | OpenTelemetry trace ID |
| `spanId` | String | OpenTelemetry span ID |

## Validation API

Fluent, composable validation with short-circuit evaluation.

### Basic validation

```java
Validator<CreateUserRequest> validator = (obj, result) -> {
    result.rejectField("email", obj.getEmail())
        .whenNull()
        .orWhen(e -> !e.contains("@"), "invalid_email");

    result.rejectField("age", obj.getAge())
        .whenNull()
        .orWhen(a -> a < 18, "too_young")
        .orWhen(a -> a > 120, "invalid_age");
};

// Option 1: check manually
ValidationResult result = validator.validate(request);
if (result.hasErrors()) { /* handle */ }

// Option 2: throw on failure
validator.validateAndThrow(request);  // throws ValidationException
```

### Hamcrest matcher support

```java
import static org.hamcrest.Matchers.*;

result.rejectValueIf(email, blankOrNullString(), "email_required");
result.rejectValueIf(age, lessThan(18), "too_young");
```

### Nested validation

```java
result.pushNestedPath("address");
result.rejectField("street", address.getStreet()).whenNull();
result.rejectField("city", address.getCity()).whenNull();
result.popNestedPath();
// Errors: "address.street", "address.city"
```

## Search specification framework

Framework-agnostic JPA Criteria API query builder.

### SearchSpecification interface

```java
public interface SearchSpecification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);

    default SearchSpecification<T> and(SearchSpecification<T> other) { ... }
    default SearchSpecification<T> or(SearchSpecification<T> other) { ... }
}
```

### Field types

| Type | Class | SQL |
|------|-------|-----|
| Exact text | `TextField` | `= ?` |
| Fuzzy text | `FuzzyTextField` | `LIKE %?%` |
| Multi text | `MultiTextField` | `IN (...)` |
| Multi fuzzy | `MultiFuzzyField` | `LIKE %?% AND/OR LIKE %?%` |
| Multi-column fuzzy | `MultiColumnFuzzyField` | `col1 LIKE %?% OR col2 LIKE %?%` |
| Numeric | `NumericField` | `= ?` |
| Boolean | `BooleanField` | `= ?` |
| Date | `DateField` | `BETWEEN ? AND ?` |
| Enum | `EnumField` | `= ?` |
| Multi enum | `MultiEnumField` | `IN (...)` |

All support inverse (`!` prefix) for negation.

### Pagination models

**Input:**
```java
SortablePageInput input = new SortablePageInput();
input.setPageNumber(0);
input.setPageSize(20);
input.setSortOrder(List.of(new SortableColumn("name", "ASC")));
input.setSearchInputs(List.of(
    new SearchInput("status", "ACTIVE"),
    new SearchInput("name", "john")
));
```

**Output:**
```java
PageResource<UserDto> page = ...;
page.getTotalElements();  // 142
page.getTotalPages();     // 8
page.getPageSize();       // 20
page.getPageNumber();     // 0
page.getContent();        // List<UserDto>
```

## Request context (ThreadLocal)

```java
// Set (typically by framework filters)
RequestId.set(UUID.randomUUID());
TransactionId.set(UUID.randomUUID());

// Read (anywhere in the request thread)
String reqId = RequestId.get();   // UUID string or null
String txId = TransactionId.get();

// Cleanup (typically in finally block)
RequestId.remove();
TransactionId.remove();
```

Both use `InheritableThreadLocal` — child threads inherit the parent's values.

## ECS/MDC logging

### Tag MDC fields

```java
import static io.github.jframe.logging.ecs.EcsFieldNames.*;

EcsFields.tag(REQUEST_ID, requestId);
EcsFields.tag(TX_ID, transactionId);
EcsFields.clear();  // removes ALL MDC fields
```

### Auto-closeable tagging

```java
try (var fields = EcsFields.tagCloseable(REQUEST_ID, reqId)
        .and(TX_ID, txId)) {
    // MDC populated within this scope
}
// MDC fields automatically removed
```

### Available MDC fields

`REQUEST_ID` (`request.id`), `TX_ID` (`transaction.id`), `TX_TYPE`, `TX_REQUEST_METHOD`, `TX_RESPONSE_SIZE`, `CALL_ID`, `TRACE_ID` (`trace.id`), `SPAN_ID` (`span.id`), `USER_NAME` (`user.name`), `HTTP_STATUS` (`http.response.status_code`), and 30+ more.

## Utilities

### JSON processing

```java
String json = ObjectMappers.toJson(object);
User user = ObjectMappers.fromJson(json, User.class);
List<User> users = ObjectMappers.fromJson(json, new TypeReference<>() {});
```

### Model conversion

```java
public class UserConverter extends AbstractModelConverter<UserEntity, UserDto> {
    public UserConverter() {
        super(UserDto.class, DefaultNullListConversionStrategies.returnEmptyList(UserDto.class));
    }

    @Override
    protected void doConvert(UserEntity source, UserDto target) {
        target.setName(source.getName());
    }
}
```

### Constants

```java
Constants.Headers.TX_ID_HEADER;       // "X-Transaction-Id"
Constants.Headers.REQ_ID_HEADER;      // "X-Request-Id"
Constants.Headers.TRACE_ID_HEADER;    // "X-Trace-Id"
```
