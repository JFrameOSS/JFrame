package io.github.jframe.exception.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that sets errorCode and errorReason based on the exception type.
 *
 * <p>The {@code cause} field is always set from the throwable message by {@link ErrorResponseResource}.
 * For {@link HttpException} with a wrapped cause, the cause is overridden with the wrapped exception's message.
 */
@ApplicationScoped
public class ErrorCodeResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {
        if (throwable instanceof final HttpException http) {
            resource.setErrorCode(http.getErrorCode());
            resource.setErrorReason(http.getErrorReason());
            if (http.getCause() != null) {
                resource.setCause(http.getCause().getMessage());
            }
        } else if (throwable instanceof ValidationException || throwable instanceof ConstraintViolationException) {
            resource.setError(JFrameErrorCode.VALIDATION_ERROR);
        } else {
            resource.setError(JFrameErrorCode.INTERNAL_ERROR);
        }
    }
}
