package io.github.jframe.logging;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.util.HttpBodyUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;

import static io.github.jframe.OpenTelemetryConstants.Logging.REQUEST_PREFIX;
import static io.github.jframe.OpenTelemetryConstants.Logging.RESPONSE_PREFIX;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;
import static io.github.jframe.util.constants.Constants.Headers.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utility class for logging HTTP requests and responses in a reactive context.
 */
@Slf4j
@SuppressWarnings("MultipleStringLiterals")
public final class HttpLogger {

    private HttpLogger() {
        // Utility class, no instantiation
    }

    /**
     * Logs the details of an HTTP request, including method, URI, headers, and body for a reactive client request.
     *
     * @param response       the ClientResponse to log
     * @param responseLength the maximum response length to log
     * @param passwordMasker the password masker to use for masking sensitive fields
     * @return a Mono that emits the logged ClientResponse
     */
    public static Mono<ClientResponse> logReactiveResponse(final ClientResponse response, final int responseLength,
        final PasswordMasker passwordMasker) {
        return response.bodyToMono(DataBuffer.class)
            .switchIfEmpty(Mono.defer(() -> {
                final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
                return Mono.just(bufferFactory.wrap(new byte[0]));
            }))
            .map(dataBuffer -> {
                try {
                    final String bodyContent = dataBuffer.toString(UTF_8);
                    logResponse(
                        response.statusCode(),
                        response.headers().asHttpHeaders(),
                        HttpBodyUtil.compressAndMaskBody(bodyContent, responseLength, passwordMasker)
                    );
                    return ClientResponse.create(response.statusCode(), response.strategies())
                        .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                        .body(Flux.just(dataBuffer))
                        .request(response.request())
                        .build();
                } catch (final Exception e) {
                    log.warn("Failed to log response body: {}", e.getMessage());
                    logResponse(response.statusCode(), response.headers().asHttpHeaders(), "[ERROR_READING_BODY]");
                    return response;
                }
            })
            .onErrorResume(throwable -> {
                log.warn("Failed to read response body for logging: {}", throwable.getMessage());
                logResponse(response.statusCode(), response.headers().asHttpHeaders(), "[ERROR_READING_BODY]");
                return Mono.just(response);
            });
    }

    /**
     * Logs the details of an HTTP response, including status code, headers, and body for a non-reactive context.
     *
     * @param statusCode the HTTP status code of the response
     * @param headers    the HTTP headers of the response
     * @param body       the body of the response, can be null or empty
     */
    public static void logResponse(final HttpStatusCode statusCode, final HttpHeaders headers, final String body) {
        if (!log.isDebugEnabled()) {
            return;
        }

        final StringBuilder message = new StringBuilder();
        message.append(RESPONSE_PREFIX).append('\n');
        message.append("Status: ").append(statusCode).append('\n');
        message.append("Headers:\n");

        appendTraceHeaders(message);
        appendHeaders(message, headers);
        appendBodyIfPresent(message, body);

        message.setLength(message.length() - 1);
        log.debug(message.toString());
    }

    /**
     * Logs the details of an HTTP request, including method, URI, headers, and body for a non-reactive context.
     *
     * @param method  the HTTP method of the request (e.g., GET, POST)
     * @param uri     the URI of the request
     * @param headers the HTTP headers of the request
     */
    public static void logRequest(final HttpMethod method, final URI uri, final HttpHeaders headers) {
        if (!log.isDebugEnabled()) {
            return;
        }

        final StringBuilder message = new StringBuilder();
        message.append(REQUEST_PREFIX).append('\n');
        message.append("Request: ").append(method).append(' ').append(uri).append('\n');
        message.append("Headers:\n");
        appendHeaders(message, headers);

        message.setLength(message.length() - 1);
        log.debug(message.toString());
    }

    private static void appendHeaders(final StringBuilder builder, final HttpHeaders headers) {
        headers.forEach(
            (key, values) -> builder.append('\t').append(key).append(": ").append(String.join(", ", values)).append('\n')
        );
    }

    private static void appendBodyIfPresent(final StringBuilder builder, final String body) {
        if (isBlank(body)) {
            return;
        }

        builder.append("Body:\n");
        body.lines().forEach(line -> builder.append('\t').append(line).append('\n'));
    }

    private static void appendTraceHeaders(final StringBuilder builder) {
        builder.append('\t').append(TX_ID_HEADER).append(": ").append(KibanaLogFields.get(TX_ID)).append('\n');
        builder.append('\t').append(REQ_ID_HEADER).append(": ").append(KibanaLogFields.get(REQUEST_ID)).append('\n');
        builder.append('\t').append(TRACE_ID_HEADER).append(": ").append(KibanaLogFields.get(TRACE_ID)).append('\n');
    }
}
