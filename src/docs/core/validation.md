# Validation Framework

Fluent validation framework with field-level error accumulation and Hamcrest matcher integration.

**Package:** `io.github.jframe.validation`

## Core Components

### ValidationResult

Accumulates validation errors with nested path support.

**Key Methods:**
```java
// Field rejection
void rejectValue(String field, String code)
void rejectValueIf(boolean condition, String field, String code)

// Fluent API
FieldRejection<T> rejectField(String field, T actual)

// Nested paths
void pushNestedPath(String path)
void pushNestedPath(String path, int index)
void popNestedPath()

// Inspection
boolean hasErrors()
List<ValidationError> getErrors()
```

### ValidationError

Represents a single validation error (field + code).

```java
public class ValidationError {
    private final String field;  // e.g., "email", "address.zipCode"
    private final String code;   // e.g., "required", "invalid"
}
```

### Validator Interface

Functional interface for reusable validators.

```java
@FunctionalInterface
public interface Validator<T> {
    void validate(T object, ValidationResult result);

    // Convenience methods
    default ValidationResult validate(T object);
    default void validateAndThrow(T object);
    default boolean isValid(T object);
}
```

### FieldRejection

Fluent API with short-circuit evaluation.

```java
// Null check
FieldRejection<T> whenNull()
FieldRejection<T> whenNull(String code)

// Matcher/Predicate checks
FieldRejection<T> when(Matcher<T> matcher)
FieldRejection<T> when(Predicate<T> predicate)
FieldRejection<T> when(Matcher<T> matcher, String code)
FieldRejection<T> when(Predicate<T> predicate, String code)

// Chaining
FieldRejection<T> or()
FieldRejection<T> orWhen(...)
```

## Validation Approaches

### 1. Direct Validation

```java
public void registerUser(RegistrationRequest request) {
    ValidationResult result = new ValidationResult();

    if (StringUtils.isBlank(request.getEmail())) {
        result.rejectValue("email", "required");
    }

    result.rejectValueIf(request.getAge() < 18, "age", "min");

    if (result.hasErrors()) {
        throw new ValidationException(result);
    }
}
```

### 2. Fluent API

```java
public void validateAddress(Address address) {
    ValidationResult result = new ValidationResult();

    result.rejectField("zipCode", address.getZipCode())
        .whenNull()
        .orWhen(z -> z.length() != 5)
        .orWhen(not(matchesPattern("\\d{5}")));

    result.rejectField("street", address.getStreet())
        .whenNull("street.required")
        .orWhen(String::isBlank, "street.empty")
        .orWhen(s -> s.length() > 100, "street.too_long");

    if (result.hasErrors()) {
        throw new ValidationException(result);
    }
}
```

### 3. Reusable Validator

```java
@Component
public class UserValidator implements Validator<User> {
    @Override
    public void validate(User user, ValidationResult result) {
        result.rejectField("username", user.getUsername())
            .whenNull()
            .orWhen(u -> u.length() < 3, "username.too_short")
            .orWhen(u -> u.length() > 20, "username.too_long");

        result.rejectField("email", user.getEmail())
            .whenNull()
            .orWhen(not(containsString("@")), "email.invalid");
    }
}

// Usage
userValidator.validateAndThrow(user);
```

## Features

### Short-Circuit Evaluation

The fluent API stops at the first matching condition:

```java
result.rejectField("zipCode", "abc")
    .whenNull()                    // Skipped (not null)
    .orWhen(z -> z.length() != 5)  // MATCHES - adds error "invalid"
    .orWhen(not(matchesPattern("\\d{5}")));  // NOT EVALUATED

// Only one error added: field="zipCode", code="invalid"
```

### Nested Path Support

```java
ValidationResult result = new ValidationResult();

// Nested object
result.pushNestedPath("shippingAddress");
result.rejectValueIf(address.getZipCode() == null, "zipCode", "required");
result.popNestedPath();

// List items with index
for (int i = 0; i < order.getItems().size(); i++) {
    result.pushNestedPath("items", i);
    OrderItem item = order.getItems().get(i);
    result.rejectValueIf(item.getQuantity() <= 0, "quantity", "min");
    result.popNestedPath();
}

// Produces errors like:
// - field="shippingAddress.zipCode", code="required"
// - field="items[0].quantity", code="min"
```

