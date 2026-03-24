package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.security.QuarkusAuthenticationUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_CONTENT_LENGTH;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_CONTENT_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_QUERY;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_STATUS_CODE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_URI;

/**
 * CDI enricher that augments error responses with OpenTelemetry trace and span IDs,
 * and enriches the current span with error details and HTTP request metadata.
 *
 * <p>When the current span is recording, this enricher:
 * <ul>
 * <li>Sets the {@code traceId} and {@code spanId} fields on the error response resource</li>
 * <li>Marks the span status as {@link StatusCode#ERROR}</li>
 * <li>Attaches error attributes ({@code error}, {@code error.type}, {@code error.message})</li>
 * <li>Attaches HTTP metadata attributes (URI, method, status code, content type, etc.)</li>
 * <li>Attaches correlation IDs from MDC (transaction ID, request ID)</li>
 * <li>Attaches the authenticated subject from the security context</li>
 * </ul>
 *
 * <p>If the current span is not recording (e.g. no tracing backend configured), no enrichment
 * is performed and the method returns immediately.
 */
@ApplicationScoped
public class TracingEnricher implements ErrorResponseEnricher {

    @Inject
    private QuarkusAuthenticationUtil authUtil;

    /**
     * No-arg constructor required by CDI.
     */
    public TracingEnricher() {
        // CDI proxy constructor
    }

    /**
     * Constructor for injection and testing.
     *
     * @param authUtil the authentication utility
     */
    public TracingEnricher(final QuarkusAuthenticationUtil authUtil) {
        this.authUtil = authUtil;
    }

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        final Span currentSpan = Span.current();
        if (!currentSpan.isRecording()) {
            return;
        }

        resource.setTraceId(currentSpan.getSpanContext().getTraceId());
        resource.setSpanId(currentSpan.getSpanContext().getSpanId());

        currentSpan.setStatus(StatusCode.ERROR);
        currentSpan.setAttribute(ERROR, true);
        currentSpan.setAttribute(ERROR_TYPE, throwable.getClass().getSimpleName());
        currentSpan.setAttribute(ERROR_MESSAGE, throwable.getMessage());
        currentSpan.setAttribute(HTTP_REMOTE_USER, authUtil.getAuthenticatedSubject());
        currentSpan.setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID));
        currentSpan.setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID));
        currentSpan.setAttribute(HTTP_URI, requestContext.getUriInfo().getRequestUri().getPath());
        currentSpan.setAttribute(HTTP_QUERY, requestContext.getUriInfo().getRequestUri().getQuery());
        currentSpan.setAttribute(HTTP_METHOD, requestContext.getMethod());
        currentSpan.setAttribute(HTTP_STATUS_CODE, statusCode);
        final jakarta.ws.rs.core.MediaType mediaType = requestContext.getMediaType();
        currentSpan.setAttribute(HTTP_CONTENT_TYPE, mediaType.getType() + "/" + mediaType.getSubtype());
        currentSpan.setAttribute(HTTP_CONTENT_LENGTH, requestContext.getLength());
    }
}
