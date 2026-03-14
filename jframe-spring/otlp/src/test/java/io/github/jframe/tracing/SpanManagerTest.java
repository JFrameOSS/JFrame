package io.github.jframe.tracing;

import io.github.support.UnitTest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;

import static io.github.jframe.OpenTelemetryConstants.Attributes.*;
import static io.github.jframe.util.constants.Constants.Headers.L7_REQUEST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpanManager}.
 *
 * <p>Verifies the SpanManager functionality including:
 * <ul>
 * <li>Creating outbound spans with proper attributes</li>
 * <li>Enriching spans with response data</li>
 * <li>Finishing spans</li>
 * <li>Injecting trace context into HTTP headers</li>
 * </ul>
 */
@DisplayName("Tracing - SpanManager")
class SpanManagerTest extends UnitTest {

    @Mock
    private Tracer tracer;
    @Mock
    private OpenTelemetry openTelemetry;
    @Mock
    private Span span;
    @Mock
    private SpanBuilder spanBuilder;
    @Mock
    private SpanContext spanContext;
    @Mock
    private ContextPropagators contextPropagators;
    @Mock
    private TextMapPropagator textMapPropagator;

    private SpanManager spanManager;

    @Override
    @BeforeEach
    public void setUp() {
        spanManager = new SpanManager(tracer, openTelemetry);

        setupSpanBuilderMocks();
        setupOpenTelemetryMocks();
        setupKibanaFields();
    }

    @Test
    @DisplayName("Should create outbound span with correct attributes")
    void shouldCreateSpanWithAttributesWhenCreatingOutboundSpan() {
        // Given: HTTP GET request with query parameters
        final HttpMethod method = HttpMethod.GET;
        final URI uri = URI.create("https://example.com:8080/api/users?page=1");

        // When: Creating an outbound span
        final Span result = spanManager.createOutboundSpan(method, uri, SERVICE_NAME_VALUE);

        // Then: Span is created with correct name
        verify(tracer).spanBuilder("GET example.com");

        // And: Service attributes are set correctly
        verify(spanBuilder).setAttribute(PEER_SERVICE, SERVICE_NAME_VALUE);
        verify(spanBuilder).setAttribute(SERVICE_NAME, "example.com");

        // And: Request attributes are set correctly
        verify(spanBuilder).setAttribute(EXT_REQUEST_URI, "example.com/api/users");
        verify(spanBuilder).setAttribute(EXT_REQUEST_QUERY, "page=1");
        verify(spanBuilder).setAttribute(EXT_REQUEST_METHOD, "GET");

        // And: Transaction identifiers are included
        verify(spanBuilder).setAttribute(HTTP_TRANSACTION_ID, TEST_TX_ID);
        verify(spanBuilder).setAttribute(HTTP_REQUEST_ID, TEST_REQUEST_ID);

        // And: HTTP remote user attribute is set
        verify(spanBuilder).setAttribute(eq(HTTP_REMOTE_USER), anyString());

        // And: Span is started and returned
        verify(spanBuilder).startSpan();
        assertThat(result).isEqualTo(span);
    }

    @Test
    @DisplayName("Should create outbound span with POST method")
    void shouldCreateSpanWithPostMethodWhenCreatingOutboundSpan() {
        // Given: HTTP POST request
        final HttpMethod method = HttpMethod.POST;
        final URI uri = URI.create("https://api.example.com/users");

        // When: Creating an outbound span
        spanManager.createOutboundSpan(method, uri, SERVICE_NAME_VALUE);

        // Then: Span name includes POST method
        verify(tracer).spanBuilder("POST api.example.com");

        // And: Request method is set to POST
        verify(spanBuilder).setAttribute(EXT_REQUEST_METHOD, "POST");
    }

    @Test
    @DisplayName("Should create outbound span with null query string")
    void shouldHandleNullQueryWhenCreatingOutboundSpan() {
        // Given: HTTP request without query string
        final HttpMethod method = HttpMethod.GET;
        final URI uri = URI.create("https://example.com/api/users");

        // When: Creating an outbound span
        spanManager.createOutboundSpan(method, uri, SERVICE_NAME_VALUE);

        // Then: Query attribute is set to null
        verify(spanBuilder).setAttribute(EXT_REQUEST_QUERY, null);
    }

    @Test
    @DisplayName("Should enrich outbound span with success status")
    void shouldAddResponseAttributesWhenEnrichingWithSuccessStatus() {
        // Given: Successful HTTP response with complete headers
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(1024);
        headers.set(L7_REQUEST_ID, L7_REQUEST_ID_VALUE);

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.OK, headers);

        // Then: Response status and content attributes are added
        verify(span).setAttribute(EXT_RESPONSE_STATUS_CODE, 200);
        verify(span).setAttribute(EXT_RESPONSE_CONTENT_TYPE, "application/json");
        verify(span).setAttribute(EXT_RESPONSE_CONTENT_LENGTH, "1024");
        verify(span).setAttribute(EXT_RESPONSE_L7_REQUEST_ID, L7_REQUEST_ID_VALUE);

