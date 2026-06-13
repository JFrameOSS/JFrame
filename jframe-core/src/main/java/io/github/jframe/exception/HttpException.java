package io.github.jframe.exception;

import lombok.Getter;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

/**
 * An HTTP exception carrying an HTTP status, optional error code and reason.
 *
 * <p>Construct via an {@link ApiError} to ensure every exception carries a structured error
 * code and reason alongside the HTTP status.
 */
@Getter
public class HttpException extends JFrameException {

    @Serial
    private static final long serialVersionUID = -5505430727908889048L;

    private static final String PARAM_HTTP_STATUS = "httpStatus";

    private final jakarta.ws.rs.core.Response.Status httpStatus;

    private final String errorCode;

    private final String errorReason;

    /** Constructs a new {@code HttpException} from the supplied {@link ApiError}. */
    public HttpException(final ApiError apiError) {
        super((String) null);
        this.httpStatus = requireNonNull(apiError.getHttpStatus(), PARAM_HTTP_STATUS);
        this.errorCode = apiError.getErrorCode();
        this.errorReason = apiError.getReason();
    }

    /** Constructs a new {@code HttpException} from the supplied {@link ApiError} and cause. */
    public HttpException(final ApiError apiError, final Throwable cause) {
        super((String) null, cause);
        this.httpStatus = requireNonNull(apiError.getHttpStatus(), PARAM_HTTP_STATUS);
        this.errorCode = apiError.getErrorCode();
        this.errorReason = apiError.getReason();
    }
}
