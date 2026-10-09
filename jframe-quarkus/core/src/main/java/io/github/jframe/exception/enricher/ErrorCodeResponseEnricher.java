package io.github.jframe.exception.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

/**
 * Sets {@code errorCode} and {@code detail}; never exposes exception messages for 5xx.
 */
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@Priority(ErrorResponseEnricher.BUILT_IN_PRIORITY)
@ApplicationScoped
public class ErrorCodeResponseEnricher implements ErrorResponseEnricher {

    private static final int SERVER_ERROR = 500;

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {
        if (throwable instanceof final HttpException http) {
            resource.setErrorCode(http.getErrorCode());
            resource.setDetail(http.getErrorReason());
        } else if (throwable instanceof ValidationException || throwable instanceof ConstraintViolationException) {
            resource.setError(JFrameErrorCode.VALIDATION_ERROR);
        } else if (statusCode >= SERVER_ERROR) {
            resource.setError(JFrameErrorCode.INTERNAL_SERVER_ERROR);
        } else {
            final Response.Status status = Response.Status.fromStatusCode(statusCode);
            resource.setErrorCode(
                JFrameErrorCode.forStatus(statusCode)
                    .map(JFrameErrorCode::name)
                    .orElse(status != null ? status.name() : String.valueOf(statusCode))
            );
            resource.setDetail(status != null ? status.getReasonPhrase() : null);
        }
    }
}
