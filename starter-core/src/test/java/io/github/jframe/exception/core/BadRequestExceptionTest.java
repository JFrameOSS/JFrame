package io.github.jframe.exception.core;

import io.github.jframe.exception.HttpException;
import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link BadRequestException}.
 *
 * <p>Verifies the BadRequestException functionality including:
 * <ul>
 * <li>Constructor variations (no args, message, cause, message+cause)</li>
 * <li>HTTP status is always BAD_REQUEST (400)</li>
 * <li>Exception hierarchy (extends HttpException)</li>
 * </ul>
 */
@DisplayName("Exception Hierarchy - Bad Request Exception")
public class BadRequestExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should create exception with no arguments and BAD_REQUEST status")
    public void shouldCreateExceptionWithNoArguments() {
        // Given: No preconditions needed

        // When: Creating exception with no arguments
        final BadRequestException exception = new BadRequestException();

        // Then: Exception is created with BAD_REQUEST status and null message and cause
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message and BAD_REQUEST status")
    public void shouldCreateExceptionWithMessage() {
        // Given: An error message
        final String message = "Invalid request parameter";

        // When: Creating exception with message
        final BadRequestException exception = new BadRequestException(message);

        // Then: Exception is created with BAD_REQUEST status, message and null cause
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    public void shouldCreateExceptionWithMessageAndCause() {
        // Given: An error message and a root cause
        final String message = "Invalid request parameter";
        final Throwable cause = new IllegalArgumentException("Parameter validation failed");

        // When: Creating exception with message and cause
        final BadRequestException exception = new BadRequestException(message, cause);

        // Then: Exception is created with BAD_REQUEST status, message and cause
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    @DisplayName("Should create exception with cause only")
    public void shouldCreateExceptionWithCause() {
        // Given: A root cause exception
        final Throwable cause = new IllegalArgumentException("Parameter validation failed");

        // When: Creating exception with cause only
        final BadRequestException exception = new BadRequestException(cause);

        // Then: Exception is created with BAD_REQUEST status, cause and message derived from cause
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(exception.getCause(), is(equalTo(cause)));
        assertThat(exception.getMessage(), containsString("Parameter validation failed"));
    }

    @Test
    @DisplayName("Should be a HttpException")
    public void shouldBeHttpException() {
        // Given: No preconditions needed

        // When: Creating a BadRequestException
        final BadRequestException exception = new BadRequestException();

        // Then: Exception is an instance of HttpException
        assertThat(exception, is(instanceOf(HttpException.class)));
    }
}
