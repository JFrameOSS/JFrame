package io.github.jframe.exception.mapper;

import io.github.jframe.exception.HttpException;
import io.github.jframe.http.QuarkusHttpStatus;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ExceptionMapper} for {@link HttpException}.
 *
 * <p>Converts the JFrame {@link HttpException} to a JAX-RS {@link Response} with the
 * appropriate HTTP status code derived from the exception's {@code httpStatus} field.
 */
@Provider
public class HttpExceptionMapper implements ExceptionMapper<HttpException> {

    @Override
    public Response toResponse(final HttpException exception) {
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(exception.getHttpStatus());
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(exception)
            .build();
    }
}
