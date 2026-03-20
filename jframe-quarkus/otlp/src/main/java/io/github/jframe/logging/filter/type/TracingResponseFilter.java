package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.JFrameFilter;
import io.github.jframe.logging.filter.TracingFilterConfig;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.tracing.OpenTelemetryConfig;
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
 * JAX-RS filter that propagates OpenTelemetry trace and span IDs into the MDC log context
 * on the inbound request, and into the HTTP response headers on the outbound response.
 *
 * <p>On the inbound request: reads the current {@link Span} and, when valid, tags
 * {@code TRACE_ID} and {@code SPAN_ID} into {@link KibanaLogFields} so that all
 * request-phase logging carries the trace context.
 *
 * <p>On the outbound response: adds the trace and span ID headers (if not already present)
 * and always clears the {@code TRACE_ID} and {@code SPAN_ID} MDC fields in a {@code finally}
 * block.
 */
@Provider
@ApplicationScoped
@Priority(50)
@Slf4j
public class TracingResponseFilter implements ContainerRequestFilter, ContainerResponseFilter, JFrameFilter {

    private final TracingFilterConfig tracingFilterConfig;
    private final OpenTelemetryConfig openTelemetryConfig;

    /**
     * Creates a new {@code TracingResponseFilter} with the given tracing filter and OTEL configuration.
     *
     * @param tracingFilterConfig the tracing filter configuration used to determine whether this filter is enabled
     * @param openTelemetryConfig the OpenTelemetry configuration used to determine whether tracing is active
     */
    public TracingResponseFilter(final TracingFilterConfig tracingFilterConfig,
                                 final OpenTelemetryConfig openTelemetryConfig) {
        this.tracingFilterConfig = tracingFilterConfig;
        this.openTelemetryConfig = openTelemetryConfig;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (!tracingFilterConfig.tracingResponse().enabled() || openTelemetryConfig.disabled()) {
            return;
        }
        final Span span = Span.current();
        if (span != null) {
            final SpanContext spanContext = span.getSpanContext();
            if (spanContext.isValid()) {
                final String traceId = spanContext.getTraceId();
                final String spanId = spanContext.getSpanId();
                KibanaLogFields.tag(TRACE_ID, traceId);
                KibanaLogFields.tag(SPAN_ID, spanId);
                log.trace("Populated trace context: traceId='{}', spanId='{}'", traceId, spanId);
            }
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        if (!tracingFilterConfig.tracingResponse().enabled() || openTelemetryConfig.disabled()) {
            return;
        }
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        try {
            final Span span = Span.current();
            if (span != null) {
                final SpanContext spanContext = span.getSpanContext();
                if (spanContext.isValid()) {
                    final String traceId = spanContext.getTraceId();
                    final String spanId = spanContext.getSpanId();
                    if (!headers.containsKey(TRACE_ID_HEADER)) {
                        headers.putSingle(TRACE_ID_HEADER, traceId);
                    }
                    if (!headers.containsKey(SPAN_ID_HEADER)) {
                        headers.putSingle(SPAN_ID_HEADER, spanId);
                    }
                }
            }
        } finally {
            KibanaLogFields.clear(TRACE_ID, SPAN_ID);
        }
    }
}
