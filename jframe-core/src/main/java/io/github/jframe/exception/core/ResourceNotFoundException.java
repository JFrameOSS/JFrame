package io.github.jframe.exception.core;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;

import java.io.Serial;

/**
 * A Resource Not Found (404) exception.
 */
public class ResourceNotFoundException extends HttpException {

    @Serial
    private static final long serialVersionUID = 7464957757015625481L;

    /** Constructs a new {@code ResourceNotFoundException}. */
    public ResourceNotFoundException() {
        super(JFrameErrorCode.NOT_FOUND);
    }

    /** Constructs a new {@code ResourceNotFoundException} with the supplied cause. */
    public ResourceNotFoundException(final Throwable cause) {
        super(JFrameErrorCode.NOT_FOUND, cause);
    }
}
