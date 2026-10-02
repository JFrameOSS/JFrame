package io.github.jframe.exception.mapper;

import io.github.jframe.exception.search.InvalidSearchException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} for {@link InvalidSearchException}.
 *
 * <p>Returns a 400 Bad Request response whose body includes the invalid search details.
 * A dedicated mapper is required because {@link HttpExceptionMapper} does not branch on subtype.
 */
@Provider
@ApplicationScoped
public class InvalidSearchExceptionMapper extends AbstractExceptionMapper<InvalidSearchException> {

    @Override
    public Response toResponse(final InvalidSearchException exception) {
        return buildResponse(exception, exception.getHttpStatus().getStatusCode());
    }
}
