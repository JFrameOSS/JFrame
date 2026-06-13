package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static java.util.Objects.requireNonNullElse;

/**
 * This enricher sets the error code and error reason from the exception type.
 *
 * <p>For {@link HttpException}: extracts errorCode and errorReason from the exception's ApiError,
 * falling back to {@link JFrameErrorCode#HTTP_ERROR} when null.
 *
 * <p>For {@link ValidationException} or {@link MethodArgumentNotValidException}: maps to
 * {@link JFrameErrorCode#VALIDATION_ERROR}.
 *
 * <p>For all other throwables: maps to {@link JFrameErrorCode#INTERNAL_ERROR} without
 * exposing the cause (leak prevention).
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
            resource.setErrorCode(requireNonNullElse(http.getErrorCode(), JFrameErrorCode.HTTP_ERROR.getErrorCode()));
            resource.setErrorReason(requireNonNullElse(http.getErrorReason(), JFrameErrorCode.HTTP_ERROR.getReason()));
            Optional.ofNullable(http.getCause()).map(Throwable::getMessage).ifPresent(resource::setCause);
        } else if (throwable instanceof ValidationException || throwable instanceof MethodArgumentNotValidException) {
            resource.setErrorCode(JFrameErrorCode.VALIDATION_ERROR.getErrorCode());
            resource.setErrorReason(JFrameErrorCode.VALIDATION_ERROR.getReason());
        } else {
            resource.setErrorCode(JFrameErrorCode.INTERNAL_ERROR.getErrorCode());
            resource.setErrorReason(JFrameErrorCode.INTERNAL_ERROR.getReason());
        }
    }
}
