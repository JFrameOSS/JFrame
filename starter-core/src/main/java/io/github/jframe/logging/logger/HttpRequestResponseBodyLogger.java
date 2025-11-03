package io.github.jframe.logging.logger;

import io.github.jframe.autoconfigure.properties.LoggingProperties;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.util.HttpBodyUtil;
import io.github.jframe.logging.wrapper.BufferedClientHttpResponse;
import io.github.jframe.logging.wrapper.WrappedContentCachingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Utility for logging requests / responses.
 *
 * <p>The utility can be used to generate HTTP request / response log strings. Both for incoming
 * service calls as outgoing calls (i.e. calls to backend systems).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRequestResponseBodyLogger {

    /** Masks passwords in json, xml and query strings. */
    private final PasswordMasker passwordMasker;

    /** The logging properties. */
    private final LoggingProperties loggingProperties;

    /**
     * Get the request body. With password masking.
     *
     * @param servletRequest The servlet request.
     * @return The body.
     * @throws IOException In case the body could not be read.
     */
    public String getTxRequestBody(final HttpServletRequest servletRequest) throws IOException {
        return passwordMasker.maskPasswordsIn(getPostBody(servletRequest));
    }

    /**
     * Get the response body. With password masking.
     *
     * @param servletResponse The servlet response.
     * @return The body.
     */
    public String getTxResponseBody(final WrappedContentCachingResponse servletResponse) {
        String characterEncoding = servletResponse.getCharacterEncoding();
        if (APPLICATION_JSON_VALUE.equals(servletResponse.getContentType())) {
            characterEncoding = "UTF-8";
        }
        if (characterEncoding == null || characterEncoding.isEmpty()) {
            characterEncoding = Charset.defaultCharset().name();
        }
        return passwordMasker.maskPasswordsIn(toString(servletResponse.getContentAsByteArray(), characterEncoding));
    }

    /**
     * Get the call request body. With password masking.
     *
     * @param body The http request body.
     * @return The body.
     */
    public String getCallRequestBody(final byte[] body) {
        return passwordMasker.maskPasswordsIn(toString(body, Charset.defaultCharset()));
    }

    /**
     * Get the call response body. With password masking.
     *
     * @param response The http response.
     * @return The body.
     */
    public String getCallResponseBody(final BufferedClientHttpResponse response) {
        return HttpBodyUtil.compressAndMaskBody(
            getResponseBody(response),
            loggingProperties.getResponseLength(),
            passwordMasker
        );
    }

    private static String toString(final byte[] body, final String charset) {
        return toString(body, Charset.forName(charset));
    }

    private static String toString(final byte[] body, final Charset charset) {
        return new String(body, charset);
    }

    private static String getPostBody(final HttpServletRequest servletRequest) throws IOException {
        final String body = IOUtils.toString(servletRequest.getInputStream(), servletRequest.getCharacterEncoding());
        if (StringUtils.isNotBlank(body)) {
            return body;
        }

        return getPostParametersBody(servletRequest);
    }

    private static String getPostParametersBody(final HttpServletRequest request) {
        final Map<String, String[]> parameters = request.getParameterMap();
        return getPostParametersBody(request, parameters);
    }

    private static String getPostParametersBody(final HttpServletRequest request, final Map<String, String[]> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder();
        final List<String> parameterNames = new ArrayList<>(parameters.keySet());
        Collections.sort(parameterNames);
        for (final String parameterName : parameterNames) {
            final String[] parameterValues = request.getParameterValues(parameterName);
            if (nonNull(parameterValues)) {
                for (final String value : parameterValues) {
                    stringBuilder.append(parameterName).append('=').append(value).append('\n');
                }
            }
        }
        return stringBuilder.toString();
    }

    private String getResponseBody(final BufferedClientHttpResponse response) {
        try {
            return response.getBodyAsString();
        } catch (final IOException exception) {
            log.warn("Could not get response body.", exception);
            return "";
        }
    }
}
