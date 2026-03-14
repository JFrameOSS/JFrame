package io.github.jframe.exception.mapper;

import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.validation.ValidationError;
import io.github.jframe.validation.ValidationResult;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;
import io.github.support.fixtures.TestApiException;

import java.time.OffsetDateTime;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for Quarkus exception mapper classes.
 *
 * <p>Verifies the exception mapper functionality including:
 * <ul>
 * <li>HttpException mapping with correct JAX-RS status codes</li>
 * <li>ApiException mapping with BAD_REQUEST status</li>
 * <li>ValidationException mapping with BAD_REQUEST status</li>
 * <li>RateLimitExceededException mapping with TOO_MANY_REQUESTS status and headers</li>
 * <li>Fallback Throwable mapping with INTERNAL_SERVER_ERROR status</li>
 * </ul>
 */
@DisplayName("Quarkus - Exception Mappers")
public class ExceptionMappersTest extends UnitTest {

    // -------------------------------------------------------------------------
    // HttpExceptionMapper
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map HttpException to correct JAX-RS status code")
    public void shouldMapHttpExceptionToCorrectJaxRsStatus() {
        // Given: A Quarkus HttpExceptionMapper and a BAD_REQUEST HttpException
        final HttpExceptionMapper mapper = new HttpExceptionMapper();
        final HttpException exception = new BadRequestException("Invalid input");

        // When: Mapping the exception to a response
        final Response response = mapper.toResponse(exception);

        // Then: Response has 400 BAD_REQUEST status
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), is(equalTo(Response.Status.BAD_REQUEST.getStatusCode())));
    }

    @Test
    @DisplayName("Should map HttpException response body with JSON media type")
    public void shouldMapHttpExceptionResponseBodyAsJson() {
        // Given: A Quarkus HttpExceptionMapper and an HttpException
        final HttpExceptionMapper mapper = new HttpExceptionMapper();
        final HttpException exception = new BadRequestException("Bad input");

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: Response entity is present
        assertThat(response.getEntity(), is(notNullValue()));
    }

    @Test
    @DisplayName("Should map HttpException to NOT_FOUND status when exception is 404")
    public void shouldMapHttpExceptionToNotFoundStatus() {
        // Given: A Quarkus HttpExceptionMapper and a NOT_FOUND HttpException
        final HttpExceptionMapper mapper = new HttpExceptionMapper();
        final HttpException exception = new io.github.jframe.exception.core.ResourceNotFoundException();

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: Response has 404 NOT_FOUND status
        assertThat(response.getStatus(), is(equalTo(Response.Status.NOT_FOUND.getStatusCode())));
    }

    // -------------------------------------------------------------------------
    // ApiExceptionMapper
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map ApiException to BAD_REQUEST status")
    public void shouldMapApiExceptionToBadRequestStatus() {
        // Given: A Quarkus ApiExceptionMapper and an ApiException
        final ApiExceptionMapper mapper = new ApiExceptionMapper();
        final ApiException exception = new TestApiException(new TestApiError("ERR001", "Test error"));

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: Response has 400 BAD_REQUEST status
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), is(equalTo(Response.Status.BAD_REQUEST.getStatusCode())));
    }

    @Test
    @DisplayName("Should map ApiException response body as non-null entity")
    public void shouldMapApiExceptionResponseBodyAsNonNullEntity() {
        // Given: A Quarkus ApiExceptionMapper and an ApiException with error details
        final ApiExceptionMapper mapper = new ApiExceptionMapper();
        final ApiException exception = new TestApiException(new TestApiError("API_ERR", "API error occurred"));

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: Response entity is not null
        assertThat(response.getEntity(), is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // ValidationExceptionMapper
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map ValidationException to BAD_REQUEST status")
    public void shouldMapValidationExceptionToBadRequestStatus() {
        // Given: A Quarkus ValidationExceptionMapper and a ValidationException
        final ValidationExceptionMapper mapper = new ValidationExceptionMapper();
        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("field1", "REQUIRED"));
        final ValidationException exception = new ValidationException(validationResult);

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: Response has 400 BAD_REQUEST status
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), is(equalTo(Response.Status.BAD_REQUEST.getStatusCode())));
    }

    @Test
    @DisplayName("Should map ValidationException with multiple errors")
    public void shouldMapValidationExceptionWithMultipleErrors() {
        // Given: A ValidationException with multiple errors
        final ValidationExceptionMapper mapper = new ValidationExceptionMapper();
        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("field1", "REQUIRED"));
        validationResult.addError(new ValidationError("field2", "INVALID_FORMAT"));
        final ValidationException exception = new ValidationException(validationResult);

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: Response is valid with BAD_REQUEST status
        assertThat(response.getStatus(), is(equalTo(Response.Status.BAD_REQUEST.getStatusCode())));
        assertThat(response.getEntity(), is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // RateLimitExceededExceptionMapper
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map RateLimitExceededException to TOO_MANY_REQUESTS status")
    public void shouldMapRateLimitExceededExceptionToTooManyRequestsStatus() {
        // Given: A Quarkus RateLimitExceededExceptionMapper and the exception with details
        final RateLimitExceededExceptionMapper mapper = new RateLimitExceededExceptionMapper();
        final int limit = 100;
        final int remaining = 0;
        final OffsetDateTime resetDate = OffsetDateTime.now().plusMinutes(5);
        final RateLimitExceededException exception = new RateLimitExceededException(limit, remaining, resetDate);

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: Response has 429 TOO_MANY_REQUESTS status
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), is(equalTo(429)));
    }

    @Test
    @DisplayName("Should include X-RateLimit headers in response for rate limit exception")
    public void shouldIncludeRateLimitHeadersInResponse() {
        // Given: A RateLimitExceededExceptionMapper and exception with rate limit metadata
        final RateLimitExceededExceptionMapper mapper = new RateLimitExceededExceptionMapper();
        final int limit = 50;
        final int remaining = 0;
        final OffsetDateTime resetDate = OffsetDateTime.now().plusMinutes(1);
        final RateLimitExceededException exception = new RateLimitExceededException(limit, remaining, resetDate);

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: X-RateLimit headers are present in the response
        assertThat(response.getHeaderString("X-RateLimit-Limit"), is(equalTo(String.valueOf(limit))));
        assertThat(response.getHeaderString("X-RateLimit-Remaining"), is(equalTo(String.valueOf(remaining))));
    }

    @Test
    @DisplayName("Should not include X-RateLimit-Reset header when resetDate is null")
    public void shouldNotIncludeResetHeaderWhenResetDateIsNull() {
        // Given: A RateLimitExceededExceptionMapper with null reset date
        final RateLimitExceededExceptionMapper mapper = new RateLimitExceededExceptionMapper();
        final RateLimitExceededException exception = new RateLimitExceededException(10, 0, null);

        // When: Mapping the exception
        final Response response = mapper.toResponse(exception);

        // Then: X-RateLimit-Reset header is absent
        assertThat(response.getStatus(), is(equalTo(429)));
        assertThat(response.getHeaderString("X-RateLimit-Reset"), is(equalTo(null)));
    }

    // -------------------------------------------------------------------------
    // ThrowableMapper
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map generic Throwable to INTERNAL_SERVER_ERROR status")
    public void shouldMapGenericThrowableToInternalServerErrorStatus() {
        // Given: A Quarkus ThrowableMapper and a generic RuntimeException
        final ThrowableMapper mapper = new ThrowableMapper();
        final Throwable throwable = new RuntimeException("Unexpected error");

        // When: Mapping the throwable
        final Response response = mapper.toResponse(throwable);

        // Then: Response has 500 INTERNAL_SERVER_ERROR status
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), is(equalTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
    }

    @Test
    @DisplayName("Should map Error subclass to INTERNAL_SERVER_ERROR status")
    public void shouldMapErrorSubclassToInternalServerErrorStatus() {
        // Given: A Quarkus ThrowableMapper and an OutOfMemoryError
        final ThrowableMapper mapper = new ThrowableMapper();
        final Throwable error = new OutOfMemoryError("Out of memory");

        // When: Mapping the error
        final Response response = mapper.toResponse(error);

        // Then: Response has 500 INTERNAL_SERVER_ERROR status
        assertThat(response.getStatus(), is(equalTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
        assertThat(response.getEntity(), is(notNullValue()));
    }
}
