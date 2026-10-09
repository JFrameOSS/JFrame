package io.github.jframe.exception.mapper;

import io.quarkus.arc.properties.IfBuildProperty;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps JAX-RS {@link WebApplicationException}s (404, 405, 415, ...) to Problem Details with their own status.
 *
 * <p>Headers of the exception's response (e.g. {@code Allow}) are preserved, except content headers.
 */
@Slf4j
@Provider
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@ApplicationScoped
public class WebApplicationExceptionMapper extends AbstractExceptionMapper<WebApplicationException> {

    private static final int SERVER_ERROR = 500;

    @Override
    public Response toResponse(final WebApplicationException exception) {
        final Response original = exception.getResponse();
        final int status = original == null ? SERVER_ERROR : original.getStatus();
        if (status >= SERVER_ERROR) {
            log.error("Web application exception", exception);
        }
        final Response.ResponseBuilder builder = responseBuilder(exception, status);
        if (original != null) {
            original.getHeaders().forEach((name, values) -> {
                if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name) && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                    values.forEach(value -> builder.header(name, value));
                }
            });
        }
        return builder.build();
    }
}
