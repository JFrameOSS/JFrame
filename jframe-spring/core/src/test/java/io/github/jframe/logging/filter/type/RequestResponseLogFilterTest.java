package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.support.UnitTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RequestResponseLogFilter}.
 *
 * <p>Verifies the RequestResponseLogFilter functionality including:
 * <ul>
 * <li>Request logging before filter chain execution (DEBUG enabled)</li>
 * <li>Response logging after filter chain execution (DEBUG enabled)</li>
 * <li>FilterVoter integration for conditional logging</li>
 * <li>DEBUG short-circuit: no body capture when DEBUG logging is disabled</li>
 * <li>Filter chain always proceeds regardless of DEBUG state</li>
 * <li>Async request handling</li>
 * </ul>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Logging Filters - Request Response Log Filter")
public class RequestResponseLogFilterTest extends UnitTest {

    @Mock
    private RequestResponseLogger requestResponseLogger;

    @Mock
    private FilterVoter filterVoter;

    // ======================== DEBUG ON: NORMAL LOGGING PATH ========================

    @Nested
    @DisplayName("DEBUG enabled — normal logging path")
    class DebugEnabledLogging {

        @Test
        @DisplayName("Should log request when filter voter is enabled and DEBUG is on")
        public void shouldLogRequestWhenFilterVoterIsEnabledAndDebugIsOn() throws Exception {
            // Given: A request response log filter with filter voter enabled and DEBUG logging on
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(true);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Request is logged
            verify(requestResponseLogger).logRequest(any());
        }

        @Test
        @DisplayName("Should log response when filter voter is enabled and DEBUG is on")
        public void shouldLogResponseWhenFilterVoterIsEnabledAndDebugIsOn() throws Exception {
            // Given: A request response log filter with filter voter enabled and DEBUG logging on
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(true);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Response is logged
            verify(requestResponseLogger).logResponse(any(), any());
        }

        @Test
        @DisplayName("Should not log request when filter voter is disabled (regardless of DEBUG state)")
        public void shouldNotLogRequestWhenFilterVoterIsDisabled() throws Exception {
            // Given: A request response log filter with filter voter disabled
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(request)).thenReturn(false);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(true);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Request is not logged
            verify(requestResponseLogger, never()).logRequest(any());
        }

        @Test
        @DisplayName("Should not log response when filter voter is disabled (regardless of DEBUG state)")
        public void shouldNotLogResponseWhenFilterVoterIsDisabled() throws Exception {
            // Given: A request response log filter with filter voter disabled
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(request)).thenReturn(false);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(true);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Response is not logged
            verify(requestResponseLogger, never()).logResponse(any(), any());
        }
    }

    // ======================== DEBUG OFF: SHORT-CIRCUIT PATH ========================


    @Nested
    @DisplayName("DEBUG disabled — short-circuit: no capture, chain still proceeds")
    class DebugDisabledShortCircuit {

        @Test
        @DisplayName("Should not capture body or log request when DEBUG is disabled")
        public void shouldNotCaptureBodyOrLogRequestWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled but DEBUG logging is OFF
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: No request logging occurs (short-circuited before body capture)
            verify(requestResponseLogger, never()).logRequest(any());
        }

        @Test
        @DisplayName("Should not capture body or log response when DEBUG is disabled")
        public void shouldNotCaptureBodyOrLogResponseWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled but DEBUG logging is OFF
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: No response logging occurs (short-circuited before body capture)
            verify(requestResponseLogger, never()).logResponse(any(), any());
        }

        @Test
        @DisplayName("Should still invoke filter chain when DEBUG is disabled")
        public void shouldStillInvokeFilterChainWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled but DEBUG logging is OFF
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: The downstream chain still processes the request normally
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("Should return unaltered response when DEBUG is disabled")
        public void shouldReturnUnalteredResponseWhenDebugIsDisabled() throws Exception {
            // Given: Filter voter enabled but DEBUG logging is OFF
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(false);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: The original (unwrapped) response object is passed to the chain — no body interception
            verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
            verify(requestResponseLogger, never()).logResponse(any(), any());
        }
    }

    // ======================== FILTER CHAIN AND ASYNC ========================


    @Nested
    @DisplayName("Filter chain and async dispatch")
    class FilterChainAndAsync {

        @Test
        @DisplayName("Should continue filter chain execution when voter enabled and DEBUG on")
        public void shouldContinueFilterChainExecution() throws Exception {
            // Given: A request response log filter with DEBUG on
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(true);

            // When: Filter is executed
            filter.doFilterInternal(request, response, filterChain);

            // Then: Filter chain is continued
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("Should log response even when filter chain throws exception (DEBUG on)")
        public void shouldLogResponseEvenWhenFilterChainThrowsException() throws Exception {
            // Given: A request response log filter with filter chain that throws exception and DEBUG on
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain filterChain = mock(FilterChain.class);

            when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
            when(request.isAsyncStarted()).thenReturn(false);
            when(requestResponseLogger.isDebugEnabled()).thenReturn(true);
            org.mockito.Mockito.doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(any(), any());

            // When/Then: Filter execution throws exception but finally block still executes
            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (final RuntimeException e) {
                // Expected exception from filter chain
            }

            // Then: Response is still logged in finally block
            verify(requestResponseLogger).logResponse(any(), any());
        }

        @Test
        @DisplayName("Should not filter async dispatch")
        public void shouldNotFilterAsyncDispatch() {
            // Given: A request response log filter
            final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);

            // When: Checking if should not filter async dispatch
            final boolean shouldNotFilter = filter.shouldNotFilterAsyncDispatch();

            // Then: Returns false to allow filtering on async dispatches
            assertThat(shouldNotFilter, is(false));
        }
    }
}
