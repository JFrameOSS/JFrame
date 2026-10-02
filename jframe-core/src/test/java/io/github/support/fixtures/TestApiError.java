package io.github.support.fixtures;

import io.github.jframe.exception.ApiError;

import jakarta.ws.rs.core.Response;

/** Test implementation of {@link ApiError}. */
public class TestApiError implements ApiError {

    private final String errorCode;
    private final String reason;
    private final Response.Status httpStatus;

    public TestApiError(final String errorCode, final String reason, final Response.Status httpStatus) {
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
