package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.RequestId;
import io.github.support.UnitTest;

import java.util.UUID;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RequestIdFilter} (Quarkus JAX-RS adapter).
 *
 * <p>Verifies the RequestIdFilter functionality including:
 * <ul>
 * <li>Unique request ID generation for each request</li>
 * <li>Request ID storage in ThreadLocal (RequestId)</li>
 * <li>Request ID addition to response header</li>
 * <li>Filter chain execution</li>
 * <li>ThreadLocal cleanup after test</li>
 * <li>MDC integration via KibanaLogFields</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Filters - Request ID Filter")
public class RequestIdFilterTest extends UnitTest {

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocal and MDC to avoid test pollution
        RequestId.remove();
        KibanaLogFields.clear();
    }

    @Test
    @DisplayName("Should generate and set request ID in ThreadLocal")
    public void shouldGenerateAndSetRequestIdInThreadLocal() throws Exception {
        // Given: A request ID filter and mocked JAX-RS request context
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: Request ID is set in ThreadLocal
        final String requestId = RequestId.get();
        assertThat(requestId, is(notNullValue()));
    }

    @Test
    @DisplayName("Should add request ID to response header")
    public void shouldAddRequestIdToResponseHeader() throws Exception {
        // Given: A request ID filter with mocked request/response contexts
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        // And: Request has already been processed (ID set in ThreadLocal)
        filter.filter(requestContext);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: Request ID is added to response headers
        assertThat(headers.containsKey(REQ_ID_HEADER), is(true));
    }

    @Test
    @DisplayName("Should not add request ID to response header if already present")
    public void shouldNotAddRequestIdToResponseHeaderIfAlreadyPresent() throws Exception {
        // Given: A request ID filter, response already has the header
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(REQ_ID_HEADER, "existing-request-id");
        when(responseContext.getHeaders()).thenReturn(headers);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: Response header count for key remains 1 (not duplicated)
        assertThat(headers.get(REQ_ID_HEADER).size(), is(1));
    }

    @Test
    @DisplayName("Should generate unique request IDs for different requests")
    public void shouldGenerateUniqueRequestIdsForDifferentRequests() throws Exception {
        // Given: A request ID filter and two separate request contexts
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final ContainerRequestContext requestContext1 = mock(ContainerRequestContext.class);

        // When: First filter execution
        filter.filter(requestContext1);
        final String requestId1 = RequestId.get();
        RequestId.remove();

        // And: Second filter execution
        final ContainerRequestContext requestContext2 = mock(ContainerRequestContext.class);
        filter.filter(requestContext2);
        final String requestId2 = RequestId.get();

        // Then: Request IDs are unique
        assertThat(requestId1, is(notNullValue()));
        assertThat(requestId2, is(notNullValue()));
        assertThat(requestId1, is(not(requestId2)));
    }

    @Test
    @DisplayName("Should generate a valid UUID as request ID")
    public void shouldGenerateValidUuidAsRequestId() throws Exception {
        // Given: A request ID filter and mocked request context
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Filter processes the request
        filter.filter(requestContext);

        // Then: The generated request ID is a valid UUID
        final String requestId = RequestId.get();
        assertThat(requestId, is(notNullValue()));
        final UUID parsed = UUID.fromString(requestId);
        assertThat(parsed, is(notNullValue()));
    }

    @Test
    @DisplayName("Should set req_id MDC field when filtering request")
    public void shouldSetRequestIdMdcFieldWhenFilteringRequest() throws Exception {
        // Given: A request ID filter and mocked JAX-RS request context
        final RequestIdFilter filter = new RequestIdFilter(REQ_ID_HEADER);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: MDC field req_id matches the request ID stored in ThreadLocal
        final String threadLocalRequestId = RequestId.get();
        final String mdcRequestId = KibanaLogFields.get(REQUEST_ID);
        assertThat(mdcRequestId, is(notNullValue()));
        assertThat(mdcRequestId, is(equalTo(threadLocalRequestId)));
    }
}
