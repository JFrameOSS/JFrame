<div align="center">

# JFrame

**Enterprise-grade utilities for Spring Boot and Quarkus applications**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Spring Boot](https://img.shields.io/badge/spring--boot-4.1.0--M1-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Quarkus](https://img.shields.io/badge/quarkus-3.20.3-blue.svg?logo=quarkus&logoColor=white)](https://quarkus.io/)
[![Java](https://img.shields.io/badge/java-21-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.java.net/projects/jdk/21/)

[Features](#-features) •
[Quick Start](#-quick-start) •
[Documentation](#-documentation) •
[Building](#-building-from-source) •
[License](#-license)

</div>

---

## Overview

JFrame provides structured exception handling, ECS-compliant logging, paginated search, and OpenTelemetry tracing for Spring Boot and Quarkus. A shared framework-agnostic core ensures consistent behaviour across both runtimes.

## ✨ Features

| Feature | Spring Boot | Quarkus |
|---------|:-----------:|:-------:|
| Structured exception handling with error enrichers | ✅ | ✅ |
| ECS-compliant MDC logging (request/transaction ID, duration) | ✅ | ✅ |
| Request/response body logging with content-type filtering | ✅ | ✅ |
| Paginated search with type-safe specifications | ✅ | ✅ |
| Fluent validation with `ValidatorBuilder` | ✅ | ✅ |
| OpenTelemetry auto-tracing (`@Service`, `@Traced`) | ✅ AOP | ✅ CDI interceptor |
| Build-time `@Traced` injection for `@ApplicationScoped` beans | — | ✅ |
| Auto-instrumentation (JDBC, HTTP, Kafka, gRPC) | ✅ | ✅ |
| Outbound HTTP correlation (RestTemplate / WebClient / JAX-RS client) | ✅ | ✅ |
| SQL query logging via datasource-proxy | ✅ | ✅ |
| OpenAPI error response schemas (400/429/500) | — | ✅ |
| Jackson 3.x configuration | — | ✅ |
| Jackson 2.x configuration | ✅ | — |

## 🚀 Quick Start

### Prerequisites

- Java 21+ (Temurin recommended)
- Spring Boot 4.1.0-M1+ **or** Quarkus 3.20.3+
- Gradle 9.x

### Installation

#### Spring Boot — Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.jframeoss:jframe-spring-core:1.2.0")
    implementation("io.github.jframeoss:jframe-spring-jpa:1.2.0")   // optional
    implementation("io.github.jframeoss:jframe-spring-otlp:1.2.0")  // optional
}
```

#### Quarkus — Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.jframeoss:jframe-quarkus-core:1.2.0")
    implementation("io.github.jframeoss:jframe-quarkus-jpa:1.2.0")   // optional
    implementation("io.github.jframeoss:jframe-quarkus-otlp:1.2.0")  // optional
}
```

<details>
<summary>Maven coordinates</summary>

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-spring-core</artifactId>
    <version>1.2.0</version>
</dependency>

<!-- Quarkus -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-quarkus-core</artifactId>
    <version>1.2.0</version>
</dependency>
```

Replace `core` with `jpa` or `otlp` as needed.
</details>

### Minimal Configuration

#### Spring Boot (`application.yml`)

```yaml
jframe:
  application:
    name: my-service
    group: com.example
    version: 1.0.0
    environment: dev
```

#### Quarkus (`application.properties`)

```properties
jframe.application.name=my-service
jframe.application.group=com.example
jframe.application.version=1.0.0
jframe.application.environment=dev
```

All four properties are **required**. See the [Configuration Reference](./src/docs/shared/configuration.md) for the full property list.

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](./src/docs/getting-started.md) | Installation, configuration, feature matrix |
| [Configuration Reference](./src/docs/shared/configuration.md) | All `jframe.*` properties, filter toggles, OTEL mapping, MDC fields |
| [Core API](./src/docs/shared/core.md) | Exceptions, validation, search framework, ECS logging |

### Spring Boot

| Module | Documentation |
|--------|---------------|
| `jframe-spring-core` — Filters, exception handling, logging, caching | [📖 Docs](./src/docs/spring/core.md) |
| `jframe-spring-jpa` — Search specifications, pagination, SQL logging | [📖 Docs](./src/docs/spring/jpa.md) |
| `jframe-spring-otlp` — Tracing, auto-instrumentation, HTTP client | [📖 Docs](./src/docs/spring/otlp.md) |

### Quarkus

| Module | Documentation |
|--------|---------------|
| `jframe-quarkus-core` — Exception mappers, JAX-RS filters, outbound correlation | [📖 Docs](./src/docs/quarkus/core.md) |
| `jframe-quarkus-jpa` — Panache search, repository, page mapping | [📖 Docs](./src/docs/quarkus/jpa.md) |
| `jframe-quarkus-otlp` — CDI tracing, build-time `@Traced`, auto-instrumentation | [📖 Docs](./src/docs/quarkus/otlp.md) |

### Migration Guides

- [Spring Boot 1.0.0 Migration](./src/docs/migration/spring-migration-1.0.0.md) — `jframe-starter-*` → `jframe-spring-*`
- [Exception Handling Simplification](./src/docs/migration/exception-handling-simplification.md) — Unified `HttpException` + `ApiError`, homogeneous `errorCode`/`errorReason` on all responses
- [ECS Naming Convention Migration](./src/docs/migration/ecs-naming-convention-migration.md) — `KibanaLogField*` → `EcsField*`

## 🏗️ Project Structure

```
jframe/
├── jframe-core/                  # Framework-agnostic: exceptions, validation, search, ECS logging, tracing utils
├── jframe-spring/
│   ├── core/                     # Auto-configuration, servlet filters, exception handler, logging
│   ├── jpa/                      # JPA search specifications, pagination, datasource-proxy
│   └── otlp/                     # AOP tracing, span management, HTTP client tracing
├── jframe-quarkus/
│   ├── core/                     # CDI producers, JAX-RS filters, exception mappers, OpenAPI
│   ├── jpa/                      # Panache search repository, page mapping, datasource-proxy
│   ├── otlp/                     # CDI tracing interceptor, auth utilities
│   └── otlp-deployment/          # Build-time @Traced annotation processor
└── src/
    ├── docs/                     # Documentation wiki
    ├── quality/                  # Spotless, SpotBugs, PMD, Checkstyle configs
    └── dist/                     # LICENSE, CHANGELOG
```

## 🛠️ Building from Source

```bash
git clone https://github.com/JFrameOSS/JFrame.git
cd JFrame

./gradlew clean build                          # Build + test all modules
./gradlew spotlessApply checkQualityMain       # Code style + quality checks
./gradlew publishToMavenLocal                  # Install to local Maven repo
```

## 🤝 Contributing

Contributions welcome — open an issue or pull request on GitHub.

- **Java 21+** with Temurin
- **Code style**: Spotless (`./gradlew spotlessApply`)
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/)

## 📄 License

Apache License 2.0 — see [LICENSE](src/dist/LICENSE).

---

<div align="center">

**[⭐ Star this repository](https://github.com/JFrameOSS/JFrame) if you find it useful!**

</div>
