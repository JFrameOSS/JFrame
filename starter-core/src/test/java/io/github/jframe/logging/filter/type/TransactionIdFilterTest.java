package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.model.TransactionId;
import io.github.support.UnitTest;

import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TransactionIdFilter}.
 *
 * <p>Verifies the TransactionIdFilter functionality including:
 * <ul>
 * <li>Transaction ID generation for new requests</li>
 * <li>Transaction ID resolution from incoming request header</li>
 * <li>Transaction ID storage in ThreadLocal (TransactionId)</li>
 * <li>Transaction ID addition to response header</li>
 * <li>Invalid UUID handling in request header</li>
 * <li>Filter chain execution</li>
 * </ul>
 */
@DisplayName("Logging Filters - Transaction ID Filter")
public class TransactionIdFilterTest extends UnitTest {

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocal to avoid test pollution
        TransactionId.remove();
    }

    @Test
    @DisplayName("Should generate and set transaction ID in ThreadLocal when no header present")
    public void shouldGenerateAndSetTransactionIdInThreadLocalWhenNoHeaderPresent() throws Exception {
        // Given: A transaction ID filter, mocked request without transaction ID header
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn(null);
        when(response.containsHeader(TX_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Transaction ID is set in ThreadLocal
        final String transactionId = TransactionId.get();
        assertThat(transactionId, is(notNullValue()));
    }

    @Test
    @DisplayName("Should use existing transaction ID from request header")
    public void shouldUseExistingTransactionIdFromRequestHeader() throws Exception {
        // Given: A transaction ID filter, mocked request with transaction ID header
        final UUID existingTxId = UUID.randomUUID();
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn(existingTxId.toString());
        when(response.containsHeader(TX_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Transaction ID from header is used
        final String transactionId = TransactionId.get();
        assertThat(transactionId, is(equalTo(existingTxId.toString())));
    }

    @Test
    @DisplayName("Should add transaction ID to response header")
    public void shouldAddTransactionIdToResponseHeader() throws Exception {
        // Given: A transaction ID filter, mocked request/response/chain
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn(null);
        when(response.containsHeader(TX_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Transaction ID is added to response header
        verify(response).addHeader(eq(TX_ID_HEADER), anyString());
    }

    @Test
    @DisplayName("Should not add transaction ID to response header if already present")
    public void shouldNotAddTransactionIdToResponseHeaderIfAlreadyPresent() throws Exception {
        // Given: A transaction ID filter with response already containing the header
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn(null);
        when(response.containsHeader(TX_ID_HEADER)).thenReturn(true);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Transaction ID is not added to response header
        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    @DisplayName("Should continue filter chain execution")
    public void shouldContinueFilterChainExecution() throws Exception {
        // Given: A transaction ID filter, mocked request/response/chain
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn(null);
        when(response.containsHeader(TX_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: Filter chain is continued
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should generate new UUID when header contains invalid UUID")
    public void shouldGenerateNewUuidWhenHeaderContainsInvalidUuid() throws Exception {
        // Given: A transaction ID filter, mocked request with invalid UUID in header
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn("invalid-uuid-format");
        when(response.containsHeader(TX_ID_HEADER)).thenReturn(false);

        // When: Filter is executed
        filter.doFilterInternal(request, response, filterChain);

        // Then: A new valid UUID is generated
        final String transactionId = TransactionId.get();
        assertThat(transactionId, is(notNullValue()));
        assertThat(transactionId, is(not(equalTo("invalid-uuid-format"))));
    }

    @Test
    @DisplayName("Should resolve UUID from valid header")
    public void shouldResolveUuidFromValidHeader() {
        // Given: A request with valid UUID in header
        final UUID expectedUuid = UUID.randomUUID();
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn(expectedUuid.toString());

        // When: Resolving UUID
        final UUID resolvedUuid = TransactionIdFilter.resolve(request, TX_ID_HEADER);

        // Then: UUID is correctly parsed
        assertThat(resolvedUuid, is(equalTo(expectedUuid)));
    }

    @Test
    @DisplayName("Should generate new UUID when header is null")
    public void shouldGenerateNewUuidWhenHeaderIsNull() {
        // Given: A request without transaction ID header
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn(null);

        // When: Resolving UUID
        final UUID resolvedUuid = TransactionIdFilter.resolve(request, TX_ID_HEADER);

        // Then: A new UUID is generated
        assertThat(resolvedUuid, is(notNullValue()));
    }

    @Test
    @DisplayName("Should generate new UUID when header is blank")
    public void shouldGenerateNewUuidWhenHeaderIsBlank() {
        // Given: A request with blank transaction ID header
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn("   ");

        // When: Resolving UUID
        final UUID resolvedUuid = TransactionIdFilter.resolve(request, TX_ID_HEADER);

        // Then: A new UUID is generated
        assertThat(resolvedUuid, is(notNullValue()));
    }

    @Test
    @DisplayName("Should generate new UUID when header contains invalid format")
    public void shouldGenerateNewUuidWhenHeaderContainsInvalidFormat() {
        // Given: A request with invalid UUID format in header
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader(TX_ID_HEADER)).thenReturn("not-a-valid-uuid");

        // When: Resolving UUID
        final UUID resolvedUuid = TransactionIdFilter.resolve(request, TX_ID_HEADER);

        // Then: A new UUID is generated instead
        assertThat(resolvedUuid, is(notNullValue()));
        assertThat(resolvedUuid.toString(), is(not(equalTo("not-a-valid-uuid"))));
    }
}
