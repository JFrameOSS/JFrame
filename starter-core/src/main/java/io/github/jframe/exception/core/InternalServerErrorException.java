package io.github.jframe.exception.core;


import io.github.jframe.exception.HttpException;

import java.io.Serial;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * An internal service error exception.
 */
public class InternalServerErrorException extends HttpException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = -7187613746044972447L;

    /** Constructs a new {@code InternalServerErrorException}. */
    public InternalServerErrorException() {
        super(INTERNAL_SERVER_ERROR);
    }

    /**
     * Constructs a new {@code InternalServerErrorException} with the supplied message.
     *
     * @param message The message to set.
     */
    public InternalServerErrorException(final String message) {
        super(message, INTERNAL_SERVER_ERROR);
    }

    /**
     * Constructs a new {@code InternalServerErrorException} with the supplied message and {@link Throwable}.
     *
     * @param message The message to set.
     * @param cause   The cause.
     */
    public InternalServerErrorException(final String message, final Throwable cause) {
        super(message, cause, INTERNAL_SERVER_ERROR);
    }

    /**
     * Constructs a new {@code InternalServerErrorException} with the supplied {@link Throwable}.
     *
     * @param cause The cause.
     */
    public InternalServerErrorException(final Throwable cause) {
        super(cause, INTERNAL_SERVER_ERROR);
    }
}
