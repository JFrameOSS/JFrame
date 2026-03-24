package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.security.QuarkusAuthenticationUtil;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

import java.net.URI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_CONTENT_LENGTH;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_CONTENT_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_QUERY;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_STATUS_CODE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TracingEnricher}.
 *
 * <p>Verifies the CDI enricher sets trace/span IDs on error resources and enriches
 * the current OpenTelemetry span with error details and HTTP metadata when recording.
 * When the span is not recording, no enrichment is performed.
 */
@DisplayName("Unit Test - Tracing Enricher")
public class TracingEnricherTest extends UnitTest {

    // ======================== CONSTANTS ========================

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";
    private static final String REMOTE_USER = "john.doe";
    private static final String TRANSACTION_ID = "tx-abc-123";
    private static final String REQUEST_ID_VALUE = "req-xyz-456";
    private static final String REQUEST_URI_PATH = "/api/v1/users";
    private static final String REQUEST_QUERY = "page=1&size=10";
    private static final String HTTP_METHOD_VALUE = "GET";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static final int CONTENT_LENGTH_VALUE = 256;
    private static final int STATUS_CODE_VALUE = 500;

    // ======================== FIXTURES ========================

    @Mock
    private Span span;

    @Mock
    private SpanContext spanContext;

    @Mock
    private QuarkusAuthenticationUtil authUtil;

    @InjectMocks
    private TracingEnricher enricher;

    @Override
    @BeforeEach
    public void setUp() {
        // Default span setup: recording with valid context
        when(span.isRecording()).thenReturn(true);
        when(span.getSpanContext()).thenReturn(spanContext);
        when(spanContext.getTraceId()).thenReturn(TRACE_ID);
        when(spanContext.getSpanId()).thenReturn(SPAN_ID);

        // Default span attribute chaining (returns self for fluent API)
        when(
            span.setAttribute(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
            )
        ).thenReturn(span);
        when(
            span.setAttribute(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyBoolean()
            )
        ).thenReturn(span);
        when(
            span.setAttribute(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
            )
        ).thenReturn(span);
        when(span.setStatus(org.mockito.ArgumentMatchers.any(StatusCode.class))).thenReturn(span);

        // Default auth: known user
        when(authUtil.getAuthenticatedSubject()).thenReturn(REMOTE_USER);
    }

    @AfterEach
    public void clearKibanaLogFields() {
        // Clean up MDC ThreadLocal to prevent cross-test pollution
        KibanaLogFields.clear();
    }

    // ======================== FACTORY METHODS ========================

    private ContainerRequestContext aRequestContext() throws Exception {
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final URI requestUri = new URI(REQUEST_URI_PATH + "?" + REQUEST_QUERY);

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(requestUri);
        when(requestContext.getMethod()).thenReturn(HTTP_METHOD_VALUE);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getLength()).thenReturn(CONTENT_LENGTH_VALUE);

