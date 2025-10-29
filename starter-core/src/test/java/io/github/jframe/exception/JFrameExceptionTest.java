package io.github.jframe.exception;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link JFrameException}.
 *
 * <p>Verifies the base exception class functionality including:
 * <ul>
 * <li>Constructor variations (no args, message, cause, message+cause)</li>
 * <li>Exception hierarchy (extends RuntimeException)</li>
 * <li>Proper propagation of messages and causes</li>
 * </ul>
 */
@DisplayName("Exception Hierarchy - JFrame Exception")
public class JFrameExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should create exception with no arguments")
    public void shouldCreateExceptionWithNoArguments() {
        // Given: No preconditions needed

        // When: Creating exception with no arguments
        final JFrameException exception = new JFrameException();

        // Then: Exception is created with null message and cause
        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message")
    public void shouldCreateExceptionWithMessage() {
        // Given: A test exception message
        final String message = "Test exception message";

        // When: Creating exception with message
        final JFrameException exception = new JFrameException(message);

        // Then: Exception is created with the message and null cause
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    public void shouldCreateExceptionWithMessageAndCause() {
        // Given: A test exception message and a root cause
        final String message = "Test exception message";
        final Throwable cause = new RuntimeException("Root cause");

        // When: Creating exception with message and cause
        final JFrameException exception = new JFrameException(message, cause);

        // Then: Exception is created with both message and cause
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    @DisplayName("Should create exception with cause only")
    public void shouldCreateExceptionWithCause() {
        // Given: A root cause exception
        final Throwable cause = new RuntimeException("Root cause");

        // When: Creating exception with cause only
        final JFrameException exception = new JFrameException(cause);

        // Then: Exception is created with the cause and message derived from cause
        assertThat(exception.getCause(), is(equalTo(cause)));
        assertThat(exception.getMessage(), containsString("Root cause"));
    }

    @Test
    @DisplayName("Should be a RuntimeException")
    public void shouldBeRuntimeException() {
        // Given: No preconditions needed

        // When: Creating a JFrameException
        final JFrameException exception = new JFrameException();

        // Then: Exception is an instance of RuntimeException
        assertThat(exception, is(instanceOf(RuntimeException.class)));
    }
}
