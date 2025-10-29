package io.github.jframe.logging.logger;

import io.github.jframe.autoconfigure.properties.LoggingProperties;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.util.HttpBodyUtil;
import io.github.jframe.logging.wrapper.WrappedContentCachingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpRetryException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import static io.github.jframe.util.constants.Constants.Characters.SYSTEM_NEW_LINE;
import static java.nio.charset.StandardCharsets.UTF_8;
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
        return maskPasswords(getPostBody(servletRequest));
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
        return maskPasswords(toString(servletResponse.getContentAsByteArray(), characterEncoding));
    }

    /**
     * Get the call request body. With password masking.
     *
     * @param body The http request body.
     * @return The body.
     */
    public String getCallRequestBody(final byte[] body) {
        return maskPasswords(toString(body, Charset.defaultCharset()));
    }

    /**
     * Get the call response body. With password masking.
     *
     * @param response The http response.
     * @return The body.
     */
    public String getCallResponseBody(final ClientHttpResponse response) {
        final StringBuilder inputStringBuilder = new StringBuilder();
        return maskPasswords(getResponseBody(inputStringBuilder, response));
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
            if (parameterValues != null) {
                for (final String value : parameterValues) {
                    stringBuilder.append(parameterName).append('=').append(value).append('\n');
                }
            }
        }
        return stringBuilder.toString();
    }

    private String getResponseBody(final StringBuilder inputStringBuilder, final ClientHttpResponse response) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(response.getBody(), UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                inputStringBuilder.append(line).append(SYSTEM_NEW_LINE);
                line = bufferedReader.readLine();
            }
        } catch (final HttpRetryException exception) {
            log.warn("Got retry exception.");
            log.trace("Stacktrace is: ", exception);
        } catch (final IOException exception) {
            log.warn("Could not get response body.", exception);
        }

        return HttpBodyUtil.compressAndMaskBody(
            inputStringBuilder.toString(),
            loggingProperties.getResponseLength(),
            passwordMasker
        );
    }

    private String maskPasswords(final String input) {
        return passwordMasker.maskPasswordsIn(input);
    }
}
