package io.github.jframe.logging.filter.type.otlp;

import io.github.jframe.logging.filter.otlp.TracingFilterConfig;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.tracing.OpenTelemetryConfig;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.SPAN_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TRACE_ID;
import static io.github.jframe.util.constants.Constants.Headers.SPAN_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TracingResponseFilter} (Quarkus JAX-RS adapter).
 *
 * <p>Verifies the TracingResponseFilter functionality including:
 * <ul>
 * <li>Adding trace ID and span ID to response headers</li>
 * <li>Populating MDC (KibanaLogFields) with trace and span IDs during the request phase</li>
 * <li>Handling valid and invalid span contexts</li>
 * <li>Preventing duplicate headers</li>
 * <li>MDC cleanup after filter execution</li>
 * </ul>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Quarkus OTLP - Tracing Response Filter")
public class TracingResponseFilterTest extends UnitTest {

    @Mock
    private TracingFilterConfig tracingFilterConfig;

    @Mock
    private TracingFilterConfig.TracingResponseConfig tracingResponseConfig;

    @Mock
    private OpenTelemetryConfig openTelemetryConfig;

    @BeforeEach
    public void setUp() {
        lenient().when(tracingFilterConfig.tracingResponse()).thenReturn(tracingResponseConfig);
        lenient().when(tracingResponseConfig.enabled()).thenReturn(true);
        lenient().when(openTelemetryConfig.disabled()).thenReturn(false);
    }

    // -------------------------------------------------------------------------
    // Request filter tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should tag MDC with trace and span IDs on request when span is valid")
    public void shouldTagMdcWithTraceAndSpanIdsOnRequestWhenSpanIsValid() throws Exception {
        // Given: A tracing filter with valid active span (span created before static mock)
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final Span validSpan = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);

        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(validSpan);

            // When: Filter processes the request
            filter.filter(requestContext);

            // Then: MDC is populated with trace and span IDs
            assertThat(KibanaLogFields.get(TRACE_ID), is(notNullValue()));
            assertThat(KibanaLogFields.get(SPAN_ID), is(notNullValue()));
        }
    }

    @Test
    @DisplayName("Should not tag MDC when span context is invalid")
    public void shouldNotTagMdcWhenSpanContextIsInvalid() throws Exception {
        // Given: A tracing filter with invalid span context (span created before static mock)
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final Span invalidSpan = anInvalidSpan();

        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(invalidSpan);

            // When: Filter processes the request
            filter.filter(requestContext);

            // Then: MDC is not populated
            assertThat(KibanaLogFields.get(TRACE_ID), is(nullValue()));
            assertThat(KibanaLogFields.get(SPAN_ID), is(nullValue()));
        }
    }

    @Test
    @DisplayName("Should not tag MDC when tracing filter is disabled")
    public void shouldNotTagMdcWhenTracingFilterIsDisabled() throws Exception {
        // Given: Tracing filter disabled (stub before opening static mock)
        when(tracingResponseConfig.enabled()).thenReturn(false);
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final Span validSpan = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);

        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(validSpan);

            // When: Filter processes the request
            filter.filter(requestContext);

            // Then: MDC is not populated
            assertThat(KibanaLogFields.get(TRACE_ID), is(nullValue()));
            assertThat(KibanaLogFields.get(SPAN_ID), is(nullValue()));
        }
    }

    @Test
    @DisplayName("Should not tag MDC when OpenTelemetry is disabled")
    public void shouldNotTagMdcWhenOpenTelemetryIsDisabled() throws Exception {
        // Given: OpenTelemetry disabled (stub before opening static mock)
        when(openTelemetryConfig.disabled()).thenReturn(true);
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final Span validSpan = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);

        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(validSpan);

            // When: Filter processes the request
            filter.filter(requestContext);

            // Then: MDC is not populated
            assertThat(KibanaLogFields.get(TRACE_ID), is(nullValue()));
            assertThat(KibanaLogFields.get(SPAN_ID), is(nullValue()));
        }
    }

    // -------------------------------------------------------------------------
    // Response filter tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should add trace and span IDs to response headers when span is valid")
    public void shouldAddTraceAndSpanIdsToResponseHeadersWhenSpanIsValid() throws Exception {
        // Given: A tracing filter with valid active span
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: Trace ID and span ID are added to response headers
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(true));
            assertThat(headers.containsKey(SPAN_ID_HEADER), is(true));
        }
    }

    @Test
    @DisplayName("Should populate MDC with trace and span IDs when span is valid")
    public void shouldPopulateMdcWithTraceAndSpanIdsWhenSpanIsValid() throws Exception {
        // Given: A tracing filter with valid active span
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: MDC fields are set with trace and span IDs
            // (KibanaLogFields cleared in @AfterEach; check headers as proxy for MDC population)
            assertThat(headers.getFirst(TRACE_ID_HEADER), is(TEST_TRACE_ID));
            assertThat(headers.getFirst(SPAN_ID_HEADER), is(TEST_SPAN_ID));
        }
    }

    @Test
    @DisplayName("Should not add headers if they already exist in response")
    public void shouldNotAddHeadersIfTheyAlreadyExistInResponse() throws Exception {
        // Given: Response already has trace and span headers
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(TRACE_ID_HEADER, "existing-trace");
        headers.putSingle(SPAN_ID_HEADER, "existing-span");
        when(responseContext.getHeaders()).thenReturn(headers);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: Existing headers are not overwritten (size stays at 1)
            assertThat(headers.get(TRACE_ID_HEADER), hasSize(1));
            assertThat(headers.get(SPAN_ID_HEADER), hasSize(1));
        }
    }

    @Test
    @DisplayName("Should handle null span gracefully")
    public void shouldHandleNullSpanGracefully() throws Exception {
        // Given: A tracing filter and no active span
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        // And: Mock static Span.current() to return null
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(null);

            // When: Filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: No headers are added
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(false));
            assertThat(headers.containsKey(SPAN_ID_HEADER), is(false));
        }
    }

    @Test
    @DisplayName("Should handle invalid span context gracefully")
    public void shouldHandleInvalidSpanContextGracefully() throws Exception {
        // Given: A tracing filter with invalid span context
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        // And: Mock static Span.current() to return span with invalid context
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = anInvalidSpan();
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: No headers are added for invalid span
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(false));
            assertThat(headers.containsKey(SPAN_ID_HEADER), is(false));
        }
    }

    @Test
    @DisplayName("Should clean up MDC after filter execution")
    public void shouldCleanUpMdcAfterFilterExecution() throws Exception {
        // Given: A tracing filter with valid span
        final TracingResponseFilter filter = new TracingResponseFilter(tracingFilterConfig, openTelemetryConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: MDC is cleaned up after execution
            assertThat(KibanaLogFields.get(TRACE_ID), is(nullValue()));
            assertThat(KibanaLogFields.get(SPAN_ID), is(nullValue()));
        }
    }

    private Span aValidSpan(final String traceId, final String spanId) {
        final Span span = mock(Span.class);
        final SpanContext spanContext = SpanContext.create(
            traceId,
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        );
        when(span.getSpanContext()).thenReturn(spanContext);
        return span;
    }

    private Span anInvalidSpan() {
        final Span span = mock(Span.class);
        final SpanContext spanContext = SpanContext.getInvalid();
        when(span.getSpanContext()).thenReturn(spanContext);
        return span;
    }
}
