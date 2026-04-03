# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — 2026-04-03

### Added

#### Multi-framework architecture
- **`jframe-core` module** — framework-agnostic shared library extracted from Spring modules
  - Exception hierarchy (`JFrameException`, `HttpException`, `ApiException`, `ValidationException`, `RateLimitExceededException`)
  - `ExceptionResponseFactory` interface for pluggable exception-to-response mapping
  - `ConstraintViolationResponseResource` for constraint violation error responses
  - Fluent validation API (`Validator<T>`, `ValidationResult`, `FieldRejection` DSL)
  - Search specification framework (`SearchSpecification<T>`, `BaseSearchSpecification`, 10 field types)
  - Pagination models (`SortablePageInput`, `PageResource<T>`, `SearchCriterium`)
  - `RequestId` / `TransactionId` thread-local context holders
  - JSON utilities (`ObjectMappers`), model converters, constants
- **Quarkus support** — full adapter modules for Quarkus applications
  - `jframe-quarkus-core`: JAX-RS exception mappers, request/response logging filters, request-scoped caching, Jackson ObjectMapper producer, `@ScheduledWithContext` CDI interceptor
  - `jframe-quarkus-jpa`: Panache-based search repository, specification builder, page mapping, SQL query logging
  - `jframe-quarkus-otlp`: OpenTelemetry CDI interceptors (`@Traced`, `@LogExecutionTime`), W3C trace propagation, outbound tracing filter, span management
  - `jframe-quarkus-otlp-deployment`: Quarkus build-time extension processor for auto-discovering `@Traced` beans via Jandex
- **Spring adapter classes** for bridging `jframe-core` types to Spring equivalents
  - `JpaSearchSpecification` — wraps `SearchSpecification<T>` as Spring `Specification<T>`
  - `SpringPageAdapter` — converts query results to `PageResource<T>`

#### Observability
- **ECS-compliant structured logging** — `EcsFields`, `EcsFieldNames`, `EcsField` with 40+ Elastic Common Schema field constants for consistent MDC tagging
  - `MdcLogContext` utility for scoped MDC field management
  - `long` overloads — `tag(field, long)`, `tagCloseable(field, long)`, and `and(field, long)` for numeric identifiers
  - Event fields (`EVENT_REASON`, `EVENT_TYPE`), HTTP client fields, event value constants
- **`UserIdentityFilter`** (Spring + Quarkus) — sets authenticated user identity in MDC once per request, removing duplicate lookups from tracing interceptors
- **`ScheduledTaskEnricher`** strategy pattern — pluggable interface for enriching `@Scheduled` task tracing spans
  - `TracingScheduledTaskEnricher` (Spring OTLP) — creates dedicated spans with `otel.library.name` and execution metadata
  - `ScheduledAspect` (Spring core) — AOP-based `@Scheduled` context propagation with transaction IDs
- **`MethodExclusionRules`** — configurable method name exclusion for auto-tracing (excludes synthetic, Object, and common framework methods)
- **`SpanNamingUtil`** — utility for generating consistent `ClassName.methodName` span names
- **`OtlpDefaults`** — shared OpenTelemetry default values (service name, excluded URLs, resource attributes)
- **`OpenTelemetryConstants`** — shared span attribute key constants extracted to `jframe-core`
- **`AuthenticationConstants`** + **`AuthenticationResolver`** — framework-agnostic authentication abstraction in `jframe-core`

#### Quarkus-specific
- **`AbstractExceptionMapper<T>`** base class — eliminates boilerplate across 5 JAX-RS exception mappers
- **`JFrameErrorResponseFilter`** (OASFilter) — automatically adds 400/429/500 error responses to all OpenAPI operations
- **Jandex indexing** for all Quarkus modules — generates `META-INF/jandex.idx` for build-time CDI and `@ConfigMapping` discovery
- **`PasswordMaskerProducer`** CDI bean — auto-wires `PasswordMasker` from `LoggingConfig.fieldsToMask()`
- **MicroProfile Config defaults** — `META-INF/microprofile-config.properties` in core and OTLP matching Spring's `jframe-properties.yml`
- **`ApplicationConfig.url()`** property — matches Spring's `ApplicationProperties.url`

#### Shared utilities
- **`AntStylePathMatcher`** — framework-agnostic Ant-style path matching (no Spring dependency required)
- **`DetailedQueryEntryCreator`** + **`SqlStatementLogging`** — shared SQL query logging extracted to `jframe-core`

