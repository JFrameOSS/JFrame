package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.security.QuarkusAuthenticationUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.quarkus.security.identity.SecurityIdentity;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_CONTENT_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_QUERY;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_STATUS_CODE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_URI;

/**
 * Enricher that sets the trace ID and span ID from the current OpenTelemetry span.
 *
 * <p>When a valid span context is present, this enricher:
 * <ul>
 * <li>Sets traceId and spanId on the {@link ErrorResponseResource}</li>
 * <li>Marks the span status as {@link StatusCode#ERROR}</li>
 * <li>Records error, HTTP context, and correlation attributes on the span</li>
 * </ul>
 *
 * <p>If no active span exists or the span context is invalid, enrichment is gracefully skipped.
 *
 * <p>Note: {@code http.content_length} is not set by this enricher because the
 * {@code doEnrich} contract only provides the {@link ContainerRequestContext} (inbound request),
 * not the response context. Content-length is a response attribute and is not available
 * at error-enrichment time without buffering the response body.
 */
@Slf4j
@ApplicationScoped
public class TracingEnricher implements ErrorResponseEnricher {

    private final Instance<SecurityIdentity> securityIdentityInstance;

    /**
     * Creates a new {@code TracingEnricher} without security identity support.
     * Used when instantiated directly (e.g., in tests without CDI context).
     */
    TracingEnricher() {
        this.securityIdentityInstance = null;
    }

    /**
     * Creates a new {@code TracingEnricher} with CDI security identity support.
     *
     * @param securityIdentityInstance the optional CDI security identity instance
     */
    @Inject
    public TracingEnricher(final Instance<SecurityIdentity> securityIdentityInstance) {
        this.securityIdentityInstance = securityIdentityInstance;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Enriches the error response and the active OpenTelemetry span with tracing context,
     * error details, and HTTP request attributes when a valid span is present.
     */
    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        final String method = requestContext.getMethod();
        final UriInfo uriInfo = requestContext.getUriInfo();
        final String uri = uriInfo != null ? uriInfo.getRequestUri().getPath() : null;
        final String query = resolveQuery(uriInfo);
        final MediaType mediaType = requestContext.getMediaType();
        final String contentType = mediaType != null ? mediaType.getType() + "/" + mediaType.getSubtype() : null;
        final SecurityIdentity securityIdentity =
            securityIdentityInstance != null && securityIdentityInstance.isResolvable()
                ? securityIdentityInstance.get() : null;

        final Span span = Span.current();
        if (span != null) {
            final SpanContext spanContext = span.getSpanContext();
            if (spanContext.isValid()) {
                resource.setTraceId(spanContext.getTraceId());
                resource.setSpanId(spanContext.getSpanId());
                span.setStatus(StatusCode.ERROR);
                span.setAttribute(ERROR, true);
                span.setAttribute(ERROR_TYPE, throwable.getClass().getSimpleName());
                span.setAttribute(ERROR_MESSAGE, throwable.getMessage());
                span.setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID));
                span.setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID));
                span.setAttribute(HTTP_REMOTE_USER, QuarkusAuthenticationUtil.getSubject(securityIdentity));
                span.setAttribute(HTTP_URI, uri);
                span.setAttribute(HTTP_METHOD, method);
                span.setAttribute(HTTP_STATUS_CODE, (long) statusCode);
                span.setAttribute(HTTP_CONTENT_TYPE, contentType);
                if (query != null) {
                    span.setAttribute(HTTP_QUERY, query);
                }
            }
        }
    }

    private static String resolveQuery(final UriInfo uriInfo) {
        if (uriInfo == null) {
            return null;
        }
        return uriInfo.getRequestUri().getQuery();
    }
}
