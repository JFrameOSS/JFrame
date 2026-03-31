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

### Filter toggles (Spring only)

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.logging.filters.tracing-id.enabled` | `true` | Enable tracing ID filter |
| `jframe.logging.filters.tracing-id.order` | `-1000` | Filter order |
| `jframe.logging.filters.request-id.enabled` | `true` | Enable request ID filter |
| `jframe.logging.filters.request-id.order` | `-900` | Filter order |
| `jframe.logging.filters.transaction-id.enabled` | `true` | Enable transaction ID filter |
| `jframe.logging.filters.transaction-id.order` | `-800` | Filter order |
| `jframe.logging.filters.request-response.enabled` | `true` | Enable request/response log filter |
| `jframe.logging.filters.request-response.order` | `-700` | Filter order |
| `jframe.logging.filters.request-duration.enabled` | `true` | Enable request duration filter |
| `jframe.logging.filters.request-duration.order` | `-600` | Filter order |

## OpenTelemetry properties

Applies to both `spring-otlp` and `quarkus-otlp` modules.

| Property | Default | Description |
|----------|---------|-------------|
| `jframe.otlp.disabled` | `true` | Disable tracing (Spring default: `true`) |
| `jframe.otlp.url` | `http://localhost:4318` | OTLP collector endpoint |
| `jframe.otlp.exporter` | `otlp` | Exporter: `otlp`, `jaeger`, `zipkin` |
| `jframe.otlp.sampling-rate` | `1.0` | Sampling rate (0.0–1.0) |
| `jframe.otlp.timeout` | `10s` | Export timeout |
| `jframe.otlp.excluded-methods` | `health, actuator, ping, status, info, metrics` | Path segments to exclude from tracing |

### OTEL SDK mapping (Spring only)

The Spring `jframe-properties.yml` maps `jframe.otlp.*` to OpenTelemetry SDK properties:

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

## MDC field reference

Fields written to SLF4J MDC by JFrame filters and interceptors.

| MDC Key | Written By | Description |
|---------|-----------|-------------|
| `request.id` | RequestIdFilter / ScheduledAspect | Unique request identifier (UUID) |
| `transaction.id` | TransactionIdFilter / ScheduledAspect | Transaction identifier (UUID) |
| `trace.id` | TracingResponseFilter | OpenTelemetry trace ID |
| `span.id` | TracingResponseFilter | OpenTelemetry span ID |

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
