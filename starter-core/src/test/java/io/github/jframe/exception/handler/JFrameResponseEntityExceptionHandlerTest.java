package io.github.jframe.exception.handler;

import io.github.jframe.exception.ApiError;
import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.validation.ValidationError;
import io.github.jframe.validation.ValidationResult;
import io.github.support.TestApiError;
import io.github.support.TestApiException;
import io.github.support.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JFrameResponseEntityExceptionHandler}.
 *
 * <p>Verifies the exception handler functionality including:
 * <ul>
 * <li>HTTP exception handling with correct status codes</li>
 * <li>API exception handling</li>
 * <li>Validation exception handling (both Spring and custom)</li>
 * <li>Security exception handling (BadCredentials, AccessDenied)</li>
 * <li>404 Not Found exception handling</li>
 * <li>Fallback Throwable handling (500 Internal Server Error)</li>
 * <li>Integration with ErrorResponseEntityBuilder</li>
 * <li>Correct response entity structure</li>
 * </ul>
 */
@DisplayName("Exception Handler - Response Entity Exception Handler")
public class JFrameResponseEntityExceptionHandlerTest extends UnitTest {

    @Mock
    private ErrorResponseEntityBuilder errorResponseEntityBuilder;

    @Mock
    private WebRequest webRequest;

    private JFrameResponseEntityExceptionHandler exceptionHandler;
    private ErrorResponseResource mockErrorResponse;

    @BeforeEach
    @Override
    public void setUp() {
        exceptionHandler = new JFrameResponseEntityExceptionHandler(errorResponseEntityBuilder);
        mockErrorResponse = new ErrorResponseResource();
        mockErrorResponse.setErrorMessage("Test error");
    }

    @Test
    @DisplayName("Should handle HttpException with correct status code")
    public void shouldHandleHttpException() {
        // Given: An HttpException with BAD_REQUEST status
        final HttpException exception = new BadRequestException("Invalid input");
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.BAD_REQUEST), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the HttpException
        final ResponseEntity<Object> response = exceptionHandler.handleHttpException(exception, webRequest);

