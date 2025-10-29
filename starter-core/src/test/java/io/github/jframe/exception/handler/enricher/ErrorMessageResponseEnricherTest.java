package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
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
 * Tests for {@link ErrorMessageResponseEnricher}.
 *
 * <p>Verifies the ErrorMessageResponseEnricher functionality including:
 * <ul>
 * <li>Error message enrichment from throwable message</li>
 * <li>Handling of null and empty throwable messages</li>
 * <li>Non-null throwable message extraction</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - Error Message Response Enricher")
public class ErrorMessageResponseEnricherTest extends UnitTest {

    private final ErrorMessageResponseEnricher enricher = new ErrorMessageResponseEnricher();

    @Test
    @DisplayName("Should enrich error message when throwable has message")
    public void shouldEnrichErrorMessageWhenThrowableHasMessage() {
        // Given: An error response resource and a throwable with message
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("Something went wrong");
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Error message is set from throwable
        assertThat(resource.getErrorMessage(), is(equalTo("Something went wrong")));
    }

    @Test
    @DisplayName("Should not enrich error message when throwable message is null")
    public void shouldNotEnrichErrorMessageWhenThrowableMessageIsNull() {
        // Given: An error response resource and a throwable without message
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Error message remains null
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich error message when throwable message is empty")
    public void shouldNotEnrichErrorMessageWhenThrowableMessageIsEmpty() {
        // Given: An error response resource and a throwable with empty message
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("");
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Error message remains null
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not enrich error message when throwable message is blank")
    public void shouldNotEnrichErrorMessageWhenThrowableMessageIsBlank() {
        // Given: An error response resource and a throwable with blank message
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("   ");
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Error message remains null
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }
}
