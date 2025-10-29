package io.github.support;

import io.github.jframe.exception.ApiError;

/**
 * Test implementation of {@link ApiError} for testing purposes.
 *
 * <p>This class provides a simple implementation of the ApiError interface
 * that can be used across multiple test classes.
 *
 * @param errorCode the error code
 * @param reason    the error reason
 */
public record TestApiError(String errorCode, String reason) implements ApiError {
}
