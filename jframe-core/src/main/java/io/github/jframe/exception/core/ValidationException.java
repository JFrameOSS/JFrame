package io.github.jframe.exception.core;

import io.github.jframe.exception.JFrameException;
import io.github.jframe.validation.ValidationError;
import io.github.jframe.validation.ValidationResult;

import java.io.Serial;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A Validation exception.
 *
 * @see ValidationResult
 */
public class ValidationException extends JFrameException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = 7243575134936095351L;

    private final ValidationResult validationResult;

    /** Constructs a new {@link ValidationException} with an empty {@link ValidationResult}. */
    public ValidationException() {
        super();
        this.validationResult = new ValidationResult();
    }

    /**
     * Constructs a new {@link ValidationException} with the supplied {@link ValidationResult}.
     *
     * @param validationResult the validation result, not null
     */
    public ValidationException(final ValidationResult validationResult) {
        super();
        requireNonNull(validationResult);
        this.validationResult = validationResult;
    }

    /**
     * Constructs a new {@link ValidationException} with the supplied {@link ValidationError}.
     *
     * @param validationError the validation error, not null
     */
    public ValidationException(final ValidationError validationError) {
        this();
        requireNonNull(validationError);
        validationResult.addError(validationError);
    }

    /**
     * Constructs a new {@link ValidationException} with the supplied {@link ValidationError}s.
     *
     * @param validationErrors the validation errors, not null
     */
    public ValidationException(final List<ValidationError> validationErrors) {
        this();
        requireNonNull(validationErrors);
        validationResult.addAllErrors(validationErrors);
    }

    /**
     * Returns the validation result containing the validation errors.
     *
     * @return the validation result containing the validation errors
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
