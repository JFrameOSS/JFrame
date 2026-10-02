package io.github.jframe.datasource.search.fields;

import lombok.Getter;

import java.io.Serial;

import static io.github.jframe.exception.search.InvalidSearchException.MAX_REJECTED_VALUE_LENGTH;

/**
 * Thrown by search fields when a raw client value cannot be parsed; exposes the single offending entry.
 *
 * <p>The message echoes the value truncated to {@link io.github.jframe.exception.search.InvalidSearchException#MAX_REJECTED_VALUE_LENGTH}.
 */
@Getter
public final class InvalidFieldValueException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = -6418827730593214155L;

    /** The offending raw value (single list entry or date bound). */
    private final String rejectedValue;

    /**
     * Creates the exception.
     *
     * @param reason        short description, e.g. {@code "Invalid integer value"}
     * @param rejectedValue the offending raw value
     */
    public InvalidFieldValueException(final String reason, final String rejectedValue) {
        super(toMessage(reason, rejectedValue));
        this.rejectedValue = rejectedValue;
    }

    /**
     * Creates the exception with a cause.
     *
     * @param reason        short description, e.g. {@code "Invalid integer value"}
     * @param rejectedValue the offending raw value
     * @param cause         the parse failure
     */
    public InvalidFieldValueException(final String reason, final String rejectedValue, final Throwable cause) {
        super(toMessage(reason, rejectedValue), cause);
        this.rejectedValue = rejectedValue;
    }

    private static String toMessage(final String reason, final String rejectedValue) {
        final String shown = rejectedValue == null || rejectedValue.length() <= MAX_REJECTED_VALUE_LENGTH
            ? rejectedValue
            : rejectedValue.substring(0, MAX_REJECTED_VALUE_LENGTH);
        return reason + ": " + shown;
    }
}
