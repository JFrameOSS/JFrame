package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

/**
 * Enricher that sets the HTTP status code and reason phrase on the response resource.
 */
@ApplicationScoped
public class StatusCodeResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        resource.setStatusCode(statusCode);
        final Response.Status status = Response.Status.fromStatusCode(statusCode);
        if (status != null) {
            resource.setStatusMessage(status.getReasonPhrase());
        }
    }
}
