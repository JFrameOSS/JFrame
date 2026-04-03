# jframe-core — AI Context

> Framework-agnostic foundation for Java 21 enterprise applications. Shared by Spring Boot and Quarkus adapters.

## Exception Hierarchy

```
RuntimeException
  └─ JFrameException (base)
      ├─ HttpException (+ Response.Status from jakarta.ws.rs)
      │   ├─ BadRequestException (400)
      │   ├─ DataNotFoundException (404)
      │   ├─ ResourceNotFoundException (404)
      │   ├─ UnauthorizedRequestException (401)
      │   ├─ InternalServerErrorException (500)
      │   └─ RateLimitExceededException (429, + limit/remaining/resetDate)
      ├─ ApiException (+ ApiError interface with errorCode/reason)
      └─ ValidationException (+ ValidationResult with List<ValidationError>)
```

**Error Response Resources** — DTOs for JSON error responses:
- `ErrorResponseResource` — base: method, uri, query, statusCode, statusMessage, errorMessage, txId, traceId, spanId
- `ApiErrorResponseResource` — adds apiErrorCode, apiErrorReason
- `ValidationErrorResponseResource` — adds List<ValidationErrorResource> (code + field)
- `ConstraintViolationResponseResource` — for Jakarta Bean Validation
- `RateLimitErrorResponseResource` — adds limit, remaining, resetDate
- `ExceptionResponseFactory` — functional interface to create responses from exceptions

## ECS Structured Logging

All logging uses Elastic Common Schema field names via SLF4J MDC.

### Core API

```java
// Interface — implement for custom fields
public interface EcsField {
    String getKey(); // MDC key (e.g., "transaction.id")
}

// Tag fields (sets MDC)
EcsFields.tag(EcsFieldNames.TX_ID, transactionId);
EcsFields.tag(EcsFieldNames.CALL_STATUS, CallResultTypes.SUCCESS);

// Auto-closeable (clears on scope exit)
try (var field = EcsFields.tagCloseable(LOG_TYPE, CALL_START)
        .and(CALL_REQUEST_METHOD, "POST")
        .and(CALL_REQUEST_URI, "/api/users")) {
    log.info("Outbound call started");
} // all 3 fields cleared

// Read/clear
String txId = EcsFields.get(EcsFieldNames.TX_ID);
EcsFields.clear(EcsFieldNames.TX_ID);
EcsFields.clear(); // clear all

// Cross-thread propagation
MdcLogContext ctx = EcsFields.getMdcContext();
executor.submit(() -> {
    EcsFields.populateFromContext(ctx);
    // child thread inherits MDC
});
```

### Key Field Names (EcsFieldNames enum)

| Constant | ECS Key | Usage |
|----------|---------|-------|
| `TX_ID` | `transaction.id` | Transaction correlation |
| `REQUEST_ID` | `request.id` | Request correlation |
| `TRACE_ID` | `trace.id` | OpenTelemetry trace |
| `SPAN_ID` | `span.id` | OpenTelemetry span |
| `TX_REQUEST_METHOD` | `http.request.method` | Inbound HTTP method |
| `TX_REQUEST_URI` | `url.path` | Inbound request path |
| `HTTP_STATUS` | `http.response.status_code` | Response status |
| `REQUEST_DURATION` | `event.duration` | Request duration |
| `LOG_TYPE` | `event.category` | Log entry type |
| `CALL_REQUEST_METHOD` | `http.client.request.method` | Outbound HTTP method |
| `CALL_REQUEST_URI` | `url.full` | Outbound URL |
| `CALL_STATUS` | `event.outcome` | Call result |
| `USER_NAME` | `user.name` | Authenticated user |

### Enums
- `CallResultTypes`: SUCCESS, TIMEOUT, FAILURE
- `LogTypeNames`: START, END, CALL_START, CALL_END, REQUEST_BODY, RESPONSE_BODY, CALL_REQUEST_BODY, CALL_RESPONSE_BODY

## Search Specification Framework

Type-safe, framework-agnostic search criteria system.

### Search Types

