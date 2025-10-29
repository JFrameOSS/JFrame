package io.github.jframe.exception.resource;

import io.github.jframe.exception.ApiException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response handler for API errors.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApiErrorResponseResource extends ErrorResponseResource {

    /** The error code. */
    private String apiErrorCode;

    /** The error reason. */
    private String apiErrorReason;

    /** Constructor with an {@code apiException}. */
    public ApiErrorResponseResource(final ApiException apiException) {
        super(apiException);
    }

}
