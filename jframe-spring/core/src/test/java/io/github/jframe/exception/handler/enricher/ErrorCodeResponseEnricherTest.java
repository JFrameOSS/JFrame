package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ErrorCodeResponseEnricher}.
 *
 * <p>Verifies the ErrorCodeResponseEnricher functionality including:
 * <ul>
 * <li>errorCode and errorReason extracted from HttpException's ApiError</li>
 * <li>cause field populated only from HttpException with a Throwable cause</li>
 * <li>ValidationException and MethodArgumentNotValidException mapped to JFRAME_VALIDATION_ERROR</li>
 * <li>Unhandled Throwables mapped to JFRAME_INTERNAL_ERROR with no cause exposed</li>
 * <li>Fallback to JFRAME_HTTP_ERROR when HttpException carries a null errorCode</li>
 * <li>Non-JFrame exceptions never expose cause (leak prevention)</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - Error Code Response Enricher")
public class ErrorCodeResponseEnricherTest extends UnitTest {

    private ErrorCodeResponseEnricher enricher;

    @BeforeEach
    @Override
    public void setUp() {
        enricher = new ErrorCodeResponseEnricher();
    }

    @Test
    @DisplayName("Should set errorCode and errorReason from HttpException's ApiError")
    public void shouldSetErrorCodeAndReasonFromHttpException() {
        // Given: An HttpException backed by a BAD_REQUEST ApiError
        final TestApiError apiError = new TestApiError("JFRAME_BAD_REQUEST", "Bad request", Response.Status.BAD_REQUEST);
        final HttpException exception = new HttpException(apiError);
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.BAD_REQUEST);

        // Then: errorCode and errorReason are set from the ApiError
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_BAD_REQUEST")));
        assertThat(resource.getErrorReason(), is(equalTo("Bad request")));
    }

    @Test
    @DisplayName("Should set cause message when HttpException was constructed with a Throwable cause")
    public void shouldSetCauseMessageFromHttpExceptionWithCause() {
        // Given: An HttpException constructed with an ApiError and a root cause
        final TestApiError apiError = new TestApiError("JFRAME_BAD_REQUEST", "Bad request", Response.Status.BAD_REQUEST);
        final RuntimeException rootCause = new RuntimeException("root cause message");
        final HttpException exception = new HttpException(apiError, rootCause);
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.BAD_REQUEST);

        // Then: cause is set to the cause's message
        assertThat(resource.getCause(), is(equalTo("root cause message")));
    }

    @Test
    @DisplayName("Should not set cause when HttpException has no Throwable cause")
    public void shouldNotSetCauseWhenHttpExceptionHasNoCause() {
        // Given: An HttpException without a root cause
        final TestApiError apiError = new TestApiError("JFRAME_NOT_FOUND", "Resource not found", Response.Status.NOT_FOUND);
        final HttpException exception = new HttpException(apiError);
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.NOT_FOUND);

        // Then: cause remains null (no root cause to expose)
        assertThat(resource.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should set JFRAME_VALIDATION_ERROR for ValidationException")
    public void shouldSetValidationErrorCodeForValidationException() {
        // Given: A ValidationException
        final ValidationException exception = new ValidationException();
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.BAD_REQUEST);

        // Then: errorCode is JFRAME_VALIDATION_ERROR and reason is "Validation failed"
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_VALIDATION_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("Validation failed")));
    }

    @Test
    @DisplayName("Should set JFRAME_VALIDATION_ERROR for MethodArgumentNotValidException")
    public void shouldSetValidationErrorCodeForMethodArgumentNotValidException() {
        // Given: A MethodArgumentNotValidException
        final BindingResult bindingResult = mock(BindingResult.class);
        final MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.BAD_REQUEST);

        // Then: errorCode is JFRAME_VALIDATION_ERROR and reason is "Validation failed" (same as ValidationException)
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_VALIDATION_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("Validation failed")));
    }

    @Test
    @DisplayName("Should set JFRAME_INTERNAL_ERROR for unhandled Throwable")
    public void shouldSetInternalErrorCodeForUnhandledThrowable() {
        // Given: An unhandled RuntimeException (not an HttpException or ValidationException)
        final RuntimeException exception = new RuntimeException("Unexpected failure");
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: errorCode is JFRAME_INTERNAL_ERROR with "Internal server error" reason
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_INTERNAL_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("Internal server error")));
    }

    @Test
    @DisplayName("Should fallback to JFRAME_HTTP_ERROR when HttpException errorCode is null")
    public void shouldFallbackToHttpErrorWhenErrorCodeIsNull() {
        // Given: An HttpException whose ApiError returns a null errorCode (edge case — shouldn't happen after refactor)
        final TestApiError nullCodeApiError = new TestApiError(null, null, Response.Status.BAD_REQUEST);
        final HttpException exception = new HttpException(nullCodeApiError);
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.BAD_REQUEST);

        // Then: Enricher falls back to JFRAME_HTTP_ERROR
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_HTTP_ERROR")));
        assertThat(resource.getErrorReason(), is(equalTo("HTTP error")));
    }

    @Test
    @DisplayName("Should not set cause for non-JFrame exceptions to prevent internal message leakage")
    public void shouldNotSetCauseForNonJFrameExceptions() {
        // Given: A RuntimeException with a message (non-JFrame exception — message must NOT be exposed)
        final RuntimeException exception = new RuntimeException("sensitive internal detail");
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: cause is null — internal exception messages are never leaked to callers
        assertThat(resource.getCause(), is(nullValue()));
    }

    @Test
    @DisplayName("Should set BadRequestException errorCode and errorReason from JFrameErrorCode.BAD_REQUEST")
    public void shouldSetErrorCodeAndReasonFromBadRequestException() {
        // Given: A BadRequestException (delegates to JFrameErrorCode.BAD_REQUEST via super(JFrameErrorCode.BAD_REQUEST))
        final BadRequestException exception = new BadRequestException();
        final ErrorResponseResource resource = new ErrorResponseResource(exception);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.BAD_REQUEST);

        // Then: errorCode and errorReason reflect JFrameErrorCode.BAD_REQUEST values
        assertThat(resource.getErrorCode(), is(equalTo("JFRAME_BAD_REQUEST")));
        assertThat(resource.getErrorReason(), is(equalTo("Bad request")));
        assertThat(resource.getCause(), is(nullValue()));
    }
}
