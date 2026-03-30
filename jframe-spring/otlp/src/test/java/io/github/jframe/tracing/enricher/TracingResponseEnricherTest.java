package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationUtil;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TracingResponseEnricher}.
 *
 * <p>Verifies the Spring enricher records exceptions on the current span,
 * sets error status, enriches HTTP metadata, and correlates trace/span IDs
 * on error response resources.
 */
@DisplayName("Tracing - TracingResponseEnricher")
class TracingResponseEnricherTest extends UnitTest {

    // ======================== CONSTANTS ========================

    private static final String TRACE_ID_VALUE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID_VALUE = "00f067aa0ba902b7";
    private static final String REMOTE_USER = "john.doe";
    private static final String TRANSACTION_ID = "tx-abc-123";
    private static final String REQUEST_ID_VALUE = "req-xyz-456";
    private static final String REQUEST_URI_PATH = "/api/v1/users";
    private static final String REQUEST_QUERY = "page=1&size=10";
    private static final String HTTP_METHOD_VALUE = "GET";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static final int CONTENT_LENGTH_VALUE = 256;
    private static final HttpStatus HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    // ======================== MOCKS ========================

    @Mock
    private Span span;

    @Mock
    private SpanContext spanContext;

    @Mock
    private ServletWebRequest servletWebRequest;

    @Mock
    private HttpServletRequest httpServletRequest;

    private TracingResponseEnricher enricher;

    // ======================== SETUP ========================

    @Override
    @BeforeEach
    public void setUp() {
        enricher = new TracingResponseEnricher();

        // Default span setup: recording with valid context
        when(span.isRecording()).thenReturn(true);
        when(span.getSpanContext()).thenReturn(spanContext);
        when(spanContext.getTraceId()).thenReturn(TRACE_ID_VALUE);
        when(spanContext.getSpanId()).thenReturn(SPAN_ID_VALUE);

        // Default span attribute chaining (returns self for fluent API)
        when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        when(span.setAttribute(anyString(), anyInt())).thenReturn(span);
        when(span.setStatus(org.mockito.ArgumentMatchers.any(StatusCode.class))).thenReturn(span);

        // Default ServletWebRequest → HttpServletRequest delegation
        when(servletWebRequest.getNativeRequest()).thenReturn(httpServletRequest);
        setupHttpServletRequest();

        setupEcsFields();
        EcsFields.tag(TX_ID, TRANSACTION_ID);
        EcsFields.tag(REQUEST_ID, REQUEST_ID_VALUE);
    }

    @AfterEach
    public void clearEcsFields() {
        EcsFields.clear();
    }

    // ======================== TESTS ========================

