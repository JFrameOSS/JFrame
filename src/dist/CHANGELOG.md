# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — 1.0.0

### Added
- **Quarkus support** — full adapter modules for Quarkus applications
  - `jframe-quarkus-core`: JAX-RS exception mappers, request/response logging filters, request-scoped caching, Jackson ObjectMapper producer, `@ScheduledWithContext` CDI interceptor
  - `jframe-quarkus-jpa`: Panache-based search repository, specification builder, page mapping, SQL query logging
  - `jframe-quarkus-otlp`: OpenTelemetry CDI interceptors (`@Traced`, `@LogExecutionTime`), W3C trace propagation, outbound tracing filter, span management
- **`jframe-core` module** — framework-agnostic shared library extracted from Spring modules
  - Exception hierarchy (`JFrameException`, `HttpException`, `ApiException`, `ValidationException`, `RateLimitExceededException`)
  - `HttpStatusCode` enum (framework-independent HTTP status codes)
  - Fluent validation API (`Validator<T>`, `ValidationResult`, `FieldRejection` DSL)
  - Search specification framework (`SearchSpecification<T>`, `BaseSearchSpecification`, 10 field types)
  - Pagination models (`SortablePageInput`, `PageResource<T>`, `SearchCriterium`)
  - `RequestId` / `TransactionId` thread-local context holders
  - `KibanaLogFields` MDC integration (40+ structured log fields)
  - JSON utilities (`ObjectMappers`), model converters, constants
- **Spring adapter classes** for bridging `jframe-core` types to Spring equivalents
  - `SpringHttpStatus` — converts `HttpStatusCode` to/from Spring `HttpStatus`
  - `SpringDataSearchSpecification` — wraps `SearchSpecification<T>` as Spring `Specification<T>`
  - `SpringPageAdapter` — converts query results to `PageResource<T>`

### Changed
- **Module rename** — Spring modules renamed for multi-framework clarity
  - `starter-core` → `jframe-spring-core`
  - `starter-jpa` → `jframe-spring-jpa`
  - `starter-otlp` → `jframe-spring-otlp`
- `HttpException` now uses `HttpStatusCode` (from `jframe-core`) instead of Spring's `HttpStatus`
- `JpaSearchSpecification` now extends `BaseSearchSpecification` from `jframe-core`
- All Spring modules now transitively depend on `jframe-core`

### Migration
See the [Spring Migration Guide](../../docs/migration/spring-migration-1.0.0.md) for upgrade instructions.

---

## [0.9.0] — 2026-01-21

### Added
- Rate limit exception handling (`RateLimitExceededException`, HTTP 429 response)
- Rate limit error response enricher with `X-RateLimit-*` headers

---

## [0.8.0] — 2026-01-21

### Added
- Request-scoped entity cache (`RequestScopedCache<K, V>`) to prevent duplicate database queries within a single HTTP request

### Changed
- Upgraded Gradle wrapper to 9.3.0

---

## [0.7.0] — 2026-01-05

### Fixed
- Sort field-to-column mapping in search metadata — sort fields now correctly resolve to database column names

---

## [0.6.0] — 2026-01-03

### Fixed
- Changelog generation workflow improvements
- Inverse predicate enhancement for search specifications

---

## [0.5.0] — 2026-01-02

### Added
- Inverse search logic — prefix search values with `!` to negate predicates
- Automated changelog generation workflow

---

## [0.4.0] — 2025-12-31

### Added
- Multi-column fuzzy search (`MULTI_COLUMN_FUZZY` search type) — search across multiple database columns with a single query term
- Changelog CI workflow for automated release notes

### Changed
- Improved error response formatting for validation errors

---

## [0.3.0] — 2025-12-28

### Added
- Jackson ObjectMapper configuration with sensible defaults (camelCase, no pretty-print, always-include)

### Changed
- Dependency updates across all modules
- Upgraded Gradle wrapper to 9.2.1

---

## [0.2.0] — 2025-12-02

### Added
- CI/CD pipeline with GitHub Actions for build, test, and Maven Central publishing

### Changed
- Upgraded Gradle wrapper to 9.2.0
