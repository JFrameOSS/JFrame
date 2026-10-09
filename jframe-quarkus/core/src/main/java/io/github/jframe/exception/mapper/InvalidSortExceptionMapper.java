package io.github.jframe.exception.mapper;

import io.github.jframe.exception.sort.InvalidSortException;
import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link jakarta.ws.rs.ext.ExceptionMapper} for {@link InvalidSortException}.
 *
 * <p>Returns a 400 Bad Request response whose body includes {@code rejectedField} and {@code sortableFields}.
 * A dedicated mapper is required because {@link HttpExceptionMapper} does not branch on subtype.
 */
@Provider
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@ApplicationScoped
public class InvalidSortExceptionMapper extends AbstractExceptionMapper<InvalidSortException> {

    @Override
    public Response toResponse(final InvalidSortException exception) {
        return buildResponse(exception, exception.getHttpStatus().getStatusCode());
    }
}
