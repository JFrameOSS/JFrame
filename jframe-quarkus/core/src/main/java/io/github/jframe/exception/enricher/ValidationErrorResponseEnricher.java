package io.github.jframe.exception.enricher;

import io.github.jframe.exception.assembler.ValidationErrorResourceAssembler;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.validation.ValidationError;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.RequiredArgsConstructor;

import java.util.List;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that populates validation errors on a {@link ValidationErrorResponseResource}.
 *
 * <p>Only enriches when the resource is a {@link ValidationErrorResponseResource}
 * and the throwable is a {@link ValidationException} with a non-empty errors list.
 */
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@Priority(ErrorResponseEnricher.BUILT_IN_PRIORITY)
@ApplicationScoped
@RequiredArgsConstructor
public class ValidationErrorResponseEnricher implements ErrorResponseEnricher {

    private final ValidationErrorResourceAssembler assembler;

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        if (resource instanceof final ValidationErrorResponseResource validationResource
            && throwable instanceof final ValidationException validationException) {
            final List<ValidationError> errors = validationException.getValidationResult().getErrors();

            if (errors != null && !errors.isEmpty()) {
                validationResource.setErrors(assembler.convert(errors));
            }
        }
    }
}
