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
 * <p>Verifies the StatusCodeResponseEnricher functionality including:
 * <ul>
 * <li>HTTP status code enrichment</li>
 * <li>Support for different HTTP status codes</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - Error Response Status Enricher")
public class StatusCodeResponseEnricherTest extends UnitTest {

    private final StatusCodeResponseEnricher enricher = new StatusCodeResponseEnricher();

    @Test
    @DisplayName("Should enrich status code from HTTP status")
    public void shouldEnrichStatusCodeFromHttpStatus() {
        // Given: An error response resource and HTTP status
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response with BAD_REQUEST status
        enricher.doEnrich(resource, throwable, request, HttpStatus.BAD_REQUEST);

        // Then: Status code is set from HTTP status
        assertThat(resource.getStatusCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
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

        // Then: Status code is correctly set
        assertThat(notFoundResource.getStatusCode(), is(equalTo(HttpStatus.NOT_FOUND.value())));

        // When: Enriching with INTERNAL_SERVER_ERROR status
        final ErrorResponseResource serverErrorResource = new ErrorResponseResource();
        enricher.doEnrich(serverErrorResource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Status code is correctly set
        assertThat(serverErrorResource.getStatusCode(), is(equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        // When: Enriching with UNAUTHORIZED status
        final ErrorResponseResource unauthorizedResource = new ErrorResponseResource();
        enricher.doEnrich(unauthorizedResource, throwable, request, HttpStatus.UNAUTHORIZED);

        // Then: Status code is correctly set
        assertThat(unauthorizedResource.getStatusCode(), is(equalTo(HttpStatus.UNAUTHORIZED.value())));
    }
}
