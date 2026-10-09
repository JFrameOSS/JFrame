package io.github.jframe.exception.resource;

import io.github.jframe.exception.core.ValidationException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response resource for validation errors.
 */
@Getter
@Setter
@NoArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(name = "ValidationProblemDetails")
@org.eclipse.microprofile.openapi.annotations.media.Schema(name = "ValidationProblemDetails")
public class ValidationErrorResponseResource extends ErrorResponseResource {

    /** The validation errors. */
    private List<ValidationErrorResource> errors;

    /** Constructor with a {@code validationException}. */
    public ValidationErrorResponseResource(final ValidationException validationException) {
        super(validationException);
    }

    /** Constructor with a generic throwable for subclasses. */
    protected ValidationErrorResponseResource(final Throwable throwable) {
        super(throwable);
    }

}
