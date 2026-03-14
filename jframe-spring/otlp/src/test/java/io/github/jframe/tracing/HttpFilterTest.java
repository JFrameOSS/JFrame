package io.github.jframe.tracing;

import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import static io.github.jframe.util.constants.Constants.Headers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HttpFilter}.
 *
 * <p>Verifies the HttpFilter functionality including:
 * <ul>
 * <li>Request interceptor creation and header injection</li>
 * <li>Span creation and enrichment</li>
 * <li>Response logging and body masking</li>
 * <li>Span cleanup on completion</li>
 * </ul>
 */
@DisplayName("Tracing - HttpFilter")
class HttpFilterTest extends UnitTest {

    @Mock
    private SpanManager spanManager;
    @Mock
    private PasswordMasker passwordMasker;
    @Mock
    private ClientHttpRequestExecution execution;
    @Mock
    private ClientHttpResponse clientHttpResponse;
    @Mock
    private Span span;
    @Mock
    private SpanContext spanContext;

    private HttpFilter httpFilter;

    @Override
    @BeforeEach
    public void setUp() {
        httpFilter = new HttpFilter(Optional.of(spanManager), passwordMasker);
        setFieldValue(httpFilter, "responseLength", RESPONSE_LENGTH);

        setupKibanaFields();
        setupSpanMocks();
        setupPasswordMasker();
    }

