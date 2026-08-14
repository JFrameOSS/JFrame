package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.filter.FilterConfig;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
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
 * <li>Delegation to {@link DebugAwareRequestResponseLogger} when FilterVoter is enabled and DEBUG is on</li>
 * <li>No delegation when FilterVoter is disabled</li>
 * <li>DEBUG short-circuit: no body capture when DEBUG logging is disabled</li>
 * <li>Filter chain always proceeds regardless of DEBUG state</li>
 * <li>ThreadLocal cleanup (TransactionId, RequestId) in response phase — even when short-circuiting</li>
 * <li>MDC cleanup (EcsFields) in response phase — even when short-circuiting</li>
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
        // DEBUG on by default in tests that exercise normal logging path
        lenient().when(requestResponseLogger.isDebugEnabled()).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocals and MDC to avoid test pollution
        TransactionId.remove();
        RequestId.remove();
        EcsFields.clear();
    }

    // ======================== DEBUG ON: NORMAL LOGGING PATH ========================

    @Nested
    @DisplayName("DEBUG enabled — normal logging path")
    class DebugEnabledLogging {

        @Test
        @DisplayName("Should call logRequest when filter voter is enabled and DEBUG is on")
        public void shouldCallLogRequestWhenFilterVoterIsEnabledAndDebugIsOn() throws Exception {
            // Given: A filter with voter enabled, mocked request context with JSON media type, DEBUG on
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
        @DisplayName("Should not call logRequest when filter voter is disabled (DEBUG on)")
        public void shouldNotCallLogRequestWhenFilterVoterIsDisabled() throws Exception {
            // Given: A filter with voter disabled; DEBUG is on
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

            when(filterVoter.enabled(requestContext)).thenReturn(false);

            // When: Filter processes the incoming request
            filter.filter(requestContext);

            // Then: logRequest is never called
            verify(requestResponseLogger, never()).logRequest(any(CachingRequestContext.class));
        }

        @Test
        @DisplayName("Should call logResponse when filter voter is enabled and DEBUG is on")
        public void shouldCallLogResponseWhenFilterVoterIsEnabledAndDebugIsOn() throws Exception {
            // Given: A filter with voter enabled; request phase already set a cached context; DEBUG on
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
        @DisplayName("Should not call logResponse when filter voter is disabled (DEBUG on)")
        public void shouldNotCallLogResponseWhenFilterVoterIsDisabled() throws Exception {
            // Given: A filter with voter disabled; DEBUG on
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
        @DisplayName("Should handle null media type in request context without throwing (DEBUG on)")
        public void shouldHandleNullMediaTypeInRequestContextWithoutThrowing() throws Exception {
            // Given: A filter with voter enabled and null media type on the request; DEBUG on
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
    }

    // ======================== DEBUG OFF: SHORT-CIRCUIT PATH ========================


    @Nested
    @DisplayName("DEBUG disabled — short-circuit: no capture, chain still proceeds, cleanup still runs")
    class DebugDisabledShortCircuit {

        @Test
        @DisplayName("Should not capture body or call logRequest when DEBUG is disabled")
        public void shouldNotCaptureBodyOrCallLogRequestWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled, filter enabled, but DEBUG logging is OFF
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

            when(filterVoter.enabled(requestContext)).thenReturn(true);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            // When: Filter processes the incoming request
            filter.filter(requestContext);

            // Then: No body capture and no logging (short-circuit before wrapping)
            verify(requestResponseLogger, never()).logRequest(any(CachingRequestContext.class));
        }

        @Test
        @DisplayName("Should not capture body or call logResponse when DEBUG is disabled")
        public void shouldNotCaptureBodyOrCallLogResponseWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled, filter enabled, but DEBUG logging is OFF
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

            when(filterVoter.enabled(requestContext)).thenReturn(true);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: No body capture and no logging
            verify(requestResponseLogger, never()).logResponse(any(), any(CachingResponseContext.class));
        }

        @Test
        @DisplayName("Should still run ThreadLocal cleanup in response filter when DEBUG is disabled")
        public void shouldStillRunThreadLocalCleanupInResponseFilterWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled, filter enabled, DEBUG OFF; ThreadLocals pre-populated
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

            when(filterVoter.enabled(requestContext)).thenReturn(true);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            TransactionId.set(java.util.UUID.randomUUID());
            RequestId.set(java.util.UUID.randomUUID());

            // When: Response filter runs (short-circuits due to DEBUG off)
            filter.filter(requestContext, responseContext);

            // Then: ThreadLocals are cleared — cleanup always runs
            assertThat(TransactionId.get(), is(nullValue()));
            assertThat(RequestId.get(), is(nullValue()));
        }

        @Test
        @DisplayName("Should still run MDC cleanup in response filter when DEBUG is disabled")
        public void shouldStillRunMdcCleanupInResponseFilterWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled, filter enabled, DEBUG OFF; MDC pre-populated
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

            when(filterVoter.enabled(requestContext)).thenReturn(true);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            EcsFields.tag(TX_ID, "some-tx-id");

            // When: Response filter runs (short-circuits due to DEBUG off)
            filter.filter(requestContext, responseContext);

            // Then: MDC is cleared — cleanup always runs
            assertThat(EcsFields.get(TX_ID), is(nullValue()));
        }
    }

    // ======================== FILTER DISABLED ENTIRELY ========================


    @Nested
    @DisplayName("Filter disabled entirely — chain proceeds, cleanup still runs")
    class FilterDisabledEntirely {

        @Test
        @DisplayName("Should not call logRequest when filter is disabled")
        public void shouldNotCallLogRequestWhenFilterIsDisabled() throws Exception {
            // Given: Filter config disables the filter; voter is irrelevant
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

            when(requestResponseConfig.enabled()).thenReturn(false);

            // When: Filter processes the incoming request
            filter.filter(requestContext);

            // Then: No logging occurs
            verify(requestResponseLogger, never()).logRequest(any(CachingRequestContext.class));
        }

        @Test
        @DisplayName("Should still run cleanup in response filter when filter is disabled")
        public void shouldStillRunCleanupInResponseFilterWhenFilterIsDisabled() throws Exception {
            // Given: Filter config disables the filter; ThreadLocals pre-populated
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter, filterConfig);
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

            when(requestResponseConfig.enabled()).thenReturn(false);

            TransactionId.set(java.util.UUID.randomUUID());
            RequestId.set(java.util.UUID.randomUUID());
            EcsFields.tag(TX_ID, "some-tx-id");

            // When: Response filter runs
            filter.filter(requestContext, responseContext);

            // Then: Cleanup still runs even though filter is disabled
            assertThat(TransactionId.get(), is(nullValue()));
            assertThat(RequestId.get(), is(nullValue()));
            assertThat(EcsFields.get(TX_ID), is(nullValue()));
        }
    }

    // ======================== EXISTING CLEANUP AND MISC ========================


    @Nested
    @DisplayName("Cleanup and miscellaneous (DEBUG on)")
    class CleanupAndMisc {

        @Test
        @DisplayName("Should clean up TransactionId and RequestId ThreadLocals in response filter (DEBUG on)")
        public void shouldCleanUpThreadLocalsInResponseFilter() throws Exception {
            // Given: A filter with voter enabled, ThreadLocals populated; DEBUG on
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
        @DisplayName("Should clear MDC fields in response filter (DEBUG on)")
        public void shouldClearMdcFieldsInResponseFilter() throws Exception {
            // Given: A filter with voter enabled, MDC pre-populated; DEBUG on
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
            EcsFields.tag(TX_ID, "some-transaction-id");

            filter.filter(requestContext);

            // When: Filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: MDC field tx_id is cleared
            assertThat(EcsFields.get(TX_ID), is(nullValue()));
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
}
