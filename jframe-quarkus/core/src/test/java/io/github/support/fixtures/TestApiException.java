package io.github.support.fixtures;

import io.github.jframe.exception.ApiError;
import io.github.jframe.exception.ApiException;

/**
 * Test implementation of {@link ApiException} for testing purposes.
 *
 * <p>This class provides a concrete implementation of ApiException with public constructors,
 * since ApiException has protected constructors. This can be used across multiple test classes.
 */
public class TestApiException extends ApiException {

    /**
     * Constructs a new test API exception with the given API error.
     *
     * @param apiError the API error
     */
    public TestApiException(final ApiError apiError) {
        super(apiError);
    }

    /**
     * Constructs a new test API exception with the given API error and message.
     *
     * @param apiError the API error
     * @param message  the error message
     */
    public TestApiException(final ApiError apiError, final String message) {
        super(apiError, message);
    }
}
