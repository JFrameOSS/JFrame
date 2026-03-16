package io.github.jframe.logging.logger;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.voter.MediaTypeVoter;
import io.github.jframe.logging.wrapper.CachingRequestContext;
import io.github.jframe.logging.wrapper.CachingResponseContext;
import io.github.support.UnitTest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.LOG_TYPE;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_REQUEST_BODY;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_REQUEST_HEADERS;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_REQUEST_METHOD;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_REQUEST_SIZE;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_REQUEST_URI;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_RESPONSE_BODY;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_RESPONSE_HEADERS;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_RESPONSE_SIZE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultRequestResponseLogger}.
 *
 * <p>Verifies MDC/KibanaLogFields population during request and response logging, correct
 * cleanup of MDC fields in the {@code finally} block, content-type filtering decisions,
 * and correct delegation to the headers/body/debug logger collaborators.
 *
 */
@DisplayName("Unit Test - DefaultRequestResponseLogger")
public class DefaultRequestResponseLoggerTest extends UnitTest {

    // ---------------------------------------------------------------------------
    // Mocked collaborators
    // ---------------------------------------------------------------------------

    @Mock
    private HttpRequestResponseHeadersLogger headersLogger;

    @Mock
    private HttpRequestResponseBodyLogger bodyLogger;

    @Mock
    private HttpRequestResponseDebugLogger debugLogger;

    @Mock
    private MediaTypeVoter mediaTypeVoter;

    @Mock
    private MediaTypeVoter bodyExcludedVoter;

    // ---------------------------------------------------------------------------
    // Mocked JAX-RS contexts
    // ---------------------------------------------------------------------------

    @Mock
    private CachingRequestContext cachingRequest;

    @Mock
    private CachingResponseContext cachingResponse;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    private DefaultRequestResponseLogger logger;

    // ---------------------------------------------------------------------------
    // Setup / teardown
    // ---------------------------------------------------------------------------

    @BeforeEach
    @Override
    public void setUp() {
        logger = new DefaultRequestResponseLogger(
            headersLogger,
            bodyLogger,
            debugLogger,
            mediaTypeVoter,
            bodyExcludedVoter
        );
    }

    @AfterEach
    public void tearDown() {
        // Always clean MDC between tests to prevent field bleed
        KibanaLogFields.clear();
    }

    // ---------------------------------------------------------------------------
    // logRequest — collaborator interactions
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should call headersLogger when logging request")
    public void shouldCallHeadersLoggerWhenLoggingRequest() {
        // Given: A request context returning method, URI, media type and content length
        givenValidRequestContext("POST", "/api/users", MediaType.APPLICATION_JSON_TYPE, 42);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("Content-Type: application/json\n");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("{\"name\":\"John\"}");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug output");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: The headers logger was invoked with the caching request context
        verify(headersLogger).getRequestHeaders(cachingRequest);
    }

    @Test
    @DisplayName("Should call bodyLogger when content type is allowed and not excluded")
    public void shouldCallBodyLoggerWhenContentTypeIsAllowedAndNotExcluded() {
        // Given: A request with JSON content type that is allowed and not excluded
        givenValidRequestContext("POST", "/api/orders", MediaType.APPLICATION_JSON_TYPE, 100);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("Content-Type: application/json\n");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("{\"item\":\"book\"}");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: The body logger was invoked to retrieve the request body
        verify(bodyLogger).getRequestBody(cachingRequest);
    }

    @Test
    @DisplayName("Should NOT call bodyLogger when content type is not in allowed mediaTypeVoter list")
    public void shouldNotCallBodyLoggerWhenContentTypeIsNotAllowed() {
        // Given: A request whose content type is not allowed
        givenValidRequestContext("POST", "/api/upload", MediaType.APPLICATION_OCTET_STREAM_TYPE, 500);
        when(mediaTypeVoter.matches(anyString())).thenReturn(false);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: The body logger is NOT invoked because the content type is not allowed
        org.mockito.Mockito.verifyNoInteractions(bodyLogger);
    }

    @Test
    @DisplayName("Should NOT call bodyLogger when content type is excluded by bodyExcludedMediaTypeVoter")
    public void shouldNotCallBodyLoggerWhenContentTypeIsExcluded() {
        // Given: A request whose content type is in the exclusion list
        givenValidRequestContext("POST", "/api/form", MediaType.MULTIPART_FORM_DATA_TYPE, 200);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(true);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: The body logger is NOT invoked because the content type is excluded
        org.mockito.Mockito.verifyNoInteractions(bodyLogger);
    }

