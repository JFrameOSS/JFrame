package io.github.jframe.exception;

import java.io.Serial;

/**
 * Base exception type for all runtime exceptions within the JFrame framework.
 * <p>
 * This class serves as the common ancestor for all custom exceptions in the framework.
 * It extends {@link RuntimeException}, allowing subclasses to represent both
 * application-level and system-level failures in a consistent manner.
 * </p>
 *
 * <p>
 * Typically, you would extend {@code JFrameException} to create more specialized
 * exception types — for example, {@link io.github.jframe.exception.ApiException}
 * for REST API errors.
 * </p>
 */
public class JFrameException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6463686875194124117L;

    /** Constructs a new {@code JFrame}. */
    public JFrameException() {
        super();
    }

    /** Constructs a new {@code JFrame} with the supplied message. */
    public JFrameException(final String message) {
        super(message);
    }

    /** Constructs a new {@code JFrame} with the supplied message and {@link Throwable}. */
    public JFrameException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /** Constructs a new {@code JFrame} with the supplied {@link Throwable}. */
    public JFrameException(final Throwable cause) {
        super(cause);
    }
}
