# jframe-spring-otlp

OpenTelemetry distributed tracing, method instrumentation, and HTTP client tracing for Spring Boot.

## Dependency versions

This module does not version any `io.opentelemetry:*` coordinate. Spring Boot's platform owns the
OpenTelemetry API, context, SDK and exporters; JFrame pins only the artifacts Spring Boot does not
manage — the instrumentation starter and the semantic conventions library — and pins them to the
release train targeting the API version Spring Boot pins.

Do not import `opentelemetry-instrumentation-bom` in your build. It manages the same coordinates as
Spring Boot's platform, and the resulting conflict fails at runtime with
`NoClassDefFoundError` rather than at build time. See [Compatibility](../getting-started.md#compatibility).

Non-W3C propagation formats (`b3`, `jaeger`, `ottrace`) need an explicit, unversioned
`io.opentelemetry:opentelemetry-extension-trace-propagators` dependency. W3C `traceparent` and
`baggage` are built in.

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

JFrame maps `jframe.otlp.*` to OpenTelemetry SDK properties (`otel.*`) automatically. The bundled `jframe-properties.yml` configures W3C TraceContext propagation, exporter settings, and instrumentation flags.

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
  "errorCode": "INTERNAL_SERVER_ERROR",
  "errorReason": "Internal server error",
  "traceId": "abc123...",
  "spanId": "def456..."
}
```

Consumers of this API can also read the W3C `traceparent` header in response headers, which contains the same trace ID in standard format.

Error spans also record the exception via OpenTelemetry's `recordException`, along with HTTP attributes (uri, method, status, content-type).

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
