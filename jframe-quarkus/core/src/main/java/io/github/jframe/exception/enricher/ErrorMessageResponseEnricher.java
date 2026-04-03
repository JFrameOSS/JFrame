package io.github.jframe.exception.enricher;

import io.github.jframe.exception.JFrameException;
import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that sets the error message on the response resource.
 *
 * <p>Uses the exception message for {@link JFrameException} subclasses.
 * For other exceptions, uses a generic message to prevent leaking internal details.
 */
@ApplicationScoped
public class ErrorMessageResponseEnricher implements ErrorResponseEnricher {

    /** Generic message used for non-JFrame exceptions. */
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        if (throwable instanceof JFrameException) {
            resource.setErrorMessage(throwable.getMessage());
        } else {
            resource.setErrorMessage(INTERNAL_SERVER_ERROR_MESSAGE);
        }
    }
}
