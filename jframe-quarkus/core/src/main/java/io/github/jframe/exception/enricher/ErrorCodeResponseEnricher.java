package io.github.jframe.exception.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that sets the error code, reason, and cause on the response resource.
 *
 * <p>Maps exception types to structured error codes:
 * <ul>
 * <li>{@link HttpException} — uses the ApiError for errorCode/errorReason, cause from wrapped throwable</li>
 * <li>{@link ValidationException} — maps to JFRAME_VALIDATION_ERROR</li>
 * <li>{@link ConstraintViolationException} — maps to JFRAME_VALIDATION_ERROR</li>
 * <li>Unhandled {@link Throwable} — maps to JFRAME_INTERNAL_ERROR, cause is never set</li>
 * </ul>
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
            resource.setErrorCode(http.getErrorCode() != null ? http.getErrorCode() : JFrameErrorCode.HTTP_ERROR.getErrorCode());
            resource.setErrorReason(http.getErrorReason() != null ? http.getErrorReason() : JFrameErrorCode.HTTP_ERROR.getReason());
            if (http.getCause() != null) {
                resource.setCause(http.getCause().getMessage());
            }
        } else if (throwable instanceof ValidationException || throwable instanceof ConstraintViolationException) {
            resource.setErrorCode(JFrameErrorCode.VALIDATION_ERROR.getErrorCode());
            resource.setErrorReason(JFrameErrorCode.VALIDATION_ERROR.getReason());
        } else {
            resource.setErrorCode(JFrameErrorCode.INTERNAL_ERROR.getErrorCode());
            resource.setErrorReason(JFrameErrorCode.INTERNAL_ERROR.getReason());
        }
    }
}
