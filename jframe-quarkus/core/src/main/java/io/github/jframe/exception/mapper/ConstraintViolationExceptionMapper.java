package io.github.jframe.exception.mapper;

import io.quarkus.arc.properties.IfBuildProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * Maps bean-validation {@link ConstraintViolationException} to 400 {@code VALIDATION_ERROR} with {@code errors}.
 *
 * <p>Return-value violations are server bugs: 500 with a generic body, logged at ERROR.
 *
 * <p>More specific than Quarkus' built-in {@code ValidationException} mapper, so it takes precedence.
 */
@Slf4j
@Provider
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@ApplicationScoped
public class ConstraintViolationExceptionMapper extends AbstractExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        if (hasReturnValueViolation(exception)) {
            log.error("Return value constraint violation", exception);
            return buildInternalServerErrorResponse();
        }
        return buildResponse(exception, BAD_REQUEST.getStatusCode());
    }

    private static boolean hasReturnValueViolation(final ConstraintViolationException exception) {
        final Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        return violations != null && violations.stream()
            .map(ConstraintViolation::getPropertyPath)
            .filter(Objects::nonNull)
            .map(Path::spliterator)
            .filter(Objects::nonNull)
            .flatMap(nodes -> StreamSupport.stream(nodes, false))
            .anyMatch(node -> node.getKind() == ElementKind.RETURN_VALUE);
    }
}
