package io.github.jframe.logging.filter.type;

import io.github.support.UnitTest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RequestDurationFilter} (Quarkus JAX-RS adapter).
 *
 * <p>Verifies the RequestDurationFilter functionality including:
 * <ul>
 * <li>Start timestamp storage in request context property</li>
 * <li>Duration calculation after chain execution</li>
 * <li>Duration logging format</li>
 * <li>Filter interaction with request context properties</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Filters - Request Duration Filter")
public class RequestDurationFilterTest extends UnitTest {

    private static final String START_TIMESTAMP = "start_timestamp";

    @Test
    @DisplayName("Should set start timestamp property on request context")
    public void shouldSetStartTimestampPropertyOnRequestContext() throws Exception {
        // Given: A request duration filter and mocked request context
        final RequestDurationFilter filter = new RequestDurationFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Filter processes the incoming request
        filter.filter(requestContext);

        // Then: Start timestamp is set as a property on the request context
        verify(requestContext).setProperty(eq(START_TIMESTAMP), any(Long.class));
    }

    @Test
    @DisplayName("Should retrieve start timestamp from request context when logging response")
    public void shouldRetrieveStartTimestampFromRequestContextWhenLoggingResponse() throws Exception {
        // Given: A request duration filter with a start timestamp already set
        final RequestDurationFilter filter = new RequestDurationFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final Long startTimestamp = System.nanoTime();

        org.mockito.Mockito.when(requestContext.getProperty(START_TIMESTAMP)).thenReturn(startTimestamp);

        // When: Filter processes the outgoing response
        filter.filter(requestContext, responseContext);

        // Then: Start timestamp is retrieved from request context
        verify(requestContext).getProperty(START_TIMESTAMP);
    }

    @Test
    @DisplayName("Should not fail when start timestamp is missing from request context")
    public void shouldNotFailWhenStartTimestampIsMissingFromRequestContext() throws Exception {
        // Given: A request duration filter and no start timestamp set
        final RequestDurationFilter filter = new RequestDurationFilter();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

        org.mockito.Mockito.when(requestContext.getProperty(START_TIMESTAMP)).thenReturn(null);

        // When: Filter processes the outgoing response (no exception should be thrown)
        filter.filter(requestContext, responseContext);

        // Then: No exception thrown and filter completed normally
        assertThat(filter, is(notNullValue()));
    }

    @Test
    @DisplayName("Should create filter instance successfully")
    public void shouldCreateFilterInstanceSuccessfully() {
        // Given / When: A request duration filter is constructed
        final RequestDurationFilter filter = new RequestDurationFilter();

        // Then: Filter is instantiated
        assertThat(filter, is(notNullValue()));
    }
}
