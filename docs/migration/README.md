# Migration Guides

Every breaking or behaviour-changing release has a guide. Read the guides for each version between the one you are on and the one you are moving to, oldest first.

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
