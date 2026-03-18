# jframe-core — Shared Library

Framework-agnostic infrastructure shared by Spring and Quarkus adapter modules. Pulled in transitively — you never need to depend on it directly.

## Exception hierarchy

```
RuntimeException
└── JFrameException
    ├── HttpException(HttpStatusCode, message)
    │   ├── BadRequestException           (400)
    │   ├── UnauthorizedRequestException  (401)
    │   ├── ResourceNotFoundException     (404)
    │   ├── DataNotFoundException         (404, with ApiError)
    │   ├── InternalServerErrorException  (500)
    │   └── RateLimitExceededException    (429, with limit/remaining/reset)
    ├── ApiException(ApiError)
    └── ValidationException(ValidationResult)
```

### ApiError interface

Define application-specific error codes:

```java
public enum UserErrors implements ApiError {
    USER_NOT_FOUND("USER_001", "User does not exist"),
    USER_DISABLED("USER_002", "User account is disabled");

    private final String errorCode;
    private final String reason;
    // constructor + getters
}

// Throw it
throw new ApiException(UserErrors.USER_DISABLED);
```

### HttpStatusCode enum

Framework-independent HTTP status codes with utility methods:

```java
HttpStatusCode.NOT_FOUND.getCode();        // 404
HttpStatusCode.NOT_FOUND.getReason();      // "Not Found"
HttpStatusCode.NOT_FOUND.is4xxClientError(); // true
HttpStatusCode.valueOf(429);               // TOO_MANY_REQUESTS
```

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

## Kibana/MDC logging

### Tag MDC fields

```java
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;

KibanaLogFields.tag(REQUEST_ID, requestId);
KibanaLogFields.tag(TX_ID, transactionId);
KibanaLogFields.clear();  // removes ALL MDC fields
```

### Auto-closeable tagging

```java
try (var fields = KibanaLogFields.tagCloseable(REQUEST_ID, reqId)
        .and(TX_ID, txId)) {
    // MDC populated within this scope
}
// MDC fields automatically removed
```

### Available MDC fields

`REQUEST_ID` (`req_id`), `TX_ID` (`tx_id`), `TX_TYPE`, `TX_REQUEST_METHOD`, `TX_RESPONSE_SIZE`, `CALL_ID`, `TRACE_ID`, `SPAN_ID`, `USER_NAME`, `HTTP_STATUS`, and 30+ more.

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
