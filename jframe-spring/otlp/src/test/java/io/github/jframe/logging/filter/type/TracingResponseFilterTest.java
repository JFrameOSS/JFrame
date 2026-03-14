package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.SPAN_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TRACE_ID;
import static io.github.jframe.util.constants.Constants.Headers.SPAN_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TracingResponseFilter}.
 *
 * <p>Verifies the TracingResponseFilter functionality including:
 * <ul>
 * <li>Adding trace ID and span ID to response headers</li>
 * <li>Populating MDC (KibanaLogFields) with trace and span IDs</li>
 * <li>Handling valid and invalid span contexts</li>
 * <li>Preventing duplicate headers</li>
 * <li>Filter chain execution</li>
 * <li>MDC cleanup after filter execution</li>
 * </ul>
 */
@DisplayName("Logging Filters - Tracing Response Filter")
class TracingResponseFilterTest extends UnitTest {

    @Test
    @DisplayName("Should add trace and span IDs to response headers when span is valid")
    void shouldAddTraceAndSpanIdsToResponseHeadersWhenSpanIsValid() throws Exception {
        // Given: A tracing filter with valid active span
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(TRACE_ID_HEADER)).thenReturn(false);
        when(response.containsHeader(SPAN_ID_HEADER)).thenReturn(false);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Trace ID and span ID are added to response headers
            verify(response).setHeader(TRACE_ID_HEADER, TEST_TRACE_ID);
            verify(response).setHeader(SPAN_ID_HEADER, TEST_SPAN_ID);
        }
    }

    @Test
    @DisplayName("Should populate MDC with trace and span IDs when span is valid")
    void shouldPopulateMdcWithTraceAndSpanIdsWhenSpanIsValid() throws Exception {
        // Given: A tracing filter with valid active span
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(TRACE_ID_HEADER)).thenReturn(false);
        when(response.containsHeader(SPAN_ID_HEADER)).thenReturn(false);

        // And: Capture MDC values during filter chain execution
        final String[] capturedTraceId = new String[1];
        final String[] capturedSpanId = new String[1];
        doAnswer(invocation -> {
            capturedTraceId[0] = KibanaLogFields.get(TRACE_ID);
            capturedSpanId[0] = KibanaLogFields.get(SPAN_ID);
            return null;
        }).when(filterChain).doFilter(request, response);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: MDC was populated with trace and span IDs during filter chain execution
            assertThat(capturedTraceId[0]).isEqualTo(TEST_TRACE_ID);
            assertThat(capturedSpanId[0]).isEqualTo(TEST_SPAN_ID);
        }
    }

    @Test
    @DisplayName("Should not add headers if they already exist in response")
    void shouldNotAddHeadersIfTheyAlreadyExistInResponse() throws Exception {
        // Given: A tracing filter with response already containing headers
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(TRACE_ID_HEADER)).thenReturn(true);
        when(response.containsHeader(SPAN_ID_HEADER)).thenReturn(true);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Headers are not added
            verify(response, never()).setHeader(eq(TRACE_ID_HEADER), anyString());
            verify(response, never()).setHeader(eq(SPAN_ID_HEADER), anyString());
        }
    }

    @Test
    @DisplayName("Should handle null span gracefully")
    void shouldHandleNullSpanGracefully() throws Exception {
        // Given: A tracing filter with no active span
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        // And: Mock static Span.current() to return null
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(null);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: No headers are added
            verify(response, never()).setHeader(anyString(), anyString());

            // And: Filter chain continues
            verify(filterChain).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("Should handle invalid span context gracefully")
    void shouldHandleInvalidSpanContextGracefully() throws Exception {
        // Given: A tracing filter with invalid span context
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        // And: Mock static Span.current() to return span with invalid context
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = anInvalidSpan();
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: No headers are added
            verify(response, never()).setHeader(anyString(), anyString());

            // And: Filter chain continues
            verify(filterChain).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("Should continue filter chain execution")
    void shouldContinueFilterChainExecution() throws Exception {
        // Given: A tracing filter
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(TRACE_ID_HEADER)).thenReturn(false);
        when(response.containsHeader(SPAN_ID_HEADER)).thenReturn(false);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Filter chain is continued
            verify(filterChain).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("Should clean up MDC after filter execution")
    void shouldCleanUpMdcAfterFilterExecution() throws Exception {
        // Given: A tracing filter
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(TRACE_ID_HEADER)).thenReturn(false);
        when(response.containsHeader(SPAN_ID_HEADER)).thenReturn(false);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: MDC is cleaned up after execution
            assertThat(KibanaLogFields.get(TRACE_ID)).isNull();
            assertThat(KibanaLogFields.get(SPAN_ID)).isNull();
        }
    }

    @Test
    @DisplayName("Should clean up MDC even when filter chain throws exception")
    void shouldCleanUpMdcEvenWhenFilterChainThrowsException() throws Exception {
        // Given: A tracing filter with filter chain that throws exception
        final TracingResponseFilter filter = new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(TRACE_ID_HEADER)).thenReturn(false);
        when(response.containsHeader(SPAN_ID_HEADER)).thenReturn(false);
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed and exception is thrown
            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (final RuntimeException exception) {
                // Expected exception
            }

            // Then: MDC is still cleaned up after exception
            assertThat(KibanaLogFields.get(TRACE_ID)).isNull();
            assertThat(KibanaLogFields.get(SPAN_ID)).isNull();
        }
    }

    @Test
    @DisplayName("Should use custom header names when provided")
    void shouldUseCustomHeaderNamesWhenProvided() throws Exception {
        // Given: A tracing filter with custom header names
        final String customTraceHeader = "Custom-Trace-Id";
        final String customSpanHeader = "Custom-Span-Id";
        final TracingResponseFilter filter = new TracingResponseFilter(customTraceHeader, customSpanHeader);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(customTraceHeader)).thenReturn(false);
        when(response.containsHeader(customSpanHeader)).thenReturn(false);

        // And: Mock static Span.current() to return valid span
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Custom header names are used
            verify(response).setHeader(customTraceHeader, TEST_TRACE_ID);
            verify(response).setHeader(customSpanHeader, TEST_SPAN_ID);
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
