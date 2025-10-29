package io.github.jframe.logging.logger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static io.github.jframe.util.IndentUtil.indent;
import static io.github.jframe.util.constants.Constants.Characters.SYSTEM_NEW_LINE;
import static java.lang.String.format;

/**
 * Utility for logging requests / responses.
 *
 * <p>The utility can be used to generate HTTP request / response log strings. Both for incoming
 * service calls as outgoing calls (i.e. calls to backend systems).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRequestResponseDebugLogger {

    /**
     * Create a request line for the {@code requestUri} and {@code protocol}.
     *
     * <p>For example: {@code GET /doc/test.html HTTP/1.1}.
     */
    private static String createRequestLine(final String method, final String request, final String protocol) {
        return format("%s %s %s", method, request, protocol);
    }

    /** Create log string where the parts have been masked already. */
    private static String createLogString(final String requestLine, final String headers, final String body) {
        final StringBuilder builder = new StringBuilder();
        if (requestLine != null) {
            builder.append(requestLine).append(SYSTEM_NEW_LINE);
        }
        if (headers != null && !headers.isEmpty()) {
            builder.append(headers);
        }
        if (body != null && !body.isEmpty()) {
            builder.append(SYSTEM_NEW_LINE).append(body);
        }
        final String value = builder.toString();

        return indent(value);
    }

    /**
     * Create a servlet request log output, containing the request line, headers and body.
     *
     * @param servletRequest The request.
     * @param headers        The headers.
     * @param body           The body.
     * @return a formatted multi-line string with the HTTP request.
     */
    public String getTxRequestDebugOutput(
        final HttpServletRequest servletRequest, final String headers, final String body) {
        final String requestLine =
            createRequestLine(
                servletRequest.getMethod(),
                servletRequest.getRequestURI(),
                servletRequest.getProtocol()
            );
        return createLogString(requestLine, headers, body);
    }

    /**
     * Create a servlet response log output, containing the request line, headers and body.
     *
     * @param protocol   The request's protocol.
     * @param httpStatus The http status.
     * @param headers    The headers.
     * @param body       The body.
     * @return a formatted multi-line string with the HTTP response.
     */
    public String getTxResponseDebugOutput(
        final String protocol, final HttpStatus httpStatus, final String headers, final String body) {

        final String statusLine = format("%s %s", protocol, httpStatus);
        return createLogString(statusLine, headers, body);
    }

    /**
     * Create a http request log output, containing the request line, headers and body.
     *
     * @param method     The method.
     * @param requestUri The request URI.
     * @param headers    The headers.
     * @param body       The body.
     * @return a formatted multi-line string with the HTTP request.
     */
    public String getCallRequestDebugOutput(final HttpMethod method, final String requestUri, final String headers, final String body) {
        return getCallRequestDebugOutput(method.name(), requestUri, headers, body);
    }

    /**
     * Create a http request log output, containing the request line, headers and body.
     *
     * @param method     The method.
     * @param requestUri The request URI.
     * @param headers    The headers.
     * @param body       The body.
     * @return a formatted multi-line string with the HTTP request.
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public String getCallRequestDebugOutput(final String method, final String requestUri, final String headers, final String body) {
        final String requestLine = createRequestLine(method, requestUri, "");
        return createLogString(requestLine, headers, body);
    }

    /**
     * Create a http response log output.
     *
     * @param headers The headers.
     * @param body    The body.
     * @return a formatted multi-line string with the HTTP response.
     */
    public String getCallResponseDebugOutput(final String headers, final String body) {
        return createLogString(null, headers, body);
    }
}
