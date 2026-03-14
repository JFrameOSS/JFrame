package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.SPAN_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TRACE_ID;

/**
 * JAX-RS response filter that propagates OpenTelemetry trace and span IDs into the HTTP
 * response headers and the Kibana MDC log context.
 *
 * <p>After filter execution the TRACE_ID and SPAN_ID fields are always cleared from the
 * MDC regardless of whether a valid span was found.
 */
public class TracingResponseFilter implements ContainerResponseFilter {

    private final String traceHeader;
    private final String spanHeader;

    /**
     * Constructs a new {@code TracingResponseFilter} with the given header names.
     *
     * @param traceHeader the HTTP header name for the trace ID
     * @param spanHeader  the HTTP header name for the span ID
     */
    public TracingResponseFilter(final String traceHeader, final String spanHeader) {
        this.traceHeader = traceHeader;
        this.spanHeader = spanHeader;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) {
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        try {
            final Span span = Span.current();
            if (span != null) {
                final SpanContext spanContext = span.getSpanContext();
                if (spanContext.isValid()) {
                    final String traceId = spanContext.getTraceId();
                    final String spanId = spanContext.getSpanId();

                    if (!headers.containsKey(traceHeader)) {
                        headers.putSingle(traceHeader, traceId);
                    }
                    if (!headers.containsKey(spanHeader)) {
                        headers.putSingle(spanHeader, spanId);
                    }

                    KibanaLogFields.tag(TRACE_ID, traceId);
                    KibanaLogFields.tag(SPAN_ID, spanId);
                }
            }
        } finally {
            KibanaLogFields.clear(TRACE_ID, SPAN_ID);
        }
    }
}
