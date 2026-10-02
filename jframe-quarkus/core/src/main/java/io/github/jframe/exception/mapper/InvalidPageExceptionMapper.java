package io.github.jframe.exception.mapper;

import io.github.jframe.exception.page.InvalidPageException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} for {@link InvalidPageException}.
 *
 * <p>Returns a 400 Bad Request response whose body includes the invalid page details.
 * A dedicated mapper is required because {@link HttpExceptionMapper} does not branch on subtype.
 */
@Provider
@ApplicationScoped
public class InvalidPageExceptionMapper extends AbstractExceptionMapper<InvalidPageException> {

    @Override
    public Response toResponse(final InvalidPageException exception) {
        return buildResponse(exception, exception.getHttpStatus().getStatusCode());
    }
}
