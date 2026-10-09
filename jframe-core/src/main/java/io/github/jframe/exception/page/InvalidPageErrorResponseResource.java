package io.github.jframe.exception.page;

import io.github.jframe.exception.resource.ErrorResponseResource;
import lombok.Getter;

/**
 * Error response body for {@link InvalidPageException}.
 * Extends the standard error body with {@code rejectedParameter} and {@code rejectedValue}.
 */
@Getter
@io.swagger.v3.oas.annotations.media.Schema(name = "InvalidPageProblemDetails")
@org.eclipse.microprofile.openapi.annotations.media.Schema(name = "InvalidPageProblemDetails")
public class InvalidPageErrorResponseResource extends ErrorResponseResource {

    private final String rejectedParameter;
    private final int rejectedValue;

    /** Creates a response resource from an {@link InvalidPageException}. */
    public InvalidPageErrorResponseResource(final InvalidPageException exception) {
        super(exception);
        this.rejectedParameter = exception.getRejectedParameter();
        this.rejectedValue = exception.getRejectedValue();
    }
}
