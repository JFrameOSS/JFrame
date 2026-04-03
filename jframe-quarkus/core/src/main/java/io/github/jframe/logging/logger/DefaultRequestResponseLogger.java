package io.github.jframe.logging.logger;

import io.github.jframe.logging.ecs.EcsFieldNames;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.ecs.LogTypeNames;
import io.github.jframe.logging.voter.MediaTypeVoter;
import io.github.jframe.logging.wrapper.CachingRequestContext;
import io.github.jframe.logging.wrapper.CachingResponseContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import static io.github.jframe.logging.ecs.EcsFieldNames.HTTP_STATUS;
import static io.github.jframe.logging.ecs.EcsFieldNames.LOG_TYPE;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_REQUEST_BODY;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_REQUEST_HEADERS;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_REQUEST_METHOD;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_REQUEST_SIZE;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_REQUEST_URI;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_RESPONSE_BODY;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_RESPONSE_HEADERS;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_RESPONSE_SIZE;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_STATUS;

/**
 * Default JAX-RS implementation of {@link RequestResponseLogger}.
 *
 * <p>Logs incoming requests and outgoing responses with MDC/ECS field population,
 * content-type filtering, password masking, and structured debug output.
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultRequestResponseLogger implements RequestResponseLogger {

    private static final String UNKNOWN_URI = "unknown";

    /** The util to use for generating request / response headers log statements. */
    private final HttpRequestResponseHeadersLogger headersLogger;

    /** The util to use for generating request / response body log statements. */
    private final HttpRequestResponseBodyLogger bodyLogger;

    /** The util to use for generating request / response debug log statements. */
    private final HttpRequestResponseDebugLogger debugLogger;

    /** The media type voter to allow request calls to be logged. */
    private final MediaTypeVoter mediaTypeVoter;

    /** The media type voter to suppress body contents for. */
    private final MediaTypeVoter bodyExcludedMediaTypeVoter;

    @Override
    public void logRequest(final CachingRequestContext request) {
        final String method = request.getMethod();
        final String requestUri = extractPath(request.getUriInfo());
        final MediaType mediaType = request.getMediaType();
        final String contentType = mediaTypeToString(mediaType);
        final int contentLength = request.getLength();

        try {
            final String requestHeaders = headersLogger.getRequestHeaders(request);

            EcsFields.tag(LOG_TYPE, LogTypeNames.REQUEST_BODY);
            EcsFields.tag(TX_REQUEST_METHOD, method);
            EcsFields.tag(TX_REQUEST_URI, requestUri);
            EcsFields.tag(TX_REQUEST_SIZE, contentLength);
            EcsFields.tag(TX_REQUEST_HEADERS, requestHeaders);

            final boolean contentTypeCanBeLogged = contentTypeCanBeLogged(contentType);

            String requestBody = "";
            if (contentTypeCanBeLogged) {
                requestBody = bodyLogger.getRequestBody(request);
            }
            addBodyTag(contentTypeCanBeLogged, TX_REQUEST_BODY, requestBody);

            log.info(
                "Invoked '{} {}' with content type '{}' and size of '{}' bytes.",
                method,
                requestUri,
                contentType,
                contentLength
            );

            if (log.isDebugEnabled()) {
                log.debug(
                    "Request is:\n{}",
                    debugLogger.getRequestDebugOutput(method, requestUri, requestHeaders, requestBody)
                );
            }
        } finally {
            EcsFields.clear(LOG_TYPE, TX_REQUEST_METHOD, TX_REQUEST_SIZE, TX_REQUEST_HEADERS, TX_REQUEST_BODY);
        }
    }

    @Override
    public void logResponse(final ContainerRequestContext request, final CachingResponseContext response) {
        try {
            EcsFields.tag(LOG_TYPE, LogTypeNames.RESPONSE_BODY);

            final int status = response.getStatus();
            final Response.Status statusEnum = Response.Status.fromStatusCode(status);
            final String reasonPhrase = statusEnum != null ? statusEnum.getReasonPhrase() : "Unknown";

            EcsFields.tag(HTTP_STATUS, status);

            if (EcsFields.get(TX_STATUS) == null) {
                EcsFields.tag(TX_STATUS, String.valueOf(status));
            }

            final int contentLength = response.getContentLength();
            EcsFields.tag(TX_RESPONSE_SIZE, contentLength);

            final String responseHeaders = headersLogger.getResponseHeaders(response);
            EcsFields.tag(TX_RESPONSE_HEADERS, responseHeaders);

            final MediaType mediaType = response.getMediaType();
            final String contentType = mediaTypeToString(mediaType);
            final boolean contentTypeCanBeLogged = contentTypeCanBeLogged(contentType);

            String responseBody = "";
            if (contentTypeCanBeLogged) {
                responseBody = bodyLogger.getResponseBody(response);
            }
            addBodyTag(contentTypeCanBeLogged, TX_RESPONSE_BODY, responseBody);

            final String requestUri = extractPath(request.getUriInfo());
            log.info(
                "Response '{}' is '{}' with content type '{}' and size of '{}' bytes.",
                requestUri,
                status + " " + reasonPhrase,
                contentType,
                contentLength
            );

            if (log.isDebugEnabled()) {
                log.debug(
                    "Response is:\n{}",
                    debugLogger.getResponseDebugOutput(status, reasonPhrase, responseHeaders, responseBody)
                );
            }
        } finally {
            EcsFields.clear(LOG_TYPE, TX_RESPONSE_SIZE, TX_RESPONSE_HEADERS, TX_RESPONSE_BODY);
        }
    }

    private static String extractPath(final UriInfo uriInfo) {
        return uriInfo != null ? uriInfo.getPath() : UNKNOWN_URI;
    }

    private static String mediaTypeToString(final MediaType mediaType) {
        return mediaType != null ? mediaType.toString() : null;
    }

    private boolean contentTypeCanBeLogged(final String contentType) {
        final boolean allowed = mediaTypeVoter.matches(contentType);
        final boolean excluded = bodyExcludedMediaTypeVoter.matches(contentType);
        return allowed && !excluded;
    }

    private static void addBodyTag(
        final boolean contentTypeCanBeLogged,
        final EcsFieldNames tag,
        final String body) {
        if (contentTypeCanBeLogged) {
            EcsFields.tag(tag, body);
        } else {
            EcsFields.tag(tag, "invalid mime type for logging");
        }
    }
}
