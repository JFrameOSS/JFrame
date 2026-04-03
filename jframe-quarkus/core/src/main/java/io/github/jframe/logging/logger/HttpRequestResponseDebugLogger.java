package io.github.jframe.logging.logger;

/**
 * Formats HTTP request and response details as human-readable debug output strings.
 *
 * <p>This is a stateless helper — no dependencies are required. Each method assembles
 * the first line (method + URI, or status + reason), followed by an optional headers block
 * and an optional blank line + body block.
 */
public class HttpRequestResponseDebugLogger {

    /** Space separator used between first-line tokens. */
    private static final String SPACE = " ";

    /**
     * Formats an HTTP request for debug logging.
     *
     * <p>Format:
     * <pre>
     * METHOD URI
     * Header: value
     *
     * body content
     * </pre>
     *
     * @param method     the HTTP method (e.g. {@code GET})
     * @param requestUri the request URI
     * @param headers    formatted headers string (may be null or empty)
     * @param body       request body (may be null or empty)
     * @return formatted debug string
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public String getRequestDebugOutput(
        final String method,
        final String requestUri,
        final String headers,
        final String body) {
        final String statusLine = method + SPACE + requestUri;
        return buildOutput(statusLine, headers, body);
    }

    /**
     * Formats an HTTP response for debug logging.
     *
     * <p>Format:
     * <pre>
     * STATUS REASON
     * Header: value
     *
     * body content
     * </pre>
     *
     * @param status       the HTTP status code
     * @param reasonPhrase the reason phrase (e.g. {@code OK})
     * @param headers      formatted headers string (may be null or empty)
     * @param body         response body (may be null or empty)
     * @return formatted debug string
     */
    public String getResponseDebugOutput(
        final int status,
        final String reasonPhrase,
        final String headers,
        final String body) {
        final String statusLine = status + SPACE + reasonPhrase;
        return buildOutput(statusLine, headers, body);
    }

    private static String buildOutput(final String firstLine, final String headers, final String body) {
        final StringBuilder builder = new StringBuilder();
        builder.append(firstLine).append(System.lineSeparator());

        final boolean hasHeaders = headers != null && !headers.isEmpty();
        final boolean hasBody = body != null && !body.isEmpty();

        if (hasHeaders) {
            builder.append(headers);
        }
        if (hasBody) {
            builder.append(System.lineSeparator()).append(body);
        }

        return builder.toString();
    }
}
