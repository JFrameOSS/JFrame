package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.support.UnitTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
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
 * <li>Request logging before filter chain execution</li>
 * <li>Response logging after filter chain execution</li>
 * <li>FilterVoter integration for conditional logging</li>
 * <li>HTTP request/response wrapping for multiple reads</li>
 * <li>Async request handling</li>
 * <li>Filter chain continuation</li>
 * </ul>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Logging Filters - Request Response Log Filter")
public class RequestResponseLogFilterTest extends UnitTest {

    @Mock
    private RequestResponseLogger requestResponseLogger;

    @Mock
    private FilterVoter filterVoter;

    @Test
    @DisplayName("Should log request when filter voter is enabled")
    public void shouldLogRequestWhenFilterVoterIsEnabled() throws Exception {
        // Given: A request response log filter with filter voter enabled
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
        when(request.isAsyncStarted()).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Request is logged
        verify(requestResponseLogger).logRequest(any());
    }

    @Test
    @DisplayName("Should log response when filter voter is enabled")
    public void shouldLogResponseWhenFilterVoterIsEnabled() throws Exception {
        // Given: A request response log filter with filter voter enabled
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
        when(request.isAsyncStarted()).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Response is logged
        verify(requestResponseLogger).logResponse(any(), any());
    }

    @Test
    @DisplayName("Should not log request when filter voter is disabled")
    public void shouldNotLogRequestWhenFilterVoterIsDisabled() throws Exception {
        // Given: A request response log filter with filter voter disabled
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(filterVoter.enabled(request)).thenReturn(false);
        when(request.isAsyncStarted()).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Request is not logged
        verify(requestResponseLogger, never()).logRequest(any());
    }

    @Test
    @DisplayName("Should not log response when filter voter is disabled")
    public void shouldNotLogResponseWhenFilterVoterIsDisabled() throws Exception {
        // Given: A request response log filter with filter voter disabled
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(filterVoter.enabled(request)).thenReturn(false);
        when(request.isAsyncStarted()).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Response is not logged
        verify(requestResponseLogger, never()).logResponse(any(), any());
    }

    @Test
    @DisplayName("Should continue filter chain execution")
    public void shouldContinueFilterChainExecution() throws Exception {
        // Given: A request response log filter
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
        when(request.isAsyncStarted()).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Filter chain is continued
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should log response even when filter chain throws exception")
    public void shouldLogResponseEvenWhenFilterChainThrowsException() throws Exception {
        // Given: A request response log filter with filter chain that throws exception
        final RequestResponseLogFilter filter = new RequestResponseLogFilter(requestResponseLogger, filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(filterVoter.enabled(any(HttpServletRequest.class))).thenReturn(true);
        when(request.isAsyncStarted()).thenReturn(false);
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
