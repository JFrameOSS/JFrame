package io.github.jframe.exception.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ExceptionMapper} for any {@link Throwable} not handled by a more specific mapper.
 *
 * <p>Always returns HTTP 500 INTERNAL_SERVER_ERROR with a non-null response entity.
 */
@Provider
public class ThrowableMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(final Throwable throwable) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(throwable.getMessage() != null ? throwable.getMessage() : "Internal server error")
            .build();
    }
}
