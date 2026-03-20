package io.github.jframe.logging;

import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.core.MultivaluedMap;

import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.LINE_BREAK;
import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.REQUEST_PREFIX;
import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.RESPONSE_PREFIX;
import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.TAB;

/**
 * Utility class for logging outbound HTTP requests and responses in JAX-RS client filters.
 *
 * <p>Logging is performed at DEBUG level only. All methods are no-ops when DEBUG is disabled.
 * Intended to be used by JAX-RS {@link jakarta.ws.rs.client.ClientRequestFilter} and
 * {@link jakarta.ws.rs.client.ClientResponseFilter} implementations such as
 * {@link io.github.jframe.tracing.filter.OutboundTracingFilter}.
 */
@Slf4j
public final class HttpLogger {

    private HttpLogger() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Logs an outbound HTTP request at DEBUG level.
     *
     * <p>Logs the HTTP method, URI, and request headers. Does nothing if DEBUG logging is disabled.
     *
     * @param method  the HTTP method (e.g. {@code GET}, {@code POST})
     * @param uri     the full URI of the outbound request
     * @param headers the outbound request headers
     */
    public static void logRequest(final String method, final String uri,
        final MultivaluedMap<String, Object> headers) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(
            "{}Method: {}{}URI: {}{}Headers: {}",
            REQUEST_PREFIX,
            method,
            LINE_BREAK + TAB,
            uri,
            LINE_BREAK + TAB,
            headers
        );
    }

    /**
     * Logs an inbound HTTP response at DEBUG level (without body).
     *
     * <p>Logs the HTTP status code and response headers. Does nothing if DEBUG logging is disabled.
     *
     * @param status  the HTTP response status code
     * @param headers the response headers
     */
    public static void logResponse(final int status, final MultivaluedMap<String, String> headers) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(
            "{}Status: {}{}Headers: {}",
            RESPONSE_PREFIX,
            status,
            LINE_BREAK + TAB,
            headers
        );
    }

    /**
     * Logs an inbound HTTP response at DEBUG level (with body).
     *
     * <p>Logs the HTTP status code, response headers, and body content.
     * Does nothing if DEBUG logging is disabled.
     *
     * @param status  the HTTP response status code
     * @param headers the response headers
     * @param body    the response body as a string; may be {@code null} or empty
     */
    public static void logResponse(final int status, final MultivaluedMap<String, String> headers,
        final String body) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(
            "{}Status: {}{}Headers: {}{}Body: {}",
            RESPONSE_PREFIX,
            status,
            LINE_BREAK + TAB,
            headers,
            LINE_BREAK + TAB,
            body
        );
    }
}
