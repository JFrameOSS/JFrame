package io.github.jframe.exception.mapper;

import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Abstract base class for JAX-RS {@link ExceptionMapper} implementations.
 *
 * <p>Provides shared infrastructure for building error responses:
 * <ul>
 * <li>CDI-injected {@link ErrorResponseEntityBuilder} for enriched response construction</li>
 * <li>JAX-RS-injected {@link ContainerRequestContext} for request metadata</li>
 * <li>A {@link #buildErrorBody(Throwable, int)} helper that handles the null-check fallback pattern</li>
 * <li>A {@link #buildResponse(Throwable, int)} convenience method wrapping the body in a {@link Response}</li>
 * </ul>
 *
 * <p>Concrete subclasses must be annotated with {@code @Provider} and {@code @ApplicationScoped}.
 * Do not add those annotations to this abstract class.
 *
 * @param <T> the exception type handled by the concrete mapper
 */
public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    @Inject
    private ErrorResponseEntityBuilder errorResponseEntityBuilder;

    @Context
    private ContainerRequestContext requestContext;

    /**
     * Builds the {@link ErrorResponseResource} for the given exception, applying enrichment
     * when CDI and JAX-RS dependencies are available, or falling back to a plain resource.
     *
     * @param exception  the exception to map
     * @param statusCode the HTTP status code for the response
     * @return the error response resource (never {@code null})
     */
    protected ErrorResponseResource buildErrorBody(final T exception, final int statusCode) {
        if (errorResponseEntityBuilder == null || requestContext == null) {
            return new ErrorResponseResource(exception);
        }
        return errorResponseEntityBuilder.buildErrorResponseBody(exception, requestContext, statusCode);
    }

    /**
     * Builds a complete {@link Response} for the given exception with the specified HTTP status code.
     *
     * <p>Sets the media type to {@code application/json} and delegates body construction
     * to {@link #buildErrorBody(Throwable, int)}.
     *
     * @param exception  the exception to map
     * @param statusCode the HTTP status code for the response
     * @return the JAX-RS response
     */
    protected Response buildResponse(final T exception, final int statusCode) {
        final ErrorResponseResource resource = buildErrorBody(exception, statusCode);
        return Response.status(statusCode)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(resource)
            .build();
    }
}
