package io.github.jframe.exception.handler;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static io.github.jframe.util.constants.Constants.Headers.*;
import static org.springframework.http.HttpStatus.*;

/**
 * This class creates proper HTTP response bodies for exceptions.
 */
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings(
    {
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity"
    }
)
public class JFrameResponseEntityExceptionHandler extends ResponseEntityExceptionHandler implements JFrameControllerAdvice {

    private final ErrorResponseEntityBuilder errorResponseEntityBuilder;

    /**
     * Handles {@code HttpException} instances.
     *
     * <p>Each {@code HttpException} has an associated {@code HttpStatus} that is used as the response
     * status.
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ExceptionHandler(HttpException.class)
    @ApiResponse(
        responseCode = "400",
        description = "Bad Request",
        content = @Content(
            mediaType = ProblemDetails.MEDIA_TYPE,
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleHttpException(final HttpException exception, final WebRequest request) {
        final HttpStatus status = HttpStatus.valueOf(exception.getHttpStatus().getStatusCode());
        return ResponseEntity
            .status(status)
            .contentType(mediaType(request))
            .body(errorResponseEntityBuilder.buildErrorResponseBody(exception, status, request));
    }

    /**
     * Handles {@code RateLimitExceededException} instances.
     *
     * <p>The response status is: 429 Too Many Requests. The response includes rate limit headers:
     * <ul>
     * <li>{@code X-RateLimit-Limit} - Maximum requests allowed</li>
     * <li>{@code X-RateLimit-Remaining} - Requests remaining in current window</li>
     * <li>{@code X-RateLimit-Reset} - When the rate limit resets (ISO 8601 format)</li>
     * </ul>
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ExceptionHandler(RateLimitExceededException.class)
    @ApiResponse(
        responseCode = "429",
        description = "Rate Limit Exceeded",
        content = @Content(
            mediaType = ProblemDetails.MEDIA_TYPE,
            schema = @Schema(implementation = RateLimitErrorResponseResource.class)
        )
    )
    public ResponseEntity<RateLimitErrorResponseResource> handleRateLimitExceeded(final RateLimitExceededException exception,
        final WebRequest request) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(X_RATELIMIT_LIMIT, String.valueOf(exception.getLimit()));
        headers.add(X_RATELIMIT_REMAINING, String.valueOf(exception.getRemaining()));
        if (exception.getResetDate() != null) {
            headers.add(X_RATELIMIT_RESET, exception.getResetDate().toString());
        }

        return ResponseEntity
            .status(TOO_MANY_REQUESTS)
            .headers(headers)
            .contentType(mediaType(request))
            .body(errorResponseEntityBuilder.buildErrorResponseBody(exception, TOO_MANY_REQUESTS, request));
    }

    /**
     * Handles {@code ValidationException} instances.
     *
     * <p>The response status is: 400 Bad Request.
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponseResource> handleJframeValidation(
        final ValidationException exception, final WebRequest request) {
        return ResponseEntity
            .status(BAD_REQUEST)
            .contentType(mediaType(request))
            .body(errorResponseEntityBuilder.buildErrorResponseBody(exception, BAD_REQUEST, request));
    }

    /**
     * Handles {@code AuthenticationException} errors.
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ExceptionHandler(AuthenticationException.class)
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = @Content(
            mediaType = ProblemDetails.MEDIA_TYPE,
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleAuthentication(final AuthenticationException exception, final WebRequest request) {
        return ResponseEntity
            .status(UNAUTHORIZED)
            .contentType(mediaType(request))
            .body(errorResponseEntityBuilder.buildErrorResponseBody(exception, UNAUTHORIZED, request));
    }

    /**
     * Handles {@code AccessDeniedException} errors.
     * <p>
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ExceptionHandler(AccessDeniedException.class)
    @ApiResponse(
        responseCode = "403",
        description = "Access Denied",
        content = @Content(
            mediaType = ProblemDetails.MEDIA_TYPE,
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleAccessDenied(final AccessDeniedException exception, final WebRequest request) {
        return ResponseEntity
            .status(FORBIDDEN)
            .contentType(mediaType(request))
            .body(errorResponseEntityBuilder.buildErrorResponseBody(exception, FORBIDDEN, request));
    }

    /**
     * Handles {@code Throwable} instances. This method acts as a fallback handler.
     *
     * @param throwable the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ExceptionHandler(Throwable.class)
    @ApiResponse(
        responseCode = "500",
        description = "Uncaught Exceptions - Internal Server Error",
        content = @Content(
            mediaType = ProblemDetails.MEDIA_TYPE,
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleThrowable(final Throwable throwable, final WebRequest request) {
        log.error(throwable.getMessage(), throwable);
        return ResponseEntity
            .status(INTERNAL_SERVER_ERROR)
            .contentType(mediaType(request))
            .body(errorResponseEntityBuilder.buildErrorResponseBody(throwable, INTERNAL_SERVER_ERROR, request));
    }

    /**
     * Handles {@code NoResourceFoundException} errors.
     *
     * @param exception the exception
     * @param headers   the headers
     * @param status    the status
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @Override
    @ApiResponse(
        responseCode = "404",
        description = "Resource Not Found",
        content = @Content(
            mediaType = ProblemDetails.MEDIA_TYPE,
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public @Nullable ResponseEntity<Object> handleNoResourceFoundException(
        @NonNull final NoResourceFoundException exception,
        @NonNull final HttpHeaders headers,
        @NonNull final HttpStatusCode status,
        @NonNull final WebRequest request) {
        return handleExceptionInternal(exception, null, headers, NOT_FOUND, request);
    }

    /**
     * Renders every Spring MVC exception as jFrame Problem Details.
     *
     * @param exception the exception
     * @param body      the body prepared by Spring, ignored
     * @param headers   the headers
     * @param status    the status
     * @param request   the current request
     * @return a response entity, or {@code null} when the response is already committed
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
        @NonNull final Exception exception,
        @Nullable final Object body,
        @NonNull final HttpHeaders headers,
        @NonNull final HttpStatusCode status,
        @NonNull final WebRequest request) {
        if (request instanceof final ServletWebRequest servletWebRequest
            && servletWebRequest.getResponse() != null
            && servletWebRequest.getResponse().isCommitted()) {
            return null;
        }
        final HttpStatus resolved = resolve(status.value());
        final HttpStatus httpStatus = resolved == null ? INTERNAL_SERVER_ERROR : resolved;
        if (httpStatus.is5xxServerError()) {
            log.error(exception.getMessage(), exception);
        }
        return ResponseEntity
            .status(httpStatus)
            .headers(headers)
            .contentType(mediaType(request))
            .body(errorResponseEntityBuilder.buildErrorResponseBody(exception, httpStatus, request));
    }

    /** Problem Details or plain JSON, per the request's {@code Accept} header. */
    static MediaType mediaType(final WebRequest request) {
        final String[] accept = request.getHeaderValues(HttpHeaders.ACCEPT);
        return MediaType.parseMediaType(ProblemDetails.negotiateMediaType(accept == null ? null : String.join(",", accept)));
    }
}