    @Test
    @DisplayName("Should record exception on span when enriching error response")
    void shouldRecordExceptionOnSpanWhenEnrichingErrorResponse() {
        // Given: A recording span and a RuntimeException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("Internal error");

        // When: doEnrich is called with the recording span active
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class);
            MockedStatic<AuthenticationUtil> authStatic = mockStatic(AuthenticationUtil.class)) {
            spanStatic.when(Span::current).thenReturn(span);
            authStatic.when(AuthenticationUtil::getAuthenticatedSubject).thenReturn(REMOTE_USER);

            enricher.doEnrich(resource, throwable, servletWebRequest, HTTP_STATUS);
        }

        // Then: Exception is recorded on span via OTLP API (not manual attributes)
        verify(span).recordException(throwable);
        verify(span).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should enrich span with HTTP metadata when enriching error response")
    void shouldEnrichSpanWithHttpMetadataWhenEnrichingErrorResponse() {
        // Given: A recording span with full HTTP request context
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("Internal error");

        // When: doEnrich is called
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class);
            MockedStatic<AuthenticationUtil> authStatic = mockStatic(AuthenticationUtil.class)) {
            spanStatic.when(Span::current).thenReturn(span);
            authStatic.when(AuthenticationUtil::getAuthenticatedSubject).thenReturn(REMOTE_USER);

            enricher.doEnrich(resource, throwable, servletWebRequest, HTTP_STATUS);
        }

        // Then: Span receives all HTTP metadata attributes
        verify(span).setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), REMOTE_USER);
        verify(span).setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), TRANSACTION_ID);
        verify(span).setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), REQUEST_ID_VALUE);
        verify(span).setAttribute(SPAN_HTTP_URI.getKey(), REQUEST_URI_PATH);
        verify(span).setAttribute(SPAN_HTTP_QUERY.getKey(), REQUEST_QUERY);
        verify(span).setAttribute(SPAN_HTTP_METHOD.getKey(), HTTP_METHOD_VALUE);
        verify(span).setAttribute(SPAN_HTTP_STATUS_CODE.getKey(), HTTP_STATUS.value());
        verify(span).setAttribute(SPAN_HTTP_CONTENT_TYPE.getKey(), CONTENT_TYPE_VALUE);
        verify(span).setAttribute(SPAN_HTTP_CONTENT_LENGTH.getKey(), CONTENT_LENGTH_VALUE);
    }

    @Test
    @DisplayName("Should record exception with null message without NPE")
    void shouldRecordExceptionWithNullMessageWithoutNpe() {
        // Given: A throwable whose getMessage() returns null
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException((String) null);

        // When: doEnrich is called — should not throw NPE
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class);
            MockedStatic<AuthenticationUtil> authStatic = mockStatic(AuthenticationUtil.class)) {
            spanStatic.when(Span::current).thenReturn(span);
            authStatic.when(AuthenticationUtil::getAuthenticatedSubject).thenReturn(REMOTE_USER);

            enricher.doEnrich(resource, throwable, servletWebRequest, HTTP_STATUS);
        }

        // Then: Exception is recorded on span (recordException handles null message)
        verify(span).recordException(throwable);
        verify(span).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should set trace correlation IDs on span")
    void shouldSetTraceCorrelationIdsOnSpan() {
        // Given: TX_ID and REQUEST_ID are set in ECS MDC
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");

        // When: doEnrich is called with a recording span
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class);
            MockedStatic<AuthenticationUtil> authStatic = mockStatic(AuthenticationUtil.class)) {
            spanStatic.when(Span::current).thenReturn(span);
            authStatic.when(AuthenticationUtil::getAuthenticatedSubject).thenReturn(REMOTE_USER);

            enricher.doEnrich(resource, throwable, servletWebRequest, HTTP_STATUS);
        }

        // Then: Span receives TX_ID and REQUEST_ID correlation attributes
        verify(span).setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), TRANSACTION_ID);
        verify(span).setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), REQUEST_ID_VALUE);

        // And: Error response resource is enriched with traceId and spanId
        assertThat(resource.getTraceId(), is(notNullValue()));
        assertThat(resource.getTraceId(), is(equalTo(TRACE_ID_VALUE)));
        assertThat(resource.getSpanId(), is(notNullValue()));
        assertThat(resource.getSpanId(), is(equalTo(SPAN_ID_VALUE)));
    }

    @Test
    @DisplayName("Should not enrich when span is not recording")
    void shouldNotEnrichWhenSpanIsNotRecording() {
        // Given: A non-recording span (e.g. NOOP span when no tracing backend is configured)
        when(span.isRecording()).thenReturn(false);
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");

        // When: doEnrich is called with a non-recording span
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            enricher.doEnrich(resource, throwable, servletWebRequest, HTTP_STATUS);
        }

        // Then: Resource traceId and spanId remain null — no enrichment performed
        assertThat(resource.getTraceId(), is(nullValue()));
        assertThat(resource.getSpanId(), is(nullValue()));

        // And: No setAttribute calls are made on the span
        verify(span, never()).setAttribute(anyString(), anyString());
        verify(span, never()).setAttribute(anyString(), anyInt());
        verify(span, never()).setStatus(org.mockito.ArgumentMatchers.any(StatusCode.class));
        verify(span, never()).recordException(org.mockito.ArgumentMatchers.any(Throwable.class));
    }

    @Test
    @DisplayName("Should set trace and span IDs on error response resource")
    void shouldSetTraceAndSpanIdsOnErrorResponseResource() {
        // Given: A recording span with valid span context
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("test error");

        // When: doEnrich is called
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class);
            MockedStatic<AuthenticationUtil> authStatic = mockStatic(AuthenticationUtil.class)) {
            spanStatic.when(Span::current).thenReturn(span);
            authStatic.when(AuthenticationUtil::getAuthenticatedSubject).thenReturn(REMOTE_USER);

            enricher.doEnrich(resource, throwable, servletWebRequest, HTTP_STATUS);
        }

        // Then: The resource's traceId and spanId are set from the span context
        assertThat(resource.getTraceId(), is(equalTo(TRACE_ID_VALUE)));
        assertThat(resource.getSpanId(), is(equalTo(SPAN_ID_VALUE)));
    }

    // ======================== FACTORY METHODS ========================

    private void setupHttpServletRequest() {
        when(httpServletRequest.getRequestURI()).thenReturn(REQUEST_URI_PATH);
        when(httpServletRequest.getQueryString()).thenReturn(REQUEST_QUERY);
        when(httpServletRequest.getMethod()).thenReturn(HTTP_METHOD_VALUE);
        when(httpServletRequest.getContentType()).thenReturn(CONTENT_TYPE_VALUE);
        when(httpServletRequest.getContentLength()).thenReturn(CONTENT_LENGTH_VALUE);
    }
}
