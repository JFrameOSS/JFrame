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
| `jframe.logging.response-length` | `-1` | Max response body chars to log (-1 = unlimited) |
| `jframe.logging.exclude-paths` | `/actuator/*` | Path patterns to skip |
| `jframe.logging.fields-to-mask` | `password, keyPassphrase, client_secret, secret` | Sensitive JSON fields to mask |
| `jframe.logging.allowed-content-types` | `application/json, application/xml, text/plain, ...` | Content types eligible for body logging |
| `jframe.logging.body-excluded-content-types` | `multipart/form-data` | Content types excluded from body logging |

### Filter toggles

All filters support `enabled` and `order` properties. Defaults shown below.

#### Spring Boot

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.logging.filters.request-duration.enabled` | `true` | Enable request duration filter |
| `jframe.logging.filters.request-duration.order` | `-17500` | Filter order (earliest) |
| `jframe.logging.filters.tracing-id.enabled` | `true` | Enable tracing ID filter |
| `jframe.logging.filters.tracing-id.order` | `-1000` | Filter order |
| `jframe.logging.filters.request-response.enabled` | `true` | Enable request/response log filter |
| `jframe.logging.filters.request-response.order` | `-950` | Filter order |
| `jframe.logging.filters.transaction-id.enabled` | `true` | Enable transaction ID filter |
| `jframe.logging.filters.transaction-id.order` | `-500` | Filter order |
| `jframe.logging.filters.request-id.enabled` | `true` | Enable request ID filter |
| `jframe.logging.filters.request-id.order` | `-400` | Filter order |

#### Quarkus

Quarkus filters use JAX-RS `@Priority` (lower = earlier). All enabled by default.

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.logging.filters.transaction-id.enabled` | `true` | Priority 100 |
| `jframe.logging.filters.request-id.enabled` | `true` | Priority 200 |
| `jframe.logging.filters.request-duration.enabled` | `true` | Priority 300 |
| `jframe.logging.filters.request-response.enabled` | `true` | Priority 400 |
| `jframe.logging.filters.outbound-correlation.enabled` | `true` | Outbound priority 100 |
| `jframe.logging.filters.outbound-logging.enabled` | `true` | Outbound priority 300 |
| `jframe.logging.filters.tracing-response.enabled` | `true` | Priority 350 (requires `quarkus-otlp`) |

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
| `jframe.otlp.propagators` | `tracecontext,baggage` | W3C trace context propagators |

### OTEL SDK mapping

JFrame maps `jframe.otlp.*` to the native OTEL SDK properties for each framework.

**Spring** (`jframe-properties.yml` → `otel.*`):

```yaml
otel:
  sdk.disabled: ${jframe.otlp.disabled}
  service.name: ${jframe.application.name}-${jframe.application.environment}
  propagators: [b3, jaeger, tracecontext]
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
| `trace.id` | TracingResponseFilter | OpenTelemetry trace ID |
| `span.id` | TracingResponseFilter | OpenTelemetry span ID |
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
