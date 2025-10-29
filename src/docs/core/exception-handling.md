# Exception Handling

Hierarchical exception framework with automatic HTTP status mapping and structured error responses.

**Package:** `io.github.jframe.exception`

## Exception Hierarchy

```
JFrameException (RuntimeException)
  ├── HttpException (HTTP status-aware)
  │     ├── BadRequestException (400)
  │     ├── UnauthorizedRequestException (401)
  │     ├── ResourceNotFoundException (404)
  │     ├── DataNotFoundException (404)
  │     └── InternalServerErrorException (500)
  ├── ApiException (custom error codes)
  └── ValidationException (field-level errors)
```

## Exception Types

### HTTP Exceptions

| Exception | Status | Use Case |
|-----------|--------|----------|
| `BadRequestException` | 400 | Invalid/malformed input |
| `UnauthorizedRequestException` | 401 | Authentication required/failed |
| `ResourceNotFoundException` | 404 | Entity not found |
| `DataNotFoundException` | 404 | Data/record not found |
| `InternalServerErrorException` | 500 | Unexpected server error |

**Examples:**
```java
// 400 - Bad Request
if (StringUtils.isBlank(username)) {
    throw new BadRequestException("Username is required");
}

// 401 - Unauthorized
if (!authService.isAuthenticated()) {
    throw new UnauthorizedRequestException("Authentication required");
}

// 404 - Not Found
User user = userRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

// 500 - Internal Server Error
try {
    externalService.process(data);
} catch (ServiceException e) {
    throw new InternalServerErrorException("Failed to process request", e);
}
```

### API Exceptions

Business logic errors with custom error codes.

**Define Error Codes:**
```java
public enum OrderErrorCode implements ApiError {
    INSUFFICIENT_INVENTORY("ORD-001", "Insufficient inventory"),
    PAYMENT_FAILED("ORD-002", "Payment processing failed"),
    DUPLICATE_ORDER("ORD-003", "Order already exists");

    private final String code;
    private final String reason;

    OrderErrorCode(String code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    @Override
    public String getErrorCode() { return code; }

    @Override
    public String getReason() { return reason; }
}
```

**Throw API Exception:**
```java
if (inventory < quantity) {
    throw new ApiException(OrderErrorCode.INSUFFICIENT_INVENTORY,
        "Only " + inventory + " items available");
}
```

### Validation Exceptions

Field-level validation errors with detailed feedback.

```java
ValidationResult result = new ValidationResult();

result.rejectField("email", user.getEmail())
    .whenNull()
    .orWhen(not(containsString("@")), "email.invalid");

result.rejectField("age", user.getAge())
    .whenNull()
    .orWhen(a -> a < 18, "age.under_minimum");

if (result.hasErrors()) {
    throw new ValidationException(result);
}
```

See [Validation Framework](./validation.md) for complete details.

## Exception Handler

**Class:** `JFrameResponseEntityExceptionHandler`

Centralized exception handler that:
- Catches all `JFrameException` types
- Maps to appropriate HTTP status codes
- Builds consistent error responses
- Includes request context (method, URI, query)
- Enriches responses with additional information
- Integrates with OpenTelemetry

**Handled Exceptions:**

| Exception | Status | Handler Method |
|-----------|--------|----------------|
| `HttpException` | From exception | `handleHttpException()` |
| `ApiException` | 400 | `handleApiException()` |
| `ValidationException` | 400 | `handleValidationException()` |
| `MethodArgumentNotValidException` | 400 | `handleValidationException()` |
| `BadCredentialsException` | 401 | `handleBadCredentialsException()` |
| `AccessDeniedException` | 403 | `handleAccessDeniedException()` |
| `NoResourceFoundException` | 404 | `handleNoResourceFoundException()` |
| `Throwable` | 500 | `handleThrowable()` |

## Error Responses

### Base Error Response

```json
{
  "statusCode": 400,
  "statusMessage": "Bad Request",
  "errorMessage": "Validation failed",
  "method": "POST",
  "uri": "/api/users",
  "query": null,
  "contentType": "application/json"
}
```

### Validation Error Response

