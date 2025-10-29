package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.util.converter.ModelConverter;
import io.github.jframe.validation.ValidationError;
import io.github.jframe.validation.ValidationResult;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import static java.util.Objects.requireNonNull;

/**
 * This enricher adds validation errors to the error response resource.
 */
@Component
public class ValidationErrorResponseEnricher implements ErrorResponseEnricher {

    private final ModelConverter<ValidationError, ValidationErrorResource> validationErrorResourceAssembler;

    /** Constructor with a {@code validationErrorResourceAssembler}. */
    public ValidationErrorResponseEnricher(final ModelConverter<ValidationError, ValidationErrorResource> resourceAssembler) {
        super();
        this.validationErrorResourceAssembler = requireNonNull(resourceAssembler);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>NOTE:</strong> This enricher only applies if throwable is a {@link
     * ValidationException} and #errorResponseResource is a {@link ValidationErrorResponseResource}.
     */
    @Override
    public void doEnrich(
        final ErrorResponseResource errorResponseResource,
        final Throwable throwable,
        final WebRequest request,
        final HttpStatus httpStatus) {
        if (throwable instanceof final ValidationException validationException
            && errorResponseResource instanceof final ValidationErrorResponseResource resource) {
            final List<ValidationError> errors = getErrors(validationException);
            if (errors != null && !errors.isEmpty()) {
                resource.setErrors(validationErrorResourceAssembler.convert(errors));
            }
        }
    }

    private static List<ValidationError> getErrors(final ValidationException validationException) {
        return getErrors(validationException.getValidationResult());
    }

    private static List<ValidationError> getErrors(final ValidationResult validationResult) {
        return validationResult.getErrors();
    }
}
