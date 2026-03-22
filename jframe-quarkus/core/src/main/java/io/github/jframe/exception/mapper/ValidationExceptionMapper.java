package io.github.jframe.exception.mapper;

import io.github.jframe.exception.core.ValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} for {@link ValidationException}.
 *
 * <p>Always returns HTTP 400 BAD_REQUEST with the {@link io.github.jframe.validation.ValidationResult}
 * as the response body. Extends {@link AbstractExceptionMapper} which provides shared null-check and response-building logic.
 */
@Provider
@ApplicationScoped
public class ValidationExceptionMapper extends AbstractExceptionMapper<ValidationException> {

    /**
     * Maps the given {@link ValidationException} to an HTTP 400 response.
     *
     * @param exception the validation exception to map
     * @return a 400 BAD_REQUEST response with an enriched error body
     */
    @Override
    public Response toResponse(final ValidationException exception) {
        return buildResponse(exception, BAD_REQUEST.getStatusCode());
    }
}
