package io.github.jframe.datasource.search.exception;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;

import java.io.Serial;

/**
 * Exceptions relating to the use of SearchCriteria.
 */
public class SearchCriteriumException extends HttpException {

    @Serial
    private static final long serialVersionUID = -1108938201833171823L;

    /** Constructs a new {@code SearchCriteriumException}. */
    public SearchCriteriumException() {
        super(JFrameErrorCode.BAD_REQUEST);
    }

    /** Constructs a new {@code SearchCriteriumException} with the supplied cause. */
    public SearchCriteriumException(final Throwable cause) {
        super(JFrameErrorCode.BAD_REQUEST, cause);
    }
}
