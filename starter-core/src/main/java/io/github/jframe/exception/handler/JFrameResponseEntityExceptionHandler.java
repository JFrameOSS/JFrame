package io.github.jframe.exception.handler;

import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.MethodArgumentNotValidResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * This class creates proper HTTP response bodies for exceptions.
 */
@Component
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@SuppressWarnings("PMD.ExcessiveImports")
public class JFrameResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

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
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(HttpException.class)
    @ApiResponse(
        responseCode = "400 (default)",
        description = "Default HTTP Exception",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleHttpException(final HttpException exception, final WebRequest request) {
        final HttpStatus status = exception.getHttpStatus();
        return ResponseEntity
            .status(status)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(exception, status, request));
    }

    /**
     * Handles {@code ApiException} instances.
     *
     * <p>The response status is: 400 Bad Request.
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(ApiException.class)
    @ApiResponse(
        responseCode = "400 (API)",
        description = "API Exception",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponseResource.class)
        )
    )
    public ResponseEntity<ApiErrorResponseResource> handleApiException(final ApiException exception, final WebRequest request) {
        return ResponseEntity
            .status(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(exception, BAD_REQUEST, request));
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
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    @ApiResponse(
        responseCode = "400 (Validation)",
        description = "Input Validation Exception",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ValidationErrorResponseResource.class)
        )
    )
    public ResponseEntity<ValidationErrorResponseResource> handleJframeValidation(
        final ValidationException exception, final WebRequest request) {
        return ResponseEntity
            .status(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(exception, BAD_REQUEST, request));
    }

    /**
     * Handles {@code BadCredentialsException} errors.
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ResponseStatus(UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException.class)
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleBadCredentials(final BadCredentialsException exception, final WebRequest request) {
        return ResponseEntity
            .status(UNAUTHORIZED)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(exception, UNAUTHORIZED, request));
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
    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    @ApiResponse(
        responseCode = "403",
        description = "Access Denied",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleAccessDenied(final AccessDeniedException exception, final WebRequest request) {
        return ResponseEntity
            .status(FORBIDDEN)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(exception, FORBIDDEN, request));
    }

    /**
     * Handles {@code Throwable} instances. This method acts as a fallback handler.
     *
     * @param throwable the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    @ApiResponse(
        responseCode = "500",
        description = "Uncaught Exceptions - Internal Server Error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<ErrorResponseResource> handleThrowable(final Throwable throwable, final WebRequest request) {
        return ResponseEntity
            .status(INTERNAL_SERVER_ERROR)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(throwable, INTERNAL_SERVER_ERROR, request));
    }

    // =========================== OVERRIDES ===========================

    /**
     * Handles {@code MethodArgumentNotValidException} instances.
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @Override
    @ApiResponse(
        responseCode = "400 (Validation)",
        description = "Input Validation Exception",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MethodArgumentNotValidResponseResource.class)
        )
    )
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull final MethodArgumentNotValidException exception,
        @NonNull final HttpHeaders headers,
        @NonNull final HttpStatusCode status,
        @NonNull final WebRequest request) {
        return ResponseEntity
            .status(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(exception, BAD_REQUEST, request));
    }

    /**
     * Handles {@code NoResourceFoundException} errors.
     *
     * @param exception the exception
     * @param request   the current request
     * @return a response entity reflecting the current exception
     */
    @Override
    @ApiResponse(
        responseCode = "404",
        description = "Resource Not Found",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponseResource.class)
        )
    )
    public ResponseEntity<Object> handleNoResourceFoundException(
        @NonNull final NoResourceFoundException exception,
        @NonNull final HttpHeaders headers,
        @NonNull final HttpStatusCode status,
        @NonNull final WebRequest request) {
        return ResponseEntity
            .status(NOT_FOUND)
            .contentType(APPLICATION_JSON)
            .body(buildErrorResponseBody(exception, NOT_FOUND, request));
    }

    // =========================== PRIVATE METHODS ===========================

    /**
     * Builds the error response body based on the provided throwable, status, and request. And adds the details to the OTLP Tracing.
     *
     * @param throwable the exception to build the error response for
     * @param status    the HTTP status to use in the response
     * @param request   the current web request
     * @return an {@link ErrorResponseResource} containing the error details
     */
    private <T extends ErrorResponseResource> T buildErrorResponseBody(
        final Throwable throwable,
        final HttpStatus status,
        final WebRequest request) {
        return errorResponseEntityBuilder.buildErrorResponseBody(throwable, status, request);
    }
}
