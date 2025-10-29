package io.github.jframe.logging.logger;

import io.github.jframe.logging.kibana.KibanaLogFieldNames;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.kibana.KibanaLogTypeNames;
import io.github.jframe.logging.voter.MediaTypeVoter;
import io.github.jframe.logging.wrapper.ResettableHttpServletRequest;
import io.github.jframe.logging.wrapper.WrappedContentCachingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;

import static io.github.jframe.logging.kibana.KibanaLogCallResultTypes.FAILURE;
import static io.github.jframe.logging.kibana.KibanaLogCallResultTypes.SUCCESS;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;

/** General logger. */
@Slf4j
@RequiredArgsConstructor
public class DefaultRequestResponseLogger implements RequestResponseLogger {

    /** The util to use for generating request / response headers log statements. */
    private final HttpRequestResponseHeadersLogger headersLogUtil;

    /** The util to use for generating request / response body log statements. */
    private final HttpRequestResponseBodyLogger bodyLogUtil;

    /** The util to use for generating request / response debug log statements. */
    private final HttpRequestResponseDebugLogger debugLogUtil;

    /** The media type voter to allow request calls to be logged. */
    private final MediaTypeVoter mediaTypeVoter;

    /** The media type voter to suppress body contents for. */
    private final MediaTypeVoter bodyExcludedMediaTypeVoter;

    @Override
    public void logRequest(final ResettableHttpServletRequest wrappedRequest) throws IOException {
        final String method = wrappedRequest.getMethod();
        final String requestUri = wrappedRequest.getRequestURI();
        final int contentLength = wrappedRequest.getContentLength();
        // contentType can be null (a GET for example, doesn't have a Content-Type header usually)
        final String contentType = getContentType(wrappedRequest);
        final boolean contentTypeCanBeLogged = contentTypeCanBeLogged(contentType);
        String requestHeaders = "";
        String requestBody = "";
        try {
            // This might force the request to be read, hence in the try, so the request can be reset.
            requestHeaders = headersLogUtil.getTxRequestHeaders(wrappedRequest);

            KibanaLogFields.tag(LOG_TYPE, KibanaLogTypeNames.REQUEST_BODY);
            KibanaLogFields.tag(TX_REQUEST_METHOD, method);
            KibanaLogFields.tag(TX_REQUEST_URI, requestUri);
            KibanaLogFields.tag(TX_REQUEST_SIZE, contentLength);
            KibanaLogFields.tag(TX_REQUEST_HEADERS, requestHeaders);
            if (contentTypeCanBeLogged) {
                requestBody = bodyLogUtil.getTxRequestBody(wrappedRequest);
            }
            addBodyTag(contentTypeCanBeLogged, TX_REQUEST_BODY, requestBody);
        } catch (final Throwable throwable) {
            log.error("Error getting payload for request.", throwable);
            throw throwable;
        } finally {
            log.info(
                "Invoked '{} {}' with content type '{}' and size of '{}' bytes.",
                method,
                requestUri,
                contentType,
                contentLength
            );
            if (contentTypeCanBeLogged && log.isDebugEnabled()) {
                log.debug("Request is:\n{}", debugLogUtil.getTxRequestDebugOutput(wrappedRequest, requestHeaders, requestBody));
            }

            // Keep request uri in all other logging!
            KibanaLogFields.clear(LOG_TYPE, TX_REQUEST_METHOD, TX_REQUEST_SIZE, TX_REQUEST_HEADERS, TX_REQUEST_BODY);
            wrappedRequest.reset();
        }
    }

    @Override
    public void logRequest(final HttpRequest request, final byte[] body) {
        try {
            KibanaLogFields.tag(LOG_TYPE, KibanaLogTypeNames.CALL_REQUEST_BODY);

            final HttpMethod method = request.getMethod();
            KibanaLogFields.tag(CALL_REQUEST_METHOD, method.name());

            final String requestUri = request.getURI().toString();
            KibanaLogFields.tag(CALL_REQUEST_URI, requestUri);

            final int contentLength = body.length;
            KibanaLogFields.tag(CALL_REQUEST_SIZE, contentLength);

            final String requestHeaders = headersLogUtil.getCallRequestHeaders(request);
            KibanaLogFields.tag(CALL_REQUEST_HEADERS, requestHeaders);

            // contentType can be null (a GET for example, doesn't have a Content-Type header usually)
            final MediaType contentType = getContentType(request);
            final boolean contentTypeCanBeLogged = contentTypeCanBeLogged(contentType);
            final String requestBody = bodyLogUtil.getCallRequestBody(body);
            addBodyTag(contentTypeCanBeLogged, CALL_REQUEST_BODY, requestBody);

            log.info(
                "Calling '{} {}' with content type '{}' and size of '{}' bytes.",
                method,
                requestUri,
                contentType,
                contentLength
            );
            if (contentTypeCanBeLogged && log.isDebugEnabled()) {
                log.debug(
                    "Call is:\n{}",
                    debugLogUtil.getCallRequestDebugOutput(
                        method,
                        requestUri,
                        requestHeaders,
                        requestBody
                    )
                );
            }
        } finally {
            KibanaLogFields.clear(
                LOG_TYPE,
                CALL_REQUEST_METHOD,
                CALL_REQUEST_URI,
                CALL_REQUEST_SIZE,
                CALL_REQUEST_HEADERS,
                CALL_REQUEST_BODY
            );
        }
    }

