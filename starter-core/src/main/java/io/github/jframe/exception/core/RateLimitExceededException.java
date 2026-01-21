package io.github.jframe.exception.core;

import io.github.jframe.exception.HttpException;
import lombok.Getter;

import java.io.Serial;
import java.time.OffsetDateTime;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

/**
 * Exception thrown when a rate limit has been exceeded.
 *
 * <p>Contains rate limit metadata that can be used to populate standard rate limit headers:
 * <ul>
 * <li>{@code X-RateLimit-Limit} - Maximum requests allowed</li>
 * <li>{@code X-RateLimit-Remaining} - Requests remaining in current window</li>
 * <li>{@code X-RateLimit-Reset} - When the rate limit resets</li>
 * </ul>
 */
@Getter
public class RateLimitExceededException extends HttpException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = 1L;

    /** Maximum number of requests allowed in the current window. */
    private final int limit;

    /** Number of requests remaining in the current window. */
    private final int remaining;

    /** The time when the rate limit resets. */
    private final OffsetDateTime resetDate;

    /** Constructs a new {@code RateLimitExceededException} with rate limit details. */
    public RateLimitExceededException(final int limit, final int remaining, final OffsetDateTime resetDate) {
        super(TOO_MANY_REQUESTS);
        this.limit = limit;
        this.remaining = remaining;
        this.resetDate = resetDate;
    }

    /** Constructs a new {@code RateLimitExceededException} with the supplied message and rate limit details. */
    public RateLimitExceededException(final String message,
                                      final int limit,
                                      final int remaining,
                                      final OffsetDateTime resetDate) {
        super(message, TOO_MANY_REQUESTS);
        this.limit = limit;
        this.remaining = remaining;
        this.resetDate = resetDate;
    }

    /**
     * Constructs a new {@code RateLimitExceededException} with the supplied message, {@link Throwable}
     * and rate limit details.
     */
    public RateLimitExceededException(final String message,
                                      final Throwable cause,
                                      final int limit,
                                      final int remaining,
                                      final OffsetDateTime resetDate) {
        super(message, cause, TOO_MANY_REQUESTS);
        this.limit = limit;
        this.remaining = remaining;
        this.resetDate = resetDate;
    }

    /** Constructs a new {@code RateLimitExceededException} with the supplied {@link Throwable} and rate limit details. */
    public RateLimitExceededException(final Throwable cause,
                                      final int limit,
                                      final int remaining,
                                      final OffsetDateTime resetDate) {
        super(cause, TOO_MANY_REQUESTS);
        this.limit = limit;
        this.remaining = remaining;
        this.resetDate = resetDate;
    }
}
