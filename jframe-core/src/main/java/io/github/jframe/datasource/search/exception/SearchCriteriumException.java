package io.github.jframe.datasource.search.exception;

import io.github.jframe.exception.HttpException;
import io.github.jframe.http.HttpStatusCode;

import java.io.Serial;

/**
 * Exceptions relating to the use of SearchCriteria.
 */
public class SearchCriteriumException extends HttpException {

    @Serial
    private static final long serialVersionUID = -1108938201833171823L;

    /**
     * The required-argument constructor.
     *
     * @param message error message.
     */
    public SearchCriteriumException(final String message) {
        super(message, HttpStatusCode.BAD_REQUEST);
    }
}
