package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.MethodArgumentNotValidResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResource;
import io.github.jframe.util.converter.ModelConverter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

/**
 * This enricher adds validation errors to the error response resource.
 */
@Component
@RequiredArgsConstructor
public class MethodArgumentNotValidResponseEnricher implements ErrorResponseEnricher {

    private final ModelConverter<ObjectError, ValidationErrorResource> objectErrorResourceAssembler;

    /**
     * {@inheritDoc}
     *
     * <p><strong>NOTE:</strong> This enricher only applies if throwable is a {@link
     * MethodArgumentNotValidException} and #errorResponseResource is a {@link MethodArgumentNotValidResponseResource}.
     */
    @Override
    public void doEnrich(
        final ErrorResponseResource errorResponseResource,
        final Throwable throwable,
        final WebRequest request,
        final HttpStatus httpStatus) {
        if (throwable instanceof final MethodArgumentNotValidException validationException
            && errorResponseResource instanceof final MethodArgumentNotValidResponseResource resource) {
            final List<ObjectError> errors = getErrors(validationException);
            if (!errors.isEmpty()) {
                resource.setErrors(objectErrorResourceAssembler.convert(errors));
            }
        }
    }

    private static List<ObjectError> getErrors(
        final MethodArgumentNotValidException methodArgumentNotValidException) {
        return getErrors(methodArgumentNotValidException.getBindingResult());
    }

    private static List<ObjectError> getErrors(final BindingResult validationResult) {
        return validationResult.getAllErrors();
    }
}
