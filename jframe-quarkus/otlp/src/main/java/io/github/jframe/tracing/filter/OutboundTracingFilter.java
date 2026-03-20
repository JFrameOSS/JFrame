package io.github.jframe.tracing.filter;

import io.github.jframe.logging.filter.TracingFilterConfig;
import io.github.jframe.tracing.OpenTelemetryConfig;
import io.github.jframe.tracing.interceptor.QuarkusSpanManager;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS client filter that creates and manages OpenTelemetry CLIENT spans for outbound HTTP calls.
 *
 * <p>On the request side: creates a span via {@link QuarkusSpanManager}, stores it as a property
 * on the request context, and injects the W3C {@code traceparent} header into the outbound request.
 * On the response side: enriches the span with the HTTP response status code and finishes it in
 * a {@code finally} block to ensure spans are always closed even when enrichment fails.
 *
 * <p>Span creation is skipped when {@link TracingFilterConfig.OutboundTracingConfig#enabled()} is
 * {@code false}, when {@link OpenTelemetryConfig#disabled()} is {@code true}, or when the URI path
 * contains any segment listed in {@link OpenTelemetryConfig#excludedMethods()}.
 */
@Provider
@ApplicationScoped
@Priority(200)
@Slf4j
public class OutboundTracingFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final String SPAN_PROPERTY_KEY =
        "io.github.jframe.tracing.filter.OutboundTracingFilter.SPAN";

    private final QuarkusSpanManager spanManager;
    private final OpenTelemetryConfig openTelemetryConfig;
    private final TracingFilterConfig tracingFilterConfig;

    /**
     * Creates a new {@code OutboundTracingFilter}.
     *
     * @param spanManager         manages span lifecycle (create, enrich, finish)
     * @param openTelemetryConfig configuration for enabling/disabling tracing and excluded paths
     * @param tracingFilterConfig configuration for enabling/disabling this specific filter
     */
    public OutboundTracingFilter(final QuarkusSpanManager spanManager,
                                 final OpenTelemetryConfig openTelemetryConfig,
                                 final TracingFilterConfig tracingFilterConfig) {
        this.spanManager = spanManager;
        this.openTelemetryConfig = openTelemetryConfig;
        this.tracingFilterConfig = tracingFilterConfig;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        final String method = requestContext.getMethod();
        final String url = requestContext.getUri().toString();
        final String path = requestContext.getUri().getPath();
        final String serviceName = requestContext.getUri().getHost();
        final MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        if (!tracingFilterConfig.outboundTracing().enabled() || openTelemetryConfig.disabled() || isExcluded(path)) {
            return;
        }
        final Span span = spanManager.createOutboundSpan(method, url, serviceName);
        requestContext.setProperty(SPAN_PROPERTY_KEY, span);
        injectTraceparent(headers, span);
    }

    @Override
    public void filter(final ClientRequestContext requestContext,
        final ClientResponseContext responseContext) throws IOException {
        final int status = responseContext.getStatus();
        final MultivaluedMap<String, String> headers = responseContext.getHeaders();
        final Span span = (Span) requestContext.getProperty(SPAN_PROPERTY_KEY);
        if (span == null) {
            return;
        }
        try {
            spanManager.enrichOutboundSpan(span, status, headers);
        } finally {
            spanManager.finishSpan(span);
        }
    }

    private boolean isExcluded(final String path) {
        return openTelemetryConfig.excludedMethods().stream()
            .anyMatch(path::contains);
    }

    private void injectTraceparent(final MultivaluedMap<String, Object> headers, final Span span) {
        final SpanContext spanContext = span.getSpanContext();
        final String traceparent = "00-" + spanContext.getTraceId()
            + "-" + spanContext.getSpanId()
            + "-01";
        headers.add("traceparent", traceparent);
    }
}
