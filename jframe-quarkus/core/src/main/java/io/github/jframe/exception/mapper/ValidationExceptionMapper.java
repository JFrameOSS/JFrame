package io.github.jframe.exception.mapper;

import io.github.jframe.exception.core.ValidationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ExceptionMapper} for {@link ValidationException}.
 *
 * <p>Always returns HTTP 400 BAD_REQUEST with the {@link io.github.jframe.validation.ValidationResult}
 * as the response body.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(final ValidationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(exception.getValidationResult())
            .build();
    }
}
