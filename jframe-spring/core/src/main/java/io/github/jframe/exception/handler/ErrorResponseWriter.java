package io.github.jframe.exception.handler;

import io.github.jframe.exception.ApiError;
import io.github.jframe.exception.resource.ErrorResponseResource;
import lombok.experimental.UtilityClass;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Static utility for writing JSON error responses outside the exception handler pipeline.
 *
 * <p>Useful in security filters, servlet filters, and other contexts where
 * {@link JFrameResponseEntityExceptionHandler} is not available.
 */
@UtilityClass
public class ErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Writes a JSON error response to the given {@link HttpServletResponse}.
     *
     * @param request   the HTTP request
     * @param response  the HTTP response to write to
     * @param status    the HTTP status
     * @param errorCode the application error code (nullable)
     * @param message   the error message (nullable)
     * @throws IOException if writing to the response fails
     */
    public static void write(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Response.Status status,
        final String errorCode,
        final String message) throws IOException {

        final ErrorResponseResource resource = new ErrorResponseResource(null);
        resource.setStatusCode(status.getStatusCode());
        resource.setStatusMessage(status.getReasonPhrase());
        resource.setErrorMessage(message);
        resource.setApiErrorCode(errorCode);
        resource.setApiErrorReason(message);
        resource.setUri(request.getRequestURI());
        resource.setMethod(request.getMethod());
        resource.setQuery(request.getQueryString());
        resource.setContentType(request.getContentType());

        response.setStatus(status.getStatusCode());
        response.setContentType(APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(response.getOutputStream(), resource);
    }

    /**
     * Writes a JSON error response from an {@link ApiError}.
     *
     * @param request  the HTTP request
     * @param response the HTTP response to write to
     * @param apiError the API error containing status, code, and reason
     * @throws IOException if writing to the response fails
     */
    public static void write(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final ApiError apiError) throws IOException {

        write(request, response, apiError.getHttpStatus(), apiError.getErrorCode(), apiError.getReason());
    }
}
