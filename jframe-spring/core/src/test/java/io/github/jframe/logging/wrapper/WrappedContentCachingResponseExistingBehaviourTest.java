package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * EXISTING BEHAVIOUR tests for {@link WrappedContentCachingResponse}.
 *
 * <p>These tests characterise the current correct behaviour preserved by the fix.
 * They compile and pass today.
 *
 * <p>See {@code WrappedContentCachingResponseBodyCapTest} for DRIVES FIX tests.
 */
@DisplayName("Unit Test - WrappedContentCachingResponse existing correctness")
public class WrappedContentCachingResponseExistingBehaviourTest extends UnitTest {

    @Test
    @DisplayName("Should cache full body with existing no-cap constructor")
    public void shouldCacheFullBodyWithExistingNoCap() throws IOException {
        // Given: A response wrapper using the existing single-arg constructor
        final MockHttpServletResponse underlying = new MockHttpServletResponse();
        final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);

        // When: Application writes 1 KB to the response
        final byte[] body = "A".repeat(1_000).getBytes(StandardCharsets.UTF_8);
        wrapper.getOutputStream().write(body);

        // Then: getContentAsByteArray() returns the full 1 KB
        assertThat(wrapper.getContentAsByteArray().length, is(equalTo(1_000)));
    }

    @Test
    @DisplayName("Should preserve redirect flag with existing constructor")
    public void shouldPreserveRedirectFlagWithExistingConstructor() throws IOException {
        // Given: A wrapper using the existing single-arg constructor
        final MockHttpServletResponse underlying = new MockHttpServletResponse();
        final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);

        // When: sendRedirect is called
        wrapper.sendRedirect("/other");

        // Then: redirect flag is set correctly
        assertThat(wrapper.isRedirect(), is(true));
    }

    @Test
    @DisplayName("Should return empty byte array for empty response")
    public void shouldReturnEmptyByteArrayForEmptyResponse() {
        // Given: A wrapper with no body written
        final MockHttpServletResponse underlying = new MockHttpServletResponse();
        final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);

        // When: Content is retrieved without writing
        final byte[] content = wrapper.getContentAsByteArray();

        // Then: No exception, empty array
        assertThat(content.length, is(equalTo(0)));
    }
}