```json
{
  "statusCode": 400,
  "statusMessage": "Bad Request",
  "errorMessage": "Validation failed",
  "method": "POST",
  "uri": "/api/users",
  "errors": [
    {
      "field": "email",
      "code": "required",
      "message": "Email is required"
    },
    {
      "field": "age",
      "code": "min",
      "message": "Must be 18 or older"
    }
  ]
}
```

### API Error Response

```json
{
  "statusCode": 400,
  "statusMessage": "Bad Request",
  "errorMessage": "Insufficient inventory",
  "errorCode": "ORD-001",
  "reason": "Insufficient inventory",
  "method": "POST",
  "uri": "/api/orders"
}
```

## Response Enrichers

**Package:** `io.github.jframe.exception.handler.enricher`

Pluggable system to add custom fields to error responses.

**Built-in Enrichers:**
- `ErrorResponseStatusEnricher` - HTTP status code and timestamp
- `ErrorMessageResponseEnricher` - Error message from exception
- `RequestInfoErrorResponseEnricher` - Request metadata (URI, method, query)
- `ValidationErrorResponseEnricher` - Field-level validation errors
- `ApiErrorResponseEnricher` - Custom error codes

**Custom Enricher:**
```java
@Component
public class TraceIdEnricher implements ErrorResponseEnricher {
    @Override
    public void doEnrich(ErrorResponseResource resource,
                         Throwable throwable,
                         WebRequest request,
                         HttpStatus httpStatus) {
        String traceId = MDC.get(KibanaLogFields.TX_ID);
        if (traceId != null) {
            resource.setTraceId(traceId);
        }
    }
}
```

## Usage Examples

### Service Layer

```java
@Service
public class UserService {
    public User createUser(CreateUserRequest request) {
        // Validate
        ValidationResult validation = new ValidationResult();
        validation.rejectField("email", request.getEmail())
            .whenNull()
            .orWhen(not(containsString("@")), "email.invalid");

        // Business rule
        if (userRepository.existsByEmail(request.getEmail())) {
            validation.rejectValue("email", "email.already_exists");
        }

        if (validation.hasErrors()) {
            throw new ValidationException(validation);
        }

        return userRepository.save(toEntity(request));
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return userMapper.toDto(user);
    }

    @PostMapping
    public UserDto createUser(@RequestBody CreateUserRequest request) {
        if (StringUtils.isBlank(request.getEmail())) {
            throw new BadRequestException("Email is required");
        }

        User user = userService.create(request);
        return userMapper.toDto(user);
    }
}
```

### API Exceptions with Error Codes

```java
@Service
public class OrderService {
    public Order createOrder(OrderRequest request) {
        // Check inventory
        int available = inventoryService.getAvailable(request.getProductId());
        if (available < request.getQuantity()) {
            throw new ApiException(OrderErrorCode.INSUFFICIENT_INVENTORY,
                "Only " + available + " items available");
        }

        // Process payment
        PaymentResult result = paymentService.charge(request.getPayment());
        if (!result.isSuccess()) {
            throw new ApiException(OrderErrorCode.PAYMENT_FAILED,
                "Payment declined: " + result.getReason());
        }

        return orderRepository.save(toEntity(request));
    }
}
```

## Custom Exceptions

### Custom HTTP Exception

```java
public class ConflictException extends HttpException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);  // 409
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause, HttpStatus.CONFLICT);
    }
}

// Usage
if (userRepository.existsByUsername(username)) {
    throw new ConflictException("Username already exists");
}
```

### Domain-Specific Exception

```java
public class PaymentException extends ApiException {
    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(PaymentErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
```

## Best Practices

1. **Use Specific Exception Types** - Not generic `JFrameException`
2. **Include Context** - Add relevant details to exception messages
3. **Use ValidationException for Fields** - Preserve field-level detail
4. **Preserve Root Cause** - Pass original exception in constructor
5. **Use API Exceptions for Business Errors** - Custom codes for domain logic
6. **Define Error Codes as Enums** - Type-safe and centralized
7. **Document Expected Exceptions** - JavaDoc on service methods

## See Also

- [Validation Framework](./validation.md)
- [Logging Framework](./logging.md)
- [starter-core](../starter-core.md)