    @Test
    @DisplayName("Should create request interceptor and inject headers")
    void shouldInjectKibanaHeadersWhenInterceptingRequest() throws IOException {
        // Given: A mock HTTP request with execution chain
        final ClientHttpRequest request = aMockedClientHttpRequest();
        final byte[] body = aRequestBody();
        when(execution.execute(any(), any())).thenReturn(clientHttpResponse);

        // And: Span manager creates outbound span
        when(spanManager.createOutboundSpan(any(), any(), eq(SERVICE_NAME_VALUE))).thenReturn(span);
        setupSuccessfulResponse();

        // When: Using the request interceptor
        final ClientHttpRequestInterceptor interceptor = httpFilter.getRequestInterceptor(SERVICE_NAME_VALUE);
        final ClientHttpResponse response = interceptor.intercept(request, body, execution);

        // Then: Kibana headers are injected
        verify(request.getHeaders()).add(REQ_ID_HEADER, TEST_REQUEST_ID);
        verify(request.getHeaders()).add(TX_ID_HEADER, TEST_TX_ID);
        verify(request.getHeaders()).add(TRACE_ID_HEADER, TEST_TRACE_ID);

        // And: Trace context is injected and span is created
        verify(spanManager).injectTraceContext(request.getHeaders());
        verify(spanManager).createOutboundSpan(HttpMethod.GET, request.getURI(), SERVICE_NAME_VALUE);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should enrich and finish span after successful response")
    void shouldEnrichAndFinishSpanWhenResponseIsSuccessful() throws IOException {
        // Given: A mock HTTP request with successful response
        final ClientHttpRequest request = aMockedClientHttpRequest();
        final byte[] body = aRequestBody();
        when(execution.execute(any(), any())).thenReturn(clientHttpResponse);
        when(spanManager.createOutboundSpan(any(), any(), eq(SERVICE_NAME_VALUE))).thenReturn(span);
        setupSuccessfulResponse();

        // When: Using the request interceptor
        final ClientHttpRequestInterceptor interceptor = httpFilter.getRequestInterceptor(SERVICE_NAME_VALUE);
        interceptor.intercept(request, body, execution);

        // Then: Span is enriched with response details
        verify(spanManager).enrichOutboundSpan(eq(span), any(HttpStatusCode.class), any(HttpHeaders.class));

        // And: Span is properly finished
        verify(spanManager).finishSpan(span);
    }

    @Test
    @DisplayName("Should finish span even when exception occurs")
    void shouldFinishSpanWhenExceptionOccurs() throws IOException {
        // Given: A mock HTTP request that throws exception during execution
        final ClientHttpRequest request = aMockedClientHttpRequest();
        final byte[] body = aRequestBody();
        when(execution.execute(any(), any())).thenThrow(new IOException("Network error"));
        when(spanManager.createOutboundSpan(any(), any(), eq(SERVICE_NAME_VALUE))).thenReturn(span);

        // When: Using the request interceptor throws exception
        final ClientHttpRequestInterceptor interceptor = httpFilter.getRequestInterceptor(SERVICE_NAME_VALUE);
        try {
            interceptor.intercept(request, body, execution);
        } catch (final IOException exception) {
            // Expected exception
        }

        // Then: Span is still finished despite exception
        verify(spanManager).finishSpan(span);

        // And: Span enrichment was never called
        verify(spanManager, never()).enrichOutboundSpan(any(), any(), any());
    }

    @Test
    @DisplayName("Should work without span manager")
    void shouldStillWorkWhenSpanManagerIsAbsent() throws IOException {
        // Given: HttpFilter without SpanManager
        final HttpFilter filterWithoutSpan = new HttpFilter(Optional.empty(), passwordMasker);
        setFieldValue(filterWithoutSpan, "responseLength", RESPONSE_LENGTH);

        // And: A mock HTTP request with successful response
        final ClientHttpRequest request = aMockedClientHttpRequest();
        final byte[] body = aRequestBody();
        when(execution.execute(any(), any())).thenReturn(clientHttpResponse);
        setupSuccessfulResponse();

        // When: Using the request interceptor without span manager
        final ClientHttpRequestInterceptor interceptor = filterWithoutSpan.getRequestInterceptor(SERVICE_NAME_VALUE);
        final ClientHttpResponse response = interceptor.intercept(request, body, execution);

        // Then: Request still processes successfully
        assertThat(response).isNotNull();

        // And: No span operations were attempted
        verify(spanManager, never()).createOutboundSpan(any(), any(), any());
    }

    @Test
    @DisplayName("Should create exchange filter for reactive requests")
    void shouldReturnExchangeFilterWhenCreatingReactiveFilter() {
        // Given: Service name

        // When: Creating exchange filter for reactive web client
        final ExchangeFilterFunction exchangeFilter = httpFilter.getExchangeFilter(SERVICE_NAME_VALUE);

        // Then: Exchange filter is created
        assertThat(exchangeFilter).isNotNull();
    }

    @Test
    @DisplayName("Should handle response with various status codes")
    void shouldHandleAllStatusCodesWhenProcessingResponse() throws IOException {
        // Given: A mock HTTP request with 404 error response
        final ClientHttpRequest request = aMockedClientHttpRequest();
        final byte[] body = aRequestBody();
        when(execution.execute(any(), any())).thenReturn(clientHttpResponse);
        when(spanManager.createOutboundSpan(any(), any(), eq(SERVICE_NAME_VALUE))).thenReturn(span);

        // And: Response has 404 status
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(clientHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("Not Found".getBytes()));

        // When: Using the request interceptor
        final ClientHttpRequestInterceptor interceptor = httpFilter.getRequestInterceptor(SERVICE_NAME_VALUE);
        final ClientHttpResponse response = interceptor.intercept(request, body, execution);

        // Then: Response is handled correctly
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // And: Span is enriched with error status
        verify(spanManager).enrichOutboundSpan(eq(span), eq(HttpStatus.NOT_FOUND), any(HttpHeaders.class));
    }

    @Test
    @DisplayName("Should mask response body based on password masker")
    void shouldMaskResponseWhenSensitiveDataIsPresent() throws IOException {
        // Given: A mock HTTP request
        final ClientHttpRequest request = aMockedClientHttpRequest();
        final byte[] body = aRequestBody();

        // And: Response contains sensitive data
        final String responseBody = "{\"password\":\"secret123\"}";
        final String maskedResponse = "{\"password\":\"***\"}";
        when(execution.execute(any(), any())).thenReturn(clientHttpResponse);
        when(spanManager.createOutboundSpan(any(), any(), eq(SERVICE_NAME_VALUE))).thenReturn(span);
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream(responseBody.getBytes()));
        when(passwordMasker.maskPasswordsIn(responseBody)).thenReturn(maskedResponse);

        // When: Using the request interceptor
        final ClientHttpRequestInterceptor interceptor = httpFilter.getRequestInterceptor(SERVICE_NAME_VALUE);
        interceptor.intercept(request, body, execution);

        // Then: Password masker is called to mask sensitive data
        verify(passwordMasker).maskPasswordsIn(responseBody);
    }

    private void setupSpanMocks() {
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(spanContext.getSpanId()).thenReturn(TEST_SPAN_ID);
    }

    private void setupPasswordMasker() {
        lenient().when(passwordMasker.maskPasswordsIn(anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void setupSuccessfulResponse() throws IOException {
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("response".getBytes()));
    }
}
