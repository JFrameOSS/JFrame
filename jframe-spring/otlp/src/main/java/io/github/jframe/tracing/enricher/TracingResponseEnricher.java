package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.handler.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static io.github.jframe.logging.ecs.EcsFieldNames.USER_NAME;

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
     * only enriches the response with the necessary information and a {@link EcsFieldNames#TX_ID} to correlate the request in ECS.
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
            currentSpan.recordException(throwable);
            currentSpan.setStatus(StatusCode.ERROR);
            currentSpan.setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), EcsFields.getOrDefault(USER_NAME, AuthenticationConstants.ANONYMOUS));
            currentSpan.setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), EcsFields.get(TX_ID));
            currentSpan.setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), EcsFields.get(REQUEST_ID));
            currentSpan.setAttribute(SPAN_HTTP_URI.getKey(), httpServletRequest.getRequestURI());
            currentSpan.setAttribute(SPAN_HTTP_QUERY.getKey(), httpServletRequest.getQueryString());
            currentSpan.setAttribute(SPAN_HTTP_METHOD.getKey(), httpServletRequest.getMethod());
            currentSpan.setAttribute(SPAN_HTTP_STATUS_CODE.getKey(), httpStatus.value());
            currentSpan.setAttribute(SPAN_HTTP_CONTENT_TYPE.getKey(), httpServletRequest.getContentType());
            currentSpan.setAttribute(SPAN_HTTP_CONTENT_LENGTH.getKey(), httpServletRequest.getContentLength());
        }
    }
}
