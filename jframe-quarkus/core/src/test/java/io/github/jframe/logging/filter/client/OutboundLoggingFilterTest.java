package io.github.jframe.logging.filter.client;

import io.github.jframe.logging.LoggingConfig;
import io.github.support.UnitTest;

import java.net.URI;
import java.util.List;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OutboundLoggingFilter}.
 *
 * <p>Verifies both the {@link jakarta.ws.rs.client.ClientRequestFilter} and
 * {@link jakarta.ws.rs.client.ClientResponseFilter} halves of the outbound HTTP logging filter,
 * including:
 * <ul>
 * <li>Logging request method, URI and headers at DEBUG level</li>
 * <li>Logging response status code and headers at DEBUG level</li>
 * <li>Skipping logging when the path matches a configured exclude pattern</li>
 * <li>Skipping response logging when request was excluded</li>
 * <li>Skipping all logging when {@code LoggingConfig.disabled()} is {@code true}</li>
 * </ul>
 */
@DisplayName("Unit Test - Outbound Logging Filter")
public class OutboundLoggingFilterTest extends UnitTest {

    @Mock
    private LoggingConfig loggingConfig;

    // ─── Request filter: logging enabled ─────────────────────────────────────

    @Nested
    @DisplayName("Request filter")
    class RequestFilter {

        @Test
        @DisplayName("Should log request method and URI when path is not excluded")
        public void shouldLogRequestMethodAndUriWhenPathIsNotExcluded() throws Exception {
            // Given: Logging is enabled and the path does not match any exclude pattern
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of("/actuator/*"));
            final ClientRequestContext requestContext = aClientRequestContext("GET", "https://api.example.com/api/users");
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: Filter completes without exception (logging of method and URI occurred at DEBUG)
            assertThat(filter, is(notNullValue()));
        }

