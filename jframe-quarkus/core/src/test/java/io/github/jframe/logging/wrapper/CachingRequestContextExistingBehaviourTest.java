package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * EXISTING BEHAVIOUR tests for {@link CachingRequestContext}.
 *
 * <p>These compile and pass today. See {@link CachingRequestContextBodyCapTest}
 * for DRIVES FIX tests (compilation blockers).
 */
@DisplayName("Unit Test - CachingRequestContext existing correctness")
public class CachingRequestContextExistingBehaviourTest extends UnitTest {

    @Mock
    private ContainerRequestContext delegate;

    @Test
    @DisplayName("Should buffer full body with existing constructor (no cap)")
    public void shouldBufferFullBodyWithExistingConstructor() throws IOException {
        // Given: A 500-byte body — existing constructor reads all bytes
        final byte[] body = "A".repeat(500).getBytes(StandardCharsets.UTF_8);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        // When: Using existing constructor
        final CachingRequestContext context = new CachingRequestContext(delegate);

        // Then: Full body cached
        assertThat(context.getCachedBodyAsString().length(), is(equalTo(500)));
    }

    @Test
    @DisplayName("Should return empty body when request has no entity")
    public void shouldReturnEmptyBodyWhenRequestHasNoEntity() throws IOException {
        // Given: A request with no entity
        when(delegate.hasEntity()).thenReturn(false);

        // When: Constructing the context
        final CachingRequestContext context = new CachingRequestContext(delegate);

        // Then: Cached body is empty, no exception
        assertThat(context.getCachedBody().length, is(equalTo(0)));
        assertThat(context.getCachedBodyAsString(), is(equalTo("")));
    }

    @Test
    @DisplayName("Should return stable value across multiple getCachedBodyAsString calls")
    public void shouldReturnStableValueAcrossMultipleGetCachedBodyAsStringCalls() throws IOException {
        // Given: A body of 50 bytes
        final String expected = "C".repeat(50);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream())
            .thenReturn(new ByteArrayInputStream(expected.getBytes(StandardCharsets.UTF_8)));

        final CachingRequestContext context = new CachingRequestContext(delegate);

        // When: getCachedBodyAsString called three times
        final String first = context.getCachedBodyAsString();
        final String second = context.getCachedBodyAsString();
        final String third = context.getCachedBodyAsString();

        // Then: All calls return the same correct value
        assertThat(first, is(equalTo(expected)));
        assertThat(second, is(equalTo(expected)));
        assertThat(third, is(equalTo(expected)));
    }

    /**
     * Regression guard: multiple getCachedBody() calls must return equal byte arrays.
     * The backend agent may optimise the repeated Arrays.copyOf away without changing semantics.
     */
    @Test
    @DisplayName("Should return equal arrays from multiple getCachedBody calls")
    public void shouldReturnEqualArraysFromMultipleGetCachedBodyCalls() throws IOException {
        // Given: A body of 20 bytes
        final byte[] body = "D".repeat(20).getBytes(StandardCharsets.UTF_8);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        final CachingRequestContext context = new CachingRequestContext(delegate);

        // When: getCachedBody called twice
        final byte[] first = context.getCachedBody();
        final byte[] second = context.getCachedBody();

        // Then: Both arrays contain the same data
        assertThat(first, is(equalTo(second)));
    }
}
