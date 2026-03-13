package io.github.jframe.exception;

import io.github.jframe.http.HttpStatusCode;
import lombok.Getter;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

/**
 * An HTTP exception.
 */
@Getter
public class HttpException extends JFrameException {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = -5505430727908889048L;

    private final HttpStatusCode httpStatus;

    /** Constructs a new {@code HttpException} with the supplied {@link HttpStatusCode}. */
    public HttpException(final HttpStatusCode httpStatus) {
        super();
        this.httpStatus = requireNonNull(httpStatus);
    }

    /** Constructs a new {@code HttpException} with the supplied message and {@link HttpStatusCode}. */
    public HttpException(final String message, final HttpStatusCode httpStatus) {
        super(message);
        this.httpStatus = requireNonNull(httpStatus);
    }

    /**
     * Constructs a new {@code HttpException} with the supplied message, {@link Throwable} and {@link HttpStatusCode}.
     */
    public HttpException(final String message, final Throwable cause, final HttpStatusCode httpStatus) {
        super(message, cause);
        this.httpStatus = requireNonNull(httpStatus);
    }

    /**
     * Constructs a new {@code HttpException} with the supplied {@link Throwable} and {@link HttpStatusCode}.
     */
    public HttpException(final Throwable cause, final HttpStatusCode httpStatus) {
        super(cause);
        this.httpStatus = requireNonNull(httpStatus);
    }
}
