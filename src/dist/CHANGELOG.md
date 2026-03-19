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
- **Jandex indexing** for all Quarkus modules — generates `META-INF/jandex.idx` for build-time CDI and `@ConfigMapping` discovery
- **`PasswordMaskerProducer`** CDI bean in `jframe-quarkus-core` — auto-wires `PasswordMasker` from `LoggingConfig.fieldsToMask()`, matching Spring's `CoreAutoConfiguration` behavior
- **`KibanaLogFields` `long` overloads** — `tag(field, long)`, `tagCloseable(field, long)`, and `and(field, long)` for logging numeric identifiers without manual `String.valueOf()` conversion
- **ECS event fields** in `KibanaLogFieldNames` — `EVENT_REASON`, `EVENT_TYPE` for Elastic Common Schema compliance
- **ECS HTTP client fields** in `KibanaLogFieldNames` — `HTTP_CLIENT_REQUEST_METHOD`, `HTTP_CLIENT_REQUEST_MIME_TYPE`, `HTTP_CLIENT_REQUEST_BODY_BYTES`, `HTTP_CLIENT_RESPONSE_STATUS_CODE`, `HTTP_CLIENT_RESPONSE_MIME_TYPE`, `HTTP_CLIENT_RESPONSE_BODY_BYTES`, `URL_FULL`
- **ECS event value constants** in `KibanaLogFieldNames` — `EVENT_VALUE_CLIENT`, `EVENT_VALUE_START`, `EVENT_VALUE_END`
- **MicroProfile Config defaults** for Quarkus modules — `META-INF/microprofile-config.properties` in core and OTLP with logging, OTLP, and OTel defaults matching Spring's `jframe-properties.yml`
- **`ApplicationConfig.url()`** property in `jframe-quarkus-core` — matches Spring's `ApplicationProperties.url`

### Fixed
- **`DatasourceProxyProducer`** — fixed CDI ambiguous resolution by using `@Alternative @Priority(1)` and injecting `AgroalDataSource` directly instead of generic `DataSource`
- **OTLP dependencies** in `jframe-quarkus-otlp` — changed from `compileOnly` to `api` scope so consumers get OpenTelemetry transitively
- **`PasswordMasker` break-on-first-match** — masking loop no longer overwrites match result with subsequent masker evaluations; short-circuits on first successful match
- **Case-insensitive password field matching** — `MaskedPasswordBuilder` now matches field names case-insensitively (e.g., `"Password"` and `"password"` both masked)
- **`DatasourceProxyProducer` CDI ambiguous resolution** — fixed dual `DataSource` bean conflict by using `@Alternative @Priority(1)` and injecting `AgroalDataSource` directly
- **OTLP dependencies scope** — `jframe-quarkus-otlp` OpenTelemetry deps changed from `compileOnly` to `api` preventing consumer `UnsatisfiedResolutionException`

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