        // And: No error status is set
        verify(span, never()).setAttribute(ERROR, true);
        verify(span, never()).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should enrich outbound span with error status for 4xx")
    void shouldMarkAsErrorWhenEnrichingWith4xxStatus() {
        // Given: HTTP 404 response
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.NOT_FOUND, headers);

        // Then: Response status is recorded
        verify(span).setAttribute(EXT_RESPONSE_STATUS_CODE, 404);

        // And: Span is marked as error
        verify(span).setAttribute(ERROR, true);
        verify(span).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should enrich outbound span with error status for 5xx")
    void shouldMarkAsErrorWhenEnrichingWith5xxStatus() {
        // Given: HTTP 500 response
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.INTERNAL_SERVER_ERROR, headers);

        // Then: Response status is recorded
        verify(span).setAttribute(EXT_RESPONSE_STATUS_CODE, 500);

        // And: Span is marked as error
        verify(span).setAttribute(ERROR, true);
        verify(span).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should enrich span with unknown content type when not present")
    void shouldUseUnknownWhenEnrichingWithoutContentType() {
        // Given: Response without content type
        final HttpHeaders headers = new HttpHeaders();

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.OK, headers);

        // Then: Content type is set to unknown
        verify(span).setAttribute(EXT_RESPONSE_CONTENT_TYPE, "unknown");
    }

    @Test
    @DisplayName("Should enrich span with -1 content length when not present")
    void shouldUseMinusOneWhenEnrichingWithoutContentLength() {
        // Given: Response without content length
        final HttpHeaders headers = new HttpHeaders();

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.OK, headers);

        // Then: Content length is set to -1
        verify(span).setAttribute(EXT_RESPONSE_CONTENT_LENGTH, "-1");
    }

    @Test
    @DisplayName("Should enrich span without L7 request ID when not present")
    void shouldNotSetAttributeWhenEnrichingWithoutL7RequestId() {
        // Given: Response without L7 request ID
        final HttpHeaders headers = new HttpHeaders();

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.OK, headers);

        // Then: L7 request ID attribute is not set
        verify(span, never()).setAttribute(eq(EXT_RESPONSE_L7_REQUEST_ID), anyString());
    }

    @Test
    @DisplayName("Should finish span")
    void shouldEndSpanWhenFinishingSpan() {
        // Given: An active span

        // When: Finishing the span
        spanManager.finishSpan(span);

        // Then: Span is ended
        verify(span).end();
    }

    @Test
    @DisplayName("Should inject trace context into HTTP headers")
    void shouldInjectContextWhenInjectingTraceContextIntoHttpHeaders() {
        // Given: HTTP headers
        final HttpHeaders headers = new HttpHeaders();

        // When: Injecting trace context
        spanManager.injectTraceContext(headers);

        // Then: Context propagator is called to inject trace context
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(textMapPropagator).inject(contextCaptor.capture(), eq(headers), any());
        assertThat(contextCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Should inject trace context into ClientRequest builder")
    void shouldInjectContextWhenInjectingTraceContextIntoClientRequestBuilder() {
        // Given: Mocked ClientRequest builder
        final ClientRequest.Builder requestBuilder = mock(ClientRequest.Builder.class);

        // When: Injecting trace context
        spanManager.injectTraceContext(requestBuilder);

        // Then: Context propagator is called to inject trace context
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(textMapPropagator).inject(contextCaptor.capture(), eq(requestBuilder), any());
        assertThat(contextCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Should enrich span with various content types")
    void shouldSetCorrectTypeWhenEnrichingWithDifferentContentTypes() {
        // Given: Response with XML content type
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.OK, headers);

        // Then: Content type is set correctly
        verify(span).setAttribute(EXT_RESPONSE_CONTENT_TYPE, "application/xml");
    }

    @Test
    @DisplayName("Should handle zero content length")
    void shouldSetMinusOneWhenEnrichingWithZeroContentLength() {
        // Given: Response with zero content length not explicitly set
        final HttpHeaders headers = new HttpHeaders();

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.OK, headers);

        // Then: Content length is -1 indicating not set
        verify(span).setAttribute(EXT_RESPONSE_CONTENT_LENGTH, "-1");
    }

    @Test
    @DisplayName("Should handle positive content length")
    void shouldSetValueWhenEnrichingWithPositiveContentLength() {
        // Given: Response with explicit content length
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(2048);

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, HttpStatus.OK, headers);

        // Then: Content length is set to the actual value
        verify(span).setAttribute(EXT_RESPONSE_CONTENT_LENGTH, "2048");
    }

    private void setupSpanBuilderMocks() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), isNull())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(spanContext.getSpanId()).thenReturn(TEST_SPAN_ID);
    }

    private void setupOpenTelemetryMocks() {
        lenient().when(openTelemetry.getPropagators()).thenReturn(contextPropagators);
        lenient().when(contextPropagators.getTextMapPropagator()).thenReturn(textMapPropagator);
    }
}
