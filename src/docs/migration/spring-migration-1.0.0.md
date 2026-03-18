# Spring Migration Guide: 0.9.x → 1.0.0

This guide covers migrating Spring Boot applications from `jframe-starter-*` (0.9.x and earlier) to `jframe-spring-*` (1.0.0).

## What changed

JFrame 1.0.0 introduces multi-framework support (Spring Boot + Quarkus). Framework-agnostic code was extracted into a shared `jframe-core` module, and Spring modules were renamed for clarity.

**What did NOT change:**
- GroupId (`io.github.jframeoss`)
- Java package names (`io.github.jframe.*`)
- Configuration properties (`jframe.*` namespace)
- Auto-configuration behavior

## Step 1: Update dependencies

### Gradle (Kotlin DSL)

```kotlin
// Before (0.9.x)
implementation("io.github.jframeoss:starter-core:0.9.0")
implementation("io.github.jframeoss:starter-jpa:0.9.0")
implementation("io.github.jframeoss:starter-otlp:0.9.0")

// After (1.0.0)
implementation("io.github.jframeoss:jframe-spring-core:1.0.0")
implementation("io.github.jframeoss:jframe-spring-jpa:1.0.0")
implementation("io.github.jframeoss:jframe-spring-otlp:1.0.0")
```

### Maven

```xml
<!-- Before (0.9.x) -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>starter-core</artifactId>
    <version>0.9.0</version>
</dependency>

<!-- After (1.0.0) -->
<dependency>
    <groupId>io.github.jframeoss</groupId>
    <artifactId>jframe-spring-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

| Old artifactId | New artifactId |
|----------------|----------------|
| `starter-core` | `jframe-spring-core` |
| `starter-jpa` | `jframe-spring-jpa` |
| `starter-otlp` | `jframe-spring-otlp` |

> **Note:** `jframe-core` is pulled in transitively — do not add it as an explicit dependency.

## Step 2: Fix breaking API changes

### HttpException uses HttpStatusCode

`HttpException` and its subclasses now use `io.github.jframe.http.HttpStatusCode` instead of Spring's `org.springframework.http.HttpStatus`.

```java
// Before
import org.springframework.http.HttpStatus;
throw new HttpException(HttpStatus.NOT_FOUND, "Not found");

// After — use jframe-core enum directly
import io.github.jframe.http.HttpStatusCode;
throw new HttpException(HttpStatusCode.NOT_FOUND, "Not found");
```

If you need to convert between the two types, use the adapter:

```java
import io.github.jframe.http.SpringHttpStatus;

HttpStatusCode coreStatus = SpringHttpStatus.fromSpring(HttpStatus.NOT_FOUND);
HttpStatus springStatus = SpringHttpStatus.toSpring(HttpStatusCode.NOT_FOUND);
```

**Most applications won't need this** — the built-in exception subclasses (`BadRequestException`, `ResourceNotFoundException`, etc.) already use the correct status codes internally.

### JpaSearchSpecification interface

`JpaSearchSpecification<T>` now extends `BaseSearchSpecification<T>` from `jframe-core` instead of directly implementing Spring's `Specification<T>`. It still implements `Specification<T>`, so most code works unchanged.

If you explicitly referenced the `Specification<T>` interface from search specs, use the adapter:

```java
import io.github.jframe.datasource.search.SpringDataSearchSpecification;

// Wrap when you need a pure Spring Specification<T>
SpringDataSearchSpecification<User> springSpec =
    new SpringDataSearchSpecification<>(mySearchSpec);
```

## Step 3: Check transitive dependencies

`jframe-core` adds these transitive API dependencies:

| Dependency | Notes |
|-----------|-------|
| `commons-lang3` | Was likely already on your classpath |
| `commons-collections4` | Was likely already on your classpath |
| `commons-io` | Was likely already on your classpath |
| `hamcrest` | **New** — used in validation API signatures |

**Action:** If you declared any of these explicitly, check for version conflicts. Remove duplicate declarations if versions match.

## Step 4: Verify

```bash
# Rebuild and run tests
./gradlew clean build test

# Check for compilation errors related to:
# - org.springframework.http.HttpStatus in exception constructors
# - Specification<T> type mismatches in search code
```

## Checklist

- [ ] Updated all `starter-*` → `jframe-spring-*` dependencies
- [ ] Updated version to `1.0.0`
- [ ] Fixed `HttpStatus` → `HttpStatusCode` references (if any)
- [ ] Verified `JpaSearchSpecification` usage compiles
- [ ] Checked for transitive dependency conflicts
- [ ] Full build passes
