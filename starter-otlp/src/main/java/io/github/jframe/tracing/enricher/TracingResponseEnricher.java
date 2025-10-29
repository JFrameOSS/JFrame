package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.handler.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static io.github.jframe.OpenTelemetryConstants.Attributes.*;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.security.AuthenticationUtil.getAuthenticatedSubject;

/**
 * Enriches the error response with tracing information when an error occurs in a web request.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "otel.sdk.disabled",
    havingValue = "false"
)
public class TracingResponseEnricher implements ErrorResponseEnricher {

    /**
     * {@inheritDoc}
     *
     * <p><strong>NOTE:</strong> This enricher only applies if the request is a {@link ServletWebRequest}. Also for security reasons, it
     * only enriches the response with the necessary information and a {@link KibanaLogFieldNames#TX_ID} to correlate the request in Kibana.
     * The span may not contain sensitive information such as user credentials or personal data.
     * </p>
     */
    @Override
    public void doEnrich(final ErrorResponseResource errorResponseResource, final Throwable throwable,
        final WebRequest request, final HttpStatus httpStatus) {
        final Span currentSpan = Span.current();
        if (currentSpan.isRecording() && request instanceof ServletWebRequest servletWebRequest) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletWebRequest.getNativeRequest();

            // Enrich the error response with trace and span IDs for correlation
            errorResponseResource.setTraceId(currentSpan.getSpanContext().getTraceId());
            errorResponseResource.setSpanId(currentSpan.getSpanContext().getSpanId());

            // Enrich the span with error information
            currentSpan.setStatus(StatusCode.ERROR);
            currentSpan.setAttribute(ERROR, true);
            currentSpan.setAttribute(ERROR_TYPE, throwable.getClass().getSimpleName());
            currentSpan.setAttribute(ERROR_MESSAGE, throwable.getMessage());
            currentSpan.setAttribute(HTTP_REMOTE_USER, getAuthenticatedSubject());
            currentSpan.setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID));
            currentSpan.setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID));
            currentSpan.setAttribute(HTTP_URI, httpServletRequest.getRequestURI());
            currentSpan.setAttribute(HTTP_QUERY, httpServletRequest.getQueryString());
            currentSpan.setAttribute(HTTP_METHOD, httpServletRequest.getMethod());
            currentSpan.setAttribute(HTTP_STATUS_CODE, httpStatus.value());
            currentSpan.setAttribute(HTTP_CONTENT_TYPE, httpServletRequest.getContentType());
            currentSpan.setAttribute(HTTP_CONTENT_LENGTH, httpServletRequest.getContentLength());
        }
    }
}
