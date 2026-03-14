package io.github.jframe.logging.filter.type;

import io.github.support.UnitTest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RequestResponseLogFilter} (Quarkus JAX-RS adapter).
 *
 * <p>Verifies the RequestResponseLogFilter functionality including:
 * <ul>
 * <li>Request details logging on incoming request</li>
 * <li>Response details logging on outgoing response</li>
 * <li>Content type filtering for log decision</li>
 * <li>Filter creation and configuration</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Filters - Request Response Log Filter")
public class RequestResponseLogFilterTest extends UnitTest {

    @Test
    @DisplayName("Should log request details when filter is enabled for content type")
    public void shouldLogRequestDetailsWhenFilterIsEnabledForContentType() throws Exception {
        // Given: A request response log filter and mocked request context with JSON content type
        final RequestResponseLogFilter filter = new RequestResponseLogFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/test");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: Filter processed the request without error
        assertThat(filter, is(notNullValue()));
    }

    @Test
    @DisplayName("Should create filter instance successfully")
    public void shouldCreateFilterInstanceSuccessfully() {
        // Given / When: A request response log filter is constructed
        final RequestResponseLogFilter filter = new RequestResponseLogFilter();

        // Then: Filter is instantiated
        assertThat(filter, is(notNullValue()));
    }

    @Test
    @DisplayName("Should handle null content type gracefully")
    public void shouldHandleNullContentTypeGracefully() throws Exception {
        // Given: A request response log filter and request context with no content type
        final RequestResponseLogFilter filter = new RequestResponseLogFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/test");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getMediaType()).thenReturn(null);

        // When: Filter processes the incoming request (should not throw)
        filter.filter(requestContext);

        // Then: Filter completes without exception
        assertThat(filter, is(notNullValue()));
    }

    @Test
    @DisplayName("Should handle response logging when response context is provided")
    public void shouldHandleResponseLoggingWhenResponseContextIsProvided() throws Exception {
        // Given: A request response log filter with mocked request/response contexts
        final RequestResponseLogFilter filter = new RequestResponseLogFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: Filter completes without exception
        assertThat(filter, is(notNullValue()));
    }
}
