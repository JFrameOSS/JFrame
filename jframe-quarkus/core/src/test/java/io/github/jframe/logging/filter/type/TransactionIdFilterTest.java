package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.model.TransactionId;
import io.github.support.UnitTest;

import java.util.UUID;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TransactionIdFilter} (Quarkus JAX-RS adapter).
 *
 * <p>Verifies the TransactionIdFilter functionality including:
 * <ul>
 * <li>Transaction ID generation when no header is present</li>
 * <li>Transaction ID resolution from incoming request header</li>
 * <li>Transaction ID storage in ThreadLocal (TransactionId)</li>
 * <li>Transaction ID addition to response header</li>
 * <li>Invalid UUID handling in request header</li>
 * <li>Null and blank header handling</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Filters - Transaction ID Filter")
public class TransactionIdFilterTest extends UnitTest {

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocal to avoid test pollution
        TransactionId.remove();
    }

    @Test
    @DisplayName("Should generate and set transaction ID in ThreadLocal when no header present")
    public void shouldGenerateAndSetTransactionIdInThreadLocalWhenNoHeaderPresent() throws Exception {
        // Given: A transaction ID filter, mocked request context without header
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        when(requestContext.getHeaderString(TX_ID_HEADER)).thenReturn(null);

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: Transaction ID is set in ThreadLocal
        final String transactionId = TransactionId.get();
        assertThat(transactionId, is(notNullValue()));
    }

    @Test
    @DisplayName("Should use existing transaction ID from request header")
    public void shouldUseExistingTransactionIdFromRequestHeader() throws Exception {
        // Given: A transaction ID filter, mocked request context with valid UUID header
        final UUID existingTxId = UUID.randomUUID();
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        when(requestContext.getHeaderString(TX_ID_HEADER)).thenReturn(existingTxId.toString());

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: Transaction ID from header is used
        final String transactionId = TransactionId.get();
        assertThat(transactionId, is(equalTo(existingTxId.toString())));
    }

    @Test
    @DisplayName("Should add transaction ID to response header")
    public void shouldAddTransactionIdToResponseHeader() throws Exception {
        // Given: A transaction ID filter — first process request to populate ThreadLocal
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        when(requestContext.getHeaderString(TX_ID_HEADER)).thenReturn(null);
        when(responseContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: Transaction ID is added to response headers
        assertThat(headers.containsKey(TX_ID_HEADER), is(true));
    }

    @Test
    @DisplayName("Should not add transaction ID to response header if already present")
    public void shouldNotAddTransactionIdToResponseHeaderIfAlreadyPresent() throws Exception {
        // Given: A transaction ID filter, response already has the TX header
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(TX_ID_HEADER, "existing-tx-id");

        when(responseContext.getHeaders()).thenReturn(headers);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: Header count remains 1 (not duplicated)
        assertThat(headers.get(TX_ID_HEADER).size(), is(1));
    }

    @Test
    @DisplayName("Should generate new UUID when header contains invalid UUID")
    public void shouldGenerateNewUuidWhenHeaderContainsInvalidUuid() throws Exception {
        // Given: A transaction ID filter, request context with invalid UUID in header
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        when(requestContext.getHeaderString(TX_ID_HEADER)).thenReturn("not-a-valid-uuid");

        // When: Filter processes the request
        filter.filter(requestContext);

        // Then: A new valid UUID is generated (not the invalid string)
        final String transactionId = TransactionId.get();
        assertThat(transactionId, is(notNullValue()));
        assertThat(transactionId, is(not(equalTo("not-a-valid-uuid"))));
    }

    @Test
    @DisplayName("Should generate new UUID when header is blank")
    public void shouldGenerateNewUuidWhenHeaderIsBlank() throws Exception {
        // Given: A transaction ID filter, request context with blank header
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        when(requestContext.getHeaderString(TX_ID_HEADER)).thenReturn("   ");

        // When: Filter processes the request
        filter.filter(requestContext);

        // Then: A new valid UUID is generated
        final String transactionId = TransactionId.get();
        assertThat(transactionId, is(notNullValue()));
    }

    @Test
    @DisplayName("Should generate unique transaction IDs for different requests")
    public void shouldGenerateUniqueTransactionIdsForDifferentRequests() throws Exception {
        // Given: A transaction ID filter and two separate requests without headers
        final TransactionIdFilter filter = new TransactionIdFilter(TX_ID_HEADER);
        final ContainerRequestContext requestContext1 = mock(ContainerRequestContext.class);
        when(requestContext1.getHeaderString(TX_ID_HEADER)).thenReturn(null);

        // When: First filter execution
        filter.filter(requestContext1);
        final String txId1 = TransactionId.get();
        TransactionId.remove();

        // And: Second filter execution
        final ContainerRequestContext requestContext2 = mock(ContainerRequestContext.class);
        when(requestContext2.getHeaderString(TX_ID_HEADER)).thenReturn(null);
        filter.filter(requestContext2);
        final String txId2 = TransactionId.get();

        // Then: Transaction IDs are unique
        assertThat(txId1, is(notNullValue()));
        assertThat(txId2, is(notNullValue()));
        assertThat(txId1, is(not(txId2)));
    }

    @Test
    @DisplayName("Should resolve UUID from valid header string")
    public void shouldResolveUuidFromValidHeaderString() {
        // Given: A valid UUID string
        final UUID expectedUuid = UUID.randomUUID();

        // When: Resolving UUID from the header value
        final UUID resolvedUuid = TransactionIdFilter.resolve(expectedUuid.toString());

        // Then: UUID is correctly parsed
        assertThat(resolvedUuid, is(equalTo(expectedUuid)));
    }

    @Test
    @DisplayName("Should generate new UUID when header value is null")
    public void shouldGenerateNewUuidWhenHeaderValueIsNull() {
        // Given: A null header value

        // When: Resolving UUID from null
        final UUID resolvedUuid = TransactionIdFilter.resolve(null);

        // Then: A new UUID is generated
        assertThat(resolvedUuid, is(notNullValue()));
    }

    @Test
    @DisplayName("Should generate new UUID when header value is invalid format")
    public void shouldGenerateNewUuidWhenHeaderValueIsInvalidFormat() {
        // Given: An invalid UUID string

        // When: Resolving UUID from invalid string
        final UUID resolvedUuid = TransactionIdFilter.resolve("invalid-uuid-format");

        // Then: A new UUID is generated
        assertThat(resolvedUuid, is(notNullValue()));
        assertThat(resolvedUuid.toString(), is(not(equalTo("invalid-uuid-format"))));
    }
}
