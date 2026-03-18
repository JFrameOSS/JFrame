# jframe-quarkus-otlp

OpenTelemetry distributed tracing with CDI interceptors, W3C trace propagation, and outbound HTTP tracing for Quarkus applications.

## Setup

```properties
# application.properties
jframe.otlp.disabled=false
jframe.otlp.url=http://localhost:4318
jframe.otlp.exporter=otlp
jframe.otlp.sampling-rate=1.0
jframe.otlp.timeout=10s
jframe.otlp.excluded-methods=health,actuator,ping,status
```

Requires `quarkus-opentelemetry` extension on your classpath.

## Method tracing with `@Traced`

CDI interceptor that creates an OTEL span around each intercepted method:

```java
@ApplicationScoped
public class OrderService {
    @Traced
    public Order createOrder(CreateOrderRequest req) {
        // Span "OrderService.createOrder" created automatically
    }
}
```

Apply at class level to trace all public methods:

```java
@ApplicationScoped
@Traced
public class PaymentService {
    public void charge(Payment p) { /* traced */ }
    public void refund(Payment p) { /* traced */ }
}
```

## Execution timing with `@LogExecutionTime`

CDI interceptor that logs method duration at `DEBUG` level without creating OTEL spans:

```java
@ApplicationScoped
public class ReportService {
    @LogExecutionTime
    public Report generate() {
        // Logs: Method 'ReportService.generate' executed in 1234 ms
    }
}
```

Combine both annotations:

```java
@Traced
@LogExecutionTime
public Result compute() {
    // Creates span AND logs duration
}
```

## Outbound HTTP tracing

`OutboundTracingFilter` creates CLIENT spans and injects W3C `traceparent` headers for outbound JAX-RS client calls:

```java
@RegisterRestClient
@RegisterProvider(OutboundTracingFilter.class)
public interface PaymentClient {
    @POST @Path("/charges")
    ChargeResult charge(ChargeRequest req);
}
```

Span attributes: `peer.service`, `ext.response.status_code`. Spans marked as ERROR for status >= 400.

### W3C Trace Context format

```
traceparent: 00-{traceId}-{spanId}-01
```

Quarkus OTEL extension auto-extracts inbound `traceparent` headers.

## Trace ID in error responses

`TracingEnricher` adds `traceId` and `spanId` to error response JSON and enriches the OTEL span with error details:

```json
{
  "statusCode": 500,
  "errorMessage": "Internal error",
  "traceId": "abc123...",
  "spanId": "def456..."
}
```

Error span attributes: `error=true`, `error.type`, `error.message`, `http.status_code`, `http.method`, `http.uri`.

## Response headers

`TracingResponseFilter` adds trace context to HTTP response headers and populates SLF4J MDC:

```
X-Trace-Id: abc123...
X-Span-Id: def456...
```

MDC fields: `TRACE_ID`, `SPAN_ID` — available for log correlation in your logging framework configuration.

## Authentication utility

```java
String user = QuarkusAuthenticationUtil.getPrincipal(securityIdentity);
// Returns username or null (null-safe)
```
