package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

/**
 * Sets {@code errorCode} and {@code detail}; never exposes exception messages on 5xx or security errors.
 */
@Order(ErrorResponseEnricher.BUILT_IN_ORDER)
public class ErrorCodeResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final WebRequest request,
        final HttpStatus httpStatus) {
        if (throwable instanceof final HttpException http) {
            resource.setErrorCode(http.getErrorCode());
            resource.setDetail(http.getErrorReason());
        } else if (throwable instanceof ValidationException || throwable instanceof MethodArgumentNotValidException) {
            resource.setError(JFrameErrorCode.VALIDATION_ERROR);
        } else if (httpStatus.is5xxServerError()) {
            resource.setError(JFrameErrorCode.INTERNAL_SERVER_ERROR);
        } else {
            resource.setErrorCode(
                JFrameErrorCode.forStatus(httpStatus.value()).map(JFrameErrorCode::getErrorCode).orElse(httpStatus.name())
            );
            resource.setDetail(httpStatus.getReasonPhrase());
        }
    }

}
