package io.github.jframe.exception.resource;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Response resource for rate limit exceeded errors.
 *
 * <p>Extends {@link ErrorResponseResource} with rate limit fields for the
 * {@code X-RateLimit-Limit}, {@code X-RateLimit-Remaining} and {@code X-RateLimit-Reset} headers.
 */
@Getter
@Setter
public class RateLimitErrorResponseResource extends ErrorResponseResource {

    private int limit;

    private int remaining;

    private OffsetDateTime resetDate;

    /** Default constructor. */
    public RateLimitErrorResponseResource() {
        super();
    }

    /** Construct a rate limit error resource with the given throwable. */
    public RateLimitErrorResponseResource(final Throwable throwable) {
        super(throwable);
    }
}
