package io.github.jframe.exception;

import io.github.support.UnitTest;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link HttpException}.
 *
 * <p>Verifies the HTTP exception functionality including:
 * <ul>
 * <li>Constructor variations with Response.Status parameter</li>
 * <li>Response.Status storage and retrieval</li>
 * <li>Null safety for Response.Status parameter</li>
 * <li>Support for different HTTP status codes</li>
 * </ul>
 */
@DisplayName("Exception Hierarchy - HTTP Exception")
public class HttpExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should create exception with HTTP status")
    public void shouldCreateExceptionWithHttpStatus() {
        // Given: An HTTP status code
        final Response.Status status = Response.Status.BAD_REQUEST;

        // When: Creating exception with HTTP status only
        final HttpException exception = new HttpException(status);

        // Then: Exception is created with the status and null message and cause
        assertThat(exception.getHttpStatus(), is(equalTo(status)));
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message and HTTP status")
    public void shouldCreateExceptionWithMessageAndHttpStatus() {
        // Given: An error message and HTTP status
        final String message = "Bad request error";
        final Response.Status status = Response.Status.BAD_REQUEST;

        // When: Creating exception with message and HTTP status
        final HttpException exception = new HttpException(message, status);

        // Then: Exception is created with the status, message and null cause
        assertThat(exception.getHttpStatus(), is(equalTo(status)));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message, cause and HTTP status")
    public void shouldCreateExceptionWithMessageCauseAndHttpStatus() {
        // Given: An error message, root cause and HTTP status
        final String message = "Bad request error";
        final Throwable cause = new RuntimeException("Root cause");
        final Response.Status status = Response.Status.BAD_REQUEST;

        // When: Creating exception with message, cause and HTTP status
        final HttpException exception = new HttpException(message, cause, status);

        // Then: Exception is created with all three parameters
        assertThat(exception.getHttpStatus(), is(equalTo(status)));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    @DisplayName("Should create exception with cause and HTTP status")
    public void shouldCreateExceptionWithCauseAndHttpStatus() {
        // Given: A root cause and HTTP status
        final Throwable cause = new RuntimeException("Root cause");
        final Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

        // When: Creating exception with cause and HTTP status
        final HttpException exception = new HttpException(cause, status);

        // Then: Exception is created with the status, cause and message derived from cause
        assertThat(exception.getHttpStatus(), is(equalTo(status)));
        assertThat(exception.getCause(), is(equalTo(cause)));
        assertThat(exception.getMessage(), containsString("Root cause"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when HTTP status is null")
    public void shouldRequireNonNullHttpStatus() {
        // Given: A null HTTP status

        // When: Creating exception with null HTTP status
        // Then: NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> new HttpException((Response.Status) null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when HTTP status is null with message")
    public void shouldRequireNonNullHttpStatusWithMessage() {
        // Given: A message and null HTTP status

        // When: Creating exception with message and null HTTP status
        // Then: NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> new HttpException("message", (Response.Status) null));
    }

    @Test
    @DisplayName("Should support different HTTP status codes")
    public void shouldSupportDifferentHttpStatusCodes() {
        // Given: Various HTTP status codes

        // When: Creating exceptions with different HTTP status codes
        // Then: Each exception correctly stores its HTTP status
        assertThat(new HttpException(Response.Status.OK).getHttpStatus(), is(equalTo(Response.Status.OK)));
        assertThat(new HttpException(Response.Status.NOT_FOUND).getHttpStatus(), is(equalTo(Response.Status.NOT_FOUND)));
        assertThat(new HttpException(Response.Status.UNAUTHORIZED).getHttpStatus(), is(equalTo(Response.Status.UNAUTHORIZED)));
        assertThat(
            new HttpException(Response.Status.INTERNAL_SERVER_ERROR).getHttpStatus(),
            is(equalTo(Response.Status.INTERNAL_SERVER_ERROR))
        );
    }
}
