package io.github.jframe.logging.filter.type;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RequestDurationFilter}.
 *
 * <p>Verifies the RequestDurationFilter functionality including:
 * <ul>
 * <li>Start timestamp storage in request attributes</li>
 * <li>Filter chain execution</li>
 * <li>Duration calculation and logging</li>
 * <li>FilterVoter integration for conditional logging</li>
 * <li>Async request handling</li>
 * <li>Timestamp preservation across filter invocations</li>
 * </ul>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Logging Filters - Request Duration Filter")
public class RequestDurationFilterTest extends UnitTest {

    private static final String START_TIMESTAMP = "start_timestamp";

    @Mock
    private FilterVoter filterVoter;

    @Test
    @DisplayName("Should set start timestamp in request attributes")
    public void shouldSetStartTimestampInRequestAttributes() throws Exception {
        // Given: A request duration filter, mocked request/response/chain
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getAttribute(START_TIMESTAMP)).thenReturn(null);
        when(request.isAsyncStarted()).thenReturn(false);
        when(filterVoter.enabled(request)).thenReturn(true);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Start timestamp is set in request attributes
        verify(request).setAttribute(eq(START_TIMESTAMP), org.mockito.ArgumentMatchers.any(Long.class));
    }

    @Test
    @DisplayName("Should not overwrite existing start timestamp")
    public void shouldNotOverwriteExistingStartTimestamp() throws Exception {
        // Given: A request duration filter with existing timestamp
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);
        final Long existingTimestamp = System.nanoTime();

        when(request.getAttribute(START_TIMESTAMP)).thenReturn(existingTimestamp);
        when(request.isAsyncStarted()).thenReturn(false);
        when(filterVoter.enabled(request)).thenReturn(true);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Start timestamp getAttribute is called but setAttribute is not called to overwrite
        verify(request, org.mockito.Mockito.atLeastOnce()).getAttribute(START_TIMESTAMP);
        verify(request, never()).setAttribute(eq(START_TIMESTAMP), org.mockito.ArgumentMatchers.any(Long.class));
    }

    @Test
    @DisplayName("Should continue filter chain execution")
    public void shouldContinueFilterChainExecution() throws Exception {
        // Given: A request duration filter, mocked request/response/chain
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getAttribute(START_TIMESTAMP)).thenReturn(null);
        when(request.isAsyncStarted()).thenReturn(false);
        when(filterVoter.enabled(request)).thenReturn(true);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Filter chain is continued
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should retrieve start timestamp for duration calculation when filter voter enabled")
    public void shouldRetrieveStartTimestampForDurationCalculationWhenFilterVoterEnabled() throws Exception {
        // Given: A request duration filter with start timestamp set
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);
        final Long startTimestamp = System.nanoTime();

        when(request.getAttribute(START_TIMESTAMP)).thenReturn(null).thenReturn(startTimestamp);
        when(request.isAsyncStarted()).thenReturn(false);
        when(filterVoter.enabled(request)).thenReturn(true);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Start timestamp is retrieved at least twice (once to check, once to log)
        verify(request, org.mockito.Mockito.atLeast(2)).getAttribute(START_TIMESTAMP);
    }

    @Test
    @DisplayName("Should not log duration when filter voter is disabled")
    public void shouldNotLogDurationWhenFilterVoterIsDisabled() throws Exception {
        // Given: A request duration filter with filter voter disabled
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getAttribute(START_TIMESTAMP)).thenReturn(null);
        when(request.isAsyncStarted()).thenReturn(false);
        when(filterVoter.enabled(request)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Filter chain is continued but duration logging is skipped
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not log duration for async started requests")
    public void shouldNotLogDurationForAsyncStartedRequests() throws Exception {
        // Given: A request duration filter with async request
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getAttribute(START_TIMESTAMP)).thenReturn(null);
        when(request.isAsyncStarted()).thenReturn(true);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Filter chain is continued but duration logging is skipped for async
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle filter chain even when exception occurs")
    public void shouldHandleFilterChainEvenWhenExceptionOccurs() throws Exception {
        // Given: A request duration filter with filter chain that throws exception
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);
        final Long startTimestamp = System.nanoTime();

        when(request.getAttribute(START_TIMESTAMP)).thenReturn(null).thenReturn(startTimestamp);
        when(request.isAsyncStarted()).thenReturn(false);
        when(filterVoter.enabled(request)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        // When/Then: Filter execution throws exception but finally block still executes
        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (final RuntimeException e) {
            // Expected exception from filter chain
        }

        // Then: Start timestamp retrieval still happens in finally block
        verify(request, org.mockito.Mockito.atLeast(2)).getAttribute(START_TIMESTAMP);
    }

    @Test
    @DisplayName("Should not filter async dispatch")
    public void shouldNotFilterAsyncDispatch() {
        // Given: A request duration filter
        final RequestDurationFilter filter = new RequestDurationFilter(filterVoter);

        // When: Checking if should not filter async dispatch
        final boolean shouldNotFilter = filter.shouldNotFilterAsyncDispatch();

        // Then: Returns false to allow filtering on async dispatches
        org.hamcrest.MatcherAssert.assertThat(shouldNotFilter, org.hamcrest.Matchers.is(false));
    }
}
