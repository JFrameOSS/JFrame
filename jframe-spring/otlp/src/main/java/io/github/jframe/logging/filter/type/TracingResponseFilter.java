package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.filter.AbstractGenericFilter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static io.github.jframe.logging.ecs.EcsFieldNames.SPAN_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TRACE_ID;

/**
 * A filter that adds OpenTelemetry trace and span IDs to the response headers and populates
 * the MDC (Mapped Diagnostic Context) for log correlation.
 *
 * <p>This filter performs two key functions:
 * <ul>
 * <li>Adds trace ID and span ID to HTTP response headers for client-side correlation</li>
 * <li>Populates the SLF4J MDC with trace and span IDs so all logs within the request include them</li>
 * </ul>
 *
 * <p>This enables seamless correlation between logs and distributed traces in observability platforms
 * like Jaeger, and Zipkin.
 */
@Slf4j
@RequiredArgsConstructor
public class TracingResponseFilter extends AbstractGenericFilter {

    /** The configured header name to write the trace id in. */
    private final String tracingHeaderName;

    /** The configured header name to write the span id in. */
    private final String spanHeaderName;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
        throws ServletException, IOException {
        final Span activeSpan = Span.current();
        if (activeSpan != null) {
            final SpanContext spanContext = activeSpan.getSpanContext();
            if (spanContext.isValid()) {
                final String traceId = spanContext.getTraceId();
                addHeader(response, tracingHeaderName, traceId);
                EcsFields.tag(TRACE_ID, traceId);

                final String spanId = spanContext.getSpanId();
                addHeader(response, spanHeaderName, spanId);
                EcsFields.tag(SPAN_ID, spanId);
                log.trace("Populated trace context: traceId='{}', spanId='{}'", traceId, spanId);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            EcsFields.clear(TRACE_ID, SPAN_ID);
        }
    }

    private void addHeader(final HttpServletResponse response, final String headerName, final String value) {
        if (!response.containsHeader(headerName)) {
            response.setHeader(headerName, value);
        }
    }
}
