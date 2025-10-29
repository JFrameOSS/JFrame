package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.TestApiError;
import io.github.support.TestApiException;
import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApiErrorResponseEnricher}.
 *
 * <p>Verifies the ApiErrorResponseEnricher functionality including:
 * <ul>
 * <li>API error code and reason enrichment for ApiException</li>
 * <li>Conditional enrichment (only for ApiException and ApiErrorResponseResource)</li>
 * <li>Error message handling (custom message or default)</li>
 * <li>No enrichment for non-ApiException throwables</li>
 * <li>No enrichment for non-ApiErrorResponseResource resources</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - API Error Response Enricher")
public class ApiErrorResponseEnricherTest extends UnitTest {

    private final ApiErrorResponseEnricher enricher = new ApiErrorResponseEnricher();

    @Test
    @DisplayName("Should enrich API error code and reason when throwable is ApiException")
    public void shouldEnrichApiErrorCodeAndReasonWhenThrowableIsApiException() {
        // Given: An API error response resource and an ApiException
        final ApiErrorResponseResource resource = new ApiErrorResponseResource();
        final TestApiError apiError = new TestApiError("INVALID_INPUT", "The input is invalid");
        final TestApiException apiException = new TestApiException(apiError, "Validation failed");
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, apiException, request, HttpStatus.BAD_REQUEST);

        // Then: API error code and reason are set
        assertThat(resource.getApiErrorCode(), is(equalTo("INVALID_INPUT")));
        assertThat(resource.getApiErrorReason(), is(equalTo("The input is invalid")));
        assertThat(resource.getErrorMessage(), is(equalTo("Validation failed")));
    }

    @Test
    @DisplayName("Should use default error message when ApiException message is null")
    public void shouldUseDefaultErrorMessageWhenApiExceptionMessageIsNull() {
        // Given: An API error response resource and an ApiException without message
        final ApiErrorResponseResource resource = new ApiErrorResponseResource();
        final TestApiError apiError = new TestApiError("ERROR_CODE", "Error reason");
        final TestApiException apiException = new TestApiException(apiError);
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, apiException, request, HttpStatus.BAD_REQUEST);

        // Then: Default error message is set
        assertThat(resource.getErrorMessage(), is(equalTo("No error message available")));
    }

    @Test
    @DisplayName("Should not enrich when throwable is not ApiException")
    public void shouldNotEnrichWhenThrowableIsNotApiException() {
        // Given: An API error response resource and a regular exception
        final ApiErrorResponseResource resource = new ApiErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Regular error");
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: API error fields remain null
        assertThat(resource.getApiErrorCode(), is(nullValue()));
        assertThat(resource.getApiErrorReason(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich when resource is not ApiErrorResponseResource")
    public void shouldNotEnrichWhenResourceIsNotApiErrorResponseResource() {
        // Given: A regular error response resource and an ApiException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final TestApiError apiError = new TestApiError("ERROR_CODE", "Error reason");
        final TestApiException apiException = new TestApiException(apiError, "Error message");
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, apiException, request, HttpStatus.BAD_REQUEST);

        // Then: Resource is not enriched (no API error fields to check)
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should handle both conditions - wrong throwable and wrong resource")
    public void shouldHandleBothConditionsWrongThrowableAndWrongResource() {
        // Given: A regular error response resource and a regular exception
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Regular error");
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Resource is not enriched
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }
}
