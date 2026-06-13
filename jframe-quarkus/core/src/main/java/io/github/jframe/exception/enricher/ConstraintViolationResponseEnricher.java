package io.github.jframe.exception.enricher;

import io.github.jframe.exception.assembler.ConstraintViolationResourceAssembler;
import io.github.jframe.exception.resource.ConstraintViolationResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that populates constraint violation errors on a {@link ConstraintViolationResponseResource}.
 *
 * <p>Only enriches when the resource is a {@link ConstraintViolationResponseResource}
 * and the throwable is a {@link ConstraintViolationException} with a non-empty violations set.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class ConstraintViolationResponseEnricher implements ErrorResponseEnricher {

    private final ConstraintViolationResourceAssembler assembler;

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        if (resource instanceof final ConstraintViolationResponseResource cvResource
            && throwable instanceof final ConstraintViolationException cvException) {
            final Set<ConstraintViolation<?>> violations = cvException.getConstraintViolations();

            if (violations != null && !violations.isEmpty()) {
                cvResource.setErrors(assembler.convert(violations));
            }
        }
    }
}
