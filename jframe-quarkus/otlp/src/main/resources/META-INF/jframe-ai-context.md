# jframe-quarkus-otlp — AI Context

> Quarkus OpenTelemetry tracing module. CDI interceptor-based auto-instrumentation with build-time annotation processing.

## @Traced Annotation

CDI interceptor binding. Target: `TYPE` + `METHOD`. Inherited.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `value()` | String | `""` | Custom span name (empty = `ClassName.methodName`) |
| `recordParameters()` | boolean | `false` | Record method params as span attributes |

```java
@Traced
@ApplicationScoped
public class OrderService {
    public Order processOrder(OrderRequest req) { ... } // auto-traced
}

@Traced("custom-span-name")
public void specialMethod() { ... }
```

## Build-Time Auto-Instrumentation

`OtlpProcessor` (deployment module) auto-adds `@Traced` to public, non-static methods of `@ApplicationScoped` and `@Route` beans at build time.

**Excluded packages:** `io.quarkus.*`, `io.smallrye.*`, `io.vertx.*`, `io.netty.*`, `io.opentelemetry.*`, `org.jboss.*`, `org.hibernate.*`, `org.eclipse.microprofile.*`, `jakarta.*`, `com.fasterxml.*`, `io.github.jframe.*`.

**Method selection:** public + non-static + not already annotated + not constructor/initializer.

## TracingInterceptor

`@Interceptor` at `Priority.LIBRARY_BEFORE`. Uses `GlobalOpenTelemetry.getTracer("jframe-otlp")`.

**Span attributes:** `service.name`, `service.method`, `http.remote_user`, `http.transaction_id`, `http.request_id`.

**Method exclusion:**
- Prefixes: `get`, `set`, `is`
- Names: `toString`, `hashCode`, `equals`, `clone`
- Config: `jframe.otlp.excluded-methods` (default: health, actuator, ping, status, info, metrics)

On exception: `span.recordException()`, `span.setStatus(ERROR)`, rethrow.

## Configuration

Prefix: `jframe.otlp` (OpenTelemetryConfig, @ApplicationScoped, lazy init with ReentrantLock)

| Property | Default | Description |
|----------|---------|-------------|
| `disabled` | `false` | Disable tracing |
| `url` | `http://localhost:4318` | OTLP endpoint |
| `timeout` | `10s` | Export timeout |
| `exporter` | `otlp` | Exporter type |
| `sampling-rate` | `1.0` | Trace sampling (0.0–1.0) |
| `excluded-methods` | `health,actuator,ping,status,info,metrics` | Methods to skip |
| `propagators` | `tracecontext,baggage` | W3C propagators |

### Quarkus OTEL Mapping (microprofile-config.properties)

jframe properties are mapped to Quarkus OTEL properties:
- `quarkus.otel.service.name` = `${jframe.application.name}-${jframe.application.environment}`
- `quarkus.otel.exporter.otlp.endpoint` = `${jframe.otlp.url}`
- `quarkus.otel.exporter.otlp.protocol` = `http/protobuf`
- `quarkus.otel.exporter.otlp.compression` = `gzip`
- `quarkus.otel.traces.sampler` = `traceidratio`
- `quarkus.otel.traces.sampler.arg` = `${jframe.otlp.sampling-rate}`

## Auto-Instrumented Libraries

| Library | Property | Default |
|---------|----------|---------|
| JDBC | `quarkus.datasource.jdbc.telemetry` | `true` |
| REST server | `quarkus.otel.instrument.resteasy` | `true` |
| REST client | `quarkus.otel.instrument.resteasy-client` | `true` |
| gRPC | `quarkus.otel.instrument.grpc` | `true` |
| Messaging | `quarkus.otel.instrument.messaging` | `true` |
| Vert.x HTTP | `quarkus.otel.instrument.rest` | `true` |

## Response Filter

`TracingResponseFilter` (@Priority 350) — adds `x-trace-id` and `x-span-id` to response headers + MDC. Toggle: `jframe.logging.filters.tracing-response.enabled`.

## Error Enrichment

`TracingEnricher` — adds trace/span IDs to error responses, records exception on span with HTTP attributes (uri, method, status, content-type).

## Security

`QuarkusAuthenticationUtil` — resolves authenticated subject from `SecurityIdentity`. Returns `ANONYMOUS` if not authenticated, `INCOMPLETE` if principal name blank. Safe when Quarkus Security not configured.
