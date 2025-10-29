package io.github.jframe.logging.logger;

import io.github.jframe.logging.masker.type.PasswordMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import static io.github.jframe.util.constants.Constants.Characters.SYSTEM_NEW_LINE;

/**
 * Utility for logging requests / responses.
 *
 * <p>The utility can be used to generate HTTP request / response header log strings. Both for
 * incoming service calls as outgoing calls (i.e. calls to backend systems).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRequestResponseHeadersLogger {

    /** Masks passwords in json strings. */
    private final PasswordMasker passwordMasker;

    /**
     * Get request headers. With password masking.
     *
     * @param servletRequest The servlet request.
     * @return The headers as string.
     */
    public String getTxRequestHeaders(final HttpServletRequest servletRequest) {
        final HttpHeaders headers = creatHttpHeaders(servletRequest);
        return createHeadersAsString(headers);
    }

    /**
     * Get response headers. With password masking.
     *
     * @param servletResponse The servlet response.
     * @return The headers as string.
     */
    public String getTxResponseHeaders(final HttpServletResponse servletResponse) {
        final HttpHeaders headers = creatHttpHeaders(servletResponse);
        return createHeadersAsString(headers);
    }

    /** Create {@link HttpHeaders} for the {@code request}. */
    private static HttpHeaders creatHttpHeaders(final HttpServletRequest request) {
        final HttpHeaders headers = new HttpHeaders();

        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            final Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                headers.add(headerName, values.nextElement());
            }
        }

        return headers;
    }

    /** Create {@link HttpHeaders} for the {@code response}. */
    private static HttpHeaders creatHttpHeaders(final HttpServletResponse response) {
        final HttpHeaders headers = new HttpHeaders();
        for (final String headerName : response.getHeaderNames()) {
            for (final String headerValue : response.getHeaders(headerName)) {
                headers.add(headerName, headerValue);
            }
        }
        return headers;
    }

    /**
     * Get call request headers. With password masking.
     *
     * @param request The http request.
     * @return The headers as string.
     */
    public String getCallRequestHeaders(final HttpRequest request) {
        final HttpHeaders headers = request.getHeaders();
        return createHeadersAsString(headers);
    }

    /**
     * Get call response headers. With password masking.
     *
     * @param response The http response.
     * @return The headers as string.
     */
    public String getCallResponseHeaders(final ClientHttpResponse response) {
        final HttpHeaders headers = response.getHeaders();
        return createHeadersAsString(headers);
    }

    private String createHeadersAsString(final HttpHeaders headers) {
        final StringBuilder builder = new StringBuilder();
        appendHeaders(builder, headers);
        final String value = builder.toString();

        // remove clear text password values and indent the multi line body.
        return passwordMasker.maskPasswordsIn(value);
    }

    private static void appendHeaders(final StringBuilder builder, final HttpHeaders headers) {
        final List<String> headerNames = new ArrayList<>(headers.keySet());
        Collections.sort(headerNames);

        for (final String headerName : headerNames) {
            builder
                .append(headerName)
                .append(": ")
                .append(String.join(", ", headers.get(headerName)))
                .append(SYSTEM_NEW_LINE);
        }
    }
}