        return requestContext;
    }

    private ContainerRequestContext aRequestContextWithQuery(final String query) throws Exception {
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final URI requestUri = query != null
            ? new URI(REQUEST_URI_PATH + "?" + query)
            : new URI(REQUEST_URI_PATH);

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(requestUri);
        when(requestContext.getMethod()).thenReturn(HTTP_METHOD_VALUE);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getLength()).thenReturn(CONTENT_LENGTH_VALUE);

        return requestContext;
    }

    // ======================== TEST 1: TRACE/SPAN ID SET ON RESOURCE ========================

    @Test
    @DisplayName("Should set traceId and spanId on resource when span is recording")
    public void shouldSetTraceIdAndSpanIdOnResourceWhenSpanIsRecording() throws Exception {
        // Given: A recording span with a valid span context
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");
        final ContainerRequestContext requestContext = aRequestContext();
        KibanaLogFields.tag(TX_ID, TRANSACTION_ID);
        KibanaLogFields.tag(REQUEST_ID, REQUEST_ID_VALUE);

        // When: doEnrich is called with the recording span active
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            enricher.doEnrich(resource, throwable, requestContext, STATUS_CODE_VALUE);
        }

        // Then: The resource's traceId and spanId are set from the span context
        assertThat(resource.getTraceId(), is(notNullValue()));
        assertThat(resource.getTraceId(), is(equalTo(TRACE_ID)));
        assertThat(resource.getSpanId(), is(notNullValue()));
        assertThat(resource.getSpanId(), is(equalTo(SPAN_ID)));
    }

    // ======================== TEST 2: SPAN ENRICHED WITH ERROR DETAILS ========================

    @Test
    @DisplayName("Should enrich span with error details when span is recording")
    public void shouldEnrichSpanWithErrorDetailsWhenSpanIsRecording() throws Exception {
        // Given: A recording span and a RuntimeException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");
        final ContainerRequestContext requestContext = aRequestContext();
        KibanaLogFields.tag(TX_ID, TRANSACTION_ID);
        KibanaLogFields.tag(REQUEST_ID, REQUEST_ID_VALUE);

        // When: doEnrich is called
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            enricher.doEnrich(resource, throwable, requestContext, STATUS_CODE_VALUE);
        }

        // Then: Span is marked with ERROR status and error attributes
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).setAttribute(ERROR, true);
        verify(span).setAttribute(ERROR_TYPE, "RuntimeException");
        verify(span).setAttribute(ERROR_MESSAGE, "test error");
        verify(span).setAttribute(HTTP_URI, REQUEST_URI_PATH);
        verify(span).setAttribute(HTTP_METHOD, HTTP_METHOD_VALUE);
        verify(span).setAttribute(HTTP_STATUS_CODE, STATUS_CODE_VALUE);
    }

    // ======================== TEST 3: SPAN ENRICHED WITH HTTP METADATA ========================

    @Test
    @DisplayName("Should enrich span with all HTTP metadata when span is recording")
    public void shouldEnrichSpanWithHttpMetadataWhenSpanIsRecording() throws Exception {
        // Given: A recording span, known user, correlation IDs in MDC, and a full request context
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");
        final ContainerRequestContext requestContext = aRequestContext();
        KibanaLogFields.tag(TX_ID, TRANSACTION_ID);
        KibanaLogFields.tag(REQUEST_ID, REQUEST_ID_VALUE);

        // When: doEnrich is called
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            enricher.doEnrich(resource, throwable, requestContext, STATUS_CODE_VALUE);
        }

        // Then: Span receives all HTTP metadata attributes
        verify(span).setAttribute(HTTP_REMOTE_USER, REMOTE_USER);
        verify(span).setAttribute(HTTP_TRANSACTION_ID, TRANSACTION_ID);
        verify(span).setAttribute(HTTP_REQUEST_ID, REQUEST_ID_VALUE);
        verify(span).setAttribute(HTTP_URI, REQUEST_URI_PATH);
        verify(span).setAttribute(HTTP_QUERY, REQUEST_QUERY);
        verify(span).setAttribute(HTTP_METHOD, HTTP_METHOD_VALUE);
        verify(span).setAttribute(HTTP_STATUS_CODE, STATUS_CODE_VALUE);
        verify(span).setAttribute(HTTP_CONTENT_TYPE, CONTENT_TYPE_VALUE);
        verify(span).setAttribute(HTTP_CONTENT_LENGTH, CONTENT_LENGTH_VALUE);
    }

    // ======================== TEST 4: NON-RECORDING SPAN → NO ENRICHMENT ========================

    @Test
    @DisplayName("Should not enrich resource when span is not recording")
    public void shouldNotEnrichResourceWhenSpanIsNotRecording() throws Exception {
        // Given: A non-recording span (e.g. NOOP span when no tracing backend is configured)
        when(span.isRecording()).thenReturn(false);
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");
        final ContainerRequestContext requestContext = aRequestContext();

        // When: doEnrich is called with a non-recording span
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            enricher.doEnrich(resource, throwable, requestContext, STATUS_CODE_VALUE);
        }

        // Then: Resource traceId and spanId remain null — no enrichment performed
        assertThat(resource.getTraceId(), is(nullValue()));
        assertThat(resource.getSpanId(), is(nullValue()));

        // And: No setAttribute calls are made on the span
        verify(span, never()).setAttribute(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
        );
        verify(span, never()).setAttribute(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean()
        );
        verify(span, never()).setAttribute(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyInt()
        );
        verify(span, never()).setStatus(org.mockito.ArgumentMatchers.any(StatusCode.class));
    }

    // ======================== TEST 5: @ApplicationScoped ANNOTATION ========================

    @Test
    @DisplayName("Should be annotated as @ApplicationScoped for CDI auto-discovery")
    public void shouldBeAnnotatedAsApplicationScoped() {
        // Given: The TracingEnricher class under test

        // When: Checking its declared annotations via reflection
        final ApplicationScoped annotation = TracingEnricher.class.getAnnotation(ApplicationScoped.class);

        // Then: @ApplicationScoped is present
        assertThat(annotation, is(notNullValue()));
    }

    // ======================== EDGE CASES ========================

    @Test
    @DisplayName("Should handle throwable with null message without NPE when span is recording")
    public void shouldHandleThrowableWithNullMessageWithoutNpeWhenSpanIsRecording() throws Exception {
        // Given: A throwable whose getMessage() returns null
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException((String) null);
        final ContainerRequestContext requestContext = aRequestContext();
        KibanaLogFields.tag(TX_ID, TRANSACTION_ID);
        KibanaLogFields.tag(REQUEST_ID, REQUEST_ID_VALUE);

        // When: doEnrich is called — should not throw NPE
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            enricher.doEnrich(resource, throwable, requestContext, STATUS_CODE_VALUE);
        }

        // Then: error.type is still set correctly, and span status is ERROR
        verify(span).setAttribute(ERROR_TYPE, "RuntimeException");
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).setAttribute(ERROR, true);
    }

    @Test
    @DisplayName("Should still call setAttribute for TX_ID and REQUEST_ID when MDC values are null")
    public void shouldStillCallSetAttributeForTxIdAndRequestIdWhenMdcValuesAreNull() throws Exception {
        // Given: No TX_ID or REQUEST_ID in MDC (clean MDC, not tagged)
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");
        final ContainerRequestContext requestContext = aRequestContext();
        // KibanaLogFields MDC is clean — TX_ID and REQUEST_ID return null

        // When: doEnrich is called with a recording span
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            enricher.doEnrich(resource, throwable, requestContext, STATUS_CODE_VALUE);
        }

        // Then: Span receives TX_ID and REQUEST_ID attributes (with null value per spec)
        verify(span).setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID));
        verify(span).setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID));
    }
}
