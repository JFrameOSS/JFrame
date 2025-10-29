package io.github.jframe.validation;


import io.github.jframe.exception.core.ValidationException;

/**
 * A validator for validating application-specific objects.
 *
 * <p>This interface is inspired on Spring's {@link org.springframework.validation.Validator}
 * mechanism. However, JFrame's validator mechanism uses its own {@link ValidationResult} instead of Spring's
 * {@link org.springframework.validation.Errors} for returning validation errors.
 *
 * <p>Implementors should typically only implement {@link Validator#validate(Object,
 * ValidationResult)} as other methods are already implemented using the interface's default methods.
 *
 * @param <T> the factory of the object to validate
 * @see ValidationError
 * @see ValidationException
 * @see ValidationResult
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Validates the supplied object.
     *
     * @param object the object to validate
     * @return the validation result
     */
    default ValidationResult validate(final T object) {
        final ValidationResult validationResult = new ValidationResult();
        validate(object, validationResult);
        return validationResult;
    }

    /**
     * Validates the supplied object.
     *
     * @param object           the object to validate
     * @param validationResult the contextual state about the validation process
     */
    void validate(T object, ValidationResult validationResult);

    /**
     * Validates the supplied object.
     *
     * @param object the object to validate
     * @throws ValidationException if the validation fails
     */
    default void validateAndThrow(final T object) {
        final ValidationResult validationResult = new ValidationResult();
        validateAndThrow(object, validationResult);
    }

    /**
     * Validates the supplied object.
     *
     * @param object           the object to validate
     * @param validationResult the contextual state about the validation process
     * @throws ValidationException if the validation fails
     */
    default void validateAndThrow(final T object, final ValidationResult validationResult) {
        validate(object, validationResult);
        if (validationResult.hasErrors()) {
            throw new ValidationException(validationResult);
        }
    }

    /**
     * Returns {@code true} if the validation of the supplied object succeeds.
     *
     * @param object the object to validate
     * @return {@code true} if the validation of the supplied object succeeds
     */
    default boolean isValid(final T object) {
        return !validate(object).hasErrors();
    }
}
