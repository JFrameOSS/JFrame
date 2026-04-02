package io.github.jframe.logging.ecs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import static java.util.Objects.nonNull;

/**
 * ECS (Elastic Common Schema) field names used as MDC keys for structured logging,
 * and span attribute keys for OpenTelemetry tracing.
 *
 * <p>All enum constants implement {@link EcsField} and are used both as MDC keys in
 * ECS logs and as OpenTelemetry span attribute keys via {@link #getKey()}.
 * The {@code SPAN_*} constants follow ECS and
 * <a href="https://opentelemetry.io/docs/specs/semconv/">OpenTelemetry semantic conventions</a>.
 *
 * <p>Use {@link #getKey()} to obtain the string key for use in MDC or span attribute calls.
 *
 * <p>{@code SPAN_TRACING_SPAN} is intentionally kept as a {@code public static final String}
 * because it is used as a Spring {@code ClientRequest} attribute key (not a span attribute key).
 */
@SuppressWarnings(
    {
        "checkstyle:MultipleStringLiterals",
        "PMD.ExcessivePublicCount"
    }
)
@Getter
@RequiredArgsConstructor
public enum EcsFieldNames implements EcsField {

    // --- Identity and infrastructure ---

    /** The session id. */
    SESSION_ID("session.id"),

    /** The host name. */
    HOST_NAME("host.name"),

    /** The software version. */
    SOFTWARE_VERSION("service.version"),

    // --- Correlation ---

    /** The request id. */
    REQUEST_ID("request.id"),

    /** The request duration. */
    REQUEST_DURATION("event.duration"),

    /** The business transaction id. */
    BUSINESS_TX_ID("transaction.business_id"),

    /** The trace id. */
    TRACE_ID("trace.id"),

    /** The span id. */
    SPAN_ID("span.id"),

    // --- Inbound transaction (server-side request/response) ---

    /** The transaction id. */
    TX_ID("transaction.id"),

    /** The transaction type. */
    TX_TYPE("transaction.type"),

    /** The transaction request ip address. */
    TX_REQUEST_IP("client.ip"),

    /** The transaction request method. */
    TX_REQUEST_METHOD("http.request.method"),

    /** The transaction request uri. */
    TX_REQUEST_URI("url.path"),

    /** The transaction request size. */
    TX_REQUEST_SIZE("http.request.body.bytes"),

    /** The transaction request headers. */
    TX_REQUEST_HEADERS("http.request.headers"),

    /** The transaction request body. */
    TX_REQUEST_BODY("http.request.body.content"),

    /** The transaction response size. */
    TX_RESPONSE_SIZE("http.response.body.bytes"),

    /** The transaction response headers. */
    TX_RESPONSE_HEADERS("http.response.headers"),

    /** The transaction response body. */
    TX_RESPONSE_BODY("http.response.body.content"),

    /** The transaction duration. */
    TX_DURATION("transaction.duration.ms"),

    /** The transaction status. */
    TX_STATUS("transaction.result"),

    /** The HTTP response status code. */
    HTTP_STATUS("http.response.status_code"),

    // --- Outbound call (client-side request/response) ---

    /** The call id. */
    CALL_ID("span.id"),

    /** The call request method. */
    CALL_REQUEST_METHOD("http.client.request.method"),

    /** The call request uri. */
    CALL_REQUEST_URI("url.full"),

    /** The call request size. */
    CALL_REQUEST_SIZE("http.client.request.body.bytes"),

    /** The call request headers. */
    CALL_REQUEST_HEADERS("http.client.request.headers"),

    /** The call request body. */
    CALL_REQUEST_BODY("http.client.request.body.content"),

    /** The call response size. */
    CALL_RESPONSE_SIZE("http.client.response.body.bytes"),

    /** The call response headers. */
    CALL_RESPONSE_HEADERS("http.client.response.headers"),

    /** The call response body. */
    CALL_RESPONSE_BODY("http.client.response.body.content"),

    /** The call duration. */
    CALL_DURATION("event.duration.ms"),