#### Build & CI
- **Java 25 LTS toolchain** with `options.release = 21` for consumer bytecode compatibility
- **Foojay resolver plugin** for automatic JDK provisioning in CI
- **CycloneDX SBOM generation** — aggregated BOM in CycloneDX 1.6 format with license text and serial numbers
- **CI artifact uploads** — test results and SBOM published as workflow artifacts
- **CI workflow modernization** — bumped to actions/checkout@v6, setup-java@v5, upload-artifact@v7, gradle/actions@v6; reads Java version from `mise.toml`
- **`llms-full.txt`** + per-module **`META-INF/jframe-ai-context.md`** — AI-friendly documentation for LLM code assistants

### Fixed
- **`PasswordMasker` break-on-first-match** — masking loop no longer overwrites match result with subsequent masker evaluations; short-circuits on first successful match
- **Case-insensitive password field matching** — `MaskedPasswordBuilder` now matches field names case-insensitively (e.g., `"Password"` and `"password"` both masked)
- **`MethodExclusionRules` prefix exclusions** — removed `get`/`set`/`is` prefix-based exclusions that incorrectly blocked service-layer spans like `getUser()` or `setStatus()`
- **`DatasourceProxyProducer` CDI ambiguous resolution** — fixed dual `DataSource` bean conflict by using `@Alternative @Priority(1)` and injecting `AgroalDataSource` directly
- **OTLP dependencies scope** — `jframe-quarkus-otlp` OpenTelemetry deps changed from `compileOnly` to `api` preventing consumer `UnsatisfiedResolutionException`

### Changed
- **Module rename** — Spring modules renamed for multi-framework clarity
  - `starter-core` → `jframe-spring-core`
  - `starter-jpa` → `jframe-spring-jpa`
  - `starter-otlp` → `jframe-spring-otlp`
- **Consolidated module layout** — nested `jframe-spring/` and `jframe-quarkus/` directory structure
- **ECS naming convention** — renamed `KibanaLogFields` → `EcsFields`, `KibanaLogFieldNames` → `EcsFieldNames`, `KibanaLogField` → `EcsField`, `AutoCloseableKibanaLogField` → `AutoCloseableEcsField`, `KibanaLogContext` → `MdcLogContext`
- **`HttpException`** now uses `jakarta.ws.rs.core.Response.Status` instead of Spring's `HttpStatus`
- **`JpaSearchSpecification`** now extends `BaseSearchSpecification` from `jframe-core`
- **All Spring modules** now transitively depend on `jframe-core`
- **Span error recording** — replaced manual error attributes with `span.recordException()` (OpenTelemetry best practice)
- **Span lifecycle logging** — downgraded from `debug` to `trace` level to reduce noise in production
- **Test framework** — migrated from AssertJ to Hamcrest across all modules
- **Dependency upgrades** — Spring Boot 4.0.1→4.0.3, OpenTelemetry 2.23.0→2.26.1, PMD 7.19.0→7.23.0, Checkstyle 12.3.0→13.4.0, Lombok plugin 9.1.0→9.2.0, Spotless 8.1.0→8.2.1, CycloneDX 3.1.0→3.2.0, NMCP 1.2.1→1.4.4, Gradle wrapper 9.3.0→9.4.0
- **Removed 10+ redundant dependencies** — cleaned up unused transitive deps and dead properties from `gradle.properties`

### Removed
- **`HttpStatusCode`** enum — replaced by `jakarta.ws.rs.core.Response.Status` (framework-independent, standard Jakarta API)
- **`SpringHttpStatus`** / **`QuarkusHttpStatus`** adapters — no longer needed after `HttpStatusCode` removal
- **`KibanaLogFields`**, **`KibanaLogFieldNames`**, **`KibanaLogContext`**, **`AutoCloseableKibanaLogField`** — replaced by ECS-named equivalents
- **`OpenTelemetryConstants`** class (from `jframe-spring-otlp`) — merged into shared `EcsFieldNames` in `jframe-core`
- **Old `starter-*` module directories** — replaced by `jframe-spring/*` and `jframe-quarkus/*`
- **Legacy documentation** — removed `architecture.md`, `caching.md`, `exception-handling.md`, `logging.md`, `validation.md`, `java-guide.md`, `ci.md`, `gradle-wrapper-update.md`, `maven-central-publishing-guide.md`, `starter-*.md`; replaced with per-framework guides
- **48 trivial tests** — removed constant assertions, annotation metadata checks, instanceOf hierarchy tests, and trivial toString tests
- **AssertJ dependency** — replaced by Hamcrest

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
