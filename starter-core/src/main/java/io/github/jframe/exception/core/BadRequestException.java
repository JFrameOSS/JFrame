package io.github.jframe.exception.core;


import io.github.jframe.exception.HttpException;

import java.io.Serial;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * A Bad Request exception.
 */
public class BadRequestException extends HttpException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = -4395628375914269570L;

    /** Constructs a new {@code BadRequestException}. */
    public BadRequestException() {
        super(BAD_REQUEST);
    }

    /** Constructs a new {@code BadRequestException} with the supplied message. */
    public BadRequestException(final String message) {
        super(message, BAD_REQUEST);
    }

    /**
     * Constructs a new {@code BadRequestException} with the supplied message and {@link Throwable}.
     */
    public BadRequestException(final String message, final Throwable cause) {
        super(message, cause, BAD_REQUEST);
    }

    /** Constructs a new {@code BadRequestException} with the supplied {@link Throwable}. */
    public BadRequestException(final Throwable cause) {
        super(cause, BAD_REQUEST);
    }
}
