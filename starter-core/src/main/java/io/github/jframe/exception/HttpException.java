package io.github.jframe.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

import static java.util.Objects.requireNonNull;

/**
 * An HTTP exception.
 */
public class HttpException extends JFrameException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = -5505430727908889048L;

    private final HttpStatus httpStatus;

    /** Constructs a new {@code HttpException} with the supplied {@link HttpStatus}. */
    public HttpException(final HttpStatus httpStatus) {
        super();
        this.httpStatus = requireNonNull(httpStatus);
    }

    /** Constructs a new {@code HttpException} with the supplied message and {@link HttpStatus}. */
    public HttpException(final String message, final HttpStatus httpStatus) {
        super(message);
        this.httpStatus = requireNonNull(httpStatus);
    }

    /**
     * Constructs a new {@code HttpException} with the supplied message, {@link Throwable} and {@link HttpStatus}.
     */
    public HttpException(final String message, final Throwable cause, final HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = requireNonNull(httpStatus);
    }

    /**
     * Constructs a new {@code HttpException} with the supplied {@link Throwable} and {@link HttpStatus}.
     */
    public HttpException(final Throwable cause, final HttpStatus httpStatus) {
        super(cause);
        this.httpStatus = requireNonNull(httpStatus);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
