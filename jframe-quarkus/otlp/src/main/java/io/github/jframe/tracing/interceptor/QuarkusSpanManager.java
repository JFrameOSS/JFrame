package io.github.jframe.tracing.interceptor;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.security.QuarkusAuthenticationUtil;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.quarkus.security.identity.SecurityIdentity;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.MultivaluedMap;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_REQUEST_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_REQUEST_QUERY;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_REQUEST_URI;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_CONTENT_LENGTH;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_CONTENT_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_L7_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_STATUS_CODE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.PEER_SERVICE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_NAME;
import static io.github.jframe.util.constants.Constants.Headers.L7_REQUEST_ID;

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

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;
    private final Instance<SecurityIdentity> securityIdentityInstance;

    /**
     * Constructs a new {@code QuarkusSpanManager} with optional tracing dependencies.
     * When OpenTelemetry is disabled, the tracer/openTelemetry instances are unavailable and
     * all span operations become no-ops.
     *
     * @param tracerInstance           the optional OpenTelemetry tracer
     * @param openTelemetryInstance    the optional OpenTelemetry instance for W3C trace injection
     * @param securityIdentityInstance the optional CDI security identity
     */
    public QuarkusSpanManager(
                              final Instance<Tracer> tracerInstance,
                              final Instance<OpenTelemetry> openTelemetryInstance,
                              final Instance<SecurityIdentity> securityIdentityInstance) {
        this.tracer = tracerInstance.isResolvable() ? tracerInstance.get() : null;
        this.openTelemetry = openTelemetryInstance.isResolvable() ? openTelemetryInstance.get() : null;
        this.securityIdentityInstance = securityIdentityInstance;
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
     *
     * <p>Parses the provided URL to extract the host and path for span attributes. When the URL
     * cannot be parsed, the raw URL string is used as a fallback.
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

        final String host = resolveHost(url);
        final String path = resolvePath(url);
        final String query = resolveQuery(url);
        final SecurityIdentity securityIdentity =
            securityIdentityInstance.isResolvable() ? securityIdentityInstance.get() : null;

        return tracer.spanBuilder(method + " " + host)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(PEER_SERVICE, serviceName)
            .setAttribute(SERVICE_NAME, host)
            .setAttribute(HTTP_REMOTE_USER, QuarkusAuthenticationUtil.getSubject(securityIdentity))
            .setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID))
            .setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID))
            .setAttribute(EXT_REQUEST_URI, host + path)
            .setAttribute(EXT_REQUEST_QUERY, query)
            .setAttribute(EXT_REQUEST_METHOD, method)
            .startSpan();
    }

    /**
     * Enriches an existing span with HTTP response attributes.
     * Marks the span as {@link StatusCode#ERROR} for status codes &gt;= 400.
     * No-op when {@code span} is {@code null}.
     *
     * @param span       the span to enrich (may be {@code null})
     * @param statusCode HTTP response status code
     * @param headers    response headers used for content-type, content-length, and L7 request ID
     */
    public void enrichOutboundSpan(final Span span, final int statusCode, final MultivaluedMap<String, String> headers) {
        if (span == null) {
            return;
        }
        log.trace("Enriching outbound span with status code: {}", statusCode);
        span.setAttribute(EXT_RESPONSE_STATUS_CODE, (long) statusCode);
        span.setAttribute(EXT_RESPONSE_CONTENT_TYPE, extractContentType(headers));
        span.setAttribute(EXT_RESPONSE_CONTENT_LENGTH, extractContentLength(headers));
        setL7RequestId(span, headers);
        if (statusCode >= HTTP_ERROR_THRESHOLD) {
            span.setAttribute(ERROR, true);
            span.setStatus(StatusCode.ERROR);
        }
    }

    /**
     * Injects W3C trace context headers into the given outbound request headers map.
     * No-op when {@link OpenTelemetry} is unavailable.
     *
     * @param headers the JAX-RS request headers to inject trace context into
     */
    public void injectTraceContext(final MultivaluedMap<String, Object> headers) {
        if (openTelemetry == null) {
            return;
        }
        openTelemetry.getPropagators().getTextMapPropagator()
            .inject(Context.current(), headers, MultivaluedMap::putSingle);
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

    private String resolveHost(final String url) {
        try {
            final String host = java.net.URI.create(url).getHost();
            return host != null ? host : url;
        } catch (final IllegalArgumentException e) {
            log.warn("Failed to parse URL for span host extraction: {}", url, e);
            return url;
        }
    }

    private String resolvePath(final String url) {
        try {
            final String path = java.net.URI.create(url).getPath();
            return path != null ? path : "";
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    private String resolveQuery(final String url) {
        try {
            final String query = java.net.URI.create(url).getQuery();
            return query != null ? query : "";
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    private static String extractContentType(final MultivaluedMap<String, String> headers) {
        final String contentType = headers.getFirst("Content-Type");
        return contentType != null ? contentType : "unknown";
    }

    private static String extractContentLength(final MultivaluedMap<String, String> headers) {
        final String contentLength = headers.getFirst("Content-Length");
        return contentLength != null ? contentLength : "-1";
    }

    private static void setL7RequestId(final Span span, final MultivaluedMap<String, String> headers) {
        final String l7RequestId = headers.getFirst(L7_REQUEST_ID);
        if (l7RequestId != null) {
            span.setAttribute(EXT_RESPONSE_L7_REQUEST_ID, l7RequestId);
        }
    }
}
