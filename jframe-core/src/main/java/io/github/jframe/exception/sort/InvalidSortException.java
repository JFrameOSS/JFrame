package io.github.jframe.exception.sort;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import lombok.Getter;

import java.io.Serial;
import java.util.List;

import static java.util.Objects.requireNonNullElse;

/**
 * Thrown when a sort request names an unknown field or an invalid direction.
 *
 * <p>Carries {@code rejectedField} and {@code sortableFields} for inclusion in the 400 response body.
 */
@Getter
public class InvalidSortException extends HttpException {

    @Serial
    private static final long serialVersionUID = -8847634903271843519L;

    /** The requested sort field that was rejected (may be {@code null} when direction is invalid). */
    private final String rejectedField;

    /** The endpoint's full sortable set (sortable + virtual fields). */
    private final List<String> sortableFields;

    /**
     * Creates the exception.
     *
     * @param rejectedField  the rejected sort field name; may be {@code null}
     * @param sortableFields the fields this endpoint accepts for sorting
     */
    public InvalidSortException(final String rejectedField, final List<String> sortableFields) {
        super(JFrameErrorCode.INVALID_SORT);
        this.rejectedField = rejectedField;
        this.sortableFields = List.copyOf(requireNonNullElse(sortableFields, List.of()));
    }
}
