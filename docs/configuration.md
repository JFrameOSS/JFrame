# Configuration Reference

Complete configuration property reference for all JFrame modules.

## Application properties

Required for all modules. Spring: `application.yml`. Quarkus: `application.properties`.

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.application.name` | *required* | Service name (used in tracing, logs) |
| `jframe.application.group` | *required* | Organization/namespace |
| `jframe.application.version` | *required* | Application version |
| `jframe.application.environment` | `dev` | Runtime environment |
| `jframe.application.url` | `http://localhost:8080` | Base URL (Spring only) |

## Logging properties

HTTP logging configuration. Applies to both Spring and Quarkus core modules.

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.logging.disabled` | `false` | Disable all HTTP logging |
| `jframe.logging.response-length` | `-1` | Max response body chars to log (-1 = unlimited). Cap is applied before the body is decoded and masked; it bounds only the copy passed to the logger. The application and downstream client always receive the full body. |
| `jframe.logging.exclude-paths` | `/actuator/*` | Path patterns to skip |
| `jframe.logging.fields-to-mask` | `password, keyPassphrase, client_secret, secret` | Sensitive JSON fields to mask |
| `jframe.logging.allowed-content-types` | `application/json, application/xml, text/plain, ...` | Content types eligible for body logging |
| `jframe.logging.body-excluded-content-types` | `multipart/form-data` | Content types excluded from body logging |

### Filter toggles

All filters support `enabled` and `order` properties. Defaults shown below.

#### Spring Boot

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.logging.filters.request-duration.enabled` | `true` | Measure request duration |
| `jframe.logging.filters.request-duration.order` | `-17500` | Filter order (earliest) |
| `jframe.logging.filters.request-response.enabled` | `true` | Log request/response bodies |
| `jframe.logging.filters.request-response.order` | `-950` | Filter order |
| `jframe.logging.filters.transaction-id.enabled` | `false` | Read/generate transaction ID (opt-in) |
| `jframe.logging.filters.transaction-id.order` | `-500` | Filter order |
| `jframe.logging.filters.request-id.enabled` | `false` | Enable request ID filter (opt-in) |
| `jframe.logging.filters.request-id.order` | `-400` | Filter order |
| `jframe.logging.filters.user-identity.enabled` | `true` | Capture authenticated user |
| `jframe.logging.filters.user-identity.order` | `-100` | Filter order |

#### Quarkus

Quarkus filters use JAX-RS `@Priority` (lower = earlier). Three enabled by default, rest opt-in.

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.logging.filters.transaction-id.enabled` | `false` | Read/generate transaction ID (Priority 100, opt-in) |
| `jframe.logging.filters.request-id.enabled` | `false` | Generate unique request ID (Priority 200, opt-in) |
| `jframe.logging.filters.request-duration.enabled` | `true` | Measure request duration (Priority 300) |
| `jframe.logging.filters.request-response.enabled` | `true` | Log request/response bodies (Priority 400) |
| `jframe.logging.filters.outbound-correlation.enabled` | `false` | Propagate correlation IDs (Outbound Priority 100, opt-in) |
| `jframe.logging.filters.outbound-logging.enabled` | `false` | Log outbound calls (Outbound Priority 300, opt-in) |
| `jframe.logging.filters.user-identity.enabled` | `true` | Capture authenticated user (Priority 50) |

## OpenTelemetry properties

Applies to both `spring-otlp` and `quarkus-otlp` modules.

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.otlp.disabled` | `true` (Spring) / `false` (Quarkus) | Disable tracing |
| `jframe.otlp.url` | `http://localhost:4318` | OTLP collector endpoint |
| `jframe.otlp.exporter` | `otlp` | Exporter: `otlp`, `jaeger`, `zipkin` |
| `jframe.otlp.sampling-rate` | `1.0` | Sampling rate (0.0–1.0) |
| `jframe.otlp.timeout` | `10s` | Export timeout |
| `jframe.otlp.excluded-methods` | `health, actuator, ping, status, info, metrics` | Method names to exclude from tracing |
| `jframe.otlp.propagators` | `tracecontext,baggage` | W3C trace context propagators (Quarkus only; Spring config removed) |

### OTEL SDK mapping

JFrame maps `jframe.otlp.*` to the native OTEL SDK properties for each framework.

**Spring** (`jframe-properties.yml` → `otel.*`):

```yaml
otel:
  sdk.disabled: ${jframe.otlp.disabled}
  service.name: ${jframe.application.name}-${jframe.application.environment}
  propagators: [tracecontext, baggage]
  exporter.otlp:
    endpoint: ${jframe.otlp.url}
    timeout: ${jframe.otlp.timeout}
    protocol: http/protobuf
    compression: gzip
  traces.exporter: ${jframe.otlp.exporter}
```

**Quarkus** (`microprofile-config.properties` → `quarkus.otel.*`):

```properties
quarkus.otel.enabled=true
quarkus.otel.service.name=${jframe.application.name}-${jframe.application.environment}
quarkus.otel.propagators=${jframe.otlp.propagators:tracecontext,baggage}
quarkus.otel.exporter.otlp.endpoint=${jframe.otlp.url}
quarkus.otel.exporter.otlp.protocol=http/protobuf
quarkus.otel.exporter.otlp.compression=gzip
quarkus.otel.traces.sampler=traceidratio
quarkus.otel.traces.sampler.arg=${jframe.otlp.sampling-rate}
```

## MDC field reference

Fields written to SLF4J MDC by JFrame filters and interceptors. Both Spring and Quarkus use the same ECS field names.

| MDC Key | Written By | Description |
|---------|-----------|-------------|
| `request.id` | RequestIdFilter / ScheduledAspect | Unique request identifier (UUID) |
| `transaction.id` | TransactionIdFilter / ScheduledAspect | Business transaction identifier (UUID) |
| `transaction.duration` | RequestDurationFilter | Request processing time in ms |
| `event.duration` | RequestDurationFilter | Same as `transaction.duration` |
| `trace.id` | Span enrichment (via OTEL instrumentation) | OpenTelemetry trace ID |
| `span.id` | Span enrichment (via OTEL instrumentation) | OpenTelemetry span ID |
| `log.type` | Various filters | Log entry type (`request_body`, `response_body`, `call_request_body`, `call_response_body`, `end`) |
| `http.request.method` | RequestResponseLogFilter | HTTP method (GET, POST, etc.) |
| `url.path` | RequestResponseLogFilter | Request URI path |
| `http.response.status_code` | RequestResponseLogFilter | HTTP response status |
| `event.outcome` | RequestResponseLogFilter | `SUCCESS` or `FAILURE` |

### Logback pattern example

```xml
<pattern>%d{ISO8601} [%thread] %-5level %logger{36} [req=%X{request.id} tx=%X{transaction.id} trace=%X{trace.id}] - %msg%n</pattern>
```

## Spring Boot minimal example

```yaml
jframe:
  application:
    name: order-service
    group: com.example
    version: 1.0.0
  logging:
    fields-to-mask:
      - password
      - creditCard
  otlp:
    disabled: false
    url: http://jaeger:4318
```

## Quarkus minimal example

```properties
jframe.application.name=order-service
jframe.application.group=com.example
jframe.application.version=1.0.0
jframe.logging.fields-to-mask=password,creditCard
jframe.otlp.disabled=false
jframe.otlp.url=http://jaeger:4318
```