    /** The call status. */
    CALL_STATUS("event.outcome"),

    // --- Scheduled task ---

    /** The task id. */
    TASK_ID("task.id"),

    // --- User and security ---

    /** The username. */
    USER_NAME("user.name"),

    /** The user roles. */
    USER_ROLES("user.roles"),

    // --- Log metadata ---

    /** The log type / event category. */
    LOG_TYPE("event.category"),

    /** The thread. */
    THREAD("process.thread.name"),

    /** The log level. */
    LEVEL("log.level"),

    /** The timestamp. */
    TIMESTAMP("@timestamp"),

    /** The log origin location. */
    LOG_LOCATION("log.origin"),

    /** The log message. */
    MESSAGE("message"),

    // --- Span attribute keys (OpenTelemetry) ---

    /** Service name span attribute. */
    SPAN_SERVICE_NAME("service.name"),

    /** Service method span attribute. */
    SPAN_SERVICE_METHOD("service.method"),

    /** Remote user / enduser span attribute. */
    SPAN_HTTP_REMOTE_USER("enduser.id"),

    /** Transaction ID span attribute. */
    SPAN_HTTP_TRANSACTION_ID("transaction.id"),

    /** Request ID span attribute. */
    SPAN_HTTP_REQUEST_ID("request.id"),

    /** URL path span attribute. */
    SPAN_HTTP_URI("url.path"),

    /** URL query span attribute. */
    SPAN_HTTP_QUERY("url.query"),

    /** HTTP method span attribute. */
    SPAN_HTTP_METHOD("http.request.method"),

    /** HTTP response status code span attribute. */
    SPAN_HTTP_STATUS_CODE("http.response.status_code"),

    /** HTTP content type span attribute. */
    SPAN_HTTP_CONTENT_TYPE("http.request.mime_type"),

    /** HTTP content length span attribute. */
    SPAN_HTTP_CONTENT_LENGTH("http.request.body.bytes"),

    /** Peer service span attribute. */
    SPAN_PEER_SERVICE("peer.service"),

    /** External request URI span attribute. */
    SPAN_EXT_REQUEST_URI("url.full"),

    /** External request query span attribute. */
    SPAN_EXT_REQUEST_QUERY("url.query"),

    /** External request method span attribute. */
    SPAN_EXT_REQUEST_METHOD("http.request.method"),

    /** External response status code span attribute. */
    SPAN_EXT_RESPONSE_STATUS_CODE("http.response.status_code"),

    /** External response content length span attribute. */
    SPAN_EXT_RESPONSE_CONTENT_LENGTH("http.response.body.bytes"),

    /** External response content type span attribute. */
    SPAN_EXT_RESPONSE_CONTENT_TYPE("http.response.mime_type"),

    /** External response L7 request ID span attribute. */
    SPAN_EXT_RESPONSE_L7_REQUEST_ID("http.response.header.x_request_id");

    /**
     * Tracing span key for {@code ClientRequest} attributes.
     *
     * <p>This is NOT a span attribute key — it is used to store and retrieve {@link io.opentelemetry.api.trace.Span}
     * objects in Spring's {@code ClientRequest.attribute()} / {@code request.getAttributes().get(...)}.
     * Kept as a plain {@code String} constant because {@code ClientRequest.attribute()} takes a {@code String} key.
     */
    public static final String SPAN_TRACING_SPAN = "tracing.span";

    // --- Enum implementation ---

    /** The ECS field key for this entry (used for both MDC and span attributes). */
    private final String key;

    /**
     * Lookup method that does not throw an exception if the specified key is not found.
     *
     * @param key the key to look for
     * @return the EcsFieldNames constant with the given key, or null
     */
    public static EcsFieldNames fromKey(final String key) {
        EcsFieldNames result = null;
        if (nonNull(key)) {
            result = Arrays.stream(values())
                .filter(fieldName -> fieldName.matches(key))
                .findAny()
                .orElse(null);
        }
        return result;
    }
}
