package io.github.jframe.exception;

import jakarta.ws.rs.core.Response;

/**
 * Standard JFrame error codes implementing {@link ApiError}.
 *
 * <p>These codes represent common error situations within the JFrame framework and are
 * intended to be used as the {@link ApiError} argument when constructing
 * {@link HttpException} subclasses.
 */
public enum JFrameErrorCode implements ApiError {

    BAD_REQUEST("JFRAME_BAD_REQUEST", "Bad request", Response.Status.BAD_REQUEST),
    NOT_FOUND("JFRAME_NOT_FOUND", "Resource not found", Response.Status.NOT_FOUND),
    RATE_LIMITED("JFRAME_RATE_LIMITED", "Rate limit exceeded", Response.Status.TOO_MANY_REQUESTS),
    VALIDATION_ERROR("JFRAME_VALIDATION_ERROR", "Validation failed", Response.Status.BAD_REQUEST),
    INTERNAL_ERROR("JFRAME_INTERNAL_ERROR", "Internal server error", Response.Status.INTERNAL_SERVER_ERROR),
    HTTP_ERROR("JFRAME_HTTP_ERROR", "HTTP error", Response.Status.BAD_REQUEST);

    private final String errorCode;
    private final String reason;
    private final Response.Status httpStatus;

    JFrameErrorCode(final String errorCode, final String reason, final Response.Status httpStatus) {
        this.errorCode = errorCode;
        this.reason = reason;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public Response.Status getHttpStatus() {
        return httpStatus;
    }
}
