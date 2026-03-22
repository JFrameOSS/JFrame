package io.github.jframe.exception;

import lombok.Getter;

import java.io.Serial;
import jakarta.ws.rs.core.Response;

import static java.util.Objects.requireNonNull;

/**
 * An HTTP exception.
 */
@Getter
public class HttpException extends JFrameException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = -5505430727908889048L;

    private final Response.Status httpStatus;

    /** Constructs a new {@code HttpException} with the supplied {@link Response.Status}. */
    public HttpException(final Response.Status httpStatus) {
        super();
        this.httpStatus = requireNonNull(httpStatus);
    }

    /** Constructs a new {@code HttpException} with the supplied message and {@link Response.Status}. */
    public HttpException(final String message, final Response.Status httpStatus) {
        super(message);
        this.httpStatus = requireNonNull(httpStatus);
    }

    /**
     * Constructs a new {@code HttpException} with the supplied message, {@link Throwable} and {@link Response.Status}.
     */
    public HttpException(final String message, final Throwable cause, final Response.Status httpStatus) {
        super(message, cause);
        this.httpStatus = requireNonNull(httpStatus);
    }

    /**
     * Constructs a new {@code HttpException} with the supplied {@link Throwable} and {@link Response.Status}.
     */
    public HttpException(final Throwable cause, final Response.Status httpStatus) {
        super(cause);
        this.httpStatus = requireNonNull(httpStatus);
    }
}
