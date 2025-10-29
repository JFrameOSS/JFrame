# JFrame Architecture

High-level overview of JFrame architecture, design principles, and module interactions.

## Design Principles

### Convention Over Configuration
- Sensible defaults for all components
- Zero-configuration setup for most use cases
- Easy customization when needed via properties

### Modular Design
- Independent modules with clear boundaries
- Optional dependencies based on application needs
- Extensible architecture for custom implementations

### Spring Boot Integration
- Native Spring Boot auto-configuration
- Follows Spring Boot conventions
- Seamless integration with Spring ecosystem

## Module Dependencies

```
starter-otlp ──┐
               ├─► starter-core (foundation)
starter-jpa ───┘
```

### starter-core (Foundation)
**Role:** Core utilities and shared configuration for all modules

**Responsibilities:**
- Application metadata (`ApplicationProperties`)
- Exception handling framework
- HTTP logging infrastructure
- Validation framework
- Utilities (JSON, MapStruct, converters)

**Configuration:** `jframe-properties.yml`

**Key Features:**
- Central configuration file for all properties
- Used by other modules for service identification
- Minimal external dependencies

### starter-jpa (Data Layer)
**Role:** Enhanced JPA capabilities for database operations

**Responsibilities:**
- Dynamic search with JPA specifications
- Standardized pagination responses
- Database query monitoring

**Dependencies:** Spring Data JPA, starter-core

### starter-otlp (Observability)
**Role:** Comprehensive observability and monitoring

**Responsibilities:**
- Distributed tracing across services
- Metrics collection
- Auto-instrumentation

**Dependencies:** OpenTelemetry stack, starter-core

**Key Features:**
- Uses `ApplicationProperties` for service naming
- Configurable via `jframe.otlp.*` properties

## Configuration System

### Centralized Configuration

All JFrame properties defined in `starter-core/src/main/resources/jframe-properties.yml`:

```yaml
jframe:
  application:        # ApplicationProperties (starter-core)
    name: "--- UNSET ---"
    version: "--- UNSET ---"
    environment: "dev"

  logging:            # LoggingProperties (starter-core)
    disabled: false

  otlp:               # OpenTelemetryProperties (starter-otlp)
    disabled: true
    url: "http://localhost:4318"
```

### Property Cross-References

Modules can reference each other's properties:

```yaml
# starter-otlp uses ApplicationProperties
jframe:
  otlp:
    service-name: "${jframe.application.name}"
```

### Auto-Configuration

Each module provides Spring Boot auto-configuration:

- `CoreAutoConfiguration` (starter-core) - Registers properties, filters, handlers
- `OpenTelemetryAutoConfiguration` (starter-otlp) - Registers OTLP configuration

**Discovery:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

## Request Flow

### Typical HTTP Request

```
1. HTTP Request → Logging Filters (correlation IDs, logging)
2. → Spring Web Controller
3. → Service Layer (with validation, custom tracing)
4. → Repository Layer (JPA with auto-instrumentation)
5. → Database (with query logging)
6. → Response mapping (ObjectMappers)
7. → Trace export to observability backend
```

### Search Operation

```
1. SearchInput via REST API
2. → JpaSearchSpecification builds dynamic query
3. → Spring Data JPA executes with pagination
4. → Results mapped to PageResource
5. → JSON serialization via ObjectMappers
6. → Complete operation traced end-to-end
```

## Cross-Cutting Concerns

### Logging
- HTTP request/response logging (starter-core)
- Correlation IDs for request tracking
- Structured logging for Kibana/ELK
- OpenTelemetry trace correlation (starter-otlp)

### Error Handling
- Consistent error response formats
- Hierarchical exception structure
- Request context in error responses
- Trace correlation in errors

### Observability
- Automatic HTTP instrumentation
- Database query tracing
- Custom business metric collection
- Distributed tracing across services

## Performance Considerations

### Memory Management
- Lazy loading of heavy components
- Efficient JSON processing with streaming
- Connection pooling for database
- Batch processing for trace exports

### CPU Optimization
- Compile-time code generation (MapStruct)
- Efficient query building with JPA Criteria API
- Optimized instrumentation with minimal overhead
- Conditional bean creation

### I/O Optimization
- Asynchronous trace export
- Database connection pooling
- HTTP client connection reuse
- Efficient resource loading

## Extensibility

### Custom Components

**Search Fields:**
```java
public class CustomSearchField implements SearchField<MyEntity> {
    @Override
    public Specification<MyEntity> toSpecification() {
        // Custom search logic
    }
}
```

**Error Response Enrichers:**
```java
@Component
public class TraceIdEnricher implements ErrorResponseEnricher {
    @Override
    public void doEnrich(ErrorResponseResource resource, ...) {
        // Add custom fields to error responses
    }
}
```

**Custom Validators:**
```java
@Component
public class EmailValidator implements Validator<String> {
    @Override
    public void validate(String email, ValidationResult result) {
        // Reusable validation logic
    }
}
```

## See Also

- [starter-core](./starter-core.md)
- [starter-jpa](./starter-jpa.md)
- [starter-otlp](./starter-otlp.md)