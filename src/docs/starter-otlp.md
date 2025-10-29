# starter-otlp

OpenTelemetry integration providing distributed tracing, metrics collection, and observability for Spring Boot applications.

## Location

```
starter-otlp/src/main/java/io/github/jframe/
├── autoconfigure/          # Auto-configuration
│   ├── properties/         # OpenTelemetryProperties
│   └── OpenTelemetryAutoConfiguration.java
└── tracing/                # Tracing components
    ├── aspect/            # @Traced annotation support
    ├── config/            # TracingConfig
    └── enricher/          # Response enrichers
```

## Components

### Auto-Instrumentation

Automatic instrumentation for:
- HTTP requests and responses
- Database queries and transactions
- External service calls (RestTemplate, WebClient)
- Custom business logic (via `@Traced`)

### Distributed Tracing

**Key Classes:**
- `OpenTelemetryAutoConfiguration` - Main auto-configuration
- `TracingConfig` - Core tracing configuration
- `TracingAspect` - AOP-based method tracing
- `TracingResponseEnricher` - Adds trace info to error responses

**Custom Tracing:**
```java
@Service
public class UserService {
    @Traced(operationName = "user.lookup")
    public User findById(Long id) {
        // Automatically traced with span "user.lookup"
        return repository.findById(id);
    }
}
```

**Manual Spans:**
```java
@Service
public class OrderService {
    private final Tracer tracer;

    public Order createOrder(OrderRequest request) {
        Span span = tracer.spanBuilder("order.create")
            .setAttribute("order.amount", request.getAmount())
            .setAttribute("order.currency", request.getCurrency())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Order order = processOrder(request);
            span.setAttribute("order.id", order.getId());
            span.setStatus(StatusCode.OK);
            return order;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Metrics Collection

- Application performance metrics
- Custom business metrics
- Infrastructure monitoring integration
- JVM and runtime metrics

## Dependencies

| Dependency | Purpose |
|------------|---------|
| OpenTelemetry Instrumentation BOM | OTEL version management |
| Spring Boot Starter | Auto-configuration integration |
| Spring Boot Web/WebFlux | Web application instrumentation |
| Spring Boot AOP | Aspect-oriented tracing |
| Apache HttpClient 5 | HTTP client instrumentation |
| starter-core | Foundation (ApplicationProperties, logging) |

## Configuration

**Config:** `jframe.otlp.*`

```yaml
jframe:
  otlp:
    disabled: false
    url: "http://localhost:4318"
    timeout: "10s"
    service-name: "${jframe.application.name}"  # Uses ApplicationProperties

  logging:  # Also configured in this module
    disabled: false
```

### Complete Example

```yaml
# OpenTelemetry OTLP endpoint
management:
  otlp:
    tracing:
      endpoint: "http://jaeger-collector:14250"
    metrics:
      endpoint: "http://prometheus:9090/api/v1/otlp"

# JFrame tracing configuration
jframe:
  otlp:
    disabled: false
    url: "http://localhost:4318"
    timeout: "10s"

  # Uses ApplicationProperties for service identification
  application:
    name: "user-service"
    version: "1.0.0"
    environment: "production"

  # Logging configuration
  logging:
    disabled: false
    include-request-body: false
    include-response-body: false
```

### Resource Attributes

```yaml
otel:
  resource:
    attributes:
      service.name: "${jframe.application.name}"
      service.version: "${jframe.application.version}"
      service.namespace: "${jframe.application.group}"
      deployment.environment: "${jframe.application.environment}"
```

## Monitoring Backends

### Jaeger
```yaml
management:
  otlp:
    tracing:
      endpoint: "http://jaeger-collector:14250"
```

### Zipkin
```yaml
management:
  tracing:
    zipkin:
      endpoint: "http://zipkin:9411/api/v2/spans"
```

### Prometheus
```yaml
management:
  otlp:
    metrics:
      endpoint: "http://prometheus:9090/api/v1/otlp"
```

## Examples

### Automatic HTTP Tracing
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        // Automatically traced with HTTP instrumentation
        return ResponseEntity.ok(userService.findById(id));
    }
}
```

### Custom Span Attributes
```java
@Service
public class PaymentService {
    @Traced(operationName = "payment.process")
    public PaymentResult process(PaymentRequest request) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("payment.amount", request.getAmount());
        currentSpan.setAttribute("payment.method", request.getMethod());

        try {
            PaymentResult result = doProcess(request);
            currentSpan.setAttribute("payment.status", result.getStatus());
            return result;
        } catch (PaymentException e) {
            currentSpan.recordException(e);
            throw e;
        }
    }
}
```

### Async Operations
```java
@Service
public class NotificationService {
    @Async
    @Traced(operationName = "notification.send")
    public CompletableFuture<Void> sendAsync(String message) {
        // Trace context properly propagated to async thread
        return CompletableFuture.runAsync(() -> {
            sendNotification(message);
        });
    }
}
```

## Best Practices

### Span Naming
- Use consistent format: `service.operation`
- Include business context: `user.profile.update`
- Avoid high-cardinality values in names

### Attributes
- Use semantic conventions where available
- Prefer attributes over span name variations
- Include business-relevant context
- Monitor attribute cardinality

### Sampling
```yaml
jframe:
  tracing:
    sampling:
      default-rate: 1.0
      rules:
        - path-pattern: "/health/**"
          sample-rate: 0.0
        - path-pattern: "/api/critical/**"
          sample-rate: 1.0
        - path-pattern: "/api/**"
          sample-rate: 0.1
```

### Error Handling
```java
try {
    Order order = processOrder(request);
    Span.current().setStatus(StatusCode.OK);
    return order;
} catch (Exception e) {
    Span currentSpan = Span.current();
    currentSpan.recordException(e);
    currentSpan.setStatus(StatusCode.ERROR, e.getMessage());
    throw new OrderCreationException("Failed to create order", e);
}
```

## Integration

### With starter-core
- Uses `ApplicationProperties` for service naming in traces
- Inherits `LoggingProperties` configuration
- Trace correlation in exception responses

### With starter-jpa
- Automatic database query instrumentation
- Search operation tracing
- Query performance monitoring

## Troubleshooting

| Issue | Check |
|-------|-------|
| Traces not appearing | OTLP endpoint, network connectivity, sampling config |
| High memory usage | Batch export settings, sampling rates, trace context leaks |
| Missing async context | `@Traced` annotation, thread pool configuration |
| Performance impact | Batch sizes, export intervals, attribute collection |

## See Also

- [starter-core](./starter-core.md)
- [starter-jpa](./starter-jpa.md)