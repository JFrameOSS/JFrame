package io.github.jframe.exception.sort;

import io.github.jframe.exception.resource.ErrorResponseResource;
import lombok.Getter;

import java.util.List;

/**
 * Error response body for {@link InvalidSortException}.
 * Extends the standard error body with {@code rejectedField} and {@code sortableFields}.
 */
@Getter
public class InvalidSortErrorResponseResource extends ErrorResponseResource {

    private final String rejectedField;
    private final List<String> sortableFields;

    /** Creates a response resource from an {@link InvalidSortException}. */
    public InvalidSortErrorResponseResource(final InvalidSortException exception) {
        super(exception);
        this.rejectedField = exception.getRejectedField();
        this.sortableFields = exception.getSortableFields();
    }
}
