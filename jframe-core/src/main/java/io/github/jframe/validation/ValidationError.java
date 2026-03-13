package io.github.jframe.validation;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Encapsulates a validation error.
 *
 * @see Validator
 * @see ValidationResult
 */
@Getter
public class ValidationError implements Serializable {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = 6587801940281589895L;

    private final String field;

    private final String code;

    /**
     * Constructs a new {@link ValidationError} with the supplied error code.
     *
     * @param code the error code
     */
    public ValidationError(final String code) {
        this(null, code);
    }

    /**
     * Constructs a new {@link ValidationError} with the supplied field name and error code.
     *
     * @param field the field name
     * @param code  the error code
     */
    public ValidationError(final String field, final String code) {
        this.field = field;
        this.code = requireNonNull(code);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
}
