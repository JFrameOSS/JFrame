# jframe-spring-otlp — AI Context

> Spring Boot OpenTelemetry tracing module. AOP-based auto-instrumentation, span management, and OTLP exporter configuration.

## Auto-Instrumentation

`TracingAspect` — `@Aspect` auto-traces methods in `@Service`, `@Controller`, `@RestController`, and `@Traced` classes.

**Auto-excluded methods:** `get*()`, `set*()`, `is*()`, `toString()`, `hashCode()`, `equals(..)`.
**Configurable exclusions:** `jframe.otlp.excluded-methods` (default: health, actuator, ping, status, info, metrics).

### Span Attributes

| Attribute | Source |
|-----------|--------|
| `service.name` | Resolved class name (proxy suffixes stripped) |
| `service.method` | Method name |
| `http.remote_user` | Spring Security subject |
| `tx.id` | Transaction ID from MDC |
| `request.id` | Request ID from MDC |

### @Traced Annotation

`ElementType.TYPE` only (class-level). Classes annotated with `@Service`/`@Controller`/`@RestController` are auto-traced without needing `@Traced`.

## Configuration Properties

Prefix: `jframe.otlp` (OpenTelemetryProperties)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `disabled` | boolean | `false` | Disable tracing |
| `url` | String | `http://localhost:4318` | OTLP endpoint |
| `timeout` | String | `10s` | Export timeout (pattern: `\d+[smh]`) |
| `exporter` | String | `otlp` | Exporter type: otlp, jaeger, zipkin |
| `samplingRate` | double | `1.0` | Trace sampling rate (0.0–1.0) |
| `excludedMethods` | Set | health, actuator, ping, status, info, metrics | Methods to exclude |
| `propagators` | String | `tracecontext,baggage` | W3C propagators |

## Auto-Configuration

`OpenTelemetryAutoConfiguration` — registers `Tracer` bean when `jframe.otlp.disabled=false`.

**Default config in jframe-properties.yml:**
- Auto-instrumentation: JDBC, spring-web, spring-webmvc, kafka, mongo, r2dbc, logback-mdc

## Outbound HTTP Tracing

`HttpFilter` — provides tracing interceptors for outbound HTTP calls:

```java
@Bean
public RestTemplate restTemplate(HttpFilter httpFilter) {
    RestTemplate rt = new RestTemplate();
    rt.getInterceptors().add(httpFilter.getRequestInterceptor("order-service"));
    return rt;
}

@Bean
public WebClient webClient(HttpFilter httpFilter) {
    return WebClient.builder()
        .filter(httpFilter.getExchangeFilter("payment-service"))
        .build();
}
```

`SpanManager` — creates/enriches outbound spans with attributes: `peer.service`, HTTP method/URI, response status.

## Response Filter

`TracingResponseFilter` (order -1000) — adds `x-trace-id` and `x-span-id` to HTTP response headers + MDC. Toggle: `jframe.logging.filters.tracing-id.enabled`.

## Error Response Enrichment

`TracingResponseEnricher` — adds trace/span IDs to error responses, records exceptions on current span with HTTP attributes.

## Execution Timer

```java
@LogExecutionTime
public void slowMethod() { ... }
// Logs: [Execution Timer] Method 'slowMethod' took 123ms
```

## Security

`AuthenticationUtil` — static method `getAuthenticatedSubject()` returns user name from Spring Security, or `ANONYMOUS`/`INCOMPLETE`.

## SSL Factory

`HttpClientSSLFactory.createRequestFactory(useSecure, trustStorePath, trustStorePassword, connectTimeout, readTimeout)` — creates `HttpComponentsClientHttpRequestFactory` with optional TLS.
