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
public class ValidationErrorResponseResource extends ErrorResponseResource {

    /** The validation errors. */
    private List<ValidationErrorResource> errors;

    /** Constructor with a {@code validationException}. */
    public ValidationErrorResponseResource(final ValidationException validationException) {
        super(validationException);
    }

}
