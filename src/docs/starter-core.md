# starter-core

Foundation module providing core utilities, exception handling, logging, validation, caching, and shared configuration.

## Location

```
starter-core/src/main/java/io/github/jframe/
├── autoconfigure/          # Auto-configuration and properties
│   └── properties/         # ApplicationProperties, LoggingProperties
├── cache/                  # Request-scoped caching infrastructure
├── exception/              # Exception hierarchy and handlers
│   ├── core/              # HTTP exceptions (400, 401, 404, 500)
│   └── handler/           # Exception handlers and enrichers
├── logging/                # HTTP logging infrastructure
│   ├── filter/            # Request/response logging filters
│   ├── voter/             # Content filtering logic
│   ├── masker/            # Sensitive data masking
│   └── kibana/            # Structured logging (MDC)
├── validation/             # Validation framework
│   └── field/             # Fluent field validation API
└── util/                   # Utilities and mappers
```

## Components

### Application Properties

Centralized application metadata accessible across all modules.

**Config:** `jframe.application.*`

| Property | Description | Default |
|----------|-------------|---------|
| `name` | Service name | `--- UNSET ---` |
| `group` | Service group/namespace | `--- UNSET ---` |
| `version` | Application version | `--- UNSET ---` |
| `environment` | Runtime environment | `dev` |
| `url` | Base application URL | `https://localhost:8080/` |

**Usage:** `@Autowired ApplicationProperties`

### Exception Handling

Hierarchical exception structure with automatic HTTP status mapping. See [detailed docs](./core/exception-handling.md).

**Hierarchy:**
```
JFrameException
  ├── HttpException (400, 401, 404, 500)
  ├── ApiException (custom error codes)
  └── ValidationException (field-level errors)
```

**Key Classes:**
- `BadRequestException` (400) - Invalid input
- `UnauthorizedRequestException` (401) - Auth required
- `ResourceNotFoundException` (404) - Entity not found
- `InternalServerErrorException` (500) - Server error
- `ValidationException` (400) - Field validation errors
- `ApiException` (400) - Business logic errors with custom codes
- `JFrameResponseEntityExceptionHandler` - Centralized error handling

**Features:** Automatic error responses, request context, field-level errors, OpenTelemetry integration, pluggable enrichers

### Logging

HTTP request/response logging with correlation tracking and masking. See [detailed docs](./core/logging.md).

**Config:** `jframe.logging.*`

**Filters (execution order):**

| Order | Filter | Purpose |
|-------|--------|---------|
| -400 | RequestIdFilter | Generate/extract request correlation ID (`x-request-id`) |
| -300 | TransactionIdFilter | Track transactions (`x-transaction-id`) |
| -200 | RequestDurationFilter | Measure request duration |
| -100 | RequestResponseLogFilter | Log request/response bodies |

**Features:**
- Automatic correlation IDs
- Request/response body logging
- Sensitive field masking (passwords, tokens)
- Content type filtering
- Path exclusion
- MDC population for Kibana/ELK
- REST client interceptor

**Configuration:**
```yaml
jframe:
  logging:
    disabled: false
    response-length: -1
    allowed-content-types: [application/json, application/xml]
    exclude-paths:
      - method: GET
        path: /actuator/**
    fields-to-mask: [password, secret, token]
```

### Validation

Fluent validation framework with field-level error accumulation. See [detailed docs](./core/validation.md).

**Key Classes:**
- `ValidationResult` - Accumulates validation errors
- `ValidationError` - Represents field + error code
- `Validator<T>` - Functional interface for reusable validators
- `FieldRejection` - Fluent API with short-circuit evaluation

**Example:**
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

**Features:** Fluent API, error accumulation, nested paths, Hamcrest matchers, Spring-independent

### Request-Scoped Caching

Generic request-scoped cache for eliminating duplicate database queries within a single HTTP request. See [detailed docs](./core/caching.md).

**Key Classes:**
- `RequestScopedCache<K, V>` - Abstract base class for entity caching

**Example:**
```java
@Component
@RequestScope
public class UserCache extends RequestScopedCache<Long, User> {
    @Override
    protected Long getId(User entity) {
        return entity.getId();
    }
}

// Usage in service
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserCache userCache;
    private final UserRepository userRepository;

    public void processOrder(Order order) {
        // Cache hit or load from DB (single item)
        User user = userCache.getOrLoad(order.getUserId(), 
            id -> userRepository.findById(id)).orElseThrow();
        
        // Batch loading for multiple IDs
        Map<Long, User> users = userCache.getAllOrLoad(userIds, 
            ids -> userRepository.findAllById(ids));
    }
}
```

**Features:**
- Automatic cache eviction at request end
- Single and batch entity loading
- Thread-safe (ConcurrentHashMap backing)
- Zero configuration required

### Utilities

**ObjectMappers** - Pre-configured Jackson utilities:
```java
String json = ObjectMappers.toJson(object);
MyObject obj = ObjectMappers.fromJson(json, MyObject.class);
```

**MapStruct** - Shared mapper configuration:
```java
@Mapper(config = SharedMapperConfig.class)
public interface UserMapper {
    UserDto toDto(User user);
}
```

**Model Converters** - Generic converter interface for object transformation

## Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot Starter | Core Spring Boot functionality |
| Spring Boot Web | Filters and REST support |
| MapStruct | Compile-time object mapping |
| Jackson JSR310 | Java Time API support |
| Apache Commons Lang3 | Utility methods |

## Auto-Configuration

`CoreAutoConfiguration` automatically:
- Registers `ApplicationProperties` and `LoggingProperties`
- Configures logging filters
- Registers exception handlers
- Loads `jframe-properties.yml`

**Discovery:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

## Configuration

### Complete Example

```yaml
jframe:
  application:
    name: "my-app"
    version: "1.0.0"
    environment: "production"

  logging:
    disabled: false
    response-length: 10000
    allowed-content-types:
      - application/json
      - application/xml
    exclude-paths:
      - method: "*"
        path: /actuator/**
      - method: "*"
        path: /health
    fields-to-mask:
      - password
      - secret
      - token
      - apiKey
```

## See Also

- [Exception Handling](./core/exception-handling.md)
- [Logging Framework](./core/logging.md)
- [Validation Framework](./core/validation.md)
- [Request-Scoped Caching](./core/caching.md)
- [starter-jpa](./starter-jpa.md)
- [starter-otlp](./starter-otlp.md)
