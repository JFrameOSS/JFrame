package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.model.TransactionId;
import io.github.support.UnitTest;

import java.util.UUID;
import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionIdResponseEnricher}.
 *
 * <p>Verifies the enricher correctly sets the transaction ID from the ThreadLocal including:
 * <ul>
 * <li>txId set when TransactionId ThreadLocal has value</li>
 * <li>txId is null when ThreadLocal is empty</li>
 * <li>Different transaction ID values are correctly propagated</li>
 * </ul>
 */
@DisplayName("Unit Test - Transaction ID Response Enricher")
public class TransactionIdResponseEnricherTest extends UnitTest {

    private TransactionIdResponseEnricher enricher;

    @BeforeEach
    public void setUp() {
        enricher = new TransactionIdResponseEnricher();
    }

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocal to avoid test pollution
        TransactionId.remove();
    }

    @Test
    @DisplayName("Should set txId when TransactionId ThreadLocal has value")
    public void shouldSetTxIdWhenTransactionIdThreadLocalHasValue() {
        // Given: An error response resource and transaction ID set in ThreadLocal
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UUID txUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TransactionId.set(txUuid);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, requestContext, 500);

        // Then: Transaction ID is set on the resource
        assertThat(resource.getTxId(), is(equalTo(txUuid.toString())));
    }

    @Test
    @DisplayName("Should set txId to null when TransactionId ThreadLocal is empty")
    public void shouldSetTxIdToNullWhenTransactionIdThreadLocalIsEmpty() {
        // Given: An error response resource with no transaction ID in ThreadLocal
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        // No TransactionId.set() — ThreadLocal is empty

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, requestContext, 400);

        // Then: Transaction ID is null
        assertThat(resource.getTxId(), is(nullValue()));
    }

    @Test
    @DisplayName("Should propagate different transaction ID values correctly")
    public void shouldPropagateDifferentTransactionIdValuesCorrectly() {
        // Given: First transaction ID
        final ErrorResponseResource resource1 = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UUID txUuid1 = UUID.randomUUID();
        TransactionId.set(txUuid1);

        // When: Enriching with first transaction ID
        enricher.doEnrich(resource1, throwable, requestContext, 400);

        // Then: First transaction ID is set
        assertThat(resource1.getTxId(), is(equalTo(txUuid1.toString())));

        // Given: Second transaction ID (different UUID)
        TransactionId.remove();
        final ErrorResponseResource resource2 = new ErrorResponseResource();
        final UUID txUuid2 = UUID.randomUUID();
        TransactionId.set(txUuid2);

        // When: Enriching with second transaction ID
        enricher.doEnrich(resource2, throwable, requestContext, 500);

        // Then: Second transaction ID is set (not the first)
        assertThat(resource2.getTxId(), is(equalTo(txUuid2.toString())));
    }

    @Test
    @DisplayName("Should set txId regardless of HTTP status code")
    public void shouldSetTxIdRegardlessOfHttpStatusCode() {
        // Given: A transaction ID set in ThreadLocal
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UUID txUuid = UUID.randomUUID();
        TransactionId.set(txUuid);

        // When: Enriching with various status codes
        final ErrorResponseResource resource400 = new ErrorResponseResource();
        enricher.doEnrich(resource400, throwable, requestContext, 400);

        final ErrorResponseResource resource500 = new ErrorResponseResource();
        enricher.doEnrich(resource500, throwable, requestContext, 500);

        final ErrorResponseResource resource404 = new ErrorResponseResource();
        enricher.doEnrich(resource404, throwable, requestContext, 404);

        // Then: All resources have the same transaction ID
        assertThat(resource400.getTxId(), is(equalTo(txUuid.toString())));
        assertThat(resource500.getTxId(), is(equalTo(txUuid.toString())));
        assertThat(resource404.getTxId(), is(equalTo(txUuid.toString())));
    }
}