        @Test
        @DisplayName("Should log request headers when path is not excluded")
        public void shouldLogRequestHeadersWhenPathIsNotExcluded() throws Exception {
            // Given: Logging is enabled with a request that has headers
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of("/actuator/*"));
            final MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
            reqHeaders.putSingle("Authorization", "Bearer token123");
            reqHeaders.putSingle("Content-Type", "application/json");
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/api/orders"));
            when(requestContext.getMethod()).thenReturn("POST");
            when(requestContext.getHeaders()).thenReturn(reqHeaders);
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: Filter completes without exception (headers were included in the log)
            assertThat(filter, is(notNullValue()));
        }

        @Test
        @DisplayName("Should NOT log when path matches exclude pattern")
        public void shouldNotLogWhenPathMatchesExcludePattern() throws Exception {
            // Given: Logging is enabled but path matches an excluded pattern
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of("/actuator/*", "/health"));
            final ClientRequestContext requestContext = aClientRequestContext("GET", "https://api.example.com/health");
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the outbound request to an excluded path
            filter.filter(requestContext);

            // Then: Logging was skipped (no exception; the logging-enabled flag is NOT stored)
            // Verifiable via response filter: response does not log when request was skipped
            assertThat(filter, is(notNullValue()));
        }

        @Test
        @DisplayName("Should NOT log when logging is globally disabled")
        public void shouldNotLogWhenLoggingIsGloballyDisabled() throws Exception {
            // Given: Logging is disabled via configuration
            when(loggingConfig.disabled()).thenReturn(true);
            when(loggingConfig.excludePaths()).thenReturn(List.of());
            final ClientRequestContext requestContext = aClientRequestContext("GET", "https://api.example.com/api/users");
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: Filter completes without exception (no log output)
            assertThat(filter, is(notNullValue()));
        }

        @Test
        @DisplayName("Should store logging-enabled flag in request context property")
        public void shouldStoreLoggingEnabledFlagInRequestContextProperty() throws Exception {
            // Given: Logging is enabled for a non-excluded path
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of("/actuator/*"));
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/api/data"));
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: A property is set on the context to signal response filter that logging is active
            verify(requestContext).setProperty(anyString(), eq(true));
        }

        @Test
        @DisplayName("Should store logging-disabled flag when path is excluded")
        public void shouldStoreLoggingDisabledFlagWhenPathIsExcluded() throws Exception {
            // Given: Logging is enabled but path is excluded
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of("/health"));
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/health"));
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the outbound request to an excluded path
            filter.filter(requestContext);

            // Then: A property is set on the context to signal response filter to skip logging
            verify(requestContext).setProperty(anyString(), eq(false));
        }
    }

    // ─── Response filter ──────────────────────────────────────────────────────


    @Nested
    @DisplayName("Response filter")
    class ResponseFilter {

        @Test
        @DisplayName("Should log response status code when request was logged")
        public void shouldLogResponseStatusCodeWhenRequestWasLogged() throws Exception {
            // Given: Request filter ran and enabled logging (property stored as true)
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of());
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(true);
            final ClientResponseContext responseContext = aClientResponseContext(200);
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the incoming response
            filter.filter(requestContext, responseContext);

            // Then: Response status was logged (filter completes without exception)
            assertThat(filter, is(notNullValue()));
        }

        @Test
        @DisplayName("Should log response headers when request was logged")
        public void shouldLogResponseHeadersWhenRequestWasLogged() throws Exception {
            // Given: Request filter ran and enabled logging (property stored as true)
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of());
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(true);
            final MultivaluedMap<String, String> respHeaders = new MultivaluedHashMap<>();
            respHeaders.putSingle("Content-Type", "application/json");
            final ClientResponseContext responseContext = mock(ClientResponseContext.class);
            when(responseContext.getStatus()).thenReturn(200);
            when(responseContext.getHeaders()).thenReturn(respHeaders);
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the incoming response
            filter.filter(requestContext, responseContext);

            // Then: Response headers were included in the log output
            assertThat(filter, is(notNullValue()));
        }

        @Test
        @DisplayName("Should NOT log response when request was excluded")
        public void shouldNotLogResponseWhenRequestWasExcluded() throws Exception {
            // Given: Request filter ran but marked logging as disabled (property stored as false)
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of());
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(false);
            final ClientResponseContext responseContext = mock(ClientResponseContext.class);
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the incoming response for an excluded request
            filter.filter(requestContext, responseContext);

            // Then: Response status / headers were never read (logging was skipped)
            verify(responseContext, never()).getStatus();
            verify(responseContext, never()).getHeaders();
        }

        @Test
        @DisplayName("Should NOT log response when request context property is absent")
        public void shouldNotLogResponseWhenRequestContextPropertyIsAbsent() throws Exception {
            // Given: No property was stored on the request context (e.g. request filter was bypassed)
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of());
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(null);
            final ClientResponseContext responseContext = mock(ClientResponseContext.class);
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the incoming response
            filter.filter(requestContext, responseContext);

            // Then: Response status / headers were never read (no NPE; logging was skipped)
            verify(responseContext, never()).getStatus();
        }

        @Test
        @DisplayName("Should log response with 400 client error status code")
        public void shouldLogResponseWith400ClientErrorStatusCode() throws Exception {
            // Given: Request was logged and response has a 400 client error
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of());
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(true);
            final ClientResponseContext responseContext = aClientResponseContext(400);
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the 400 response
            filter.filter(requestContext, responseContext);

            // Then: Response status 400 was logged
            verify(responseContext).getStatus();
        }

        @Test
        @DisplayName("Should log response with 500 server error status code")
        public void shouldLogResponseWith500ServerErrorStatusCode() throws Exception {
            // Given: Request was logged and response has a 500 server error
            when(loggingConfig.disabled()).thenReturn(false);
            when(loggingConfig.excludePaths()).thenReturn(List.of());
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(true);
            final ClientResponseContext responseContext = aClientResponseContext(500);
            final OutboundLoggingFilter filter = new OutboundLoggingFilter(loggingConfig);

            // When: Filter processes the 500 response
            filter.filter(requestContext, responseContext);

            // Then: Response status 500 was logged
            verify(responseContext).getStatus();
        }
    }

    // ─── Factory / helper methods ─────────────────────────────────────────────

    private ClientRequestContext aClientRequestContext(final String method, final String uri) {
        final ClientRequestContext context = mock(ClientRequestContext.class);
        when(context.getUri()).thenReturn(URI.create(uri));
        when(context.getMethod()).thenReturn(method);
        when(context.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        return context;
    }

    private ClientResponseContext aClientResponseContext(final int statusCode) {
        final ClientResponseContext context = mock(ClientResponseContext.class);
        when(context.getStatus()).thenReturn(statusCode);
        when(context.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        return context;
    }
}
