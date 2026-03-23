package io.github.jframe.tracing.filter;

import io.github.jframe.logging.filter.otlp.TracingFilterConfig;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.tracing.OpenTelemetryConfig;
import io.github.jframe.tracing.interceptor.QuarkusSpanManager;
import io.opentelemetry.api.trace.Span;
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

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.LINE_BREAK;
import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.REQUEST_PREFIX;
import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.RESPONSE_PREFIX;
import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.TAB;
import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;

/**
 * JAX-RS client filter that creates and manages OpenTelemetry CLIENT spans for outbound HTTP calls.
 *
 * <p>On the request side: creates a span via {@link QuarkusSpanManager}, stores it as a property
 * on the request context, injects W3C trace context headers via
 * {@link QuarkusSpanManager#injectTraceContext(MultivaluedMap)}, and propagates correlation headers
 * ({@code x-transaction-id}, {@code x-request-id}, {@code x-trace-id}) from the current
 * {@link KibanaLogFields} context.
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
     * @param spanManager         manages span lifecycle (create, enrich, finish) and trace propagation
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
        spanManager.injectTraceContext(headers);
        addCorrelationHeaders(headers, span);
        logRequest(method, url, headers);
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
        logResponse(status, headers);
    }

    private boolean isExcluded(final String path) {
        return openTelemetryConfig.excludedMethods().stream()
            .anyMatch(path::contains);
    }

    private static void addCorrelationHeaders(final MultivaluedMap<String, Object> headers, final Span span) {
        final String txId = KibanaLogFields.get(TX_ID);
        final String requestId = KibanaLogFields.get(REQUEST_ID);
        final String traceId = span != null ? span.getSpanContext().getTraceId() : null;
        if (txId != null) {
            headers.putSingle(TX_ID_HEADER, txId);
        }
        if (requestId != null) {
            headers.putSingle(REQ_ID_HEADER, requestId);
        }
        if (traceId != null) {
            headers.putSingle(TRACE_ID_HEADER, traceId);
        }
    }

    private void logRequest(final String method, final String uri,
        final MultivaluedMap<String, Object> headers) {
        if (log.isDebugEnabled()) {
            log.debug(
                "{}Method: {}{}URI: {}{}Headers: {}",
                REQUEST_PREFIX,
                method,
                LINE_BREAK + TAB,
                uri,
                LINE_BREAK + TAB,
                headers
            );
        }
    }

    private void logResponse(final int status, final MultivaluedMap<String, String> headers) {
        if (log.isDebugEnabled()) {
            log.debug(
                "{}Status: {}{}Headers: {}",
                RESPONSE_PREFIX,
                status,
                LINE_BREAK + TAB,
                headers
            );
        }
    }
}
