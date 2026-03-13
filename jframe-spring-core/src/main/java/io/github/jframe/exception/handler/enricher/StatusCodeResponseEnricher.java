package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * This enricher copies the http status value and text onto the error response resource.
 */
@Component
public class StatusCodeResponseEnricher implements ErrorResponseEnricher {

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
        errorResponseResource.setStatusCode(httpStatus.value());
        errorResponseResource.setStatusMessage(httpStatus.getReasonPhrase());
    }
}
