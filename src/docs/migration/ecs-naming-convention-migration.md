# ECS Naming Convention Migration Guide

This guide covers migrating applications that previously used `KibanaLogFieldNames` for MDC field keys
and `OpenTelemetryConstants.Attributes` for span attribute keys. Both have been unified into a single
enum class `EcsFieldNames` following
[Elastic Common Schema (ECS)](https://www.elastic.co/guide/en/ecs/current/index.html) and
[OpenTelemetry semantic conventions](https://opentelemetry.io/docs/specs/semconv/).

---

## Breaking changes

### 1. `KibanaLogFieldNames` renamed to `EcsFieldNames`

The enum class has been renamed. The package remains the same (`io.github.jframe.logging.kibana`).

```java
// Before
import io.github.jframe.logging.kibana.KibanaLogFieldNames;
KibanaLogFieldNames.TX_ID

// After
import io.github.jframe.logging.kibana.EcsFieldNames;
EcsFieldNames.TX_ID
```

All enum constant names (e.g., `TX_ID`, `REQUEST_ID`, `TRACE_ID`) are unchanged.

### 2. `OpenTelemetryConstants.Attributes` merged into `EcsFieldNames`

The `Attributes` inner class has been removed from `OpenTelemetryConstants`. All span attribute
key constants are now **enum constants** on `EcsFieldNames`, prefixed with `SPAN_`. Use `.getKey()`
to obtain the string key for `span.setAttribute()` calls.

```java
// Before
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_NAME;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
span.setAttribute(SERVICE_NAME, className);
span.setAttribute(ERROR_TYPE, exceptionName);

// After
import static io.github.jframe.logging.kibana.EcsFieldNames.SPAN_SERVICE_NAME;
import static io.github.jframe.logging.kibana.EcsFieldNames.SPAN_ERROR_TYPE;
span.setAttribute(SPAN_SERVICE_NAME.getKey(), className);
span.setAttribute(SPAN_ERROR_TYPE.getKey(), exceptionName);
```

Or with a wildcard import:

```java
import static io.github.jframe.logging.kibana.EcsFieldNames.*;
span.setAttribute(SPAN_SERVICE_NAME.getKey(), className);
```

> **Exception:** `SPAN_TRACING_SPAN` remains a `public static final String` because it is used as a
> Spring `ClientRequest` attribute key (not a span attribute key).

### 3. `KibanaLogField.getLogName()` renamed to `getKey()`

The `KibanaLogField` interface method has been renamed to better reflect that it returns the ECS field
key (used for both MDC and span attributes).

```java
// Before
field.getLogName()

// After
field.getKey()
```

### 4. `OpenTelemetryConstants.Attributes.ERROR` removed

The boolean `ERROR` constant (`"error"`) has been removed. Use `span.setStatus(StatusCode.ERROR)` instead:

```java
// Before
span.setAttribute(ERROR, true);
span.setStatus(StatusCode.ERROR);

// After
span.setStatus(StatusCode.ERROR);
```

**What is unchanged:**

- The `Logging` inner class in `OpenTelemetryConstants`
- The string values emitted as MDC log field names and span attribute keys (those were updated in a prior migration)

---

## Constant name mapping: `OpenTelemetryConstants.Attributes` → `EcsFieldNames`

| Old constant | New constant |
|---|---|
| `Attributes.SERVICE_NAME` | `SPAN_SERVICE_NAME` |
| `Attributes.SERVICE_METHOD` | `SPAN_SERVICE_METHOD` |
| `Attributes.EXCLUDE_TRACING` | *Removed (unused)* |
| `Attributes.HTTP_REMOTE_USER` | `SPAN_HTTP_REMOTE_USER` |
| `Attributes.HTTP_TRANSACTION_ID` | `SPAN_HTTP_TRANSACTION_ID` |
| `Attributes.HTTP_REQUEST_ID` | `SPAN_HTTP_REQUEST_ID` |
| `Attributes.HTTP_URI` | `SPAN_HTTP_URI` |
| `Attributes.HTTP_QUERY` | `SPAN_HTTP_QUERY` |
| `Attributes.HTTP_METHOD` | `SPAN_HTTP_METHOD` |
| `Attributes.HTTP_STATUS_CODE` | `SPAN_HTTP_STATUS_CODE` |
| `Attributes.HTTP_CONTENT_TYPE` | `SPAN_HTTP_CONTENT_TYPE` |
| `Attributes.HTTP_CONTENT_LENGTH` | `SPAN_HTTP_CONTENT_LENGTH` |
| `Attributes.PEER_SERVICE` | `SPAN_PEER_SERVICE` |
| `Attributes.TRACING_SPAN` | `SPAN_TRACING_SPAN` |
| `Attributes.EXT_REQUEST_URI` | `SPAN_EXT_REQUEST_URI` |
| `Attributes.EXT_REQUEST_QUERY` | `SPAN_EXT_REQUEST_QUERY` |
| `Attributes.EXT_REQUEST_METHOD` | `SPAN_EXT_REQUEST_METHOD` |
| `Attributes.EXT_RESPONSE_STATUS_CODE` | `SPAN_EXT_RESPONSE_STATUS_CODE` |
| `Attributes.EXT_RESPONSE_CONTENT_LENGTH` | `SPAN_EXT_RESPONSE_CONTENT_LENGTH` |
| `Attributes.EXT_RESPONSE_CONTENT_TYPE` | `SPAN_EXT_RESPONSE_CONTENT_TYPE` |
| `Attributes.EXT_RESPONSE_L7_REQUEST_ID` | `SPAN_EXT_RESPONSE_L7_REQUEST_ID` |
| `Attributes.ERROR_TYPE` | `SPAN_ERROR_TYPE` |
| `Attributes.ERROR_MESSAGE` | `SPAN_ERROR_MESSAGE` |
| `Attributes.ERROR` | *Removed — use `span.setStatus(StatusCode.ERROR)`* |

---

## MDC field key value changes (from prior migration)

| Java constant | Old MDC key | New MDC key |
|---|---|---|
| `SESSION_ID` | `session_id` | `session.id` |
| `HOST_NAME` | `host_name` | `host.name` |
| `SOFTWARE_VERSION` | `software_version` | `service.version` |
| `REQUEST_ID` | `req_id` | `request.id` |
| `REQUEST_DURATION` | `req_duration` | `event.duration` |
| `BUSINESS_TX_ID` | `business_tx_id` | `transaction.business_id` |
| `TRACE_ID` | `traceId` | `trace.id` |
| `SPAN_ID` | `spanId` | `span.id` |
| `TX_ID` | `tx_id` | `transaction.id` |
| `TX_TYPE` | `tx_type` | `transaction.type` |
| `TX_REQUEST_IP` | `tx_request_ip` | `client.ip` |
| `TX_REQUEST_METHOD` | `tx_request_method` | `http.request.method` |
| `TX_REQUEST_URI` | `tx_request_uri` | `url.path` |
| `TX_REQUEST_SIZE` | `tx_request_size` | `http.request.body.bytes` |
| `TX_REQUEST_HEADERS` | `tx_request_headers` | `http.request.headers` |
| `TX_REQUEST_BODY` | `tx_request_body` | `http.request.body.content` |
| `TX_RESPONSE_SIZE` | `tx_response_size` | `http.response.body.bytes` |
| `TX_RESPONSE_HEADERS` | `tx_response_headers` | `http.response.headers` |
| `TX_RESPONSE_BODY` | `tx_response_body` | `http.response.body.content` |
| `TX_DURATION` | `tx_duration` | `transaction.duration.ms` |
| `TX_STATUS` | `tx_status` | `transaction.result` |
| `HTTP_STATUS` | `http_status` | `http.response.status_code` |
| `CALL_ID` | `call_id` | `span.id` |
| `CALL_REQUEST_METHOD` | `call_request_method` | `http.client.request.method` |
| `CALL_REQUEST_URI` | `call_request_uri` | `url.full` |
| `CALL_REQUEST_SIZE` | `call_request_size` | `http.client.request.body.bytes` |
| `CALL_REQUEST_HEADERS` | `call_request_headers` | `http.client.request.headers` |
| `CALL_REQUEST_BODY` | `call_request_body` | `http.client.request.body.content` |
| `CALL_RESPONSE_SIZE` | `call_response_size` | `http.client.response.body.bytes` |
| `CALL_RESPONSE_HEADERS` | `call_response_headers` | `http.client.response.headers` |
| `CALL_RESPONSE_BODY` | `call_response_body` | `http.client.response.body.content` |
| `CALL_DURATION` | `call_duration` | `event.duration.ms` |
| `CALL_STATUS` | `call_status` | `event.outcome` |
| `TASK_ID` | `task_id` | `task.id` |
| `USER_NAME` | `user_name` | `user.name` |
| `LOG_TYPE` | `log_type` | `event.category` |
| `THREAD` | `thread` | `process.thread.name` |
| `LEVEL` | `level` | `log.level` |
| `TIMESTAMP` | `timestamp` | `@timestamp` |
| `LOG_LOCATION` | `log_loc` | `log.origin` |
| `MESSAGE` | `message` | `message` |

---

## Class renames (Kibana → ECS/generic)

The following classes in `io.github.jframe.logging.kibana` have been renamed. The package itself
remains unchanged.

| Old class name | New class name |
|---|---|
| `KibanaLogField` | `EcsField` |
| `KibanaLogFields` | `EcsFields` |
| `KibanaLogContext` | `EcsLogContext` |
| `AutoCloseableKibanaLogField` | `AutoCloseableEcsField` |
| `AutoCloseableKibanaLogFieldImpl` | `AutoCloseableEcsFieldImpl` |
| `CompoundAutocloseableKibanaLogField` | `CompoundAutoCloseableEcsField` |
| `KibanaLogTypeNames` | `LogTypeNames` |
| `KibanaLogCallResultTypes` | `CallResultTypes` |

### Method rename

| Old method | New method | Class |
|---|---|---|
| `registerKibanaLogFieldsInThisThread()` | `registerEcsFieldsInThisThread()` | `EcsLogContext` |

### Lombok added to `EcsFieldNames`

`EcsFieldNames` now uses `@Getter` and `@RequiredArgsConstructor` from Lombok.
The manual constructor and `getKey()` method have been removed.
The internal field `fieldKey` has been renamed to `key` (Lombok generates `getKey()` from it).

```java
// Before
KibanaLogFields.tag(KibanaLogFieldNames.TX_ID, "123");
final KibanaLogContext ctx = KibanaLogFields.getContext();
ctx.registerKibanaLogFieldsInThisThread();

// After
EcsFields.tag(EcsFieldNames.TX_ID, "123");
final EcsLogContext ctx = EcsFields.getContext();
ctx.registerEcsFieldsInThisThread();
```

---

## Checklist

- [ ] Rename all `KibanaLogField` imports to `EcsField`
- [ ] Rename all `KibanaLogFields` imports to `EcsFields`
- [ ] Rename all `KibanaLogContext` imports to `EcsLogContext`
- [ ] Rename all `AutoCloseableKibanaLogField` imports to `AutoCloseableEcsField`
- [ ] Rename all `KibanaLogTypeNames` imports to `LogTypeNames`
- [ ] Rename all `KibanaLogCallResultTypes` imports to `CallResultTypes`
- [ ] Replace `registerKibanaLogFieldsInThisThread()` with `registerEcsFieldsInThisThread()`
- [ ] Rename all `KibanaLogFieldNames` imports to `EcsFieldNames`
- [ ] Replace all `OpenTelemetryConstants.Attributes.X` imports with `EcsFieldNames.SPAN_X`
- [ ] Replace all `Attributes.ERROR` usage with `span.setStatus(StatusCode.ERROR)`
- [ ] Replace all `.getLogName()` calls with `.getKey()`
- [ ] Add `.getKey()` to all `SPAN_*` constant usages in `span.setAttribute()` calls (they are now enum constants)
- [ ] Search your codebase for raw string literals like `"tx_id"`, `"traceId"`, `"req_id"` used as MDC keys and update them
- [ ] Update Kibana/Elasticsearch index mappings or dashboards that reference old field names
- [ ] Update OpenTelemetry collector pipelines or exporters that filter on old attribute names
- [ ] Full build and quality checks pass: `./gradlew clean build test spotlessCheck pmdMain`
