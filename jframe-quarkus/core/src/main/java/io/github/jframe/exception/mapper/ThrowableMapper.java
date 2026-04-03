package io.github.jframe.exception.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} for any {@link Throwable} not handled by a more specific mapper.
 *
 * <p>Always returns HTTP 500 INTERNAL_SERVER_ERROR with a non-null response entity.
 * Extends {@link AbstractExceptionMapper} which provides shared null-check and response-building logic.
 */
@Provider
@ApplicationScoped
public class ThrowableMapper extends AbstractExceptionMapper<Throwable> {

    /**
     * Maps the given {@link Throwable} to an HTTP 500 response.
     *
     * @param throwable the unhandled throwable
     * @return a 500 INTERNAL_SERVER_ERROR response with an enriched error body
     */
    @Override
    public Response toResponse(final Throwable throwable) {
        return buildResponse(throwable, INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
