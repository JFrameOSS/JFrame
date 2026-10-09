package io.github.jframe.exception.search;

import io.github.jframe.exception.resource.ErrorResponseResource;
import lombok.Getter;

import java.util.List;

/**
 * Error response body for {@link InvalidSearchException}.
 * Extends the standard error body with {@code rejectedField}, {@code rejectedValue} and {@code searchableFields}.
 */
@Getter
@io.swagger.v3.oas.annotations.media.Schema(name = "InvalidSearchProblemDetails")
@org.eclipse.microprofile.openapi.annotations.media.Schema(name = "InvalidSearchProblemDetails")
public class InvalidSearchErrorResponseResource extends ErrorResponseResource {

    private final String rejectedField;
    private final String rejectedValue;
    private final List<String> searchableFields;

    /** Creates a response resource from an {@link InvalidSearchException}. */
    public InvalidSearchErrorResponseResource(final InvalidSearchException exception) {
        super(exception);
        this.rejectedField = exception.getRejectedField();
        this.rejectedValue = exception.getRejectedValue();
        this.searchableFields = exception.getSearchableFields();
    }
}
