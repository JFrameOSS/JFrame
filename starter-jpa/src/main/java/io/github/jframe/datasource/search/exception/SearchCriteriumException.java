package io.github.jframe.datasource.search.exception;

import io.github.jframe.exception.HttpException;

import java.io.Serial;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
        super(message, BAD_REQUEST);
    }
}
