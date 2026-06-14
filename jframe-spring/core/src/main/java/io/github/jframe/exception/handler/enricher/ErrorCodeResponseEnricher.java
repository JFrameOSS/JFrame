package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

/**
 * Enricher that sets errorCode and errorReason based on the exception type.
 *
 * <p>The {@code cause} field is always set from the throwable message by {@link ErrorResponseResource}.
 * For {@link HttpException} with a wrapped cause, the cause is overridden with the wrapped exception's message.
 */
@Component
public class ErrorCodeResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final WebRequest request,
        final HttpStatus httpStatus) {
        if (throwable instanceof final HttpException http) {
            resource.setErrorCode(http.getErrorCode());
            resource.setErrorReason(http.getErrorReason());
            if (http.getCause() != null) {
                resource.setCause(http.getCause().getMessage());
            }
        } else if (throwable instanceof ValidationException || throwable instanceof MethodArgumentNotValidException) {
            resource.setError(JFrameErrorCode.VALIDATION_ERROR);
        } else {
            resource.setError(JFrameErrorCode.INTERNAL_ERROR);
        }
    }
}
