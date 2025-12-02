package io.github.support;

import io.github.jframe.exception.ApiError;

/**
 * Test implementation of {@link ApiError} for testing purposes.
 *
 * <p>This class provides a simple implementation of the ApiError interface
 * that can be used across multiple test classes.
 *
 * @param getErrorCode the error code
 * @param getReason    the error reason
 */
public record TestApiError(String getErrorCode, String getReason) implements ApiError {
}
