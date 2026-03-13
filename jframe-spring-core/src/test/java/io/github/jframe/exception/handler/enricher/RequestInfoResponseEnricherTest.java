package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RequestInfoResponseEnricher}.
 *
 * <p>Verifies the RequestInfoErrorResponseEnricher functionality including:
 * <ul>
 * <li>Request URI enrichment</li>
 * <li>Query string enrichment</li>
 * <li>HTTP method enrichment</li>
 * <li>Content type enrichment</li>
 * <li>Conditional enrichment (only for ServletWebRequest)</li>
 * <li>Handling of null query string</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - Request Info Error Response Enricher")
public class RequestInfoResponseEnricherTest extends UnitTest {

    private final RequestInfoResponseEnricher enricher = new RequestInfoResponseEnricher();

    @Test
    @DisplayName("Should enrich request info when request is ServletWebRequest")
    public void shouldEnrichRequestInfoWhenRequestIsServletWebRequest() {
        // Given: An error response resource and a ServletWebRequest
        final ErrorResponseResource resource = new ErrorResponseResource();
        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("/api/users");
        when(httpServletRequest.getQueryString()).thenReturn("page=1&size=10");
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getContentType()).thenReturn("application/json");

        final ServletWebRequest servletWebRequest = mock(ServletWebRequest.class);
        when(servletWebRequest.getNativeRequest()).thenReturn(httpServletRequest);

        final Throwable throwable = new RuntimeException();

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, servletWebRequest, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Request info is set
        assertThat(resource.getUri(), is(equalTo("/api/users")));
        assertThat(resource.getQuery(), is(equalTo("page=1&size=10")));
        assertThat(resource.getMethod(), is(equalTo("GET")));
        assertThat(resource.getContentType(), is(equalTo("application/json")));
    }

    @Test
    @DisplayName("Should enrich request info with null query string")
    public void shouldEnrichRequestInfoWithNullQueryString() {
        // Given: An error response resource and a ServletWebRequest with no query string
        final ErrorResponseResource resource = new ErrorResponseResource();
        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("/api/users");
        when(httpServletRequest.getQueryString()).thenReturn(null);
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getContentType()).thenReturn("application/json");

        final ServletWebRequest servletWebRequest = mock(ServletWebRequest.class);
        when(servletWebRequest.getNativeRequest()).thenReturn(httpServletRequest);

        final Throwable throwable = new RuntimeException();

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, servletWebRequest, HttpStatus.BAD_REQUEST);

        // Then: Request info is set with null query
        assertThat(resource.getUri(), is(equalTo("/api/users")));
        assertThat(resource.getQuery(), is(nullValue()));
        assertThat(resource.getMethod(), is(equalTo("POST")));
        assertThat(resource.getContentType(), is(equalTo("application/json")));
    }

    @Test
    @DisplayName("Should not enrich when request is not ServletWebRequest")
    public void shouldNotEnrichWhenRequestIsNotServletWebRequest() {
        // Given: An error response resource and a non-ServletWebRequest
        final ErrorResponseResource resource = new ErrorResponseResource();
        final WebRequest webRequest = mock(WebRequest.class);
        final Throwable throwable = new RuntimeException();

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, webRequest, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Request info remains null
        assertThat(resource.getUri(), is(nullValue()));
        assertThat(resource.getQuery(), is(nullValue()));
        assertThat(resource.getMethod(), is(nullValue()));
        assertThat(resource.getContentType(), is(nullValue()));
    }

    @Test
    @DisplayName("Should enrich different HTTP methods")
    public void shouldEnrichDifferentHttpMethods() {
        // Given: An error response resource and ServletWebRequest with PUT method
        final ErrorResponseResource resource = new ErrorResponseResource();
        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("/api/users/123");
        when(httpServletRequest.getMethod()).thenReturn("PUT");
        when(httpServletRequest.getContentType()).thenReturn("application/json");

        final ServletWebRequest servletWebRequest = mock(ServletWebRequest.class);
        when(servletWebRequest.getNativeRequest()).thenReturn(httpServletRequest);

        final Throwable throwable = new RuntimeException();

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, servletWebRequest, HttpStatus.BAD_REQUEST);

        // Then: Method is correctly set
        assertThat(resource.getMethod(), is(equalTo("PUT")));
    }

    @Test
    @DisplayName("Should enrich different content types")
    public void shouldEnrichDifferentContentTypes() {
        // Given: An error response resource and ServletWebRequest with XML content type
        final ErrorResponseResource resource = new ErrorResponseResource();
        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("/api/data");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getContentType()).thenReturn("application/xml");

        final ServletWebRequest servletWebRequest = mock(ServletWebRequest.class);
        when(servletWebRequest.getNativeRequest()).thenReturn(httpServletRequest);

        final Throwable throwable = new RuntimeException();

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, servletWebRequest, HttpStatus.BAD_REQUEST);

        // Then: Content type is correctly set
        assertThat(resource.getContentType(), is(equalTo("application/xml")));
    }
}
