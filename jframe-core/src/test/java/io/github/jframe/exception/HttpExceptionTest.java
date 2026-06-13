package io.github.jframe.exception;

import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link HttpException}.
 *
 * <p>Verifies the HTTP exception functionality including:
 * <ul>
 * <li>Constructor variations with ApiError parameter</li>
 * <li>Response.Status storage and retrieval via ApiError</li>
 * <li>Null safety for ApiError parameter</li>
 * <li>Support for different HTTP status codes via ApiError</li>
 * </ul>
 */
@DisplayName("Exception Hierarchy - HTTP Exception")
public class HttpExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should create exception from ApiError")
    public void shouldCreateExceptionFromApiError() {
        // Given: An ApiError with status, errorCode and reason
        final TestApiError apiError = new TestApiError("ERR_001", "Test error", Response.Status.BAD_REQUEST);

        // When: Creating exception from ApiError
        final HttpException exception = new HttpException(apiError);

        // Then: Exception carries status, errorCode, errorReason and null message
        assertThat(exception.getHttpStatus(), is(equalTo(Response.Status.BAD_REQUEST)));
        assertThat(exception.getErrorCode(), is(equalTo("ERR_001")));
        assertThat(exception.getErrorReason(), is(equalTo("Test error")));
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception from ApiError with cause")
    public void shouldCreateExceptionFromApiErrorWithCause() {
        // Given: An ApiError and a root cause
        final TestApiError apiError = new TestApiError("ERR_001", "Test error", Response.Status.BAD_REQUEST);
        final Throwable cause = new RuntimeException("Root cause");

        // When: Creating exception from ApiError and cause
        final HttpException exception = new HttpException(apiError, cause);

        // Then: Exception carries status, errorCode, errorReason and the cause
        assertThat(exception.getHttpStatus(), is(equalTo(Response.Status.BAD_REQUEST)));
        assertThat(exception.getErrorCode(), is(equalTo("ERR_001")));
        assertThat(exception.getErrorReason(), is(equalTo("Test error")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    @DisplayName("Should return null errorCode when ApiError has null error code")
    public void shouldReturnNullErrorCodeWhenCreatedWithStatusOnly() {
        // Given: An ApiError that returns null for getErrorCode()
        final TestApiError apiError = new TestApiError(null, "Some reason", Response.Status.BAD_REQUEST);

        // When: Creating exception from ApiError with null error code
        final HttpException exception = new HttpException(apiError);

        // Then: errorCode is null, errorReason is still present
        assertThat(exception.getErrorCode(), is(nullValue()));
        assertThat(exception.getErrorReason(), is(equalTo("Some reason")));
    }

    @Test
    @DisplayName("Should propagate HttpStatus from ApiError for different status codes")
    public void shouldCreateExceptionFromApiErrorWithDifferentStatuses() {
        // Given: ApiErrors with different HTTP statuses

        // When: Creating exceptions from each ApiError
        final HttpException notFound = new HttpException(new TestApiError("ERR_NF", "Not found", Response.Status.NOT_FOUND));
        final HttpException unauthorized = new HttpException(new TestApiError("ERR_UA", "Unauthorized", Response.Status.UNAUTHORIZED));
        final HttpException forbidden = new HttpException(new TestApiError("ERR_FB", "Forbidden", Response.Status.FORBIDDEN));

        // Then: Each exception correctly carries its HTTP status from the ApiError
        assertThat(notFound.getHttpStatus(), is(equalTo(Response.Status.NOT_FOUND)));
        assertThat(unauthorized.getHttpStatus(), is(equalTo(Response.Status.UNAUTHORIZED)));
        assertThat(forbidden.getHttpStatus(), is(equalTo(Response.Status.FORBIDDEN)));
    }

    @Test
    @DisplayName("Should throw NullPointerException when ApiError is null")
    public void shouldRequireNonNullApiError() {
        // Given: A null ApiError

        // When: Creating exception with null ApiError
        // Then: NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> new HttpException((ApiError) null));
    }
}
