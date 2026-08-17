<div align="center">

# JFrame

**Enterprise-grade utilities for Spring Boot and Quarkus applications**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Spring Boot](https://img.shields.io/badge/spring--boot-4.1.0-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Quarkus](https://img.shields.io/badge/quarkus-3.38.2-blue.svg?logo=quarkus&logoColor=white)](https://quarkus.io/)
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
    implementation("io.github.jframeoss:jframe-spring-core:1.5.0")
    implementation("io.github.jframeoss:jframe-spring-jpa:1.5.0")   // optional
    implementation("io.github.jframeoss:jframe-spring-otlp:1.5.0")  // optional
}
```

#### Quarkus — Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.jframeoss:jframe-quarkus-core:1.5.0")
    implementation("io.github.jframeoss:jframe-quarkus-jpa:1.5.0")   // optional
    implementation("io.github.jframeoss:jframe-quarkus-otlp:1.5.0")  // optional
}
```

<details>
<summary>Maven coordinates</summary>

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-spring-core</artifactId>
    <version>1.4.0</version>
</dependency>

<!-- Quarkus -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-quarkus-core</artifactId>
    <version>1.4.0</version>
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

All four properties are **required**. See the [Configuration Reference](./docs/configuration.md) for the full property list.

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](./docs/getting-started.md) | Installation, configuration, feature matrix |
| [Configuration Reference](./docs/configuration.md) | All `jframe.*` properties, filter toggles, OTEL mapping, MDC fields |
| [Core API](./docs/modules/core.md) | Exceptions, validation, search framework, ECS logging |

### Spring Boot

| Module | Documentation |
|--------|---------------|
| `jframe-spring-core` — Filters, exception handling, logging, caching | [📖 Docs](./docs/modules/spring-core.md) |
| `jframe-spring-jpa` — Search specifications, pagination, SQL logging | [📖 Docs](./docs/modules/spring-jpa.md) |
| `jframe-spring-otlp` — Tracing, auto-instrumentation, HTTP client | [📖 Docs](./docs/modules/spring-otlp.md) |

### Quarkus

| Module | Documentation |
|--------|---------------|
| `jframe-quarkus-core` — Exception mappers, JAX-RS filters, outbound correlation | [📖 Docs](./docs/modules/quarkus-core.md) |
| `jframe-quarkus-jpa` — Panache search, repository, page mapping | [📖 Docs](./docs/modules/quarkus-jpa.md) |
| `jframe-quarkus-otlp` — CDI tracing, build-time `@Traced`, auto-instrumentation | [📖 Docs](./docs/modules/quarkus-otlp.md) |

### Migration Guides

Newest first. See the [migration index](./docs/migration/README.md) for the full list.

| Version | Guide | What changed |
|---------|-------|--------------|
| 1.5.0 | [Upgrading to 1.5.0](./docs/migration/1.5.0-upgrading.md) | **Start here** — consolidated checklist for the whole release |
| 1.5.0 | [Filters Opt-In](./docs/migration/1.5.0-filters-opt-in.md) | Correlation ID filters are opt-in, W3C `traceparent` only |
| 1.5.0 | [Library Efficiency](./docs/migration/1.5.0-library-efficiency.md) | Dependency and toolchain update, body-size caps |
| 1.2.0 | [Exception Handling](./docs/migration/1.2.0-exception-handling.md) | Unified `HttpException` + `ApiError`, homogeneous `errorCode`/`errorReason` |
| 1.0.0 | [Spring Modules](./docs/migration/1.0.0-spring-modules.md) | `jframe-starter-*` → `jframe-spring-*` |
| 1.0.0 | [ECS Naming Convention](./docs/migration/1.0.0-ecs-naming-convention.md) | `KibanaLogField*` → `EcsField*` |

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

Locally, `spotlessApply` runs automatically before every compile, so you never have
to think about formatting. CI disables that with `-PdisableAutoFormat` and runs
`spotlessCheck` as a real gate — code that was never formatted locally will fail the
build rather than being silently reformatted on the runner.

### Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request to `master` and
`develop`, nightly, and on a pushed `X.Y.Z` tag.

| Gate | What it does |
| --- | --- |
| Formatting | `spotlessCheck -PdisableAutoFormat`, fails fast before the build |
| Build and test | `clean build cyclonedxBom`, which also runs PMD, Checkstyle, SpotBugs and CodeNarc |
| Deprecations | Fails on any Gradle deprecation warning that is not in `.github/gradle-deprecation-allowlist.txt`. The allowlist holds only warnings raised inside third-party plugins, each annotated with the plugin and version responsible. Anything new in our own build files breaks the build. |
| Version drift | On a tag, the tag must match `gradle.properties` **and** every `io.github.jframeoss:jframe-*` coordinate in `README.md` and `docs/getting-started.md`. A release cannot ship with stale examples. |

Supporting workflows: `dependency-updates.yml` reports outdated dependencies weekly,
`update-gradle-wrapper.yml` opens a wrapper-upgrade PR against `develop`, and
`.github/dependabot.yml` keeps the pinned GitHub Actions and Gradle dependencies
current. All actions are pinned to commit SHAs.

Each release publishes to Maven Central and attaches a CycloneDX SBOM to the GitHub
Release.

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
