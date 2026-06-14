# ECS Naming Convention Migration Guide

This guide walks you through migrating from the legacy Kibana-based naming convention to the Elastic Common Schema (ECS) standard. ECS is the industry-standard schema for observability data, enabling better compatibility with Elasticsearch, Kibana, and other observability tools.

## What Changed

### Package Name
- **Old**: `io.github.jframe.logging.kibana`
- **New**: `io.github.jframe.logging.ecs`

### Class Renames

| Old Class | New Class |
|---|---|
| `KibanaLogField` | `EcsField` |
| `KibanaLogFields` | `EcsFields` |
| `KibanaLogFieldNames` | `EcsFieldNames` |
| `KibanaLogContext` | `MdcLogContext` |
| `AutoCloseableKibanaLogField` | `AutoCloseableEcsField` |
| `AutoCloseableKibanaLogFieldImpl` | `AutoCloseableEcsFieldImpl` |
| `CompoundAutocloseableKibanaLogField` | `CompoundAutoCloseableEcsField` |
| `KibanaLogTypeNames` | `LogTypeNames` |
| `KibanaLogCallResultTypes` | `CallResultTypes` |

### Method Renames
- `getLogName()` → `getKey()`

### MDC Key Changes
All 42 MDC string keys changed from custom snake_case to ECS dot-notation:

| Old Key | New Key | Purpose |
|---|---|---|
| `request_id` | `request.id` | Request identifier |
| `transaction_id` | `transaction.id` | Transaction identifier |
| `trace_id` | `trace.id` | Distributed trace ID |
| `span_id` | `span.id` | Distributed span ID |
| `log_type` | `event.category` | Event category |
| `request_method` | `http.request.method` | HTTP request method |
| `request_uri` | `url.path` | Request URI path |
| `response_status` | `http.response.status_code` | HTTP response status |
| `call_request_method` | `http.client.request.method` | Outbound HTTP method |
| `user_name` | `user.name` | User identifier |
| `error_code` | `error.code` | Error code |
| `error_message` | `error.message` | Error message |

See `EcsFieldNames` enum for the complete list of 42 keys.

### Removed
- `OpenTelemetryConstants.Attributes` class (all span attributes now in `EcsFieldNames` as `SPAN_*` constants)
- `ERROR` boolean span attribute (use `error.type` in logs instead)

## What Did NOT Change

- `EcsFields` API methods: `tag()`, `tagCloseable()`, `get()`, `clear()` — same signatures
- MDC-based logging approach and auto-closeable field pattern
- Overall observability architecture

## Migration Steps

### Step 1: Update Imports

**Before:**
```java
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.kibana.KibanaLogFieldNames;
```

**After:**
```java
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.ecs.EcsFieldNames;
```

### Step 2: Rename Class Usages

**Before:**
```java
private static final KibanaLogFields fields = new KibanaLogFields();
```

**After:**
```java
private static final EcsFields fields = new EcsFields();
```

### Step 3: Update Method Calls

**Before:**
```java
String key = field.getLogName();
```

**After:**
```java
String key = field.getKey();
```

### Step 4: Update Log Consumer Configurations

Update Kibana dashboards, Elasticsearch queries, and log parsers to use new ECS key names.

**Before (Kibana query):**
```
request_id:"12345"
```

**After (Kibana query):**
```
request.id:"12345"
```

## Migration Checklist

- [ ] Update all `import` statements from `kibana` to `ecs` package
- [ ] Rename `KibanaLogFields*` classes to `EcsFields*`
- [ ] Rename `KibanaLogContext` to `MdcLogContext`
- [ ] Update `getLogName()` calls to `getKey()`
- [ ] Update OpenTelemetry span attributes to use `EcsFieldNames.SPAN_*` constants
- [ ] Update Kibana dashboards to use new ECS dot-notation key names
- [ ] Update Elasticsearch queries and filters
- [ ] Test observability pipeline end-to-end
- [ ] Update documentation and runbooks
- [ ] Verify metrics and trace collection in new ECS format
