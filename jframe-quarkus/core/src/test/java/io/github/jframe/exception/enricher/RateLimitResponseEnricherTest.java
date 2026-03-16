package io.github.jframe.exception.enricher;

import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.support.UnitTest;

import java.time.OffsetDateTime;
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
 * Tests for {@link RateLimitResponseEnricher}.
 *
 * <p>Verifies the enricher correctly populates rate limit fields including:
 * <ul>
 * <li>Sets limit, remaining, resetDate from RateLimitExceededException</li>
 * <li>Handles null resetDate</li>
 * <li>No enrichment when throwable is not RateLimitExceededException</li>
 * <li>No enrichment when resource is not RateLimitErrorResponseResource</li>
 * </ul>
 */
@DisplayName("Unit Test - Rate Limit Response Enricher")
public class RateLimitResponseEnricherTest extends UnitTest {

    private RateLimitResponseEnricher enricher;

    @BeforeEach
    public void setUp() {
        enricher = new RateLimitResponseEnricher();
    }

    @Test
    @DisplayName("Should enrich limit, remaining and resetDate from RateLimitExceededException")
    public void shouldEnrichLimitRemainingAndResetDateFromRateLimitExceededException() {
        // Given: A RateLimitErrorResponseResource and RateLimitExceededException with all details
        final RateLimitErrorResponseResource resource = new RateLimitErrorResponseResource();
        final int limit = 100;
        final int remaining = 0;
        final OffsetDateTime resetDate = OffsetDateTime.now().plusMinutes(5);
        final RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", limit, remaining, resetDate);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 429);

        // Then: Rate limit fields are correctly set
        assertThat(resource.getLimit(), is(equalTo(limit)));
        assertThat(resource.getRemaining(), is(equalTo(remaining)));
        assertThat(resource.getResetDate(), is(equalTo(resetDate)));
    }

    @Test
    @DisplayName("Should enrich with null resetDate when exception has no reset date")
    public void shouldEnrichWithNullResetDateWhenExceptionHasNoResetDate() {
        // Given: A RateLimitErrorResponseResource and RateLimitExceededException with null resetDate
        final RateLimitErrorResponseResource resource = new RateLimitErrorResponseResource();
        final int limit = 50;
        final int remaining = 10;
        final RateLimitExceededException exception = new RateLimitExceededException(limit, remaining, null);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 429);

        // Then: Limit and remaining are set, resetDate is null
        assertThat(resource.getLimit(), is(equalTo(limit)));
        assertThat(resource.getRemaining(), is(equalTo(remaining)));
        assertThat(resource.getResetDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich when throwable is not RateLimitExceededException")
    public void shouldNotEnrichWhenThrowableIsNotRateLimitExceededException() {
        // Given: A RateLimitErrorResponseResource but a generic RuntimeException
        final RateLimitErrorResponseResource resource = new RateLimitErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Generic error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: Rate limit fields remain at default values
        assertThat(resource.getLimit(), is(equalTo(0)));
        assertThat(resource.getRemaining(), is(equalTo(0)));
        assertThat(resource.getResetDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich when resource is not RateLimitErrorResponseResource")
    public void shouldNotEnrichWhenResourceIsNotRateLimitErrorResponseResource() {
        // Given: A base ErrorResponseResource and a RateLimitExceededException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RateLimitExceededException exception = new RateLimitExceededException(100, 0, OffsetDateTime.now().plusMinutes(5));
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response (type mismatch)
        enricher.doEnrich(resource, exception, requestContext, 429);

        // Then: Base resource is unchanged (no rate limit fields)
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should enrich with zero remaining when all requests consumed")
    public void shouldEnrichWithZeroRemainingWhenAllRequestsConsumed() {
        // Given: A RateLimitErrorResponseResource and exception with zero remaining
        final RateLimitErrorResponseResource resource = new RateLimitErrorResponseResource();
        final OffsetDateTime resetDate = OffsetDateTime.now().plusHours(1);
        final RateLimitExceededException exception = new RateLimitExceededException(200, 0, resetDate);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 429);

        // Then: Remaining is explicitly 0
        assertThat(resource.getRemaining(), is(equalTo(0)));
        assertThat(resource.getLimit(), is(equalTo(200)));
    }
}
