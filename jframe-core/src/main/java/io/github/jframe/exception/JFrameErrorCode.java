package io.github.jframe.exception;

import java.util.Arrays;
import java.util.Optional;
import jakarta.ws.rs.core.Response;

/**
 * Standard JFrame error codes implementing {@link ApiError}.
 *
 * <p>These codes represent common error situations within the JFrame framework and are
 * intended to be used as the {@link ApiError} argument when constructing {@link HttpException}
 * subclasses.
 */
public enum JFrameErrorCode implements ApiError {

    BAD_REQUEST(Response.Status.BAD_REQUEST),
    UNAUTHORIZED(Response.Status.UNAUTHORIZED),
    FORBIDDEN(Response.Status.FORBIDDEN),
    NOT_FOUND(Response.Status.NOT_FOUND),
    METHOD_NOT_ALLOWED(Response.Status.METHOD_NOT_ALLOWED),
    NOT_ACCEPTABLE(Response.Status.NOT_ACCEPTABLE),
    UNSUPPORTED_MEDIA_TYPE(Response.Status.UNSUPPORTED_MEDIA_TYPE),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "Rate limit exceeded", Response.Status.TOO_MANY_REQUESTS),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed", Response.Status.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(
        "INTERNAL_SERVER_ERROR",
        "Internal server error",
        Response.Status.INTERNAL_SERVER_ERROR
    ),
    HTTP_ERROR("HTTP_ERROR", "HTTP error", Response.Status.BAD_REQUEST),
    INVALID_SORT("INVALID_SORT", "Invalid sort field or direction", Response.Status.BAD_REQUEST),
    INVALID_SEARCH("INVALID_SEARCH", "Invalid search field or value", Response.Status.BAD_REQUEST),
    INVALID_PAGE("INVALID_PAGE", "Invalid page number or size", Response.Status.BAD_REQUEST);

    private final String errorCode;
    private final String reason;
    private final Response.Status httpStatus;

    /** Status-derived code: name of the status, reason phrase as reason. */
    JFrameErrorCode(final Response.Status httpStatus) {
        this(httpStatus.name(), httpStatus.getReasonPhrase(), httpStatus);
    }

    JFrameErrorCode(final String errorCode, final String reason, final Response.Status httpStatus) {
        this.errorCode = errorCode;
        this.reason = reason;
        this.httpStatus = httpStatus;
    }

    /**
     * Returns the status-derived code for the status.
     *
     * @param status the HTTP status code
     * @return the matching code, or empty when none is status-derived
     */
    public static Optional<JFrameErrorCode> forStatus(final int status) {
        return Arrays.stream(values())
            .filter(code -> code.name().equals(code.httpStatus.name()) && code.httpStatus.getStatusCode() == status)
            .findFirst();
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
