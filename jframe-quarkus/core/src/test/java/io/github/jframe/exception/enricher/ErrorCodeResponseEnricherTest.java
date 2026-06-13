package io.github.jframe.exception.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.validation.ValidationResult;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;

import java.util.Collections;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ErrorCodeResponseEnricher}.
 *
 * <p>Verifies the enricher correctly sets {@code errorCode}, {@code errorReason}, and {@code cause}
 * on the response resource for all supported exception types:
 * <ul>
 * <li>{@link HttpException} — uses ApiError for errorCode/errorReason, cause from wrapped throwable</li>
 * <li>{@link ValidationException} — maps to JFRAME_VALIDATION_ERROR / "Validation failed"</li>
 * <li>{@link ConstraintViolationException} — maps to JFRAME_VALIDATION_ERROR / "Validation failed"</li>
 * <li>Unhandled {@link Throwable} — maps to JFRAME_INTERNAL_ERROR / "Internal server error", cause=null</li>
 * </ul>
 */
@DisplayName("Unit Test - Error Code Response Enricher")
public class ErrorCodeResponseEnricherTest extends UnitTest {

    private ErrorCodeResponseEnricher enricher;

    @BeforeEach
    public void setUp() {
        enricher = new ErrorCodeResponseEnricher();
    }

    @Test
    @DisplayName("Should set errorCode and errorReason from HttpException ApiError")
    public void shouldSetErrorCodeAndReasonFromHttpException() {
        // Given: An HttpException built from an ApiError with a specific code and reason
        final ErrorResponseResource resource = new ErrorResponseResource();
        final HttpException exception = new HttpException(
            new TestApiError("MY_ERROR_CODE", "My error reason", Response.Status.CONFLICT)
        );
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 409);

        // Then: errorCode and errorReason are taken directly from the ApiError
        assertThat(resource.getErrorCode(), is(equalTo("MY_ERROR_CODE")));
        assertThat(resource.getErrorReason(), is(equalTo("My error reason")));
    }

    @Test
    @DisplayName("Should set cause field to wrapped exception message when HttpException has a cause")
    public void shouldSetCauseMessageFromHttpExceptionWithCause() {
        // Given: An HttpException constructed with a wrapped cause
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RuntimeException rootCause = new RuntimeException("root cause message");
        final HttpException exception = new HttpException(
            new TestApiError("MY_ERROR_CODE", "My error reason", Response.Status.BAD_REQUEST),
            rootCause
        );
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: cause field is set to the wrapped exception's message
        assertThat(resource.getCause(), is(equalTo("root cause message")));
    }

    @Test
    @DisplayName("Should not set cause when HttpException has no wrapped cause")
    public void shouldNotSetCauseWhenHttpExceptionHasNoCause() {
        // Given: An HttpException constructed from an ApiError only (no cause)
        final ErrorResponseResource resource = new ErrorResponseResource();
        final HttpException exception = new HttpException(
            new TestApiError("MY_ERROR_CODE", "My error reason", Response.Status.BAD_REQUEST)
        );
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: cause field is null
        assertThat(resource.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should set JFRAME_VALIDATION_ERROR code and reason for ValidationException")
    public void shouldSetValidationErrorCodeForValidationException() {
        // Given: A JFrame ValidationException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final ValidationException exception = new ValidationException(new ValidationResult());
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: errorCode is JFRAME_VALIDATION_ERROR and errorReason is "Validation failed"
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_VALIDATION_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("Validation failed")));
    }

    @Test
    @DisplayName("Should set JFRAME_VALIDATION_ERROR code and reason for ConstraintViolationException")
    public void shouldSetValidationErrorCodeForConstraintViolationException() {
        // Given: A Jakarta ConstraintViolationException (Quarkus-specific validation failure)
        final ErrorResponseResource resource = new ErrorResponseResource();
        final ConstraintViolationException exception = new ConstraintViolationException(
            "Constraint violated",
            Collections.emptySet()
        );
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: errorCode is JFRAME_VALIDATION_ERROR and errorReason is "Validation failed"
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_VALIDATION_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("Validation failed")));
    }

    @Test
    @DisplayName("Should set JFRAME_INTERNAL_ERROR code and null cause for unhandled Throwable")
    public void shouldSetInternalErrorCodeForUnhandledThrowable() {
        // Given: An unhandled RuntimeException that is not a JFrame or validation exception
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RuntimeException exception = new RuntimeException("sensitive internal details");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: errorCode is JFRAME_INTERNAL_ERROR, errorReason is generic, cause is null
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_INTERNAL_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("Internal server error")));
        assertThat(resource.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should fallback to JFRAME_HTTP_ERROR when HttpException errorCode is null")
    public void shouldFallbackToHttpErrorWhenErrorCodeIsNull() {
        // Given: An HttpException from an ApiError that returns null for errorCode
        final ErrorResponseResource resource = new ErrorResponseResource();
        final HttpException exception = new HttpException(
            new TestApiError(null, null, Response.Status.BAD_REQUEST)
        );
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: errorCode falls back to JFRAME_HTTP_ERROR and errorReason to "HTTP error"
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_HTTP_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("HTTP error")));
    }

    @Test
    @DisplayName("Should not set cause field for non-JFrame exceptions")
    public void shouldNotSetCauseForNonJFrameExceptions() {
        // Given: A plain RuntimeException with a message (not an HttpException)
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RuntimeException exception = new RuntimeException("should not appear in cause");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: cause field is null — non-JFrame exceptions never have their message set as cause
        assertThat(resource.getCause(), is(nullValue()));
    }
}
