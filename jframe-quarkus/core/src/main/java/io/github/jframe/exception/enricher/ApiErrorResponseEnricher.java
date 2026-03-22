package io.github.jframe.exception.enricher;

import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that populates API error fields when the resource and throwable are of the correct types.
 *
 * <p>Only enriches when the resource is an {@link ApiErrorResponseResource}
 * and the throwable is an {@link ApiException}.
 */
@ApplicationScoped
public class ApiErrorResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        if (resource instanceof final ApiErrorResponseResource apiResource
            && throwable instanceof final ApiException apiException) {
            apiResource.setApiErrorCode(apiException.getErrorCode());
            apiResource.setApiErrorReason(apiException.getReason());
        }
    }
}
