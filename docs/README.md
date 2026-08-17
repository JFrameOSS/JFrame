# JFrame Documentation

JFrame is a Java library for Spring Boot and Quarkus applications. It provides HTTP request/response logging, exception handling, ECS-structured logging, a JPA search framework, and OpenTelemetry tracing — with feature parity across both frameworks.

## Start here

| Document | What it covers |
|----------|----------------|
| [Getting Started](./getting-started.md) | Installation, required properties, feature matrix |
| [Configuration Reference](./configuration.md) | Every `jframe.*` property, filter toggles, OTEL mapping, MDC fields |
| [Upgrading to 1.5.0](./migration/1.5.0-upgrading.md) | Consolidated upgrade checklist for the current release |

## Modules

Framework-agnostic:

| Document | Artifact |
|----------|----------|
| [Core API](./modules/core.md) | `jframe-core` — exceptions, validation, search framework, ECS logging |

Spring Boot:

| Document | Artifact |
|----------|----------|
| [Spring Core](./modules/spring-core.md) | `jframe-spring-core` — filters, exception handling, logging, caching |
| [Spring JPA](./modules/spring-jpa.md) | `jframe-spring-jpa` — search specifications, pagination, SQL logging |
| [Spring OTLP](./modules/spring-otlp.md) | `jframe-spring-otlp` — tracing, auto-instrumentation, HTTP client |

Quarkus:

| Document | Artifact |
|----------|----------|
| [Quarkus Core](./modules/quarkus-core.md) | `jframe-quarkus-core` — exception mappers, JAX-RS filters, outbound correlation |
| [Quarkus JPA](./modules/quarkus-jpa.md) | `jframe-quarkus-jpa` — Panache search, repository, page mapping |
| [Quarkus OTLP](./modules/quarkus-otlp.md) | `jframe-quarkus-otlp` — CDI tracing, build-time `@Traced`, auto-instrumentation |

## Migration

See the [migration index](./migration/README.md) for every guide, newest first.

## Conventions

- File names are lowercase kebab-case.
- Module documents are named after their artifact with the `jframe-` prefix dropped, so every file name is unique across the tree.
- Migration guides are prefixed with the release they apply to (`1.5.0-upgrading.md`), so they sort chronologically and the version is visible without opening the file.
