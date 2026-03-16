package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ConstraintViolationResourceAssembler;
import io.github.jframe.exception.resource.ConstraintViolationResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that populates constraint violation errors on a {@link ConstraintViolationResponseResource}.
 *
 * <p>Only enriches when the resource is a {@link ConstraintViolationResponseResource}
 * and the throwable is a {@link ConstraintViolationException} with a non-empty violations set.
 */
public class ConstraintViolationResponseEnricher implements ErrorResponseEnricher {

    private final ConstraintViolationResourceAssembler assembler;

    /**
     * Constructs a new {@code ConstraintViolationResponseEnricher}.
     *
     * @param assembler the assembler to convert constraint violations
     */
    public ConstraintViolationResponseEnricher(final ConstraintViolationResourceAssembler assembler) {
        this.assembler = assembler;
    }

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