### Hamcrest Matchers

```java
import static org.hamcrest.Matchers.*;

// String matchers
result.rejectField("name", name)
    .when(containsString("admin"), "name.reserved");

result.rejectField("zipCode", zipCode)
    .when(not(matchesPattern("\\d{5}")), "zipCode.invalid_format");

// Numeric matchers
result.rejectValueIf(age, lessThan(18), "age", "underage");
result.rejectValueIf(price, greaterThan(1000.0), "price", "too_expensive");

// Collection matchers
result.rejectValueIf(list, empty(), "items", "required");
result.rejectValueIf(tags, hasSize(lessThan(3)), "tags", "insufficient");
```

## Error Codes

### Default Codes

| Code | Triggered By |
|------|--------------|
| `required` | `whenNull()` |
| `invalid` | `when()`, `orWhen()` with matcher/predicate |

### Custom Codes

```java
result.rejectValue("age", "age.under_18");

result.rejectField("email", email)
    .whenNull("email.required")
    .orWhen(not(containsString("@")), "email.missing_at_sign")
    .orWhen(e -> e.length() > 100, "email.too_long");
```

### Recommended Format

`<field>.<violation>`

Examples:
- `email.required`
- `email.invalid`
- `email.already_exists`
- `password.too_short`
- `age.under_minimum`

## Integration

### With Exception Handling

When `ValidationException` is thrown, the exception handler produces detailed JSON responses:

**Request:**
```http
POST /api/users HTTP/1.1
{"email": "", "age": 15}
```

**Response (400):**
```json
{
  "statusCode": 400,
  "statusMessage": "Bad Request",
  "errorMessage": "Validation failed",
  "errors": [
    {"field": "email", "code": "required"},
    {"field": "age", "code": "min"}
  ]
}
```

### Service Layer Usage

```java
@Service
public class UserService {
    private final UserValidator userValidator;

    public User createUser(CreateUserRequest request) {
        ValidationResult validation = new ValidationResult();

        // Field validation
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

## Advanced Patterns

### Conditional Validation

```java
if (user.getType() == UserType.BUSINESS) {
    result.rejectField("taxId", user.getTaxId())
        .whenNull("tax_id.required_for_business");
}
```

### Cross-Field Validation

```java
if (user.getPassword() != null && !user.getPassword().equals(user.getConfirmPassword())) {
    result.rejectValue("confirmPassword", "password.mismatch");
}

if (order.getStartDate() != null && order.getEndDate() != null) {
    if (order.getStartDate().isAfter(order.getEndDate())) {
        result.rejectValue("endDate", "date.end_before_start");
    }
}
```

### Composite Validator

```java
@Component
public class OrderValidator implements Validator<Order> {
    private final AddressValidator addressValidator;
    private final PaymentValidator paymentValidator;

    @Override
    public void validate(Order order, ValidationResult result) {
        result.rejectField("orderNumber", order.getOrderNumber())
            .whenNull();

        result.pushNestedPath("shippingAddress");
        addressValidator.validate(order.getShippingAddress(), result);
        result.popNestedPath();

        result.pushNestedPath("payment");
        paymentValidator.validate(order.getPayment(), result);
        result.popNestedPath();
    }
}
```

## Best Practices

1. **Use Fluent API** - More readable than imperative style
2. **Meaningful Error Codes** - Use descriptive codes like `email.invalid_format`
3. **Fail-Safe Validation** - Accumulate all errors, don't stop at first
4. **Reusable Validators** - Extract complex validation to components
5. **Nested Paths for Complex Objects** - Provide clear field locations
6. **Validate at Service Layer** - Not just at API boundary

## Comparison with Spring Validation

| Feature | JFrame | Spring |
|---------|--------|--------|
| **Fluent API** | Yes | No |
| **Nested Paths** | Yes | Yes |
| **Hamcrest Integration** | Yes | No |
| **Exception Type** | `ValidationException` | `MethodArgumentNotValidException` |
| **Object Binding** | Not required | Required |
| **Declarative Annotations** | No | Yes (`@Valid`, `@NotNull`) |
| **Spring Independence** | Yes | No |

## See Also

- [Exception Handling](./exception-handling.md)
- [Logging Framework](./logging.md)
- [starter-core](../starter-core.md)