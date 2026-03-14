package io.github.jframe.exception.mapper;

import io.github.jframe.exception.ApiException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ExceptionMapper} for {@link ApiException}.
 *
 * <p>Always returns HTTP 400 BAD_REQUEST with the API error object as the response body.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(final ApiException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(exception.getApiError())
            .build();
    }
}
