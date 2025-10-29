package io.github.jframe.exception;

import io.github.support.TestApiError;
import io.github.support.TestApiException;
import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests for {@link ApiException}.
 *
 * <p>Verifies the ApiException functionality including:
 * <ul>
 * <li>Constructor variations with ApiError parameter</li>
 * <li>ApiError storage and retrieval</li>
 * <li>Convenience methods for error code and reason</li>
 * <li>Exception hierarchy (extends JFrameException)</li>
 * <li>Null handling for optional parameters</li>
 * </ul>
 */
@DisplayName("Exception Hierarchy - API Exception")
public class ApiExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should create exception with ApiError only")
    public void shouldCreateExceptionWithApiError() {
        // Given: An ApiError
        final ApiError apiError = new TestApiError("ERR001", "Test error");

        // When: Creating exception with ApiError only
        final ApiException exception = new TestApiException(apiError);

        // Then: Exception is created with ApiError and null message and cause
        assertThat(exception.getApiError(), is(equalTo(apiError)));
        assertThat(exception.getErrorCode(), is(equalTo("ERR001")));
        assertThat(exception.getReason(), is(equalTo("Test error")));
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with ApiError and message")
    public void shouldCreateExceptionWithApiErrorAndMessage() {
        // Given: An ApiError and a custom message
        final ApiError apiError = new TestApiError("ERR002", "Validation error");
        final String message = "User input validation failed";

        // When: Creating exception with ApiError and message
        final ApiException exception = new TestApiException(apiError, message);

        // Then: Exception is created with ApiError, message and null cause
        assertThat(exception.getApiError(), is(equalTo(apiError)));
        assertThat(exception.getErrorCode(), is(equalTo("ERR002")));
        assertThat(exception.getReason(), is(equalTo("Validation error")));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with ApiError and cause")
    public void shouldCreateExceptionWithApiErrorAndCause() {
        // Given: An ApiError and a root cause
        final ApiError apiError = new TestApiError("ERR003", "Database error");
        final Throwable cause = new RuntimeException("Connection failed");

        // When: Creating exception with ApiError and cause
        final ApiException exception = new TestApiException(apiError, cause);

        // Then: Exception is created with ApiError, cause and null message
        assertThat(exception.getApiError(), is(equalTo(apiError)));
        assertThat(exception.getErrorCode(), is(equalTo("ERR003")));
        assertThat(exception.getReason(), is(equalTo("Database error")));
        assertThat(exception.getCause(), is(equalTo(cause)));
        assertThat(exception.getMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with ApiError, cause and message")
    public void shouldCreateExceptionWithApiErrorCauseAndMessage() {
        // Given: An ApiError, root cause and custom message
        final ApiError apiError = new TestApiError("ERR004", "External service error");
        final Throwable cause = new RuntimeException("Timeout occurred");
        final String message = "Failed to call external service";

        // When: Creating exception with all parameters
        final ApiException exception = new TestApiException(apiError, cause, message);

        // Then: Exception is created with ApiError, cause and message
        assertThat(exception.getApiError(), is(equalTo(apiError)));
        assertThat(exception.getErrorCode(), is(equalTo("ERR004")));
        assertThat(exception.getReason(), is(equalTo("External service error")));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    @DisplayName("Should return null for error code when ApiError is null")
    public void shouldReturnNullErrorCodeWhenApiErrorIsNull() {
        // Given: A null ApiError

        // When: Creating exception with null ApiError
        final ApiException exception = new TestApiException(null);

        // Then: Convenience methods return null
        assertThat(exception.getErrorCode(), is(nullValue()));
        assertThat(exception.getReason(), is(nullValue()));
    }

    @Test
    @DisplayName("Should be a JFrameException")
    public void shouldBeJFrameException() {
        // Given: An ApiError
        final ApiError apiError = new TestApiError("ERR005", "Test error");

        // When: Creating an ApiException
        final ApiException exception = new TestApiException(apiError);

        // Then: Exception is an instance of JFrameException
        assertThat(exception, is(instanceOf(JFrameException.class)));
    }
}
