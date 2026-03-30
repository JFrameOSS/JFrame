package io.github.jframe.logging.filter.client;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.filter.FilterConfig;
import io.github.support.UnitTest;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static io.github.jframe.logging.ecs.EcsFieldNames.REQUEST_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TRACE_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OutboundCorrelationFilter}.
 *
 * <p>Verifies the {@code ClientRequestFilter} that propagates correlation headers
 * from MDC (via {@link EcsFields}) to outbound HTTP calls, including:
 * <ul>
 * <li>Adding x-transaction-id, x-request-id and x-trace-id headers when MDC values are present</li>
 * <li>Skipping headers when MDC values are null or empty</li>
 * <li>Not overwriting existing headers that are already set on the outbound request</li>
 * </ul>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Unit Test - Outbound Correlation Filter")
public class OutboundCorrelationFilterTest extends UnitTest {

    @Mock
    private ClientRequestContext clientRequestContext;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private FilterConfig.OutboundCorrelationConfig outboundCorrelationConfig;

    @BeforeEach
    public void setUp() {
        lenient().when(filterConfig.outboundCorrelation()).thenReturn(outboundCorrelationConfig);
        lenient().when(outboundCorrelationConfig.enabled()).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        EcsFields.clear();
    }

    // ─── Header propagation: all MDC values present ──────────────────────────

    @Nested
    @DisplayName("When MDC values are present")
    class WhenMdcValuesArePresent {

        @Test
        @DisplayName("Should add x-transaction-id header from MDC when present")
        public void shouldAddTransactionIdHeaderWhenMdcValueIsPresent() throws Exception {
            // Given: MDC contains a transaction ID and an empty outbound headers map
            EcsFields.tag(TX_ID, "test-tx-id");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: x-transaction-id header is added with the MDC value
            assertThat(headers.containsKey(TX_ID_HEADER), is(true));
            assertThat(headers.getFirst(TX_ID_HEADER), is(equalTo("test-tx-id")));
        }

        @Test
        @DisplayName("Should add x-request-id header from MDC when present")
        public void shouldAddRequestIdHeaderWhenMdcValueIsPresent() throws Exception {
            // Given: MDC contains a request ID and an empty outbound headers map
            EcsFields.tag(REQUEST_ID, "test-req-id");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: x-request-id header is added with the MDC value
            assertThat(headers.containsKey(REQ_ID_HEADER), is(true));
            assertThat(headers.getFirst(REQ_ID_HEADER), is(equalTo("test-req-id")));
        }

        @Test
        @DisplayName("Should add x-trace-id header from MDC when present")
        public void shouldAddTraceIdHeaderWhenMdcValueIsPresent() throws Exception {
            // Given: MDC contains a trace ID and an empty outbound headers map
            EcsFields.tag(TRACE_ID, "0123456789abcdef0123456789abcdef");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: x-trace-id header is added with the MDC value
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(true));
            assertThat(headers.getFirst(TRACE_ID_HEADER), is(equalTo("0123456789abcdef0123456789abcdef")));
        }

