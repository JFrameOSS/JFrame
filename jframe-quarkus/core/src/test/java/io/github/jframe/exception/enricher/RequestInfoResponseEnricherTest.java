package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;

import java.net.URI;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RequestInfoResponseEnricher}.
 *
 * <p>Verifies the enricher correctly extracts request info from ContainerRequestContext including:
 * <ul>
 * <li>HTTP method extracted from getMethod()</li>
 * <li>URI extracted from getUriInfo().getRequestUri()</li>
 * <li>Query string extracted from URI's raw query</li>
 * <li>Content type extracted from getMediaType()</li>
 * <li>Null content type handled gracefully</li>
 * </ul>
 */
@DisplayName("Unit Test - Request Info Response Enricher")
public class RequestInfoResponseEnricherTest extends UnitTest {

    private RequestInfoResponseEnricher enricher;

    @BeforeEach
    public void setUp() {
        enricher = new RequestInfoResponseEnricher();
    }

    @Test
    @DisplayName("Should enrich method, uri, query, and contentType from ContainerRequestContext")
    public void shouldEnrichMethodUriQueryAndContentTypeFromContainerRequestContext() {
        // Given: A mocked ContainerRequestContext with full request details
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/api/users?page=1&size=10"));
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, requestContext, 400);

        // Then: Request info is set from context
        assertThat(resource.getMethod(), is(equalTo("GET")));
        assertThat(resource.getUri(), is(equalTo("http://localhost/api/users")));
        assertThat(resource.getQuery(), is(equalTo("page=1&size=10")));
        assertThat(resource.getContentType(), is(equalTo("application/json")));
    }

    @Test
    @DisplayName("Should set null query when URI has no query string")
    public void shouldSetNullQueryWhenUriHasNoQueryString() {
        // Given: A mocked ContainerRequestContext with no query string in URI
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/api/users"));
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, requestContext, 400);

        // Then: Query is null
        assertThat(resource.getQuery(), is(nullValue()));
        assertThat(resource.getUri(), is(equalTo("http://localhost/api/users")));
    }

    @Test
    @DisplayName("Should set null contentType when media type is null")
    public void shouldSetNullContentTypeWhenMediaTypeIsNull() {
        // Given: A mocked ContainerRequestContext with null media type
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(requestContext.getMethod()).thenReturn("DELETE");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/api/users/123"));
        when(requestContext.getMediaType()).thenReturn(null);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, requestContext, 404);

        // Then: Content type is null
        assertThat(resource.getContentType(), is(nullValue()));
        assertThat(resource.getMethod(), is(equalTo("DELETE")));
    }

}
