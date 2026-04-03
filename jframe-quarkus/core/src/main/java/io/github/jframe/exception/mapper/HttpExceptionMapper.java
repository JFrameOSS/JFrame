package io.github.jframe.exception.mapper;

import io.github.jframe.exception.HttpException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} for {@link HttpException}.
 *
 * <p>Converts the JFrame {@link HttpException} to a JAX-RS {@link Response} with the
 * appropriate HTTP status code derived from the exception's {@code httpStatus} field.
 * Extends {@link AbstractExceptionMapper} which provides shared null-check and response-building logic.
 */
@Provider
@ApplicationScoped
public class HttpExceptionMapper extends AbstractExceptionMapper<HttpException> {

    /**
     * Maps the given {@link HttpException} to a response with the corresponding HTTP status.
     *
     * @param exception the HTTP exception to map
     * @return a response with the derived HTTP status code and an enriched error body
     */
    @Override
    public Response toResponse(final HttpException exception) {
        return buildResponse(exception, exception.getHttpStatus().getStatusCode());
    }
}
