package io.github.jframe.exception.page;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * Thrown when a paging request has an invalid page number.
 *
 * <p>Carries {@code rejectedParameter} and {@code rejectedValue} for inclusion in the 400 response body.
 */
@Getter
public class InvalidPageException extends HttpException {

    @Serial
    private static final long serialVersionUID = -2961548373041187620L;

    /** The rejected parameter name (e.g. {@code pageNumber}). */
    private final String rejectedParameter;

    /** The rejected value. */
    private final int rejectedValue;

    /**
     * Creates the exception.
     *
     * @param rejectedParameter the rejected parameter name
     * @param rejectedValue     the rejected value
     */
    public InvalidPageException(final String rejectedParameter, final int rejectedValue) {
        super(JFrameErrorCode.INVALID_PAGE);
        this.rejectedParameter = rejectedParameter;
        this.rejectedValue = rejectedValue;
    }
}
