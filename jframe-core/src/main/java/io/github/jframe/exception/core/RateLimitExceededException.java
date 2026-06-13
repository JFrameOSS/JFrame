package io.github.jframe.exception.core;

import io.github.jframe.exception.HttpException;
import lombok.Getter;

import java.io.Serial;
import java.time.OffsetDateTime;

import static jakarta.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;

/**
 * Exception thrown when a rate limit has been exceeded (429 Too Many Requests).
 *
 * <p>Carries rate limit metadata for populating standard response headers:
 * {@code X-RateLimit-Limit}, {@code X-RateLimit-Remaining}, {@code X-RateLimit-Reset}.
 */
@Getter
public class RateLimitExceededException extends HttpException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int limit;

    private final int remaining;

    private final OffsetDateTime resetDate;

    /** Constructs a new {@code RateLimitExceededException} with rate limit details. */
    public RateLimitExceededException(final int limit, final int remaining, final OffsetDateTime resetDate) {
        super(TOO_MANY_REQUESTS);
        this.limit = limit;
        this.remaining = remaining;
        this.resetDate = resetDate;
    }

    /** Constructs a new {@code RateLimitExceededException} with message and rate limit details. */
    public RateLimitExceededException(final String message,
                                      final int limit,
                                      final int remaining,
                                      final OffsetDateTime resetDate) {
        super(message, TOO_MANY_REQUESTS);
        this.limit = limit;
        this.remaining = remaining;
        this.resetDate = resetDate;
    }

    /** Constructs a new {@code RateLimitExceededException} with message, cause and rate limit details. */
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

    /** Constructs a new {@code RateLimitExceededException} with cause and rate limit details. */
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
