package io.github.jframe.exception.handler;

import io.github.jframe.exception.ApiError;
import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;
import lombok.experimental.UtilityClass;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.Serial;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Writes Problem Details error responses from servlet filters, e.g. security entry points.
 *
 * <p>Uses the application's JSON mapper and jFrame enrichers when available in the request's servlet context.
 */
@UtilityClass
public class ErrorResponseWriter {

    private static final ObjectMapper FALLBACK_MAPPER = JsonMapper.builder().build();

    /**
     * Writes a Problem Details error response.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response to write to
     * @param status      the HTTP status
     * @param errorCode   the application error code (nullable)
     * @param errorReason the error reason (nullable)
     * @throws IOException if writing to the response fails
     */
    public static void write(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Response.Status status,
        final String errorCode,
        final String errorReason) throws IOException {
        write(request, response, new SimpleApiError(errorCode, errorReason, status));
    }

    /**
     * Writes a Problem Details error response from an {@link ApiError}.
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
        final HttpStatus status = HttpStatus.valueOf(apiError.getHttpStatus().getStatusCode());
        final WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        final ErrorResponseEntityBuilder builder =
            context == null ? null : context.getBeanProvider(ErrorResponseEntityBuilder.class).getIfAvailable();
        final ObjectMapper mapper =
            context == null ? FALLBACK_MAPPER : context.getBeanProvider(ObjectMapper.class).getIfAvailable(() -> FALLBACK_MAPPER);

        final ErrorResponseResource resource;
        if (builder == null) {
            resource = new ErrorResponseResource();
            resource.setError(apiError);
            ProblemDetails.apply(resource, status.value(), request.getRequestURI(), ProblemDetails.DEFAULT_TYPE_BASE_URI);
        } else {
            resource = builder.buildErrorResponseBody(new HttpException(apiError), status, new ServletWebRequest(request, response));
        }

        response.setStatus(status.value());
        response.setContentType(ProblemDetails.MEDIA_TYPE);
        mapper.writeValue(response.getOutputStream(), resource);
    }

    /** {@link ApiError} built from loose values. */
    private static final class SimpleApiError implements ApiError {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String errorCode;
        private final String reason;
        private final Response.Status httpStatus;

        private SimpleApiError(final String errorCode, final String reason, final Response.Status httpStatus) {
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
}
