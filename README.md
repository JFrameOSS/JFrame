<div align="center">

# JFrame

**A modern Java framework for building enterprise-grade Spring Boot and Quarkus applications**

[![GitHub stars](https://img.shields.io/github/stars/JFrameOSS/JFrame?style=social)](https://github.com/JFrameOSS/JFrame/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.3-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Quarkus](https://img.shields.io/badge/quarkus-3.x-blue.svg?logo=quarkus&logoColor=white)](https://quarkus.io/)
[![Java](https://img.shields.io/badge/java-21--temurin-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.java.net/projects/jdk/21/)
[![Maven Central](https://img.shields.io/badge/maven--central--0.10.0-SNAPSHOT-blue.svg)](https://search.maven.org/search?q=g:io.github.jframeoss)

[Features](#-features) •
[Quick Start](#-quick-start) •
[Documentation](#-documentation) •
[Modules](#-modules) •
[Contributing](#-contributing) •
[License](#-license)

</div>

---

## 📋 Overview

JFrame is a comprehensive Java framework providing enterprise-grade utilities, configurations, and best practices for building robust, scalable, and maintainable Spring Boot and Quarkus applications. It offers a modular architecture with specialized adapters for each framework, built on a shared framework-agnostic core that accelerates application delivery across both runtimes.

## ✨ Features

- **Core Utilities**: JSON processing, object mapping with MapStruct, and resource loading utilities
- **JPA Enhancements**: Advanced search capabilities, pagination, and database query logging
- **OpenTelemetry Integration**: Distributed tracing, metrics collection, and observability out-of-the-box
- **Type-Safe Configuration**: Strongly-typed configuration properties with IDE autocomplete support
- **Quarkus Support**: Full Quarkus adapter modules with JAX-RS, Panache, and CDI interceptor integration
- **Zero Dependencies**: Minimal external dependencies, reducing potential conflicts
- **Java 21+ Ready**: Built with modern Java features and best practices

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher (Temurin recommended)
- Spring Boot 3.5.3+ **or** Quarkus 3.x
- Gradle 9.x

### Installation

#### Spring Boot — Gradle (Kotlin DSL)

```kotlin
dependencies {
    // Core utilities and shared configurations
    implementation("io.github.jframeoss:jframe-spring-core:0.10.0-SNAPSHOT")

    // JPA enhancements (optional)
    implementation("io.github.jframeoss:jframe-spring-jpa:0.10.0-SNAPSHOT")

    // OpenTelemetry integration (optional)
    implementation("io.github.jframeoss:jframe-spring-otlp:0.10.0-SNAPSHOT")
}
```

#### Spring Boot — Maven

```xml
<dependencies>
    <!-- Core utilities and shared configurations -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-spring-core</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>

    <!-- JPA enhancements (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-spring-jpa</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>

    <!-- OpenTelemetry integration (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-spring-otlp</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

#### Quarkus — Gradle (Kotlin DSL)

```kotlin
dependencies {
    // Exception mappers and request logging
    implementation("io.github.jframeoss:jframe-quarkus-core:0.10.0-SNAPSHOT")

    // Panache search integration (optional)
    implementation("io.github.jframeoss:jframe-quarkus-jpa:0.10.0-SNAPSHOT")

    // OpenTelemetry tracing (optional)
    implementation("io.github.jframeoss:jframe-quarkus-otlp:0.10.0-SNAPSHOT")
}
```

#### Quarkus — Maven

```xml
<dependencies>
    <!-- Exception mappers and request logging -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-quarkus-core</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>

    <!-- Panache search integration (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-quarkus-jpa</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>

    <!-- OpenTelemetry tracing (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-quarkus-otlp</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Basic Configuration

Add to your `application.yml`:

```yaml
jframe:
  application:
    name: my-awesome-app
    group: com.example
    version: 1.0.0
    environment: dev
```

## 📚 Documentation

### Framework Modules

JFrame is organized into focused, reusable modules:

- **[Architecture Overview](./src/docs/architecture.md)** - Framework design and patterns

#### Spring Boot Modules

| Module | Description | Documentation |
|--------|-------------|---------------|
| **jframe-spring-core** | Core utilities, JSON processing, and shared application properties | [📖 Documentation](./src/docs/jframe-spring-core.md) |
| **jframe-spring-jpa** | JPA enhancements including advanced search, pagination, and query logging | [📖 Documentation](./src/docs/jframe-spring-jpa.md) |
| **jframe-spring-otlp** | OpenTelemetry integration for distributed tracing and observability | [📖 Documentation](./src/docs/jframe-spring-otlp.md) |

#### Quarkus Modules

| Module | Description | Documentation |
|--------|-------------|---------------|
| **jframe-quarkus-core** | JAX-RS exception mappers and request logging filters | [📖 Quarkus Guide](./docs/quarkus-guide.md) |
| **jframe-quarkus-jpa** | Panache search integration and page mapping | [📖 Quarkus Guide](./docs/quarkus-guide.md) |
| **jframe-quarkus-otlp** | OpenTelemetry tracing with CDI interceptors | [📖 Quarkus Guide](./docs/quarkus-guide.md) |

#### Shared Core

| Module | Description |
|--------|-------------|
| **jframe-core** | Framework-agnostic exceptions, validation, HTTP status, and search specs (transitive dependency) |

### Guides

- **[Migration Guide](./docs/migration-guide.md)** — Migrating from `jframe-starter-*` to `jframe-spring-*`
- **[Quarkus Adoption Guide](./docs/quarkus-guide.md)** — Getting started with JFrame on Quarkus

### CI/CD & Automation

- **[GitHub Actions Workflows](./src/docs/github-actions/)** - CI/CD pipelines and automation
  - [CI Pipeline](./src/docs/github-actions/ci.md) - Build, test, and publish to Maven Central
  - [Gradle Wrapper Auto-Update](./src/docs/github-actions/gradle-wrapper-update.md) - Automated dependency management


## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/JFrameOSS/JFrame.git
cd JFrame

# Build all modules
./gradlew clean build

# Run code quality checks
./gradlew spotlessApply checkQualityMain

# Publish to local Maven repository
./gradlew publishLocal
```

## 🏗️ Project Structure

```
jframe/
├── jframe-core/           # Framework-agnostic exceptions, validation, search specs
├── jframe-spring-core/    # Spring Boot core utilities and shared properties
├── jframe-spring-jpa/     # Spring Boot JPA enhancements and search framework
├── jframe-spring-otlp/    # Spring Boot OpenTelemetry integration
├── jframe-quarkus-core/   # Quarkus JAX-RS exception mappers and logging filters
├── jframe-quarkus-jpa/    # Quarkus Panache search integration
├── jframe-quarkus-otlp/   # Quarkus OpenTelemetry CDI interceptors
├── jframe-tests-contract/ # Shared contract tests across frameworks
├── jframe-tests-spring/   # Spring Boot integration tests
├── jframe-tests-quarkus/  # Quarkus integration tests
├── docs/                  # Guides (migration, Quarkus adoption)
├── src/
│   ├── docs/             # Module documentation
│   ├── quality/          # Code quality configurations
│   └── dist/             # Distribution files
└── gradle/               # Gradle wrapper and configurations
```

## 🤝 Contributing

Any contributions are welcome! Contact me via GitHub issues or pull requests.

### Development Guidelines

- **Java Version**: Java 21+ (Temurin distribution recommended)
- **Code Style**: Follow the Spotless configuration in `src/quality/config/`
- **Testing**: Write tests for new features (test framework TBD)
- **Documentation**: Update relevant documentation for API changes
- **Commits**: Use conventional commit messages

### Quick Contribution Steps

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes and ensure tests pass
4. Run code quality checks (`./gradlew spotlessApply checkQualityMain`)
5. Commit your changes (`git commit -m 'feat: add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 🔄 Release Management

This project follows [Git Flow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow):

- **master**: Production-ready releases
- **develop**: Active development branch
- **feature/***: New features
- **release/***: Release preparation
- **hotfix/***: Production hotfixes

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](src/dist/LICENSE) file for details.

---

<div align="center">

**[⭐ Star this repository](https://github.com/JFrameOSS/JFrame) if you find it useful!**

[![Stargazers over time](https://starchart.cc/JFrameOSS/JFrame.svg?variant=adaptive)](https://starchart.cc/JFrameOSS/JFrame)

</div>
