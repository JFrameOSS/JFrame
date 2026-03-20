package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.FilterConfig;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.model.RequestId;
import io.github.jframe.logging.model.TransactionId;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.jframe.logging.wrapper.CachingRequestContext;
import io.github.jframe.logging.wrapper.CachingResponseContext;
import io.github.support.UnitTest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RequestResponseLogFilter} (Quarkus JAX-RS adapter).
 *
 * <p>Verifies the RequestResponseLogFilter functionality including:
 * <ul>
 * <li>Delegation to RequestResponseLogger when FilterVoter is enabled</li>
 * <li>No delegation when FilterVoter is disabled</li>
 * <li>ThreadLocal cleanup (TransactionId, RequestId) in response phase</li>
 * <li>MDC cleanup (KibanaLogFields) in response phase</li>
 * <li>Graceful handling of null MediaType</li>
 * <li>Constructor smoke test</li>
 * </ul>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Quarkus Logging Filters - Request Response Log Filter")
public class RequestResponseLogFilterTest extends UnitTest {

    @Mock
    private RequestResponseLogger requestResponseLogger;

    @Mock
    private FilterVoter filterVoter;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private FilterConfig.RequestResponseConfig requestResponseConfig;

    @BeforeEach
    public void setUp() {
        lenient().when(filterConfig.requestResponse()).thenReturn(requestResponseConfig);
        lenient().when(requestResponseConfig.enabled()).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocals and MDC to avoid test pollution
        TransactionId.remove();
        RequestId.remove();
        KibanaLogFields.clear();
    }

    @Test
    @DisplayName("Should call logRequest when filter voter is enabled")
    public void shouldCallLogRequestWhenFilterVoterIsEnabled() throws Exception {
        // Given: A filter with voter enabled, mocked request context with JSON media type
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(filterVoter.enabled(requestContext)).thenReturn(true);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/test");
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.hasEntity()).thenReturn(false);

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: logRequest is called with a CachingRequestContext
        verify(requestResponseLogger).logRequest(any(CachingRequestContext.class));
    }

    @Test
    @DisplayName("Should not call logRequest when filter voter is disabled")
    public void shouldNotCallLogRequestWhenFilterVoterIsDisabled() throws Exception {
        // Given: A filter with voter disabled
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        when(filterVoter.enabled(requestContext)).thenReturn(false);

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: logRequest is never called
        verify(requestResponseLogger, never()).logRequest(any(CachingRequestContext.class));
    }

    @Test
    @DisplayName("Should call logResponse when filter voter is enabled")
    public void shouldCallLogResponseWhenFilterVoterIsEnabled() throws Exception {
        // Given: A filter with voter enabled; request phase already set a cached context
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(filterVoter.enabled(requestContext)).thenReturn(true);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/test");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.hasEntity()).thenReturn(false);
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // Simulate the request phase to populate internal state
        filter.filter(requestContext);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: logResponse is called
        verify(requestResponseLogger).logResponse(eq(requestContext), any(CachingResponseContext.class));
    }

    @Test
    @DisplayName("Should not call logResponse when filter voter is disabled")
    public void shouldNotCallLogResponseWhenFilterVoterIsDisabled() throws Exception {
        // Given: A filter with voter disabled
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

        when(filterVoter.enabled(requestContext)).thenReturn(false);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: logResponse is never called
        verify(requestResponseLogger, never()).logResponse(any(ContainerRequestContext.class), any(CachingResponseContext.class));
    }

    @Test
    @DisplayName("Should clean up TransactionId and RequestId ThreadLocals in response filter")
    public void shouldCleanUpThreadLocalsInResponseFilter() throws Exception {
        // Given: A filter with voter enabled, ThreadLocals populated
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(filterVoter.enabled(requestContext)).thenReturn(true);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/test");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.hasEntity()).thenReturn(false);
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // And: ThreadLocals are pre-populated
        TransactionId.set(java.util.UUID.randomUUID());
        RequestId.set(java.util.UUID.randomUUID());

        filter.filter(requestContext);

        // When: Filter processes the response
        filter.filter(requestContext, responseContext);

        // Then: ThreadLocals are cleared
        assertThat(TransactionId.get(), is(nullValue()));
        assertThat(RequestId.get(), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear MDC fields in response filter")
    public void shouldClearMdcFieldsInResponseFilter() throws Exception {
        // Given: A filter with voter enabled, MDC pre-populated
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(filterVoter.enabled(requestContext)).thenReturn(true);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/test");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.hasEntity()).thenReturn(false);
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // And: MDC field tx_id is pre-populated
        KibanaLogFields.tag(TX_ID, "some-transaction-id");

        filter.filter(requestContext);

        // When: Filter processes the response
        filter.filter(requestContext, responseContext);

        // Then: MDC field tx_id is cleared
        assertThat(KibanaLogFields.get(TX_ID), is(nullValue()));
    }

    @Test
    @DisplayName("Should handle null media type in request context without throwing")
    public void shouldHandleNullMediaTypeInRequestContextWithoutThrowing() throws Exception {
        // Given: A filter with voter enabled and null media type on the request
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(filterVoter.enabled(requestContext)).thenReturn(true);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/test");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getMediaType()).thenReturn(null);
        when(requestContext.hasEntity()).thenReturn(false);

        // When: Filter processes the incoming request (should not throw NPE)
        filter.filter(requestContext);

        // Then: Filter completes without exception
        assertThat(filter, is(notNullValue()));
    }

    @Test
    @DisplayName("Should create filter instance with dependencies")
    public void shouldCreateFilterInstanceWithDependencies() {
        // Given / When: A filter is constructed with required dependencies
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);

        // Then: Filter instance is created successfully
        assertThat(filter, is(notNullValue()));
    }
}
