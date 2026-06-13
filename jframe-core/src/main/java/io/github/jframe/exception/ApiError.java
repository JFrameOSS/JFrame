package io.github.jframe.exception;

import java.io.Serializable;
import jakarta.ws.rs.core.Response;

/**
 * Interface that defines an API error of the application.
 *
 * <p>An API error is a well-defined error situation with a unique error code.
 *
 * <p>An application built on JFrame might implement this using an enum.
 */
public interface ApiError extends Serializable {

    /** Returns the unique error code. */
    String getErrorCode();

    /** Returns the human-readable error reason. */
    String getReason();

    /** Returns the HTTP status associated with this error. */
    Response.Status getHttpStatus();
}
