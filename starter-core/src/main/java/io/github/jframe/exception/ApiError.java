package io.github.jframe.exception;

import java.io.Serializable;

/**
 * Interface that defines an API error of the application.
 *
 * <p>An API error is a well-defined error situation with a unique error code.
 *
 * <p>An application built on JFrame might implement this using an enum.
 */
public interface ApiError extends Serializable {

    /**
     * Getter for the error code.
     *
     * @return the error code
     */
    String errorCode();

    /**
     * Get the error reason.
     *
     * @return the reason
     */
    String reason();
}
