package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;
import io.github.support.fixtures.TestApiException;

import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApiErrorResponseEnricher}.
 *
 * <p>Verifies the enricher correctly sets API error fields when conditions are met including:
 * <ul>
 * <li>Sets apiErrorCode and apiErrorReason from ApiException when resource is ApiErrorResponseResource</li>
 * <li>No enrichment when throwable is not ApiException</li>
 * <li>No enrichment when resource is not ApiErrorResponseResource</li>
 * </ul>
 */
@DisplayName("Unit Test - Api Error Response Enricher")
public class ApiErrorResponseEnricherTest extends UnitTest {

    private ApiErrorResponseEnricher enricher;

    @BeforeEach
    public void setUp() {
        enricher = new ApiErrorResponseEnricher();
    }

    @Test
    @DisplayName("Should set apiErrorCode and apiErrorReason from ApiException")
    public void shouldSetApiErrorCodeAndApiErrorReasonFromApiException() {
        // Given: An ApiErrorResponseResource and a TestApiException with error details
        final ApiErrorResponseResource resource = new ApiErrorResponseResource();
        final TestApiError apiError = new TestApiError("INVALID_INPUT", "The input is invalid");
        final TestApiException apiException = new TestApiException(apiError, "Validation failed");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, apiException, requestContext, 400);

        // Then: API error code and reason are set on the resource
        assertThat(resource.getApiErrorCode(), is(equalTo("INVALID_INPUT")));
        assertThat(resource.getApiErrorReason(), is(equalTo("The input is invalid")));
    }

    @Test
    @DisplayName("Should not enrich when throwable is not ApiException")
    public void shouldNotEnrichWhenThrowableIsNotApiException() {
        // Given: An ApiErrorResponseResource but a RuntimeException (not ApiException)
        final ApiErrorResponseResource resource = new ApiErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Regular error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: API error fields remain null
        assertThat(resource.getApiErrorCode(), is(nullValue()));
        assertThat(resource.getApiErrorReason(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich when resource is not ApiErrorResponseResource")
    public void shouldNotEnrichWhenResourceIsNotApiErrorResponseResource() {
        // Given: A base ErrorResponseResource and an ApiException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final TestApiError apiError = new TestApiError("CODE", "Reason");
        final TestApiException apiException = new TestApiException(apiError, "Error message");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response (resource type mismatch)
        enricher.doEnrich(resource, apiException, requestContext, 400);

        // Then: Base resource is not enriched with API fields
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should handle ApiException with null errorCode")
    public void shouldHandleApiExceptionWithNullErrorCode() {
        // Given: An ApiErrorResponseResource and an ApiException with null ApiError content
        final ApiErrorResponseResource resource = new ApiErrorResponseResource();
        final TestApiError apiError = new TestApiError(null, null);
        final TestApiException apiException = new TestApiException(apiError, "Error message");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, apiException, requestContext, 400);

        // Then: Null error code and reason are set (not thrown)
        assertThat(resource.getApiErrorCode(), is(nullValue()));
        assertThat(resource.getApiErrorReason(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich when both resource and throwable types are wrong")
    public void shouldNotEnrichWhenBothResourceAndThrowableTypesAreWrong() {
        // Given: A base ErrorResponseResource and a generic RuntimeException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Generic error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: Resource remains unchanged
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }
}
