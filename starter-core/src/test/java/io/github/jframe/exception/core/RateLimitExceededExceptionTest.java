package io.github.jframe.exception.core;

import io.github.jframe.exception.HttpException;
import io.github.support.UnitTest;

import java.time.OffsetDateTime;

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
 * Tests for {@link RateLimitExceededException}.
 *
 * <p>Verifies the RateLimitExceededException functionality including:
 * <ul>
 * <li>Constructor variations (rate limit details, message, cause, message+cause)</li>
 * <li>HTTP status is always TOO_MANY_REQUESTS (429)</li>
 * <li>Rate limit metadata (limit, remaining, resetDate) is correctly stored</li>
 * <li>Exception hierarchy (extends HttpException)</li>
 * </ul>
 */
@DisplayName("Exception Hierarchy - Rate Limit Exceeded Exception")
public class RateLimitExceededExceptionTest extends UnitTest {

    private static final int LIMIT = 100;
    private static final int REMAINING = 0;
    private static final OffsetDateTime RESET_DATE = OffsetDateTime.now().plusMinutes(5);

    @Test
    @DisplayName("Should create exception with rate limit details and TOO_MANY_REQUESTS status")
    public void shouldCreateExceptionWithRateLimitDetails() {
        // Given: Rate limit details

        // When: Creating exception with rate limit details
        final RateLimitExceededException exception = new RateLimitExceededException(LIMIT, REMAINING, RESET_DATE);

        // Then: Exception is created with TOO_MANY_REQUESTS status and correct rate limit details
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.TOO_MANY_REQUESTS)));
        assertThat(exception.getLimit(), is(equalTo(LIMIT)));
        assertThat(exception.getRemaining(), is(equalTo(REMAINING)));
        assertThat(exception.getResetDate(), is(equalTo(RESET_DATE)));
        assertThat(exception.getMessage(), is(nullValue()));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message and rate limit details")
    public void shouldCreateExceptionWithMessageAndRateLimitDetails() {
        // Given: An error message and rate limit details
        final String message = "Rate limit exceeded for API key";

        // When: Creating exception with message and rate limit details
        final RateLimitExceededException exception = new RateLimitExceededException(message, LIMIT, REMAINING, RESET_DATE);

        // Then: Exception is created with TOO_MANY_REQUESTS status, message and rate limit details
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.TOO_MANY_REQUESTS)));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getLimit(), is(equalTo(LIMIT)));
        assertThat(exception.getRemaining(), is(equalTo(REMAINING)));
        assertThat(exception.getResetDate(), is(equalTo(RESET_DATE)));
        assertThat(exception.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create exception with message, cause and rate limit details")
    public void shouldCreateExceptionWithMessageCauseAndRateLimitDetails() {
        // Given: An error message, a root cause and rate limit details
        final String message = "Rate limit exceeded";
        final Throwable cause = new IllegalStateException("Too many requests");

        // When: Creating exception with message, cause and rate limit details
        final RateLimitExceededException exception = new RateLimitExceededException(message, cause, LIMIT, REMAINING, RESET_DATE);

        // Then: Exception is created with TOO_MANY_REQUESTS status, message, cause and rate limit details
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.TOO_MANY_REQUESTS)));
        assertThat(exception.getMessage(), is(equalTo(message)));
        assertThat(exception.getCause(), is(equalTo(cause)));
        assertThat(exception.getLimit(), is(equalTo(LIMIT)));
        assertThat(exception.getRemaining(), is(equalTo(REMAINING)));
        assertThat(exception.getResetDate(), is(equalTo(RESET_DATE)));
    }

    @Test
    @DisplayName("Should create exception with cause and rate limit details")
    public void shouldCreateExceptionWithCauseAndRateLimitDetails() {
        // Given: A root cause exception and rate limit details
        final Throwable cause = new IllegalStateException("Too many requests");

        // When: Creating exception with cause and rate limit details
        final RateLimitExceededException exception = new RateLimitExceededException(cause, LIMIT, REMAINING, RESET_DATE);

        // Then: Exception is created with TOO_MANY_REQUESTS status, cause and rate limit details
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.TOO_MANY_REQUESTS)));
        assertThat(exception.getCause(), is(equalTo(cause)));
        assertThat(exception.getMessage(), containsString("Too many requests"));
        assertThat(exception.getLimit(), is(equalTo(LIMIT)));
        assertThat(exception.getRemaining(), is(equalTo(REMAINING)));
        assertThat(exception.getResetDate(), is(equalTo(RESET_DATE)));
    }

    @Test
    @DisplayName("Should be a HttpException")
    public void shouldBeHttpException() {
        // Given: No preconditions needed

        // When: Creating a RateLimitExceededException
        final RateLimitExceededException exception = new RateLimitExceededException(LIMIT, REMAINING, RESET_DATE);

        // Then: Exception is an instance of HttpException
        assertThat(exception, is(instanceOf(HttpException.class)));
    }

    @Test
    @DisplayName("Should handle null resetDate")
    public void shouldHandleNullResetDate() {
        // Given: A null reset date

        // When: Creating exception with null reset date
        final RateLimitExceededException exception = new RateLimitExceededException(LIMIT, REMAINING, null);

        // Then: Exception is created with null resetDate
        assertThat(exception.getHttpStatus(), is(equalTo(HttpStatus.TOO_MANY_REQUESTS)));
        assertThat(exception.getLimit(), is(equalTo(LIMIT)));
        assertThat(exception.getRemaining(), is(equalTo(REMAINING)));
        assertThat(exception.getResetDate(), is(nullValue()));
    }
}
