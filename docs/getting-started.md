# Getting Started with JFrame

JFrame is a Java 21 library providing enterprise infrastructure for **Spring Boot** and **Quarkus** applications. It handles HTTP logging, exception handling, JPA search/pagination, OpenTelemetry tracing, and structured logging out of the box.

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  jframe-core                     │
│  Exceptions · Validation · Search · Logging MDC  │
├────────────────────┬────────────────────────────┤
│   Spring Adapters  │     Quarkus Adapters       │
│  ┌──────────────┐  │  ┌──────────────────────┐  │
│  │ spring-core  │  │  │   quarkus-core       │  │
│  │ spring-jpa   │  │  │   quarkus-jpa        │  │
│  │ spring-otlp  │  │  │   quarkus-otlp       │  │
│  └──────────────┘  │  └──────────────────────┘  │
└────────────────────┴────────────────────────────┘
```

All modules share `jframe-core` (pulled in transitively). You never need to depend on it directly.

## Installation

### Spring Boot

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.jframeoss:jframe-spring-core:1.5.0")
    implementation("io.github.jframeoss:jframe-spring-jpa:1.5.0")   // optional
    implementation("io.github.jframeoss:jframe-spring-otlp:1.5.0")  // optional
}
```

### Quarkus

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.jframeoss:jframe-quarkus-core:1.5.0")
    implementation("io.github.jframeoss:jframe-quarkus-jpa:1.5.0")   // optional
    implementation("io.github.jframeoss:jframe-quarkus-otlp:1.5.0")  // optional
}
```

## Compatibility

JFrame builds on its host framework's dependency management rather than shipping a competing one.
Each JFrame minor targets one framework minor.

| JFrame | Spring Boot | Quarkus | Java |
| --- | --- | --- | --- |
| 1.5.x | 4.1.x | 3.38.x | 21+ |
| 1.4.x | 4.0.x | 3.34.x | 21+ |

Use the matching row. JFrame does not version any `io.opentelemetry:*` coordinate on Spring — those
come from Spring Boot's platform, and the OpenTelemetry instrumentation artifacts JFrame does pin
belong to the release train targeting the API version your Spring Boot version pins. Mixing rows
produces a version conflict that the build cannot resolve for you.

## Minimal configuration

### Spring Boot (`application.yml`)

```yaml
jframe:
  application:
    name: my-service
    group: com.example
    version: 1.0.0
```

### Quarkus (`application.properties`)

```properties
jframe.application.name=my-service
jframe.application.group=com.example
jframe.application.version=1.0.0
```

That's it. All HTTP filters, exception handlers, and logging are auto-configured with sensible defaults.

## What you get out of the box

| Feature | Spring module | Quarkus module |
|---------|--------------|----------------|
| Request/transaction ID tracking | `spring-core` | `quarkus-core` |
| HTTP request/response logging | `spring-core` | `quarkus-core` |
| Sensitive field masking | `spring-core` | `quarkus-core` |
| Global exception handling | `spring-core` | `quarkus-core` |
| Structured error responses | `spring-core` | `quarkus-core` |
| Request-scoped entity caching | `spring-core` | `quarkus-core` |
| Scheduled task context propagation | `spring-core` | `quarkus-core` |
| JPA search specifications | `spring-jpa` | `quarkus-jpa` |
| Paginated search with sorting | `spring-jpa` | `quarkus-jpa` |
| SQL query logging | `spring-jpa` | `quarkus-jpa` |
| OpenTelemetry span creation | `spring-otlp` | `quarkus-otlp` |
| Method execution timing | `spring-otlp` | `quarkus-otlp` |
| Outbound HTTP tracing | `spring-otlp` | `quarkus-otlp` |
| Trace ID in error responses | `spring-otlp` | `quarkus-otlp` |

## Module documentation

- **Spring:** [Core](modules/spring-core.md) · [JPA](modules/spring-jpa.md) · [OTLP](modules/spring-otlp.md)
- **Quarkus:** [Core](modules/quarkus-core.md) · [JPA](modules/quarkus-jpa.md) · [OTLP](modules/quarkus-otlp.md)
- **Shared:** [Core library](modules/core.md) · [Configuration reference](configuration.md)
- **Migration:** [Spring 0.9.x → 1.0.0](migration/1.0.0-spring-modules.md) · [ECS naming convention](migration/1.0.0-ecs-naming-convention.md)
