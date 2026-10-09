# Migration Guides

Every breaking or behaviour-changing release has a guide. Read the guides for each version between the one you are on and the one you are moving to, oldest first.

## 1.8.0

| Guide | What changed |
|-------|--------------|
| [RFC 9457 Problem Details](./1.8.0-problem-details.md) | Error responses now follow RFC 9457 Problem Details format (`application/problem+json`); old proprietary JSON shape removed; `statusCode` → `status`, `errorReason` → `detail`, `uri` → `instance`, `cause`/`method`/`query`/`contentType` removed; new `type`/`title` fields; extension members (`errorCode`, `txId`, `traceId`, `spanId`, `errors`, `limit`, `remaining`, `resetDate`); new properties `jframe.exception.enabled` and `jframe.exception.type-base-uri`; enrichers now registered as `@Bean` (Spring) or `@ApplicationScoped` (Quarkus); deterministic enricher ordering via `@Order`/`@Priority` |

## 1.7.1

| Guide | What changed |
|-------|--------------|
| [Sort Expressions](./1.7.1-sort-expressions.md) | `addSortExpression` orders virtual sort keys inside `toSearchSpecification`; `isVirtual` now true when any column is virtual; composite `tiebreaker()`; `toPageResource(page, null)` is ambiguous — cast to `(AppliedSort)` |

## 1.7.0

| Guide | What changed |
|-------|--------------|
| [Strict Search Input](./1.7.0-strict-sort.md) | Sort, filtering, and paging are now strictly validated — unknown fields, invalid values, and negative page numbers throw 400 instead of being silently dropped or coerced; fuzzy search terms (`%`, `_`, `\`) now match literally; Quarkus sort now matches Spring (case-insensitive, nulls last, dotted paths) |

## 1.6.2

| Guide | What changed |
|-------|--------------|
| [OpenAPI Validation Schema](./1.6.2-openapi-validation-schema.md) | The 400 validation schema in the generated OpenAPI spec is now always `ValidationErrorResponseResource` (was random) — re-baseline contract tests once |

## 1.6.1

| Guide | What changed |
|-------|--------------|
| [Spring Log Export](./1.6.1-spring-log-export.md) | `jframe.otlp.logs.enabled` was inert on Spring and is now wired up — Spring applications with telemetry enabled start exporting log records |

## 1.6.0

| Guide | What changed |
|-------|--------------|
| [OTLP Opt-In and Hardening](./1.6.0-otlp-opt-in.md) | Telemetry is now opt-in (Quarkus behaviour change); process command-line attributes excluded by default for security; `sampling-rate` now honoured on Spring; per-signal toggles for traces, metrics and logs |

## 1.5.0

| Guide | What changed |
|-------|--------------|
| [Upgrading to 1.5.0](./1.5.0-upgrading.md) | **Start here.** Consolidated checklist covering the whole release — what breaks the compile, what changes silently, and what is new |
| [Filters Opt-In](./1.5.0-filters-opt-in.md) | Correlation ID filters became opt-in, request logging moved to DEBUG, trace context uses only the standard W3C `traceparent` header |
| [Library Efficiency](./1.5.0-library-efficiency.md) | Dependency and toolchain update, published Jackson constraint, request/response body-size caps |

## 1.2.0

| Guide | What changed |
|-------|--------------|
| [Exception Handling](./1.2.0-exception-handling.md) | `ApiException` merged into `HttpException`, unified `ApiError` contract, homogeneous `errorCode`/`errorReason` on every error response |

## 1.0.0

| Guide | What changed |
|-------|--------------|
| [Spring Modules](./1.0.0-spring-modules.md) | `jframe-starter-*` artifacts renamed to `jframe-spring-*` |
| [ECS Naming Convention](./1.0.0-ecs-naming-convention.md) | `KibanaLogField*` replaced by `EcsField*`, log fields moved to the Elastic Common Schema |

## File naming

Guides are named `<version>-<topic>.md`. The version is the release the change shipped in, so a guide applies to anyone upgrading *to* that version or past it. Every guide also states its version under the title.
