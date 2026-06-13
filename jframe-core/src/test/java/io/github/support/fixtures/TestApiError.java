package io.github.support.fixtures;

import io.github.jframe.exception.ApiError;

import jakarta.ws.rs.core.Response;

/**
 * Test implementation of {@link ApiError} for testing purposes.
 *
 * <p>This class provides a simple implementation of the ApiError interface
 * that can be used across multiple test classes.
 *
 * @param getErrorCode  the error code
 * @param getReason     the error reason
 * @param getHttpStatus the HTTP status
 */
public record TestApiError(String getErrorCode, String getReason, Response.Status getHttpStatus) implements ApiError {
}
