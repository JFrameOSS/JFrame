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

MDC fields: `trace.id`, `span.id` — available for log correlation in your logging framework configuration.

## Auto-instrumentation defaults

JFrame configures Quarkus OpenTelemetry auto-instrumentation via `microprofile-config.properties`:

| Instrumentation | Enabled | Property |
|----------------|---------|----------|
| JDBC | ✅ | `quarkus.datasource.jdbc.telemetry=true` |
| RESTEasy (server) | ✅ | `quarkus.otel.instrument.resteasy=true` |
| RESTEasy (client) | ✅ | `quarkus.otel.instrument.resteasy-client=true` |
| gRPC | ✅ | `quarkus.otel.instrument.grpc=true` |
| Messaging (Kafka/RabbitMQ) | ✅ | `quarkus.otel.instrument.messaging=true` |
| Vert.x HTTP | ✅ | `quarkus.otel.instrument.rest=true` |

Override any in your `application.properties`:

```properties
quarkus.otel.instrument.grpc=false
```

## Build-time auto-tracing

The `jframe-quarkus-otlp-deployment` module includes a Quarkus build-time processor (`OtlpProcessor`) that auto-adds `@Traced` to all public methods of `@ApplicationScoped` beans via Jandex bytecode transformation. Your services get tracing without manual annotation.

**Excluded from auto-tracing:** Framework packages (`io.quarkus.*`, `io.smallrye.*`, `jakarta.*`, `io.github.jframe.*`, etc.), static methods, constructors, and methods already annotated.

## Authentication utility

`QuarkusAuthenticationUtil` resolves the authenticated subject from Quarkus `SecurityIdentity`:

```java
@Inject QuarkusAuthenticationUtil authUtil;

String user = authUtil.getAuthenticatedSubject();
// Returns: principal name, "ANONYMOUS", or "INCOMPLETE"
```

Safe when Quarkus Security is not configured — no exceptions thrown.
