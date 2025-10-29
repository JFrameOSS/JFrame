# JFrame Quick Start Guide

Quick start guide for using JFrame in Spring Boot applications.

## Installation

Add JFrame modules to your Spring Boot project:

```gradle
dependencies {
    // Core utilities and application properties
    implementation 'io.github.jframe:starter-core:1.0.0'

    // JPA utilities (includes starter-core)
    implementation 'io.github.jframe:starter-jpa:1.0.0'

    // OpenTelemetry integration (includes starter-core)
    implementation 'io.github.jframe:starter-otlp:1.0.0'
}
```

## Module Overview

### [starter-core](./starter-core.md)
Core utilities, exception handling, logging, and validation.

**Provides:**
- Application properties (`jframe.application.*`)
- Exception handling framework
- HTTP request/response logging
- Validation framework
- Utilities (JSON, MapStruct, converters)

### [starter-jpa](./starter-jpa.md)
JPA enhancements for database operations.

**Provides:**
- Dynamic search with JPA specifications
- Standardized pagination
- Database query monitoring

### [starter-otlp](./starter-otlp.md)
OpenTelemetry observability.

**Provides:**
- Distributed tracing
- Metrics collection
- Auto-instrumentation

## Configuration

### Minimal Setup

```yaml
jframe:
  application:
    name: "my-app"
    version: "1.0.0"
    environment: "production"

  logging:
    disabled: false
    fields-to-mask: [password, secret, token]

  otlp:
    disabled: false
    url: "http://localhost:4318"
```

See individual module documentation for complete configuration options.

## Build Commands

```bash
# Build all modules
./gradlew clean build

# Check code quality
./gradlew spotlessApply checkQualityMain

# Publish to local Maven
./gradlew publishLocal
```

## Project Structure

```
jframe/
├── starter-core/          # Core utilities
├── starter-jpa/           # JPA enhancements
├── starter-otlp/          # Observability
└── src/
    ├── docs/              # Documentation
    ├── dist/              # Distribution files
    └── quality/           # Quality configs
```

## Documentation

### Modules
- [starter-core](./starter-core.md) - Core utilities and shared components
- [starter-jpa](./starter-jpa.md) - JPA and database features
- [starter-otlp](./starter-otlp.md) - OpenTelemetry integration

### Features
- [Exception Handling](./core/exception-handling.md) - Hierarchical exceptions
- [Logging Framework](./core/logging.md) - HTTP logging and correlation
- [Validation Framework](./core/validation.md) - Field-level validation

### Architecture
- [Architecture Overview](./architecture.md) - Design and module interactions

## Changelog

See [CHANGELOG.md](../dist/CHANGELOG.md) for release history and migration notes.