    private boolean contentTypeCanBeLogged(final MediaType contentType) {
        return mediaTypeVoter.mediaTypeMatches(contentType)
            && !bodyExcludedMediaTypeVoter.mediaTypeMatches(contentType);
    }

    private boolean contentTypeCanBeLogged(final String contentType) {
        return mediaTypeVoter.mediaTypeMatches(contentType)
            && !bodyExcludedMediaTypeVoter.mediaTypeMatches(contentType);
    }

    @Override
    public void logResponse(final HttpServletRequest servletRequest, final WrappedContentCachingResponse wrappedResponse) {
        try {
            KibanaLogFields.tag(LOG_TYPE, KibanaLogTypeNames.RESPONSE_BODY);

            final HttpStatus httpStatus = HttpStatus.valueOf(wrappedResponse.getStatus());
            KibanaLogFields.tag(HTTP_STATUS, httpStatus.value());

            if (KibanaLogFields.get(TX_STATUS) == null) {
                KibanaLogFields.tag(TX_STATUS, httpStatus);
            }

            final int contentLength = wrappedResponse.getContentSize();
            KibanaLogFields.tag(TX_RESPONSE_SIZE, contentLength);

            final String responseHeaders = headersLogUtil.getTxResponseHeaders(wrappedResponse);
            KibanaLogFields.tag(TX_RESPONSE_HEADERS, responseHeaders);

            final String contentType = getContentType(wrappedResponse);
            final boolean contentTypeCanBeLogged = contentTypeCanBeLogged(contentType);
            final String responseBody = bodyLogUtil.getTxResponseBody(wrappedResponse);
            addBodyTag(contentTypeCanBeLogged, TX_RESPONSE_BODY, responseBody);

            final String requestUri = servletRequest.getRequestURI();

            log.info(
                "Response '{}' is '{}' with content type '{}' and size of '{}' bytes.",
                requestUri,
                httpStatus,
                contentType,
                contentLength
            );
            if (contentTypeCanBeLogged && log.isDebugEnabled()) {
                log.debug(
                    "Response is:\n{}",
                    debugLogUtil.getTxResponseDebugOutput(
                        servletRequest.getProtocol(),
                        httpStatus,
                        responseHeaders,
                        responseBody
                    )
                );
            }
        } finally {
            KibanaLogFields.clear(LOG_TYPE, TX_RESPONSE_SIZE, TX_RESPONSE_HEADERS, CALL_RESPONSE_BODY);
        }
    }

    @Override
    public void logResponse(final ClientHttpResponse response) throws IOException {
        try {
            KibanaLogFields.tag(LOG_TYPE, KibanaLogTypeNames.CALL_RESPONSE_BODY);

            final MediaType contentType = getContentType(response);
            final boolean contentTypeCanBeLogged = contentTypeCanBeLogged(contentType);
            final String responseBody = bodyLogUtil.getCallResponseBody(response);
            addBodyTag(contentTypeCanBeLogged, CALL_RESPONSE_BODY, responseBody);

            final int contentLength = responseBody.length();
            KibanaLogFields.tag(CALL_RESPONSE_SIZE, contentLength);

            final String responseHeaders = headersLogUtil.getCallResponseHeaders(response);
            KibanaLogFields.tag(CALL_RESPONSE_HEADERS, responseHeaders);

            final HttpStatusCode httpStatus = response.getStatusCode();
            if (httpStatus.is2xxSuccessful() || httpStatus.is3xxRedirection()) {
                KibanaLogFields.tag(CALL_STATUS, SUCCESS);
            } else {
                KibanaLogFields.tag(CALL_STATUS, FAILURE);
            }

            log.info(
                "Got response '{}' with content type '{}' and size of '{}' bytes.",
                httpStatus,
                contentType,
                contentLength
            );
            if (contentTypeCanBeLogged && log.isDebugEnabled()) {
                log.debug(
                    "Got response '{}':\n{}",
                    httpStatus,
                    debugLogUtil.getCallResponseDebugOutput(responseHeaders, responseBody)
                );
            }
        } finally {
            KibanaLogFields.clear(
                LOG_TYPE,
                CALL_RESPONSE_SIZE,
                CALL_RESPONSE_HEADERS,
                CALL_RESPONSE_BODY
            );
        }
    }

    private static void addBodyTag(final boolean contentTypeCanBeLogged, final KibanaLogFieldNames tag, final String responseBody) {
        if (contentTypeCanBeLogged) {
            KibanaLogFields.tag(tag, responseBody);
        } else {
            KibanaLogFields.tag(tag, "invalid mime type for logging");
        }
    }

    private static MediaType getContentType(final HttpRequest request) {
        return getContentType(request.getHeaders());
    }

    private static MediaType getContentType(final ClientHttpResponse response) {
        return getContentType(response.getHeaders());
    }

    private static MediaType getContentType(final HttpHeaders httpHeaders) {
        return httpHeaders.getContentType();
    }

    private static String getContentType(final ServletRequest wrappedRequest) {
        return wrappedRequest.getContentType();
    }

    private static String getContentType(final ServletResponse response) {
        return response.getContentType();
    }
}
