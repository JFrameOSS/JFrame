/**
 * Comprehensive exception handling framework with hierarchical exception types and unified error responses.
 *
 * <h2>Overview</h2>
 * This package provides a complete exception handling solution for Spring Boot applications, offering:
 * <ul>
 * <li>Hierarchical exception types for different error scenarios</li>
 * <li>Automatic HTTP status code mapping</li>
 * <li>Structured error responses with enrichment capabilities</li>
 * <li>Validation error handling with field-level details</li>
 * <li>Custom API error codes for business logic errors</li>
 * <li>Integration with Spring's {@link org.springframework.web.bind.annotation.RestControllerAdvice}</li>
 * </ul>
 *
 * <h2>Exception Hierarchy</h2>
 * The framework defines a three-tier exception hierarchy:
 * <pre>
 * JFrameException (base)
 * ├── HttpException (HTTP status-aware)
 * │ └── [Specific HTTP exceptions in .core package]
 * ├── ApiException (custom error codes)
 * └── ValidationException (field-level validation errors)
 * </pre>
 *
 * <h3>Base Exception: {@link io.github.jframe.exception.JFrameException}</h3>
 * <ul>
 * <li>Root of all JFrame exceptions</li>
 * <li>Extends {@link java.lang.RuntimeException} (unchecked)</li>
 * <li>Provides standard constructors for message and cause</li>
 * </ul>
 *
 * <h3>HTTP Exceptions: {@link io.github.jframe.exception.HttpException}</h3>
 * <ul>
 * <li>Associates exceptions with HTTP status codes</li>
 * <li>Carries an {@link org.springframework.http.HttpStatus} field</li>
 * <li>Used for standard HTTP error responses (400, 401, 404, 500, etc.)</li>
 * <li>Concrete implementations in {@link io.github.jframe.exception.core} package</li>
 * </ul>
 *
 * <h3>API Exceptions: {@link io.github.jframe.exception.ApiException}</h3>
 * <ul>
 * <li>Carries custom {@link io.github.jframe.exception.ApiError} with error code and reason</li>
 * <li>Used for business logic errors requiring specific error codes</li>
 * <li>Applications typically implement {@code ApiError} as an enum</li>
 * <li>Example: {@code PAYMENT_FAILED}, {@code INSUFFICIENT_FUNDS}, {@code DUPLICATE_ORDER}</li>
 * </ul>
 *
 * <h2>Exception Handling Flow</h2>
 * <ol>
 * <li><strong>Exception Thrown</strong> - Application code throws a JFrame exception</li>
 * <li><strong>Handler Intercepts</strong> - {@link io.github.jframe.exception.handler.JFrameResponseEntityExceptionHandler}
 * catches the exception via {@code @ExceptionHandler}</li>
 * <li><strong>Response Created</strong> - Factory creates appropriate
 * {@link io.github.jframe.exception.resource.ErrorResponseResource}</li>
 * <li><strong>Enrichers Apply</strong> - Chain of {@link io.github.jframe.exception.handler.enricher.ErrorResponseEnricher}
 * beans populate the response with contextual data</li>
 * <li><strong>Response Returned</strong> - JSON error response sent to client with appropriate HTTP status</li>
 * </ol>
 *
 * <h2>Error Response Structure</h2>
 * All exceptions produce JSON responses with a consistent structure:
 * <pre>
 * {
 * "statusCode": 400,
 * "statusMessage": "Bad Request",
 * "errorMessage": "Validation failed",
 * "method": "POST",
 * "uri": "/api/users",
 * "query": null,
 * "contentType": "application/json"
 * }
 * </pre>
 *
 * Extended responses for specific exception types:
 * <ul>
 * <li><strong>Validation Errors</strong> - Includes {@code errors[]} array with field names and messages</li>
 * <li><strong>API Errors</strong> - Includes {@code errorCode} and {@code reason} fields</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Throwing HTTP Exceptions</h3>
 * <pre>
 * // Using specific HTTP exception types from .core package
 * if (user == null) {
 * throw new ResourceNotFoundException("User not found with id: " + userId);
 * }
 *
 * if (!isAuthorized) {
 * throw new UnauthorizedRequestException("Invalid credentials");
 * }
 *
 * if (invalidInput) {
 * throw new BadRequestException("Invalid request format");
 * }
 * </pre>
 *
 * <h3>Throwing Validation Exceptions</h3>
 * <pre>
 * ValidationResult result = new ValidationResult();
 * result.rejectValue("email", "Email is required");
 * result.rejectValue("age", "Must be 18 or older");
 *
 * if (result.hasErrors()) {
 * throw new ValidationException(result);
 * }
 * </pre>
 *
 * <h3>Throwing API Exceptions with Custom Error Codes</h3>
 * <pre>
 * // Define your API errors (typically as enum)
 * public enum OrderError implements ApiError {
 * INSUFFICIENT_INVENTORY("ORD-001", "Insufficient inventory"),
 * PAYMENT_FAILED("ORD-002", "Payment processing failed");
 *
 * private final String code;
 * private final String reason;
 *
 * // implement getErrorCode() and getReason()
 * }
 *
 * // Throw with custom error code
 * if (inventory &lt; quantity) {
 * throw new ApiException(OrderError.INSUFFICIENT_INVENTORY, "Only " + inventory + " items available");
 * }
 * </pre>
 *
 * <h2>Customization Points</h2>
 * <ul>
 * <li><strong>Custom Exceptions</strong> - Extend {@code HttpException}, {@code ApiException}, or {@code JFrameException}</li>
 * <li><strong>Custom Enrichers</strong> - Implement {@link io.github.jframe.exception.handler.enricher.ErrorResponseEnricher}
 * to add custom fields to error responses</li>
 * <li><strong>Custom Response Types</strong> - Extend {@link io.github.jframe.exception.resource.ErrorResponseResource}
 * for application-specific error response formats</li>
 * <li><strong>Custom Error Codes</strong> - Implement {@link io.github.jframe.exception.ApiError} as enum for
 * domain-specific error codes</li>
 * </ul>
 *
 * <h2>Integration with Spring Security</h2>
 * The exception handler also catches Spring Security exceptions:
 * <ul>
 * <li>{@link org.springframework.security.authentication.BadCredentialsException} → 401 Unauthorized</li>
 * <li>{@link org.springframework.security.access.AccessDeniedException} → 403 Forbidden</li>
 * </ul>
 *
 * <h2>OpenAPI/Swagger Integration</h2>
 * All exception handlers are annotated with {@code @ApiResponse} for automatic OpenAPI documentation
 * generation, ensuring error responses are documented in your API specification.
 *
 * @see io.github.jframe.exception.core Package containing concrete exception implementations
 * @see io.github.jframe.exception.handler Package containing exception handler and enrichers
 * @see io.github.jframe.exception.resource Package containing error response DTOs
 * @see io.github.jframe.exception.factory Package containing response builder factories
 */
package io.github.jframe.exception;
