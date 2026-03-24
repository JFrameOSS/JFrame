package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.FilterConfig;
import io.github.jframe.logging.filter.JFrameFilter;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.SPAN_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TRACE_ID;
import static io.github.jframe.util.constants.Constants.Headers.SPAN_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;

/**
 * JAX-RS filter that propagates OpenTelemetry trace and span IDs to HTTP response headers.
 *
 * <p>On the inbound request: reads the current OpenTelemetry {@link Span} and, if its context is
 * valid, stores the trace ID and span ID in the Kibana MDC via {@link KibanaLogFields}.
 *
 * <p>On the outbound response: adds {@code x-trace-id} and {@code x-span-id} headers from the MDC
 * values (if present and not already set). The MDC fields are always cleared in a {@code finally}
 * block to prevent leakage across requests.
 *
 * <p>The filter can be disabled via configuration:
 * <pre>{@code
 * jframe.logging.filters.tracing-response.enabled=false
 * }</pre>
 */
@Provider
@ApplicationScoped
@Priority(350)
@Slf4j
public class TracingResponseFilter implements ContainerRequestFilter, ContainerResponseFilter, JFrameFilter {

    private final FilterConfig filterConfig;

    /**
     * Creates a new {@code TracingResponseFilter} with the given filter configuration.
     *
     * @param filterConfig the filter configuration used to determine whether the filter is enabled
     */
    public TracingResponseFilter(final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (!filterConfig.tracingResponse().enabled()) {
            return;
        }
        final SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            KibanaLogFields.tag(TRACE_ID, spanContext.getTraceId());
            KibanaLogFields.tag(SPAN_ID, spanContext.getSpanId());
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        if (!filterConfig.tracingResponse().enabled()) {
            return;
        }
        try {
            final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
            final String traceId = KibanaLogFields.get(TRACE_ID);
            if (traceId != null && !headers.containsKey(TRACE_ID_HEADER)) {
                headers.putSingle(TRACE_ID_HEADER, traceId);
            }
            final String spanId = KibanaLogFields.get(SPAN_ID);
            if (spanId != null && !headers.containsKey(SPAN_ID_HEADER)) {
                headers.putSingle(SPAN_ID_HEADER, spanId);
            }
        } finally {
            KibanaLogFields.clear(TRACE_ID, SPAN_ID);
        }
    }
}
