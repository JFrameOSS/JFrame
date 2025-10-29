/**
 * Comprehensive HTTP request/response logging infrastructure with correlation tracking, sensitive data masking, and conditional logging.
 *
 * <h2>Overview</h2>
 * This package provides a complete logging solution for Spring Boot applications, featuring:
 * <ul>
 * <li><strong>Filter Chain</strong> - Ordered servlet filters for capturing request/response metadata</li>
 * <li><strong>Correlation IDs</strong> - Request and transaction IDs for distributed tracing</li>
 * <li><strong>Body Capture</strong> - Request/response body logging with content type filtering</li>
 * <li><strong>Sensitive Data Masking</strong> - Automatic masking of passwords and secrets</li>
 * <li><strong>Conditional Logging</strong> - Content type and path-based filtering</li>
 * <li><strong>MDC Integration</strong> - SLF4J MDC population for structured logging</li>
 * <li><strong>Kibana/ELK Support</strong> - Pre-configured fields for log aggregation platforms</li>
 * </ul>
 *
 * <h2>Architecture Overview</h2>
 * The logging framework consists of four main subsystems:
 *
 * <h3>1. Filter Chain ({@link io.github.jframe.logging.filter})</h3>
 * Ordered servlet filters that execute in sequence to capture and log HTTP traffic:
 * <ol>
 * <li><strong>RequestIdFilter</strong> (order: -400) - Extracts or generates request IDs for correlation</li>
 * <li><strong>TransactionIdFilter</strong> (order: -300) - Manages transaction IDs for distributed tracing</li>
 * <li><strong>RequestDurationFilter</strong> (order: -200) - Tracks request processing duration</li>
 * <li><strong>RequestResponseLogFilter</strong> (order: -100) - Logs request/response details with body capture</li>
 * </ol>
 *
 * <h3>2. Voting Mechanism ({@link io.github.jframe.logging.voter})</h3>
 * Determines whether logging should be active for a given request based on:
 * <ul>
 * <li><strong>Media Type Voting</strong> - Content type must be in allowed list (e.g., JSON, XML)</li>
 * <li><strong>Request Path Voting</strong> - Path must not match exclusion patterns (e.g., /actuator/**, /health)</li>
 * </ul>
 *
 * <h3>3. Sensitive Data Masking ({@link io.github.jframe.logging.masker})</h3>
 * Redacts sensitive information before logging:
 * <ul>
 * <li><strong>JSON Masking</strong> - Masks password fields in JSON payloads</li>
 * <li><strong>URI Masking</strong> - Masks sensitive query parameters</li>
 * <li><strong>Configurable Fields</strong> - Define which field names trigger masking (password, apiKey, token, etc.)</li>
 * <li><strong>Format Support</strong> - Extensible masker strategy for different data formats</li>
 * </ul>
 *
 * <h3>4. Structured Logging ({@link io.github.jframe.logging.kibana})</h3>
 * MDC-based structured logging for log aggregation platforms:
 * <ul>
 * <li>Request ID, Transaction ID, User ID</li>
 * <li>HTTP method, URI, status code</li>
 * <li>Request/response timestamps and duration</li>
 * <li>Client IP, User-Agent</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * Complete configuration via application properties:
 * <pre>
 * jframe:
 * logging:
 * # Global logging control
 * disabled: false
 *
 * # Response body length limit (-1 for unlimited)
 * response-length: -1
 *
 * # Content types that trigger logging
 * allowed-content-types:
 * - application/json
 * - application/xml
 * - text/xml
 * - text/plain
 *
 * # Paths to exclude from logging (Ant-style patterns)
 * exclude-paths:
 * - method: GET
 * path: /actuator/**
 * - method: "*"
 * path: /health
 *
 * # Field names to mask in logs
 * fields-to-mask:
 * - password
 * - passwd
 * - apiKey
 * - authorization
 * - secret
 * - token
 *
 * # Individual filter configuration
 * filters:
 * request-id:
 * enabled: true
 * order: -400
 * transaction-id:
 * enabled: true
 * order: -300
 * request-duration:
 * enabled: true
 * order: -200
 * request-response-log:
 * enabled: true
 * order: -100
 * </pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Accessing Correlation IDs in Application Code</h3>
 * <pre>
 * import org.slf4j.MDC;
 * import static io.github.jframe.logging.kibana.KibanaLogFields.*;
 *
 * public class OrderService {
 * public void processOrder(Order order) {
 * String requestId = MDC.get(REQUEST_ID);
 * String txId = MDC.get(TX_ID);
 *
 * log.info("Processing order {} for request {}", order.getId(), requestId);
 * // Correlation IDs are automatically included in logs
 * }
 * }
 * </pre>
 *
 * <h3>Custom Masker for XML Format</h3>
 * <pre>
 * {@literal @}Component
 * public class XmlPasswordMasker implements Masker {
 * {@literal @}Override
 * public boolean matches(MaskedPasswordBuilder builder) {
 * // Implement XML password detection and masking
 * // Return true if masking occurred
 * }
 * }
 * </pre>
 *
 * <h3>Custom Filter Voter</h3>
 * <pre>
 * {@literal @}Component
 * public class CustomLoggingVoter {
 * private final FilterVoter filterVoter;
 *
 * public boolean shouldLog(HttpServletRequest request) {
 * // Combine default voting with custom logic
 * return filterVoter.enabled(request) && !isInternalRequest(request);
 * }
 * }
 * </pre>
 *
 * <h2>Log Output Example</h2>
 * With structured logging enabled, logs will include correlation metadata:
 * <pre>
 * {
 * "timestamp": "2025-01-15T10:30:45.123Z",
 * "level": "INFO",
 * "message": "Processing order 12345",
 * "requestId": "req-abc123",
 * "txId": "tx-xyz789",
 * "method": "POST",
 * "uri": "/api/orders",
 * "statusCode": 200,
 * "duration": 125
 * }
 * </pre>
 *
 * <h2>Request/Response Body Logging</h2>
 * Request and response bodies are automatically captured and logged when:
 * <ul>
 * <li>Content type matches allowed types (JSON, XML, text)</li>
 * <li>Path is not excluded</li>
 * <li>Body size is within configured limit</li>
 * </ul>
 *
 * Sensitive fields are automatically masked:
 * <pre>
 * // Original request body
 * {"username": "john", "password": "secret123"}
 *
 * // Logged request body
 * {"username": "john", "password": "***"}
 * </pre>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 * <li><strong>Body Capture</strong> - Uses wrapper classes for multiple reads without performance degradation</li>
 * <li><strong>Masking</strong> - Character-by-character processing; consider response-length limits for large payloads</li>
 * <li><strong>Conditional Logging</strong> - Voter results are cached per request to avoid redundant evaluations</li>
 * <li><strong>MDC Cleanup</strong> - Filters automatically clear MDC after request completion</li>
 * </ul>
 *
 * <h2>Security Best Practices</h2>
 * <ul>
 * <li>Always configure {@code fields-to-mask} with your application's sensitive field names</li>
 * <li>Use {@code exclude-paths} to prevent logging of authentication endpoints</li>
 * <li>Set {@code response-length} limits to prevent logging of large binary responses</li>
 * <li>Review {@code allowed-content-types} to exclude binary formats (images, PDFs, etc.)</li>
 * <li>Masking is case-insensitive (matches "password", "Password", "PASSWORD")</li>
 * <li>Masked values ({@code "***"}) are not reversible</li>
 * </ul>
 *
 * <h2>Integration with OpenTelemetry</h2>
 * When using {@code starter-otlp}, logging is enhanced with:
 * <ul>
 * <li>Trace ID propagation to logs</li>
 * <li>Span ID correlation</li>
 * <li>Custom spans for filter execution</li>
 * <li>Distributed tracing across microservices</li>
 * </ul>
 *
 * <h2>Testing Support</h2>
 * Filters implement {@link io.github.jframe.logging.filter.MockMvcFilter} for easy inclusion in MockMvc tests:
 * <pre>
 * mockMvc.perform(post("/api/orders")
 * .contentType(MediaType.APPLICATION_JSON)
 * .content(orderJson))
 * .andExpect(status().isOk());
 *
 * // Request ID and transaction ID are automatically generated in tests
 * </pre>
 *
 * <h2>Package Structure</h2>
 * <ul>
 * <li><strong>{@link io.github.jframe.logging.filter}</strong> - Filter infrastructure and base classes</li>
 * <li><strong>{@link io.github.jframe.logging.filter.config}</strong> - Individual filter configurations</li>
 * <li><strong>{@link io.github.jframe.logging.filter.type}</strong> - Concrete filter implementations</li>
 * <li><strong>{@link io.github.jframe.logging.voter}</strong> - Conditional logging voters</li>
 * <li><strong>{@link io.github.jframe.logging.masker}</strong> - Sensitive data masking strategies</li>
 * <li><strong>{@link io.github.jframe.logging.kibana}</strong> - Structured logging field constants</li>
 * <li><strong>{@link io.github.jframe.logging.wrapper}</strong> - Request/response wrapper classes</li>
 * <li><strong>{@link io.github.jframe.logging.model}</strong> - Configuration models</li>
 * </ul>
 *
 * @see io.github.jframe.autoconfigure.CoreAutoConfiguration Auto-configuration entry point
 * @see io.github.jframe.autoconfigure.properties.LoggingProperties Configuration properties
 */
package io.github.jframe.logging;
