package io.github.jframe.exception.mapper;

import io.github.jframe.exception.core.RateLimitExceededException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ExceptionMapper} for {@link RateLimitExceededException}.
 *
 * <p>Returns HTTP 429 TOO_MANY_REQUESTS and populates standard rate-limit headers:
 * <ul>
 * <li>{@code X-RateLimit-Limit}</li>
 * <li>{@code X-RateLimit-Remaining}</li>
 * <li>{@code X-RateLimit-Reset} (only when non-null)</li>
 * </ul>
 */
@Provider
public class RateLimitExceededExceptionMapper implements ExceptionMapper<RateLimitExceededException> {

    private static final int TOO_MANY_REQUESTS = 429;

    @Override
    public Response toResponse(final RateLimitExceededException exception) {
        final Response.ResponseBuilder builder = Response.status(TOO_MANY_REQUESTS)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(exception.getMessage())
            .header("X-RateLimit-Limit", String.valueOf(exception.getLimit()))
            .header("X-RateLimit-Remaining", String.valueOf(exception.getRemaining()));

        if (exception.getResetDate() != null) {
            builder.header("X-RateLimit-Reset", exception.getResetDate().toString());
        }

        return builder.build();
    }
}
