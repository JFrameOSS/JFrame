package io.github.jframe.exception.resource;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Response resource for rate limit exceeded errors.
 *
 * <p>Extends the standard error response with rate limit specific fields
 * that can be used by clients to understand the rate limit state.
 */
@Getter
@Setter
public class RateLimitErrorResponseResource extends ErrorResponseResource {

    /** Maximum number of requests allowed in the current window. */
    private int limit;

    /** Number of requests remaining in the current window. */
    private int remaining;

    /** The time when the rate limit resets. */
    private OffsetDateTime resetDate;

    /** Default constructor. */
    public RateLimitErrorResponseResource() {
        super();
    }

    /**
     * Construct a rate limit error resource with a throwable.
     *
     * @param throwable the throwable
     */
    public RateLimitErrorResponseResource(final Throwable throwable) {
        super(throwable);
    }
}
