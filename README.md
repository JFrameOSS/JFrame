<div align="center">

# JFrame

**A modern Java framework for building enterprise-grade Spring Boot applications**

[![GitHub stars](https://img.shields.io/github/stars/JFrameOSS/JFrame?style=social)](https://github.com/JFrameOSS/JFrame/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.3-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/java-25--temurin-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.java.net/projects/jdk/25/)
[![Maven Central](https://img.shields.io/badge/maven--central--0.7.0-SNAPSHOT-blue.svg)](https://search.maven.org/search?q=g:io.github.jframeoss)

[Features](#-features) •
[Quick Start](#-quick-start) •
[Documentation](#-documentation) •
[Modules](#-modules) •
[Contributing](#-contributing) •
[License](#-license)

</div>

---

## 📋 Overview

JFrame is a comprehensive Java framework providing enterprise-grade utilities, configurations, and best practices for building robust, scalable, and maintainable Spring Boot applications. It offers a modular architecture with specialized starters that enhance your development experience and accelerate application delivery.

## ✨ Features

- **Core Utilities**: JSON processing, object mapping with MapStruct, and resource loading utilities
- **JPA Enhancements**: Advanced search capabilities, pagination, and database query logging
- **OpenTelemetry Integration**: Distributed tracing, metrics collection, and observability out-of-the-box
- **Type-Safe Configuration**: Strongly-typed configuration properties with IDE autocomplete support
- **Zero Dependencies**: Minimal external dependencies, reducing potential conflicts
- **Java 21+ Ready**: Built with modern Java features and best practices

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher (Temurin recommended)
- Spring Boot 3.5.3+
- Gradle 9.x

### Installation

#### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // Core utilities and shared configurations
    implementation("io.github.jframeoss:starter-core:0.7.0-SNAPSHOT")

    // JPA enhancements (optional)
    implementation("io.github.jframeoss:starter-jpa:0.7.0-SNAPSHOT")

    // OpenTelemetry integration (optional)
    implementation("io.github.jframeoss:starter-otlp:0.7.0-SNAPSHOT")
}
```

#### Maven

```xml
<dependencies>
    <!-- Core utilities and shared configurations -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>starter-core</artifactId>
        <version>0.7.0-SNAPSHOT</version>
    </dependency>

    <!-- JPA enhancements (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>starter-jpa</artifactId>
        <version>0.7.0-SNAPSHOT</version>
    </dependency>

    <!-- OpenTelemetry integration (optional) -->
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>starter-otlp</artifactId>
        <version>0.7.0-SNAPSHOT</version>
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

| Module | Description | Documentation |
|--------|-------------|---------------|
| **starter-core** | Core utilities, JSON processing, and shared application properties | [📖 Documentation](./src/docs/starter-core.md) |
| **starter-jpa** | JPA enhancements including advanced search, pagination, and query logging | [📖 Documentation](./src/docs/starter-jpa.md) |
| **starter-otlp** | OpenTelemetry integration for distributed tracing and observability | [📖 Documentation](./src/docs/starter-otlp.md) |

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
├── starter-core/          # Core utilities and shared properties
├── starter-jpa/           # JPA enhancements and search framework
├── starter-otlp/          # OpenTelemetry integration
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
