package io.github.jframe.exception.core;

import io.github.jframe.exception.HttpException;

import java.io.Serial;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * Exception thrown when the requested resource is not available for the user performing the request.
 */
public class UnauthorizedRequestException extends HttpException {

    /** The serial version uuid. */
    @Serial
    private static final long serialVersionUID = -2865532032411939533L;

    /** Default constructor. */
    public UnauthorizedRequestException() {
        super(UNAUTHORIZED);
    }

    /**
     * Constructor with a message.
     *
     * @param message The message to set.
     */
    public UnauthorizedRequestException(final String message) {
        super(message, UNAUTHORIZED);
    }

    /**
     * Constructor with a message and an underlying cause.
     *
     * @param message The message to set.
     * @param cause   The cause.
     */
    public UnauthorizedRequestException(final String message, final Throwable cause) {
        super(message, cause, UNAUTHORIZED);
    }

    /**
     * Constructor with an underlying cause.
     *
     * @param cause The cause.
     */
    public UnauthorizedRequestException(final Throwable cause) {
        super(cause, UNAUTHORIZED);
    }
}
