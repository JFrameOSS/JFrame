package io.github.jframe.exception.core;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;

import java.io.Serial;

/**
 * A Bad Request (400) exception.
 */
public class BadRequestException extends HttpException {

    @Serial
    private static final long serialVersionUID = -4395628375914269570L;

    /** Constructs a new {@code BadRequestException}. */
    public BadRequestException() {
        super(JFrameErrorCode.BAD_REQUEST);
    }

    /** Constructs a new {@code BadRequestException} with the supplied cause. */
    public BadRequestException(final Throwable cause) {
        super(JFrameErrorCode.BAD_REQUEST, cause);
    }
}
