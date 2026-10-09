package io.github.jframe.exception.mapper;

import io.github.jframe.exception.core.RateLimitExceededException;
import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import static io.github.jframe.util.constants.Constants.Headers.*;
import static jakarta.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} for {@link RateLimitExceededException}.
 *
 * <p>Returns HTTP 429 TOO_MANY_REQUESTS and populates standard rate-limit headers:
 * <ul>
 * <li>{@code X-RateLimit-Limit}</li>
 * <li>{@code X-RateLimit-Remaining}</li>
 * <li>{@code X-RateLimit-Reset} (only when non-null)</li>
 * </ul>
 * Extends {@link AbstractExceptionMapper} which provides shared null-check and response-building logic.
 */
@Provider
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@ApplicationScoped
public class RateLimitExceededExceptionMapper extends AbstractExceptionMapper<RateLimitExceededException> {

    /**
     * Maps the given {@link RateLimitExceededException} to an HTTP 429 response with rate-limit headers.
     *
     * @param exception the rate limit exception to map
     * @return a 429 TOO_MANY_REQUESTS response with rate-limit headers and an enriched error body
     */
    @Override
    public Response toResponse(final RateLimitExceededException exception) {
        final Response.ResponseBuilder builder = responseBuilder(exception, TOO_MANY_REQUESTS.getStatusCode())
            .header(X_RATELIMIT_LIMIT, String.valueOf(exception.getLimit()))
            .header(X_RATELIMIT_REMAINING, String.valueOf(exception.getRemaining()));

        if (exception.getResetDate() != null) {
            builder.header(X_RATELIMIT_RESET, exception.getResetDate().toString());
        }

        return builder.build();
    }
}
