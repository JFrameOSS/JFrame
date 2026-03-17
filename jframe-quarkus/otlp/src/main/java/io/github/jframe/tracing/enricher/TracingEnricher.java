package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

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
 */
public class TracingEnricher implements ErrorResponseEnricher {

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
        final MediaType mediaType = requestContext.getMediaType();
        final String contentType = mediaType != null ? mediaType.getType() + "/" + mediaType.getSubtype() : null;

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
                span.setAttribute(HTTP_URI, uri);
                span.setAttribute(HTTP_METHOD, method);
                span.setAttribute(HTTP_STATUS_CODE, (long) statusCode);
                span.setAttribute(HTTP_CONTENT_TYPE, contentType);
            }
        }
    }
}
