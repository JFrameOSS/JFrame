package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.support.UnitTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ErrorTransactionIdEnricher}.
 *
 * <p>Verifies the ErrorTransactionIdEnricher functionality including:
 * <ul>
 * <li>Transaction ID enrichment from MDC</li>
 * <li>Handling of null transaction ID</li>
 * <li>Support for different transaction ID values</li>
 * </ul>
 * </p>
 */
@DisplayName("Exception Response Enrichers - Error Transaction ID Enricher")
public class ErrorTransactionIdEnricherTest extends UnitTest {

    private final ErrorTransactionIdEnricher enricher = new ErrorTransactionIdEnricher();

    @AfterEach
    public void tearDown() {
        KibanaLogFields.clear(TX_ID);
    }

    @Test
    @DisplayName("Should enrich transaction ID from MDC")
    public void shouldEnrichTransactionIdFromMdc() {
        // Given: An error response resource and transaction ID set in MDC
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);
        final String transactionId = "tx-12345";
        KibanaLogFields.tag(TX_ID, transactionId);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Transaction ID is set from MDC
        assertThat(resource.getTxId(), is(equalTo(transactionId)));
    }

    @Test
    @DisplayName("Should enrich with null when transaction ID is not set in MDC")
    public void shouldEnrichWithNullWhenTransactionIdIsNotSetInMdc() {
        // Given: An error response resource with no transaction ID in MDC
        final ErrorResponseResource resource = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching the response
        enricher.doEnrich(resource, throwable, request, HttpStatus.BAD_REQUEST);

        // Then: Transaction ID is null
        assertThat(resource.getTxId(), is(nullValue()));
    }

    @Test
    @DisplayName("Should enrich with different transaction ID values")
    public void shouldEnrichWithDifferentTransactionIdValues() {
        // Given: An error response resource and a UUID-style transaction ID
        final ErrorResponseResource resource1 = new ErrorResponseResource();
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);
        final String uuidTransactionId = "550e8400-e29b-41d4-a716-446655440000";
        KibanaLogFields.tag(TX_ID, uuidTransactionId);

        // When: Enriching the response
        enricher.doEnrich(resource1, throwable, request, HttpStatus.NOT_FOUND);

        // Then: Transaction ID is correctly set
        assertThat(resource1.getTxId(), is(equalTo(uuidTransactionId)));

        // Given: A different transaction ID format
        final ErrorResponseResource resource2 = new ErrorResponseResource();
        final String shortTransactionId = "abc-123";
        KibanaLogFields.tag(TX_ID, shortTransactionId);

        // When: Enriching another response
        enricher.doEnrich(resource2, throwable, request, HttpStatus.UNAUTHORIZED);

        // Then: New transaction ID is correctly set
        assertThat(resource2.getTxId(), is(equalTo(shortTransactionId)));
    }

    @Test
    @DisplayName("Should enrich transaction ID regardless of HTTP status")
    public void shouldEnrichTransactionIdRegardlessOfHttpStatus() {
        // Given: An error response resource with transaction ID in MDC
        final String transactionId = "tx-status-test";
        KibanaLogFields.tag(TX_ID, transactionId);
        final Throwable throwable = new RuntimeException();
        final WebRequest request = mock(WebRequest.class);

        // When: Enriching with BAD_REQUEST status
        final ErrorResponseResource badRequestResource = new ErrorResponseResource();
        enricher.doEnrich(badRequestResource, throwable, request, HttpStatus.BAD_REQUEST);

        // Then: Transaction ID is set
        assertThat(badRequestResource.getTxId(), is(equalTo(transactionId)));

        // When: Enriching with INTERNAL_SERVER_ERROR status
        final ErrorResponseResource serverErrorResource = new ErrorResponseResource();
        enricher.doEnrich(serverErrorResource, throwable, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Transaction ID is set
        assertThat(serverErrorResource.getTxId(), is(equalTo(transactionId)));

        // When: Enriching with NOT_FOUND status
        final ErrorResponseResource notFoundResource = new ErrorResponseResource();
        enricher.doEnrich(notFoundResource, throwable, request, HttpStatus.NOT_FOUND);

        // Then: Transaction ID is set
        assertThat(notFoundResource.getTxId(), is(equalTo(transactionId)));
    }
}
