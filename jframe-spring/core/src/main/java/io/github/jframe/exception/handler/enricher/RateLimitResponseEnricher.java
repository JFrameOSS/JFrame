package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * This enricher adds rate limit information to the error response resource.
 *
 * <p>It only applies to a {@link RateLimitExceededException}.</p>
 */
@Component
public class RateLimitResponseEnricher implements ErrorResponseEnricher {

    /**
     * {@inheritDoc}
     *
     * <p><strong>NOTE:</strong> This enricher only applies if throwable is a {@link RateLimitExceededException} and #errorResponseResource
     * is a {@link RateLimitErrorResponseResource}.
     */
    @Override
    public void doEnrich(final ErrorResponseResource resource, final Throwable cause, final WebRequest request, final HttpStatus status) {
        if (cause instanceof final RateLimitExceededException exception
            && resource instanceof final RateLimitErrorResponseResource rateLimitResource) {
            rateLimitResource.setLimit(exception.getLimit());
            rateLimitResource.setRemaining(exception.getRemaining());
            rateLimitResource.setResetDate(exception.getResetDate());
        }
    }
}