        @Test
        @DisplayName("Should add all three correlation headers when all MDC values are present")
        public void shouldAddAllThreeCorrelationHeadersWhenAllMdcValuesArePresent() throws Exception {
            // Given: MDC is fully populated with all three correlation IDs
            EcsFields.tag(TX_ID, "tx-111");
            EcsFields.tag(REQUEST_ID, "req-222");
            EcsFields.tag(TRACE_ID, "0123456789abcdef0123456789abcdef");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: All three headers are added
            assertThat(headers.containsKey(TX_ID_HEADER), is(true));
            assertThat(headers.containsKey(REQ_ID_HEADER), is(true));
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(true));
        }
    }

    // ─── Header propagation: null / empty MDC values ─────────────────────────


    @Nested
    @DisplayName("When MDC values are absent")
    class WhenMdcValuesAreAbsent {

        @Test
        @DisplayName("Should NOT add x-transaction-id header when MDC value is null")
        public void shouldNotAddTransactionIdHeaderWhenMdcValueIsNull() throws Exception {
            // Given: MDC does not contain a transaction ID (null)
            // EcsFields deliberately NOT populated for TX_ID
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: x-transaction-id header is NOT added
            assertThat(headers.containsKey(TX_ID_HEADER), is(false));
        }

        @Test
        @DisplayName("Should NOT add x-request-id header when MDC value is null")
        public void shouldNotAddRequestIdHeaderWhenMdcValueIsNull() throws Exception {
            // Given: MDC does not contain a request ID (null)
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: x-request-id header is NOT added
            assertThat(headers.containsKey(REQ_ID_HEADER), is(false));
        }

        @Test
        @DisplayName("Should NOT add x-trace-id header when MDC value is null")
        public void shouldNotAddTraceIdHeaderWhenMdcValueIsNull() throws Exception {
            // Given: MDC does not contain a trace ID (null)
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: x-trace-id header is NOT added
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(false));
        }

        @Test
        @DisplayName("Should NOT add any headers when all MDC values are absent")
        public void shouldNotAddAnyHeadersWhenAllMdcValuesAreAbsent() throws Exception {
            // Given: MDC is completely empty (no correlation IDs)
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: No correlation headers are added
            assertThat(headers.containsKey(TX_ID_HEADER), is(false));
            assertThat(headers.containsKey(REQ_ID_HEADER), is(false));
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(false));
        }

        @Test
        @DisplayName("Should NOT add x-transaction-id header when MDC value is blank")
        public void shouldNotAddTransactionIdHeaderWhenMdcValueIsBlank() throws Exception {
            // Given: MDC contains a blank (whitespace-only) transaction ID
            // EcsFields.tag ignores blank values (does a clear instead)
            EcsFields.tag(TX_ID, "   ");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: x-transaction-id header is NOT added (blank is treated as absent)
            assertThat(headers.containsKey(TX_ID_HEADER), is(false));
        }
    }

    // ─── Existing headers must NOT be overwritten ─────────────────────────────


    @Nested
    @DisplayName("When outbound request already has correlation headers")
    class WhenHeadersAlreadyPresent {

        @Test
        @DisplayName("Should NOT overwrite existing x-transaction-id header")
        public void shouldNotOverwriteExistingTransactionIdHeader() throws Exception {
            // Given: MDC has a transaction ID and outbound request already carries that header
            EcsFields.tag(TX_ID, "new-tx-id");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putSingle(TX_ID_HEADER, "existing-tx-id");
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: The existing header value is preserved and no duplicate is added
            assertThat(headers.get(TX_ID_HEADER), hasSize(1));
            assertThat(headers.getFirst(TX_ID_HEADER), is(equalTo("existing-tx-id")));
        }

        @Test
        @DisplayName("Should NOT overwrite existing x-request-id header")
        public void shouldNotOverwriteExistingRequestIdHeader() throws Exception {
            // Given: MDC has a request ID and outbound request already carries that header
            EcsFields.tag(REQUEST_ID, "new-req-id");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putSingle(REQ_ID_HEADER, "existing-req-id");
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: The existing header value is preserved
            assertThat(headers.get(REQ_ID_HEADER), hasSize(1));
            assertThat(headers.getFirst(REQ_ID_HEADER), is(equalTo("existing-req-id")));
        }

        @Test
        @DisplayName("Should NOT overwrite existing x-trace-id header")
        public void shouldNotOverwriteExistingTraceIdHeader() throws Exception {
            // Given: MDC has a trace ID and outbound request already carries that header
            EcsFields.tag(TRACE_ID, "0123456789abcdef0123456789abcdef");
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putSingle(TRACE_ID_HEADER, "existing-trace-id");
            when(clientRequestContext.getHeaders()).thenReturn(headers);
            final OutboundCorrelationFilter filter = new OutboundCorrelationFilter(filterConfig);

            // When: Filter processes the outbound request
            filter.filter(clientRequestContext);

            // Then: The existing header value is preserved
            assertThat(headers.get(TRACE_ID_HEADER), hasSize(1));
            assertThat(headers.getFirst(TRACE_ID_HEADER), is(equalTo("existing-trace-id")));
        }
    }
}
