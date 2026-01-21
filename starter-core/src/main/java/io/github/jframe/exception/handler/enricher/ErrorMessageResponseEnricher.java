package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.WebRequest;

/**
 * This enricher copies the error message from the exception onto the error response resource.
 */
@Component
public class ErrorMessageResponseEnricher implements ErrorResponseEnricher {

    /**
     * {@inheritDoc}
     *
     * <p><strong>NOTE:</strong> This enricher applies to all exceptions.</p>
     */
    @Override
    public void doEnrich(
        final ErrorResponseResource errorResponseResource,
        final Throwable throwable,
        final WebRequest request,
        final HttpStatus httpStatus) {
        final String message = throwable.getMessage();
        if (StringUtils.hasText(message)) {
            errorResponseResource.setErrorMessage(message);
        }
    }
}