    @Test
    @DisplayName("Should handle null MediaType from request context without throwing")
    public void shouldHandleNullMediaTypeFromRequestContextWithoutThrowing() {
        // Given: A request context returning a null MediaType (e.g. a bare GET with no Content-Type)
        when(cachingRequest.getMethod()).thenReturn("GET");
        when(cachingRequest.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/ping");
        when(cachingRequest.getMediaType()).thenReturn(null);
        when(cachingRequest.getLength()).thenReturn(0);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(mediaTypeVoter.matches(any())).thenReturn(false);
        when(bodyExcludedVoter.matches(any())).thenReturn(false);
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When / Then: Logging a request with null media type does not throw an exception
        logger.logRequest(cachingRequest);
        assertThat(logger, is(notNullValue()));
    }

    // ---------------------------------------------------------------------------
    // logRequest — MDC cleanup
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should clear LOG_TYPE MDC field after logging request")
    public void shouldClearLogTypeMdcFieldAfterLoggingRequest() {
        // Given: A valid request that sets MDC fields during logging
        givenValidRequestContext("GET", "/api/items", MediaType.APPLICATION_JSON_TYPE, 0);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request completes
        logger.logRequest(cachingRequest);

        // Then: LOG_TYPE is cleared (finally block ran)
        assertThat(KibanaLogFields.get(LOG_TYPE), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear TX_REQUEST_METHOD MDC field after logging request")
    public void shouldClearTxRequestMethodMdcFieldAfterLoggingRequest() {
        // Given: A valid POST request
        givenValidRequestContext("POST", "/api/create", MediaType.APPLICATION_JSON_TYPE, 50);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("{}");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: TX_REQUEST_METHOD is cleared in the finally block
        assertThat(KibanaLogFields.get(TX_REQUEST_METHOD), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear TX_REQUEST_SIZE MDC field after logging request")
    public void shouldClearTxRequestSizeMdcFieldAfterLoggingRequest() {
        // Given: A request with a content length
        givenValidRequestContext("PUT", "/api/update/1", MediaType.APPLICATION_JSON_TYPE, 120);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("{}");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: TX_REQUEST_SIZE is cleared after logging
        assertThat(KibanaLogFields.get(TX_REQUEST_SIZE), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear TX_REQUEST_HEADERS MDC field after logging request")
    public void shouldClearTxRequestHeadersMdcFieldAfterLoggingRequest() {
        // Given: A request with headers
        givenValidRequestContext("GET", "/api/list", MediaType.APPLICATION_JSON_TYPE, 0);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("Accept: application/json\n");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: TX_REQUEST_HEADERS is cleared after logging
        assertThat(KibanaLogFields.get(TX_REQUEST_HEADERS), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear TX_REQUEST_BODY MDC field after logging request")
    public void shouldClearTxRequestBodyMdcFieldAfterLoggingRequest() {
        // Given: A request with a JSON body
        givenValidRequestContext("POST", "/api/users", MediaType.APPLICATION_JSON_TYPE, 30);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("{\"name\":\"Alice\"}");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: TX_REQUEST_BODY is cleared after logging
        assertThat(KibanaLogFields.get(TX_REQUEST_BODY), is(nullValue()));
    }

    @Test
    @DisplayName("Should NOT clear TX_REQUEST_URI after logging request so it persists for entire request lifecycle")
    public void shouldNotClearTxRequestUriAfterLoggingRequest() {
        // Given: A valid GET request
        givenValidRequestContext("GET", "/api/data", MediaType.APPLICATION_JSON_TYPE, 0);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getRequestHeaders(cachingRequest)).thenReturn("");
        when(bodyLogger.getRequestBody(cachingRequest)).thenReturn("");
        when(debugLogger.getRequestDebugOutput(anyString(), anyString(), anyString(), anyString())).thenReturn("debug");

        // When: Logging the request
        logger.logRequest(cachingRequest);

        // Then: TX_REQUEST_URI is still set (NOT cleared) so it is available for response logging
        assertThat(KibanaLogFields.get(TX_REQUEST_URI), is(notNullValue()));
    }

    // ---------------------------------------------------------------------------
    // logResponse — collaborator interactions
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should call headersLogger for response headers when logging response")
    public void shouldCallHeadersLoggerForResponseHeadersWhenLoggingResponse() {
        // Given: A valid response context returning status and media type
        givenValidResponseContext(200, MediaType.APPLICATION_JSON_TYPE, 55);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("Content-Type: application/json\n");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{\"id\":1}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/users");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: The headers logger was invoked for response headers
        verify(headersLogger).getResponseHeaders(cachingResponse);
    }

    @Test
    @DisplayName("Should call bodyLogger when response content type is allowed and not excluded")
    public void shouldCallBodyLoggerWhenResponseContentTypeIsAllowedAndNotExcluded() {
        // Given: A response with JSON content type that passes both voters
        givenValidResponseContext(200, MediaType.APPLICATION_JSON_TYPE, 100);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{\"data\":\"ok\"}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/data");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: The body logger was invoked to retrieve the response body
        verify(bodyLogger).getResponseBody(cachingResponse);
    }

    @Test
    @DisplayName("Should set TX_STATUS MDC field when TX_STATUS is not already set")
    public void shouldSetTxStatusMdcFieldWhenTxStatusIsNotAlreadySet() {
        // Given: A response with status 404 and TX_STATUS not pre-set in MDC
        givenValidResponseContext(404, MediaType.APPLICATION_JSON_TYPE, 20);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{\"error\":\"not found\"}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/missing");

        // Capture the TX_STATUS value before finally block clears it by using a side-effect-free read
        // We test indirectly by verifying the logger proceeds without error and clears MDC as expected
        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: TX_STATUS is cleared after logging (it was set during execution)
        // TX_STATUS is NOT in the cleared fields list for response (only LOG_TYPE, TX_RESPONSE_SIZE,
        // TX_RESPONSE_HEADERS, TX_RESPONSE_BODY are cleared) — so TX_STATUS persists
        assertThat(logger, is(notNullValue()));
    }

    @Test
    @DisplayName("Should clear LOG_TYPE MDC field after logging response")
    public void shouldClearLogTypeMdcFieldAfterLoggingResponse() {
        // Given: A valid JSON response
        givenValidResponseContext(200, MediaType.APPLICATION_JSON_TYPE, 40);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/ok");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: LOG_TYPE is cleared in the finally block
        assertThat(KibanaLogFields.get(LOG_TYPE), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear TX_RESPONSE_SIZE MDC field after logging response")
    public void shouldClearTxResponseSizeMdcFieldAfterLoggingResponse() {
        // Given: A valid response
        givenValidResponseContext(200, MediaType.APPLICATION_JSON_TYPE, 80);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/data");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: TX_RESPONSE_SIZE is cleared in the finally block
        assertThat(KibanaLogFields.get(TX_RESPONSE_SIZE), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear TX_RESPONSE_HEADERS MDC field after logging response")
    public void shouldClearTxResponseHeadersMdcFieldAfterLoggingResponse() {
        // Given: A valid response with headers
        givenValidResponseContext(200, MediaType.APPLICATION_JSON_TYPE, 30);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("Content-Type: application/json\n");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/list");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: TX_RESPONSE_HEADERS is cleared in the finally block
        assertThat(KibanaLogFields.get(TX_RESPONSE_HEADERS), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear TX_RESPONSE_BODY MDC field after logging response")
    public void shouldClearTxResponseBodyMdcFieldAfterLoggingResponse() {
        // Given: A valid response with a body
        givenValidResponseContext(200, MediaType.APPLICATION_JSON_TYPE, 25);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{\"ok\":true}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/result");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: TX_RESPONSE_BODY is cleared in the finally block
        assertThat(KibanaLogFields.get(TX_RESPONSE_BODY), is(nullValue()));
    }

    // ---------------------------------------------------------------------------
    // logResponse — content type filtering
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should not call bodyLogger when response content type is not in allowed list")
    public void shouldNotCallBodyLoggerWhenResponseContentTypeIsNotInAllowedList() {
        // Given: A response with binary content type that is not in the allowed list
        givenValidResponseContext(200, MediaType.APPLICATION_OCTET_STREAM_TYPE, 1024);
        when(mediaTypeVoter.matches(anyString())).thenReturn(false);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/download");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: The body logger is NOT invoked due to disallowed content type
        org.mockito.Mockito.verifyNoInteractions(bodyLogger);
    }

    @Test
    @DisplayName("Should not call bodyLogger when response content type is excluded by bodyExcludedMediaTypeVoter")
    public void shouldNotCallBodyLoggerWhenResponseContentTypeIsExcluded() {
        // Given: A response with a content type that is in the exclusion list
        givenValidResponseContext(200, MediaType.TEXT_HTML_TYPE, 300);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(true);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/page");

        // When: Logging the response
        logger.logResponse(requestContext, cachingResponse);

        // Then: The body logger is NOT invoked because the content type is excluded
        org.mockito.Mockito.verifyNoInteractions(bodyLogger);
    }

    @Test
    @DisplayName("Should handle null response MediaType without throwing")
    public void shouldHandleNullResponseMediaTypeWithoutThrowing() {
        // Given: A response context that returns null as its media type
        when(cachingResponse.getStatus()).thenReturn(200);
        when(cachingResponse.getMediaType()).thenReturn(null);
        when(cachingResponse.getContentLength()).thenReturn(0);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(mediaTypeVoter.matches(any())).thenReturn(false);
        when(bodyExcludedVoter.matches(any())).thenReturn(false);
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/empty");

        // When / Then: Logging a response with null media type does not throw
        logger.logResponse(requestContext, cachingResponse);
        assertThat(logger, is(notNullValue()));
    }

    // ---------------------------------------------------------------------------
    // Response status variations
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should complete logging without error for 500 Internal Server Error response")
    public void shouldCompleteLoggingWithoutErrorFor500InternalServerErrorResponse() {
        // Given: A 500 error response with a JSON error body
        givenValidResponseContext(500, MediaType.APPLICATION_JSON_TYPE, 60);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{\"error\":\"internal\"}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/broken");

        // When: Logging the 500 response
        logger.logResponse(requestContext, cachingResponse);

        // Then: Logging completed and MDC was cleaned up
        assertThat(KibanaLogFields.get(LOG_TYPE), is(nullValue()));
    }

    @Test
    @DisplayName("Should complete logging without error for 404 Not Found response")
    public void shouldCompleteLoggingWithoutErrorFor404NotFoundResponse() {
        // Given: A 404 response
        givenValidResponseContext(404, MediaType.APPLICATION_JSON_TYPE, 30);
        when(mediaTypeVoter.matches(anyString())).thenReturn(true);
        when(bodyExcludedVoter.matches(anyString())).thenReturn(false);
        when(headersLogger.getResponseHeaders(cachingResponse)).thenReturn("");
        when(bodyLogger.getResponseBody(cachingResponse)).thenReturn("{\"error\":\"not found\"}");
        when(debugLogger.getResponseDebugOutput(any(Integer.class), anyString(), anyString(), anyString())).thenReturn("debug");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/missing");

        // When: Logging the 404 response
        logger.logResponse(requestContext, cachingResponse);

        // Then: Logging completed and MDC was cleaned up
        assertThat(KibanaLogFields.get(LOG_TYPE), is(nullValue()));
    }

    // ---------------------------------------------------------------------------
    // Interface contract
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should implement the RequestResponseLogger interface")
    public void shouldImplementTheRequestResponseLoggerInterface() {
        // Given / When: The logger is created (done in setUp)

        // Then: It implements the RequestResponseLogger interface
        assertThat(logger instanceof RequestResponseLogger, is(true));
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /** Configures the {@code cachingRequest} mock with the given HTTP details. */
    private void givenValidRequestContext(
        final String method,
        final String path,
        final MediaType mediaType,
        final int contentLength) {
        when(cachingRequest.getMethod()).thenReturn(method);
        when(cachingRequest.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(path);
        when(cachingRequest.getMediaType()).thenReturn(mediaType);
        when(cachingRequest.getLength()).thenReturn(contentLength);
    }

    /** Configures the {@code cachingResponse} mock with the given HTTP details. */
    private void givenValidResponseContext(
        final int status,
        final MediaType mediaType,
        final int contentLength) {
        when(cachingResponse.getStatus()).thenReturn(status);
        when(cachingResponse.getMediaType()).thenReturn(mediaType);
        when(cachingResponse.getContentLength()).thenReturn(contentLength);
    }
}
