# Logging Framework

HTTP request/response logging with correlation tracking, sensitive data masking, and structured logging for Kibana/ELK.

**Package:** `io.github.jframe.logging`
**Config:** `jframe.logging.*`

## Architecture

### Filter Chain

Ordered servlet filters that capture and log HTTP traffic:

| Order | Filter | Purpose |
|-------|--------|---------|
| -400 | `RequestIdFilter` | Generate/extract request correlation ID (`x-request-id`) |
| -300 | `TransactionIdFilter` | Track multi-request transactions (`x-transaction-id`) |
| -200 | `RequestDurationFilter` | Measure request processing time |
| -100 | `RequestResponseLogFilter` | Log request/response bodies with filtering/masking |

**Package:** `io.github.jframe.logging.filter`

### Voting Mechanism

Determines whether logging should occur based on:
- **Content Type** - Must be in allowed list (JSON, XML, text)
- **Request Path** - Must not match exclusion patterns (e.g., `/actuator/**`, `/health`)

**Package:** `io.github.jframe.logging.voter`

### Sensitive Data Masking

Redacts sensitive information before logging:
- **JSON Masking** - Masks field values in JSON payloads
- **URI Masking** - Masks query parameters
- **Configurable Fields** - Define which field names trigger masking

**Package:** `io.github.jframe.logging.masker`

**Example:**
```json
// Original
{"username": "john", "password": "secret123"}

// Logged
{"username": "john", "password": "***"}
```

### Structured Logging

MDC-based logging for log aggregation platforms (Kibana/ELK).

**Package:** `io.github.jframe.logging.kibana`

**MDC Fields:**

| Field | Description |
|-------|-------------|
| `REQUEST_ID` | Unique request identifier |
| `TX_ID` | Transaction identifier |
| `TX_REQUEST_METHOD` | HTTP method |
| `TX_REQUEST_URI` | Request URI |
| `TX_REQUEST_SIZE` | Request body size |
| `TX_RESPONSE_SIZE` | Response body size |
| `HTTP_STATUS` | Response status code |

## Configuration

```yaml
jframe:
  logging:
    # Master switch
    disabled: false

    # Response body length limit (-1 = unlimited)
    response-length: -1

    # Content types to log
    allowed-content-types:
      - application/json
      - application/xml
      - text/xml
      - text/plain

    # Paths to exclude (Ant-style patterns)
    exclude-paths:
      - method: GET
        path: /actuator/**
      - method: "*"
        path: /health

    # Field names to mask
    fields-to-mask:
      - password
      - passwd
      - apiKey
      - authorization
      - secret
      - token
      - client_secret

    # Individual filter configuration
    filters:
      request-id:
        enabled: true
        order: -400
      transaction-id:
        enabled: true
        order: -300
      request-duration:
        enabled: true
        order: -200
      request-response-log:
        enabled: true
        order: -100
```

## Features

### Correlation IDs

**Request ID:**
```java
String requestId = RequestId.get();
```

**Transaction ID:**
```java
String txId = TransactionId.get();
```

**Automatic Propagation:**
- Request/transaction IDs stored in ThreadLocal
- Automatically included in MDC for all log statements
- Propagated to outgoing HTTP calls via interceptor

### Request/Response Logging

Logs HTTP traffic with:
- Request method, URI, headers, body
- Response status, headers, body
- Request processing duration
- Correlation IDs

### Data Masking

**Supported Formats:**
- JSON: `{"password": "***"}`
- URI: `/api/users?password=***`

**Configuration:**
```yaml
jframe:
  logging:
    fields-to-mask:
      - password
      - secret
      - token
      - apiKey
```

**Custom Masker:**
```java
@Component
public class XmlPasswordMasker implements Masker {
    @Override
    public boolean matches(MaskedPasswordBuilder builder) {
        // Detect and mask XML format
        return true; // if masking occurred
    }
}
```

## Usage

### Accessing Correlation IDs

```java
import org.slf4j.MDC;
import static io.github.jframe.logging.kibana.KibanaLogFields.*;

public class OrderService {
    public void processOrder(Order order) {
        String requestId = MDC.get(REQUEST_ID);
        String txId = MDC.get(TX_ID);

        log.info("Processing order {} for request {}", order.getId(), requestId);
        // Correlation IDs automatically included in logs
    }
}
```

### Tagging Logs with Context

```java
@Service
public class PaymentService {
    public void processPayment(Payment payment) {
        try (var field = KibanaLogFields.tagCloseable(
                KibanaLogFieldNames.TX_ID, payment.getTransactionId())) {

            log.info("Processing payment for amount: {}", payment.getAmount());
            // All logs in this block include the transaction ID
        }
        // Transaction ID automatically cleared from MDC
    }
}
```

### Cross-Thread Context Propagation

```java
@Service
public class AsyncService {
    public CompletableFuture<Result> processAsync() {
        // Capture current thread's log context
        KibanaLogContext context = KibanaLogFields.getContext();

        return CompletableFuture.supplyAsync(() -> {
            // Propagate context to new thread
            KibanaLogFields.populateFromContext(context);

            log.info("Processing asynchronously");
            return doWork();
        });
    }
}
```

### REST Client Logging

```java
@Configuration
public class RestTemplateConfig {
    @Autowired
    private LoggingClientHttpRequestInterceptor loggingInterceptor;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(loggingInterceptor);
        return restTemplate;
    }
}
```

## Performance

### Body Capture
Uses wrapper classes for multiple reads without performance impact:
- `CachedBodyHttpServletRequest`
- `CachedBodyHttpServletResponse`

### Optimization Tips
- Set `response-length` limits for large payloads
- Use content type filtering to exclude binary data
- Exclude paths that don't require auditing
- Configure `fields-to-mask` only for necessary fields

## Security

### Best Practices

1. **Configure fields-to-mask** - Include all sensitive field names
2. **Exclude auth endpoints** - Prevent logging authentication requests
3. **Set response-length limits** - Prevent logging large binaries
4. **Review allowed-content-types** - Exclude binary formats
5. **Use path exclusions** - Exclude health checks and monitoring endpoints

### Masking Details
- Case-insensitive matching (matches "password", "Password", "PASSWORD")
- Masking occurs before logs are written
- Masked value `***` is not reversible

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Logging not working | Check `disabled: false`, filters enabled, path not excluded, content type allowed |
| Passwords not masked | Verify field names in `fields-to-mask`, check format supported (JSON/URI) |
| Missing correlation IDs | Verify `RequestIdFilter` enabled and ordered correctly (-400) |
| High memory usage | Set `response-length` limit, exclude large response paths |

## Integration

### With starter-otlp
When using `starter-otlp`, logging is enhanced with:
- Trace ID propagation to logs
- Span ID correlation
- Custom spans for filter execution
- Distributed tracing across microservices

## See Also

- [Exception Handling](./exception-handling.md)
- [Validation Framework](./validation.md)
- [starter-core](../starter-core.md)
- [starter-otlp](../starter-otlp.md)