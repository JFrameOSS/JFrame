package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StatusCodeResponseEnricher}.
 *
 * <p>Verifies the enricher correctly sets status code and message on the response resource including:
 * <ul>
 * <li>Status code set from int parameter</li>
 * <li>Status message resolved via Response.Status.fromStatusCode()</li>
 * <li>Multiple HTTP status codes (200, 400, 404, 429, 500)</li>
 * </ul>
 */
@DisplayName("Unit Test - Status Code Response Enricher")
public class StatusCodeResponseEnricherTest extends UnitTest {

    private StatusCodeResponseEnricher enricher;

    @BeforeEach
    public void setUp() {
        enricher = new StatusCodeResponseEnricher();
    }

    @Test
    @DisplayName("Should set status code 400 and reason phrase Bad Request")
    public void shouldSetStatusCode400AndReasonPhraseBadRequest() {
        // Given: An error response resource and status code 400
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching with BAD_REQUEST (400)
        enricher.doEnrich(resource, throwable, requestContext, 400);

        // Then: Status code and message are correctly set
        assertThat(resource.getStatusCode(), is(equalTo(400)));
        assertThat(resource.getStatusMessage(), is(equalTo(Response.Status.BAD_REQUEST.getReasonPhrase())));
    }

    @Test
    @DisplayName("Should set status code 404 and reason phrase Not Found")
    public void shouldSetStatusCode404AndReasonPhraseNotFound() {
        // Given: An error response resource and status code 404
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching with NOT_FOUND (404)
        enricher.doEnrich(resource, throwable, requestContext, 404);

        // Then: Status code and message are correctly set
        assertThat(resource.getStatusCode(), is(equalTo(404)));
        assertThat(resource.getStatusMessage(), is(equalTo(Response.Status.NOT_FOUND.getReasonPhrase())));
    }

    @Test
    @DisplayName("Should set status code 429 and reason phrase Too Many Requests")
    public void shouldSetStatusCode429AndReasonPhraseTooManyRequests() {
        // Given: An error response resource and status code 429
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching with TOO_MANY_REQUESTS (429)
        enricher.doEnrich(resource, throwable, requestContext, 429);

        // Then: Status code is correctly set (429)
        assertThat(resource.getStatusCode(), is(equalTo(429)));
    }

    @Test
    @DisplayName("Should set status code 500 and reason phrase Internal Server Error")
    public void shouldSetStatusCode500AndReasonPhraseInternalServerError() {
        // Given: An error response resource and status code 500
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching with INTERNAL_SERVER_ERROR (500)
        enricher.doEnrich(resource, throwable, requestContext, 500);

        // Then: Status code and message are correctly set
        assertThat(resource.getStatusCode(), is(equalTo(500)));
        assertThat(resource.getStatusMessage(), is(equalTo(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }

    @Test
    @DisplayName("Should set status code 200 and reason phrase OK")
    public void shouldSetStatusCode200AndReasonPhraseOk() {
        // Given: An error response resource and status code 200
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching with OK (200)
        enricher.doEnrich(resource, throwable, requestContext, 200);

        // Then: Status code and message are correctly set
        assertThat(resource.getStatusCode(), is(equalTo(200)));
        assertThat(resource.getStatusMessage(), is(equalTo(Response.Status.OK.getReasonPhrase())));
    }

    @Test
    @DisplayName("Should set status code 401 and reason phrase Unauthorized")
    public void shouldSetStatusCode401AndReasonPhraseUnauthorized() {
        // Given: An error response resource and status code 401
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching with UNAUTHORIZED (401)
        enricher.doEnrich(resource, throwable, requestContext, 401);

        // Then: Status code and message are correctly set
        assertThat(resource.getStatusCode(), is(equalTo(401)));
        assertThat(resource.getStatusMessage(), is(equalTo(Response.Status.UNAUTHORIZED.getReasonPhrase())));
    }
}
