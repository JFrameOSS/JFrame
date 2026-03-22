package io.github.jframe.exception.enricher;

import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that populates rate limit fields on a {@link RateLimitErrorResponseResource}.
 *
 * <p>Only enriches when the resource is a {@link RateLimitErrorResponseResource}
 * and the throwable is a {@link RateLimitExceededException}.
 */
@ApplicationScoped
public class RateLimitResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        if (resource instanceof final RateLimitErrorResponseResource rateLimitResource
            && throwable instanceof final RateLimitExceededException rateLimitException) {
            rateLimitResource.setLimit(rateLimitException.getLimit());
            rateLimitResource.setRemaining(rateLimitException.getRemaining());
            rateLimitResource.setResetDate(rateLimitException.getResetDate());
        }
    }
}
