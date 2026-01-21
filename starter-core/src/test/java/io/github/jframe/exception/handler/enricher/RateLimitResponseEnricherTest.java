package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.support.UnitTest;

import java.time.OffsetDateTime;

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
 * Tests for {@link RateLimitResponseEnricher}.
 *
 * <p>Verifies the RateLimitResponseEnricher functionality including:
 * <ul>
 * <li>Rate limit information enrichment for RateLimitExceededException</li>
 * <li>Conditional enrichment (only for RateLimitExceededException and RateLimitErrorResponseResource)</li>
 * <li>Handling of null resetDate</li>
 * <li>No enrichment for non-RateLimitExceededException throwables</li>
 * <li>No enrichment for non-RateLimitErrorResponseResource resources</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - Rate Limit Response Enricher")
public class RateLimitResponseEnricherTest extends UnitTest {

    @Test
    @DisplayName("Should enrich rate limit details when throwable is RateLimitExceededException")
    public void shouldEnrichRateLimitDetailsWhenThrowableIsRateLimitExceededException() {
        // Given: A rate limit error response resource and RateLimitExceededException with details
        final RateLimitErrorResponseResource resource = new RateLimitErrorResponseResource();
        final int limit = 100;
        final int remaining = 0;
        final OffsetDateTime resetDate = OffsetDateTime.now().plusMinutes(5);
        final RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", limit, remaining, resetDate);
        final WebRequest request = mock(WebRequest.class);

        final RateLimitResponseEnricher enricher = new RateLimitResponseEnricher();

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.TOO_MANY_REQUESTS);

        // Then: Rate limit details are set on the resource
        assertThat(resource.getLimit(), is(equalTo(limit)));
        assertThat(resource.getRemaining(), is(equalTo(remaining)));
        assertThat(resource.getResetDate(), is(equalTo(resetDate)));
    }

    @Test
    @DisplayName("Should enrich rate limit details with null resetDate")
    public void shouldEnrichRateLimitDetailsWithNullResetDate() {
        // Given: A rate limit error response resource and RateLimitExceededException with null resetDate
        final RateLimitErrorResponseResource resource = new RateLimitErrorResponseResource();
        final int limit = 50;
        final int remaining = 10;
        final RateLimitExceededException exception = new RateLimitExceededException(limit, remaining, null);
        final WebRequest request = mock(WebRequest.class);

        final RateLimitResponseEnricher enricher = new RateLimitResponseEnricher();

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.TOO_MANY_REQUESTS);

        // Then: Rate limit details are set with null resetDate
        assertThat(resource.getLimit(), is(equalTo(limit)));
        assertThat(resource.getRemaining(), is(equalTo(remaining)));
        assertThat(resource.getResetDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich when throwable is not RateLimitExceededException")
    public void shouldNotEnrichWhenThrowableIsNotRateLimitExceededException() {
        // Given: A rate limit error response resource and a regular exception
        final RateLimitErrorResponseResource resource = new RateLimitErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Regular error");
        final WebRequest request = mock(WebRequest.class);

        final RateLimitResponseEnricher enricher = new RateLimitResponseEnricher();

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Rate limit details remain at default values
        assertThat(resource.getLimit(), is(equalTo(0)));
        assertThat(resource.getRemaining(), is(equalTo(0)));
        assertThat(resource.getResetDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich when resource is not RateLimitErrorResponseResource")
    public void shouldNotEnrichWhenResourceIsNotRateLimitErrorResponseResource() {
        // Given: A regular error response resource and a RateLimitExceededException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final int limit = 100;
        final int remaining = 0;
        final OffsetDateTime resetDate = OffsetDateTime.now().plusMinutes(5);
        final RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", limit, remaining, resetDate);
        final WebRequest request = mock(WebRequest.class);

        final RateLimitResponseEnricher enricher = new RateLimitResponseEnricher();

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.TOO_MANY_REQUESTS);

        // Then: No exception is thrown and resource is unchanged (no rate limit fields on ErrorResponseResource)
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should handle both conditions - wrong throwable and wrong resource")
    public void shouldHandleBothConditionsWrongThrowableAndWrongResource() {
        // Given: A regular error response resource and a regular exception
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Regular error");
        final WebRequest request = mock(WebRequest.class);

        final RateLimitResponseEnricher enricher = new RateLimitResponseEnricher();

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: No exception is thrown and resource is unchanged
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }
}
