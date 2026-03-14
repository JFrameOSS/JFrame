# Migration Guide: jframe-starter-* → jframe-spring-*

Guide for existing `jframe-starter-*` consumers migrating to the new `jframe-spring-*` modules.

## Overview

JFrame has undergone a structural refactor to support multiple frameworks. The original `jframe-starter-*` modules have been renamed to `jframe-spring-*` and framework-agnostic code has been extracted into a new shared `jframe-core` module. This enables JFrame to now support both **Spring Boot** and **Quarkus** applications.

**What changed:**
- `jframe-starter-*` → `jframe-spring-*` (artifact rename only)
- New `jframe-core` module: framework-agnostic exceptions, validation, HTTP status, and search specs
- New `jframe-quarkus-*` modules for Quarkus adopters

**What did NOT change:**
- Package structure (`io.github.jframe.*`) — no import changes needed
- Configuration properties (`jframe.*`) — same YAML keys
- Public API and class names

---

## Dependency Changes

Update your build file artifact IDs from `jframe-starter-*` to `jframe-spring-*`.

### Gradle (Kotlin DSL)

**Before:**
```kotlin
dependencies {
    implementation("io.github.jframeoss:jframe-starter-core:0.9.x")
    implementation("io.github.jframeoss:jframe-starter-jpa:0.9.x")
    implementation("io.github.jframeoss:jframe-starter-otlp:0.9.x")
}
```

**After:**
```kotlin
dependencies {
    implementation("io.github.jframeoss:jframe-spring-core:0.10.0-SNAPSHOT")
    implementation("io.github.jframeoss:jframe-spring-jpa:0.10.0-SNAPSHOT")
    implementation("io.github.jframeoss:jframe-spring-otlp:0.10.0-SNAPSHOT")
}
```

### Maven

**Before:**
```xml
<dependencies>
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-starter-core</artifactId>
        <version>0.9.x</version>
    </dependency>
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-starter-jpa</artifactId>
        <version>0.9.x</version>
    </dependency>
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-starter-otlp</artifactId>
        <version>0.9.x</version>
    </dependency>
</dependencies>
```

**After:**
```xml
<dependencies>
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-spring-core</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-spring-jpa</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>io.github.jframeoss</groupId>
        <artifactId>jframe-spring-otlp</artifactId>
        <version>0.10.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

---

## Import Changes

**No changes required.** The package structure is unchanged:

```
io.github.jframe.*
```

All existing imports continue to compile without modification.

---

## Breaking Changes

| Change Type | Details |
|-------------|---------|
| **Module artifact names** | `jframe-starter-*` → `jframe-spring-*` |
| **API changes** | None — public API is identical |
| **Configuration changes** | None — same `jframe.*` properties |
| **Package changes** | None — same `io.github.jframe.*` packages |

The migration is purely a dependency coordinate update in your build file.

---

## New jframe-core Module

A new `jframe-core` module has been extracted containing framework-agnostic components shared across both Spring Boot and Quarkus:

- **Exception hierarchy** — `JFrameException`, `HttpException`, `ApiException`, `ValidationException`
- **Validation framework** — `ValidationResult`, `FieldRejection`, `Validator<T>`
- **HTTP status mapping** — Framework-neutral HTTP status abstraction
- **Search specifications** — `SearchInput`, `SearchCriterium`, field types

`jframe-core` is a **transitive dependency** of `jframe-spring-core`. No action needed — it is automatically included when you add `jframe-spring-core`.

---

## Configuration

No configuration changes needed. All `jframe.*` properties remain identical:

```yaml
jframe:
  application:
    name: my-app
    group: com.example
    version: 1.0.0
    environment: production

  logging:
    disabled: false
    response-length: 10000
    fields-to-mask: [password, secret, token]
```

---

## Migration Checklist

Follow these steps to complete the migration:

- [ ] **1. Update dependencies** — Replace `jframe-starter-*` with `jframe-spring-*` at version `0.10.0-SNAPSHOT` in your `build.gradle.kts` or `pom.xml`
- [ ] **2. Verify build** — Run `./gradlew clean build` (or `mvn clean package`) and confirm no compilation errors
- [ ] **3. Run tests** — Execute your test suite to confirm no behavioral regressions

---

## Quarkus Option

If you are starting a new project or considering a framework migration, Quarkus adapters are now available via the `jframe-quarkus-*` modules:

- `jframe-quarkus-core` — JAX-RS exception mappers and request logging filters
- `jframe-quarkus-jpa` — Panache search integration
- `jframe-quarkus-otlp` — OpenTelemetry tracing with CDI interceptors

See the [Quarkus Adoption Guide](./quarkus-guide.md) for details.

---

## See Also

- [Architecture Overview](../src/docs/architecture.md)
- [jframe-spring-core](../src/docs/jframe-spring-core.md)
- [jframe-spring-jpa](../src/docs/jframe-spring-jpa.md)
- [jframe-spring-otlp](../src/docs/jframe-spring-otlp.md)
- [Quarkus Adoption Guide](./quarkus-guide.md)