        // Then: Response has correct status and body
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(response.getBody(), is(sameInstance(mockErrorResponse)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(exception, HttpStatus.BAD_REQUEST, webRequest);
    }

    @Test
    @DisplayName("Should handle HttpException with custom status code")
    public void shouldHandleHttpExceptionWithCustomStatus() {
        // Given: An HttpException with NOT_FOUND status
        final HttpException exception = new HttpException(HttpStatus.NOT_FOUND);
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.NOT_FOUND), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the HttpException
        final ResponseEntity<Object> response = exceptionHandler.handleHttpException(exception, webRequest);

        // Then: Response has correct status from exception
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.NOT_FOUND)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(exception, HttpStatus.NOT_FOUND, webRequest);
    }

    @Test
    @DisplayName("Should handle ApiException with BAD_REQUEST status")
    public void shouldHandleApiException() {
        // Given: An ApiException with an API error
        final ApiError apiError = new TestApiError("ERR001", "Test API error");
        final ApiException exception = new TestApiException(apiError);
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.BAD_REQUEST), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the ApiException
        final ResponseEntity<Object> response = exceptionHandler.handleApiException(exception, webRequest);

        // Then: Response has BAD_REQUEST status and correct body
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(response.getBody(), is(sameInstance(mockErrorResponse)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(exception, HttpStatus.BAD_REQUEST, webRequest);
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with BAD_REQUEST status")
    public void shouldHandleMethodArgumentNotValidException() {
        // Given: A MethodArgumentNotValidException
        final BindingResult bindingResult = mock(BindingResult.class);
        final MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.BAD_REQUEST), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the MethodArgumentNotValidException
        final ResponseEntity<Object> response = exceptionHandler.handleValidationException(exception, webRequest);

        // Then: Response has BAD_REQUEST status and correct body
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(response.getBody(), is(sameInstance(mockErrorResponse)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(exception, HttpStatus.BAD_REQUEST, webRequest);
    }

    @Test
    @DisplayName("Should handle ValidationException with BAD_REQUEST status")
    public void shouldHandleValidationException() {
        // Given: A ValidationException with validation errors
        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("field1", "REQUIRED"));
        validationResult.addError(new ValidationError("field2", "INVALID"));
        final ValidationException exception = new ValidationException(validationResult);
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.BAD_REQUEST), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the ValidationException
        final ResponseEntity<Object> response = exceptionHandler.handleValidationException(exception, webRequest);

        // Then: Response has BAD_REQUEST status and correct body
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.BAD_REQUEST)));
        assertThat(response.getBody(), is(sameInstance(mockErrorResponse)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(exception, HttpStatus.BAD_REQUEST, webRequest);
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with UNAUTHORIZED status")
    public void shouldHandleBadCredentialsException() {
        // Given: A BadCredentialsException
        final BadCredentialsException exception = new BadCredentialsException("Invalid credentials");
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.UNAUTHORIZED), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the BadCredentialsException
        final ResponseEntity<?> response = exceptionHandler.handleBadCredentialsException(exception, webRequest);

        // Then: Response has UNAUTHORIZED status and correct body
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNAUTHORIZED)));
        assertThat(response.getBody(), is(sameInstance(mockErrorResponse)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(exception, HttpStatus.UNAUTHORIZED, webRequest);
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with FORBIDDEN status")
    public void shouldHandleAccessDeniedException() {
        // Given: An AccessDeniedException
        final AccessDeniedException exception = new AccessDeniedException("Access denied");
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.FORBIDDEN), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the AccessDeniedException
        final ResponseEntity<?> response = exceptionHandler.handleAccessDeniedException(exception, webRequest);

        // Then: Response has FORBIDDEN status and correct body
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.FORBIDDEN)));
        assertThat(response.getBody(), is(sameInstance(mockErrorResponse)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(exception, HttpStatus.FORBIDDEN, webRequest);
    }

    @Test
    @DisplayName("Should handle generic Throwable with INTERNAL_SERVER_ERROR status")
    public void shouldHandleGenericThrowable() {
        // Given: A generic Throwable
        final Throwable throwable = new RuntimeException("Unexpected error");
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.INTERNAL_SERVER_ERROR), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the generic Throwable
        final ResponseEntity<Object> response = exceptionHandler.handleThrowable(throwable, webRequest);

        // Then: Response has INTERNAL_SERVER_ERROR status and correct body
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.INTERNAL_SERVER_ERROR)));
        assertThat(response.getBody(), is(sameInstance(mockErrorResponse)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(throwable, HttpStatus.INTERNAL_SERVER_ERROR, webRequest);
    }

    @Test
    @DisplayName("Should handle Error subclass with INTERNAL_SERVER_ERROR status")
    public void shouldHandleErrorSubclass() {
        // Given: An Error subclass
        final Throwable error = new OutOfMemoryError("Out of memory");
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), eq(HttpStatus.INTERNAL_SERVER_ERROR), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling the Error
        final ResponseEntity<Object> response = exceptionHandler.handleThrowable(error, webRequest);

        // Then: Response has INTERNAL_SERVER_ERROR status
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.INTERNAL_SERVER_ERROR)));
        verify(errorResponseEntityBuilder).buildErrorResponseBody(error, HttpStatus.INTERNAL_SERVER_ERROR, webRequest);
    }

    @Test
    @DisplayName("Should return empty headers in all responses")
    public void shouldReturnEmptyHeaders() {
        // Given: Various exceptions
        final HttpException httpException = new BadRequestException();
        final ApiException apiException = new TestApiException(new TestApiError("ERR", "Error"));
        when(errorResponseEntityBuilder.buildErrorResponseBody(any(), any(), eq(webRequest)))
            .thenReturn(mockErrorResponse);

        // When: Handling various exceptions
        final ResponseEntity<Object> httpResponse = exceptionHandler.handleHttpException(httpException, webRequest);
        final ResponseEntity<Object> apiResponse = exceptionHandler.handleApiException(apiException, webRequest);

        // Then: All responses have empty headers
        assertThat(httpResponse.getHeaders().isEmpty(), is(true));
        assertThat(apiResponse.getHeaders().isEmpty(), is(true));
    }
}
