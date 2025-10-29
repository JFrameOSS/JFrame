package io.github.jframe.exception.core;


import io.github.jframe.exception.HttpException;

import java.io.Serial;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * A resource not found exception.
 */
public class ResourceNotFoundException extends HttpException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = 7464957757015625481L;

    /** Constructs a new {@code ResourceNotFoundException}. */
    public ResourceNotFoundException() {
        super(NOT_FOUND);
    }

    /**
     * Constructs a new {@code ResourceNotFoundException} with the supplied message.
     *
     * @param message The message to set.
     */
    public ResourceNotFoundException(final String message) {
        super(message, NOT_FOUND);
    }

    /**
     * Constructs a new {@code ResourceNotFoundException} with the supplied message and {@link Throwable}.
     *
     * @param message The message to set.
     * @param cause   The cause.
     */
    public ResourceNotFoundException(final String message, final Throwable cause) {
        super(message, cause, NOT_FOUND);
    }

    /**
     * Constructs a new {@code ResourceNotFoundException} with the supplied {@link Throwable}.
     *
     * @param cause The cause.
     */
    public ResourceNotFoundException(final Throwable cause) {
        super(cause, NOT_FOUND);
    }
}
