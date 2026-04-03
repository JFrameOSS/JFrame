package io.github.jframe.logging.logger;

import io.github.jframe.logging.masker.type.PasswordMasker;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Extracts and formats HTTP headers for logging.
 *
 * <p>Headers are sorted alphabetically, multi-valued headers are joined with {@code ", "},
 * and password masking is applied on the entire formatted string.
 */
@RequiredArgsConstructor
public class HttpRequestResponseHeadersLogger {

    /** Masks sensitive field values in header strings. */
    private final PasswordMasker passwordMasker;

    /**
     * Returns the formatted and masked request headers.
     *
     * @param requestContext the container request context
     * @return formatted headers string, or empty string if no headers are present
     */
    public String getRequestHeaders(final ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        return formatAndMask(headers);
    }

    /**
     * Returns the formatted and masked response headers.
     *
     * @param responseContext the container response context
     * @return formatted headers string, or empty string if no headers are present
     */
    public String getResponseHeaders(final ContainerResponseContext responseContext) {
        final MultivaluedMap<String, String> headers = responseContext.getStringHeaders();
        return formatAndMask(headers);
    }

    private String formatAndMask(final MultivaluedMap<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        final String formatted = buildHeaderString(headers);
        return passwordMasker.maskPasswordsIn(formatted);
    }

    private static String buildHeaderString(final MultivaluedMap<String, String> headers) {
        final StringBuilder builder = new StringBuilder();
        final List<String> headerNames = new ArrayList<>(headers.keySet());
        Collections.sort(headerNames);

        for (final String name : headerNames) {
            final List<String> values = headers.get(name);
            builder.append(name)
                .append(": ")
                .append(String.join(", ", values))
                .append('\n');
        }

        return builder.toString();
    }
}
