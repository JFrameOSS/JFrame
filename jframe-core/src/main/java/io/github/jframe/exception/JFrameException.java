package io.github.jframe.exception;

import java.io.Serial;

/**
 * Base exception type for all runtime exceptions within the JFrame framework.
 *
 * <p>This class serves as the common ancestor for all custom exceptions in the framework.
 * It extends {@link RuntimeException}, allowing subclasses to represent both
 * application-level and system-level failures in a consistent manner.
 *
 * <p>Typically, you would extend {@code JFrameException} to create more specialized
 * exception types — for example, {@link io.github.jframe.exception.HttpException}
 * for REST API errors.
 */
public class JFrameException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6463686875194124117L;

    /** Constructs a new {@code JFrameException}. */
    public JFrameException() {
        super();
    }

    /** Constructs a new {@code JFrameException} with the supplied message. */
    public JFrameException(final String message) {
        super(message);
    }

    /** Constructs a new {@code JFrameException} with the supplied message and cause. */
    public JFrameException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /** Constructs a new {@code JFrameException} with the supplied cause. */
    public JFrameException(final Throwable cause) {
        super(cause);
    }
}
