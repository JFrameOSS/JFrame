package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.model.RequestId;
import io.github.support.UnitTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;
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
 * Tests for {@link RequestIdFilter}.
 *
 * <p>Verifies the RequestIdFilter functionality including:
 * <ul>
 * <li>Unique request ID generation for each request</li>
 * <li>Request ID storage in ThreadLocal (RequestId)</li>
 * <li>Request ID addition to response header</li>
 * <li>Filter chain execution</li>
 * <li>ThreadLocal cleanup after test</li>
 * </ul>
 */
@DisplayName("Logging Filters - Request ID Filter")
public class RequestIdFilterTest extends UnitTest {

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocal to avoid test pollution
        RequestId.remove();
    }

    @Test
    @DisplayName("Should generate and set request ID in ThreadLocal")
    public void shouldGenerateAndSetRequestIdInThreadLocal() throws Exception {
        // Given: A request ID filter, mocked request/response/chain
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(REQ_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Request ID is set in ThreadLocal
        final String requestId = RequestId.get();
        assertThat(requestId, is(notNullValue()));
    }

    @Test
    @DisplayName("Should add request ID to response header")
    public void shouldAddRequestIdToResponseHeader() throws Exception {
        // Given: A request ID filter, mocked request/response/chain
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(REQ_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Request ID is added to response header
        verify(response).addHeader(eq(REQ_ID_HEADER), anyString());
    }

    @Test
    @DisplayName("Should not add request ID to response header if already present")
    public void shouldNotAddRequestIdToResponseHeaderIfAlreadyPresent() throws Exception {
        // Given: A request ID filter with response already containing the header
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(REQ_ID_HEADER)).thenReturn(true);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Request ID is not added to response header
        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    @DisplayName("Should continue filter chain execution")
    public void shouldContinueFilterChainExecution() throws Exception {
        // Given: A request ID filter, mocked request/response/chain
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(response.containsHeader(REQ_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Filter chain is continued
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should generate unique request IDs for different requests")
    public void shouldGenerateUniqueRequestIdsForDifferentRequests() throws Exception {
        // Given: A request ID filter and two different requests
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final HttpServletRequest request1 = mock(HttpServletRequest.class);
        final HttpServletResponse response1 = mock(HttpServletResponse.class);
        final FilterChain filterChain1 = mock(FilterChain.class);

        when(response1.containsHeader(REQ_ID_HEADER)).thenReturn(false);

        // When: First filter execution
        filter.doFilterInternal(request1, response1, filterChain1);
        final String requestId1 = RequestId.get();

        // Clean up and prepare for second request
        RequestId.remove();

        final HttpServletRequest request2 = mock(HttpServletRequest.class);
        final HttpServletResponse response2 = mock(HttpServletResponse.class);
        final FilterChain filterChain2 = mock(FilterChain.class);

        when(response2.containsHeader(REQ_ID_HEADER)).thenReturn(false);

        // When: Second filter execution
        filter.doFilterInternal(request2, response2, filterChain2);
        final String requestId2 = RequestId.get();

        // Then: Request IDs are different
        assertThat(requestId1, is(notNullValue()));
        assertThat(requestId2, is(notNullValue()));
        assertThat(requestId1.equals(requestId2), is(false));
    }
}
