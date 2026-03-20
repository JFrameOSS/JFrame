package io.github.jframe.tracing.interceptor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * CDI bean that manages OpenTelemetry spans for outbound HTTP calls in Quarkus applications.
 *
 * <p>Provides methods to create, enrich, and finish {@link Span} instances for tracing
 * outbound CLIENT requests. Spans are marked as errors for 4xx/5xx HTTP status codes.
 *
 * <p>When OpenTelemetry is disabled ({@code quarkus.otel.enabled=false}), the injected
 * {@link Tracer} is unavailable and all span operations become no-ops.
 */
@ApplicationScoped
@Slf4j
public class QuarkusSpanManager {

    /** HTTP status codes at or above this value are considered errors. */
    private static final int HTTP_ERROR_THRESHOLD = 400;

    private static final String ATTR_PEER_SERVICE = "peer.service";
    private static final String ATTR_RESPONSE_STATUS_CODE = "ext.response.status_code";

    private final Tracer tracer;

    /**
     * Constructs a new {@code QuarkusSpanManager} with an optional {@link Tracer}.
     * When OpenTelemetry is disabled, the tracer instance is not available and
     * all span operations become no-ops.
     *
     * @param tracerInstance the optional OpenTelemetry tracer
     */
    public QuarkusSpanManager(final Instance<Tracer> tracerInstance) {
        this.tracer = tracerInstance.isResolvable() ? tracerInstance.get() : null;
    }

    /**
     * Returns whether a {@link Tracer} is available for span creation.
     *
     * @return {@code true} if OpenTelemetry tracing is active
     */
    public boolean isAvailable() {
        return tracer != null;
    }

    /**
     * Creates a new CLIENT span for an outbound HTTP request.
     * Returns {@code null} when no {@link Tracer} is available.
     *
     * @param method      HTTP method (e.g. "GET", "POST")
     * @param url         target URL
     * @param serviceName name of the downstream service
     * @return the started {@link Span}, or {@code null} if tracing is unavailable
     */
    public Span createOutboundSpan(final String method, final String url, final String serviceName) {
        if (tracer == null) {
            return null;
        }
        return tracer.spanBuilder(method + " " + url)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(ATTR_PEER_SERVICE, serviceName)
            .startSpan();
    }

    /**
     * Enriches an existing span with HTTP response attributes.
     * Marks the span as {@link StatusCode#ERROR} for status codes &gt;= 400.
     * No-op when {@code span} is {@code null}.
     *
     * @param span       the span to enrich (may be {@code null})
     * @param statusCode HTTP response status code
     * @param headers    response headers (reserved for future enrichment)
     */
    public void enrichOutboundSpan(final Span span, final int statusCode, final MultivaluedMap<String, String> headers) {
        if (span == null) {
            return;
        }
        span.setAttribute(ATTR_RESPONSE_STATUS_CODE, statusCode);
        if (statusCode >= HTTP_ERROR_THRESHOLD) {
            span.setStatus(StatusCode.ERROR);
        }
    }

    /**
     * Finishes the given span by calling {@link Span#end()}.
     * No-op when {@code span} is {@code null}.
     *
     * @param span the span to finish (may be {@code null})
     */
    public void finishSpan(final Span span) {
        if (span != null) {
            span.end();
        }
    }
}