| SearchType | Field Class | SQL | Inverse |
|------------|------------|-----|---------|
| `TEXT` | `TextField` | `= value` | `!value` prefix |
| `MULTI_TEXT` | `MultiTextField` | `IN (values)` | No |
| `FUZZY_TEXT` | `FuzzyTextField` | `LIKE %value%` (case-insensitive) | No |
| `MULTI_FUZZY` | `MultiFuzzyField` | Multiple LIKE + AND/OR | No |
| `MULTI_COLUMN_FUZZY` | `MultiColumnFuzzyField` | LIKE across columns | No |
| `ENUM` | `EnumField` | `= enum` | `!value` prefix |
| `MULTI_ENUM` | `MultiEnumField` | `IN (enums)` | No |
| `BOOLEAN` | `BooleanField` | `= true/false` | No |
| `DATE` | `DateField` | `BETWEEN from AND to` | No |
| `NUMERIC` | `NumericField` | `= number` | `!value` prefix |

### DTOs (REST API)

```java
// Request
SortablePageInput {
    int pageNumber;       // 0-based
    int pageSize;
    List<SortableColumn> sortOrder;   // [{name, direction}]
    List<SearchInput> searchInputs;   // [{fieldName, operator, textValue, textValueList, fromDateValue, toDateValue}]
}

// Response
PageResource<T> {
    long totalElements;
    int totalPages, pageSize, pageNumber;
    List<T> content;
}
```

### Building Specifications

```java
List<SearchCriterium> criteria = List.of(
    new TextField("status", "ACTIVE"),
    new FuzzyTextField("name", "john"),
    new DateField("created_at", "2024-01-01T00:00:00", null)
);
BaseSearchSpecification<User> spec = new BaseSearchSpecification<>(criteria);
```

## Validation API

```java
// Fluent validation
ValidatorBuilder.create()
    .that(order.getAmount()).is(greaterThan(0))
        .otherwise("amount", "must be positive")
    .that(order.getEmail()).given(order.isNotifyEnabled())
        .is(notBlank())
        .otherwise("email", "required when notifications enabled")
    .validate(); // throws ValidationException if errors

// With Hamcrest matchers
HamcrestValidatorBuilder.create()
    .that(value).is(greaterThan(0)).otherwise("field", "error")
    .validate();
```

## Password Masking

```java
PasswordMasker masker = new PasswordMasker(List.of("password", "secret"));
String safe = masker.maskPasswordsIn(jsonOrUri);
// {"password":"***"} or ?password=***
```

Built-in maskers: `JsonPasswordMasker`, `UriQueryStringPasswordMasker`.

## Tracing Utilities

- `OtlpDefaults` — shared defaults: url=`http://localhost:4318`, timeout=`10s`, sampling=`1.0`, excluded=`health,actuator,ping,status,info,metrics`
- `SpanNamingUtil.resolveClassName("UserService_Subclass")` → `"UserService"` (strips CDI/CGLIB proxy suffixes)
- `MethodExclusionRules.isExcluded(name, configSet)` — excludes getters/setters, toString/hashCode/equals, and configured methods

## Security

```java
// Interface for authentication resolution
public interface AuthenticationResolver {
    String getAuthenticatedSubject();
    // Returns user ID, or AuthenticationConstants.ANONYMOUS / INCOMPLETE
}
```

## SQL Logging Control

```java
try (var s = SqlStatementLogging.suppress()) {
    repository.saveAll(batch); // SQL not logged
}
```

## Utilities

- `Constants.Headers` — TX_ID_HEADER, REQ_ID_HEADER, TRACE_ID_HEADER, SPAN_ID_HEADER, rate limit headers
- `Constants.DateTime` — EUROPE_AMSTERDAM zone, ISO formatter, epoch start
- `RequestId` / `TransactionId` — ThreadLocal UUID storage
- `PathDefinition` — Ant-style path matcher (`/api/**`, `/user/*`, `/item/?`)
- `HttpBodyUtil` — truncate + mask request/response bodies for logging
- `JsonUtil`, `ModelConverter`, `IndentUtil`, `ConverterUtil`
