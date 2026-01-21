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
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StatusCodeResponseEnricher}.
 *
 * <p>Verifies the ErrorResponseStatusEnricher functionality including:
 * <ul>
 * <li>HTTP status code enrichment</li>
 * <li>HTTP status reason phrase enrichment</li>
 * <li>Support for different HTTP status codes</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - Error Response Status Enricher")
public class StatusCodeResponseEnricherTest extends UnitTest {

    private final StatusCodeResponseEnricher enricher = new StatusCodeResponseEnricher();

    @Test
    @DisplayName("Should enrich status code and message from HTTP status")
    public void shouldEnrichStatusCodeAndMessageFromHttpStatus() {
        // Given: An error response resource and HTTP status
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response with BAD_REQUEST status
        enricher.doEnrich(resource, throwable, request, HttpStatus.BAD_REQUEST);

        // Then: Status code and message are set from HTTP status
        assertThat(resource.getStatusCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
        assertThat(resource.getStatusMessage(), is(equalTo(HttpStatus.BAD_REQUEST.getReasonPhrase())));
    }

    @Test
    @DisplayName("Should enrich with different HTTP status codes")
    public void shouldEnrichWithDifferentHttpStatusCodes() {
        // Given: An error response resource
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching with NOT_FOUND status
        final ErrorResponseResource notFoundResource = new ErrorResponseResource();
        enricher.doEnrich(notFoundResource, throwable, request, HttpStatus.NOT_FOUND);

        // Then: Status is correctly set
        assertThat(notFoundResource.getStatusCode(), is(equalTo(HttpStatus.NOT_FOUND.value())));
        assertThat(notFoundResource.getStatusMessage(), is(equalTo(HttpStatus.NOT_FOUND.getReasonPhrase())));

        // When: Enriching with INTERNAL_SERVER_ERROR status
        final ErrorResponseResource serverErrorResource = new ErrorResponseResource();
        enricher.doEnrich(serverErrorResource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Status is correctly set
        assertThat(serverErrorResource.getStatusCode(), is(equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        assertThat(serverErrorResource.getStatusMessage(), is(equalTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())));

        // When: Enriching with UNAUTHORIZED status
        final ErrorResponseResource unauthorizedResource = new ErrorResponseResource();
        enricher.doEnrich(unauthorizedResource, throwable, request, HttpStatus.UNAUTHORIZED);

        // Then: Status is correctly set
        assertThat(unauthorizedResource.getStatusCode(), is(equalTo(HttpStatus.UNAUTHORIZED.value())));
        assertThat(unauthorizedResource.getStatusMessage(), is(equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase())));
    }
}
