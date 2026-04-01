# jframe-spring-otlp

OpenTelemetry distributed tracing, method instrumentation, and HTTP client tracing for Spring Boot.

## Setup

```yaml
jframe:
  otlp:
    disabled: false                    # Enable tracing (default: true)
    url: http://localhost:4318         # OTLP collector endpoint
    exporter: otlp                     # otlp, jaeger, or zipkin
    sampling-rate: 1.0                 # 0.0 to 1.0
    timeout: 10s                       # Export timeout
    excluded-methods:
      - health
      - actuator
      - ping
      - status
```

JFrame maps `jframe.otlp.*` to OpenTelemetry SDK properties (`otel.*`) automatically. The bundled `jframe-properties.yml` configures propagators (B3, Jaeger, W3C TraceContext), exporter settings, and instrumentation flags.

### Auto-instrumentation defaults

JFrame enables these OpenTelemetry Java Starter instrumentations via `jframe-properties.yml`:

| Instrumentation | Property | Default |
|----------------|----------|---------|
| JDBC | `otel.instrumentation.jdbc.enabled` | `true` |
| Spring WebMVC | `otel.instrumentation.spring-webmvc.enabled` | `true` |
| Spring Web (RestTemplate) | `otel.instrumentation.spring-web.enabled` | `true` |
| Kafka | `otel.instrumentation.kafka.enabled` | `true` |
| MongoDB | `otel.instrumentation.mongo.enabled` | `true` |
| R2DBC | `otel.instrumentation.r2dbc.enabled` | `true` |
| Logback MDC | `otel.instrumentation.logback-mdc.enabled` | `true` |

Override any in `application.yml`:

```yaml
otel:
  instrumentation:
    kafka:
      enabled: false
```

## Automatic tracing

`TracingAspect` automatically creates spans for public methods in `@Service`, `@Controller`, `@RestController`, and `@Traced` classes.

```java
@Service
public class OrderService {
    public Order createOrder(CreateOrderRequest req) {
        // Span "OrderService.createOrder" created automatically
        // Attributes: service.name, http.transaction_id, http.request_id
    }
}
```

**Excluded methods:** getters, setters, `is*()`, `toString()`, `hashCode()`, `equals()`, and paths matching `excluded-methods` config.

### Trace custom classes

```java
@Traced
@Component
public class PaymentGateway {
    public void charge(Payment payment) {
        // Automatically traced
    }
}
```

## Execution timing

Log method duration without creating OTEL spans:

```java
@Service
public class ReportService {
    @LogExecutionTime
    public Report generate() {
        // Logs: [Execution Timer] Method 'generate' took 1234ms
    }
}
```

## HTTP client tracing

### RestTemplate

```java
@Bean
public RestTemplate restTemplate(HttpFilter httpFilter) {
    RestTemplate rt = new RestTemplate();
    rt.getInterceptors().add(
        httpFilter.getRequestInterceptor("payment-service")
    );
    return rt;
}
```

### WebClient

```java
@Bean
public WebClient webClient(HttpFilter httpFilter) {
    return WebClient.builder()
        .filter(httpFilter.getExchangeFilter("user-service"))
        .build();
}
```

Both create CLIENT spans with attributes: `peer.service`, request/response details, error status. Transaction and request IDs are propagated via headers.

## Trace ID in error responses

When tracing is enabled, `TracingResponseEnricher` adds `traceId` and `spanId` to error response JSON:

```json
{
  "statusCode": 500,
  "errorMessage": "Internal error",
  "traceId": "abc123...",
  "spanId": "def456..."
}
```

Error spans are also enriched with `error=true`, `error.type`, and `error.message` attributes.

## Response headers

`TracingResponseFilter` adds trace context to HTTP response headers and populates SLF4J MDC for log correlation:

```
X-Trace-Id: abc123...
X-Span-Id: def456...
```

## SSL/TLS client factory

Create SSL-enabled RestTemplate request factories:

```java
@Autowired
private HttpClientSSLFactory sslFactory;

// Secure (with truststore)
HttpComponentsClientHttpRequestFactory factory =
    sslFactory.createRequestFactory(true, truststorePath, password, 10, 30);

// Trust-all (development only — NOT for production!)
HttpComponentsClientHttpRequestFactory factory =
    sslFactory.createRequestFactory(false, null, null, 10, 30);
```

Parameters: `useSecureConnection`, `trustStorePath`, `trustStorePassword`, `connectTimeoutSeconds`, `readTimeoutSeconds`.
