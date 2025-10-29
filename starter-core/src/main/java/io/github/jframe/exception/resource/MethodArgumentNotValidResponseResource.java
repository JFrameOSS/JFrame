package io.github.jframe.exception.resource;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Response resource for validation errors.
 */
@Getter
@Setter
@NoArgsConstructor
public class MethodArgumentNotValidResponseResource extends ErrorResponseResource {

    /** The validation errors. */
    private List<ValidationErrorResource> errors;

    /** Constructor with a {@code validationException}. */
    public MethodArgumentNotValidResponseResource(final MethodArgumentNotValidException validationException) {
        super(validationException);
    }
}
