package io.github.jframe.tracing.interceptor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * CDI bean that manages OpenTelemetry spans for outbound HTTP calls in Quarkus applications.
 *
 * <p>Provides methods to create, enrich, and finish {@link Span} instances for tracing
 * outbound CLIENT requests. Spans are marked as errors for 4xx/5xx HTTP status codes.
 *
 * <p>Only activated when {@code quarkus.otel.enabled=true}; otherwise the bean is not
 * registered, avoiding an unsatisfied {@link Tracer} dependency when OTel is disabled.
 */
@ApplicationScoped
@IfBuildProperty(
    name = "quarkus.otel.enabled",
    stringValue = "true"
)
public class QuarkusSpanManager {

    /** HTTP status codes at or above this value are considered errors. */
    private static final int HTTP_ERROR_THRESHOLD = 400;

    private static final String ATTR_PEER_SERVICE = "peer.service";
    private static final String ATTR_RESPONSE_STATUS_CODE = "ext.response.status_code";

    private final Tracer tracer;

    /**
     * Constructs a new {@code QuarkusSpanManager} with the given {@link Tracer}.
     *
     * @param tracer the OpenTelemetry tracer used to create and manage spans
     */
    public QuarkusSpanManager(final Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Creates a new CLIENT span for an outbound HTTP request.
     *
     * @param method      HTTP method (e.g. "GET", "POST")
     * @param url         target URL
     * @param serviceName name of the downstream service
     * @return the started {@link Span}
     */
    public Span createOutboundSpan(final String method, final String url, final String serviceName) {
        return tracer.spanBuilder(method + " " + url)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(ATTR_PEER_SERVICE, serviceName)
            .startSpan();
    }

    /**
     * Enriches an existing span with HTTP response attributes.
     * Marks the span as {@link StatusCode#ERROR} for status codes &gt;= 400.
     *
     * @param span       the span to enrich
     * @param statusCode HTTP response status code
     * @param headers    response headers (reserved for future enrichment)
     */
    public void enrichOutboundSpan(final Span span, final int statusCode, final MultivaluedMap<String, String> headers) {
        span.setAttribute(ATTR_RESPONSE_STATUS_CODE, statusCode);
        if (statusCode >= HTTP_ERROR_THRESHOLD) {
            span.setStatus(StatusCode.ERROR);
        }
    }

    /**
     * Finishes the given span by calling {@link Span#end()}.
     *
     * @param span the span to finish
     */
    public void finishSpan(final Span span) {
        span.end();
    }
}
