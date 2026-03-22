package io.github.jframe.exception.resource;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response resource for constraint violation errors.
 *
 * <p>Extends {@link ErrorResponseResource} with a list of constraint violation errors.
 */
@Getter
@Setter
@NoArgsConstructor
public class ConstraintViolationResponseResource extends ErrorResponseResource {

    /** The list of constraint violation errors. */
    private List<ValidationErrorResource> errors;

    /**
     * Constructs a new {@code ConstraintViolationResponseResource} with the given throwable.
     *
     * @param throwable the throwable that caused this error
     */
    public ConstraintViolationResponseResource(final Throwable throwable) {
        super(throwable);
    }
}
