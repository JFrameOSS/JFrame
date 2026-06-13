package io.github.jframe.exception;

import lombok.Getter;

import java.io.Serial;
import jakarta.ws.rs.core.Response;

import static java.util.Objects.requireNonNull;

/**
 * An HTTP exception carrying an HTTP status, optional error code and reason.
 */
@Getter
public class HttpException extends JFrameException {

    @Serial
    private static final long serialVersionUID = -5505430727908889048L;

    private static final String PARAM_API_ERROR = "apiError";

    private final Response.Status httpStatus;

    private final String errorCode;

    private final String errorReason;

    /** Constructs a new {@code HttpException} with the supplied {@link Response.Status}. */
    public HttpException(final Response.Status httpStatus) {
        super();
        this.httpStatus = requireNonNull(httpStatus);
        this.errorCode = null;
        this.errorReason = null;
    }

    /** Constructs a new {@code HttpException} with the supplied message and {@link Response.Status}. */
    public HttpException(final String message, final Response.Status httpStatus) {
        super(message);
        this.httpStatus = requireNonNull(httpStatus);
        this.errorCode = null;
        this.errorReason = null;
    }

    /** Constructs a new {@code HttpException} with the supplied message, cause and {@link Response.Status}. */
    public HttpException(final String message, final Throwable cause, final Response.Status httpStatus) {
        super(message, cause);
        this.httpStatus = requireNonNull(httpStatus);
        this.errorCode = null;
        this.errorReason = null;
    }

    /** Constructs a new {@code HttpException} with the supplied cause and {@link Response.Status}. */
    public HttpException(final Throwable cause, final Response.Status httpStatus) {
        super(cause);
        this.httpStatus = requireNonNull(httpStatus);
        this.errorCode = null;
        this.errorReason = null;
    }

    /** Constructs a new {@code HttpException} from the supplied {@link ApiError}. */
    public HttpException(final ApiError apiError) {
        super((String) null);
        this.httpStatus = requireNonNull(apiError, PARAM_API_ERROR).getHttpStatus();
        this.errorCode = apiError.getErrorCode();
        this.errorReason = apiError.getReason();
    }

    /** Constructs a new {@code HttpException} from the supplied {@link ApiError} and cause. */
    public HttpException(final ApiError apiError, final Throwable cause) {
        super((String) null, cause);
        this.httpStatus = requireNonNull(apiError, PARAM_API_ERROR).getHttpStatus();
        this.errorCode = apiError.getErrorCode();
        this.errorReason = apiError.getReason();
    }
}
