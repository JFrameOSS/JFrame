package io.github.jframe.exception.search;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import lombok.Getter;

import java.io.Serial;
import java.util.List;

import static java.util.Objects.requireNonNullElse;

/**
 * Thrown when a search request names an unknown field or carries an unparseable value.
 *
 * <p>Carries {@code rejectedField}, {@code rejectedValue} and {@code searchableFields} for inclusion in the 400 response body.
 */
@Getter
public class InvalidSearchException extends HttpException {

    /** Maximum length of the echoed rejected value. */
    public static final int MAX_REJECTED_VALUE_LENGTH = 100;

    @Serial
    private static final long serialVersionUID = 4316427958118461723L;

    /** The requested search field that was rejected. */
    private final String rejectedField;

    /** The rejected raw value, truncated; {@code null} when the field itself is unknown. */
    private final String rejectedValue;

    /** The endpoint's searchable fields. */
    private final List<String> searchableFields;

    /**
     * Creates the exception.
     *
     * @param rejectedField    the rejected search field name
     * @param rejectedValue    the rejected raw value; may be {@code null}
     * @param searchableFields the fields this endpoint accepts for searching
     */
    public InvalidSearchException(final String rejectedField, final String rejectedValue, final List<String> searchableFields) {
        super(JFrameErrorCode.INVALID_SEARCH);
        this.rejectedField = rejectedField;
        this.rejectedValue = truncate(rejectedValue);
        this.searchableFields = List.copyOf(requireNonNullElse(searchableFields, List.of()));
    }

    /**
     * Creates the exception with a cause; the cause message must not echo untruncated input.
     *
     * @param rejectedField    the rejected search field name
     * @param rejectedValue    the rejected raw value; may be {@code null}
     * @param searchableFields the fields this endpoint accepts for searching
     * @param cause            the parse failure
     */
    public InvalidSearchException(final String rejectedField,
                                  final String rejectedValue,
                                  final List<String> searchableFields,
                                  final Throwable cause) {
        super(JFrameErrorCode.INVALID_SEARCH, cause);
        this.rejectedField = rejectedField;
        this.rejectedValue = truncate(rejectedValue);
        this.searchableFields = List.copyOf(requireNonNullElse(searchableFields, List.of()));
    }

    private static String truncate(final String value) {
        if (value == null || value.length() <= MAX_REJECTED_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_REJECTED_VALUE_LENGTH);
    }
}
