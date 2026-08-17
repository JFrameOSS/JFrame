package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link WrappedContentCachingResponse#getContentForLogging()} covering both
 * the {@code getWriter()} path (dominant: JSON via MVC, error pages) and the
 * {@code getOutputStream()} path (binary / streaming).
 *
 * <p>The client always receives the full body via {@link WrappedContentCachingResponse#copyBodyToResponse()}.
 * Only the logging copy is capped.
 */
@DisplayName("Unit Test - WrappedContentCachingResponse cap-aware logging copy")
public class WrappedContentCachingResponseBodyCapTest extends UnitTest {

    // =========================================================================
    // getWriter() path — dominant Spring MVC path (JSON message converters, etc.)
    // =========================================================================

    @Nested
    @DisplayName("getWriter() path")
    class WriterPath {

        @Test
        @DisplayName("Unlimited cap (-1): full body in logging copy")
        public void shouldReturnFullBodyWhenCapIsUnlimitedWriter() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, -1);

            // When: write via getWriter (dominant MVC path)
            final PrintWriter writer = wrapper.getWriter();
            writer.write("A".repeat(200));
            writer.flush();

            // Then: full body available for logging
            assertThat(wrapper.getContentForLogging().length, is(equalTo(200)));
        }

        @Test
        @DisplayName("Cap > body: no spurious truncation")
        public void shouldNotTruncateWhenBodySmallerThanCapWriter() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 500);

            // When
            final PrintWriter writer = wrapper.getWriter();
            writer.write("B".repeat(50));
            writer.flush();

            // Then
            assertThat(wrapper.getContentForLogging().length, is(equalTo(50)));
        }

        @Test
        @DisplayName("Cap < body: logging copy truncated, client receives full body")
        public void shouldTruncateLoggingCopyWhenBodyExceedsCap_Writer() throws IOException {
            // Given
            final int cap = 10;
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, cap);

            // When
            final PrintWriter writer = wrapper.getWriter();
            writer.write("C".repeat(500));
            writer.flush();

            // Then: logging copy is capped
            assertThat(wrapper.getContentForLogging().length, is(equalTo(cap)));

            // And: full body is delivered to client
            wrapper.copyBodyToResponse();
            assertThat(underlying.getContentAsByteArray().length, is(equalTo(500)));
        }

        @Test
        @DisplayName("Cap = 0: empty logging copy, full body still delivered")
        public void shouldReturnEmptyLoggingCopyWhenCapIsZero_Writer() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 0);

            // When
            final PrintWriter writer = wrapper.getWriter();
            writer.write("data");
            writer.flush();

            // Then: logging copy empty
            assertThat(wrapper.getContentForLogging().length, is(equalTo(0)));

            // And: client still receives the full body
            wrapper.copyBodyToResponse();
            assertThat(underlying.getContentAsByteArray().length, is(equalTo(4)));
        }

        @Test
        @DisplayName("Body exactly at cap: no truncation")
        public void shouldNotTruncateWhenBodyLengthEqualsCapExactly_Writer() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 5);

            // When
            final PrintWriter writer = wrapper.getWriter();
            writer.write("EXACT");
            writer.flush();

            // Then
            assertThat(wrapper.getContentForLogging().length, is(equalTo(5)));
        }

        @Test
        @DisplayName("Body at cap+1: logging copy truncated to cap")
        public void shouldTruncateWhenBodyIsCapPlusOne_Writer() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 5);

            // When
            final PrintWriter writer = wrapper.getWriter();
            writer.write("SIXBYT");
            writer.flush();

            // Then
            assertThat(wrapper.getContentForLogging().length, is(equalTo(5)));
        }

        @Test
        @DisplayName("Multi-byte UTF-8 split at cap boundary: no exception")
        public void shouldNotThrowWhenCapSplitsMultiByteUtf8Char_Writer() throws IOException {
            // Given: euro sign = 3 bytes; cap = 2 cuts through it
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 2);

            // When: write "A€B" (5 bytes: A, €×3, B); cap=2 captures "A" + 1st byte of €
            final PrintWriter writer = wrapper.getWriter();
            writer.write("A\u20ACB");
            writer.flush();

            // Then: no exception; logging copy <= 2 bytes
            assertThat(wrapper.getContentForLogging().length, is(equalTo(2)));
        }
    }

    // =========================================================================
    // getOutputStream() path — binary / streaming
    // =========================================================================


    @Nested
    @DisplayName("getOutputStream() path")
    class OutputStreamPath {

        @Test
        @DisplayName("Unlimited cap (-1): full body in logging copy")
        public void shouldReturnFullBodyWhenCapIsUnlimitedStream() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, -1);

            // When
            wrapper.getOutputStream().write("B".repeat(1_000).getBytes(StandardCharsets.UTF_8));

            // Then
            assertThat(wrapper.getContentForLogging().length, is(equalTo(1_000)));
        }

        @Test
        @DisplayName("Cap < body: logging copy truncated, client receives full body")
        public void shouldTruncateLoggingCopyWhenBodyExceedsCap_Stream() throws IOException {
            // Given
            final int cap = 100;
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, cap);

            // When
            wrapper.getOutputStream().write("C".repeat(500).getBytes(StandardCharsets.UTF_8));

            // Then: logging copy is capped
            assertThat(wrapper.getContentForLogging().length, is(equalTo(cap)));

            // And: client receives full body
            wrapper.copyBodyToResponse();
            assertThat(underlying.getContentAsByteArray().length, is(equalTo(500)));
        }

        @Test
        @DisplayName("Cap = 0: empty logging copy, full body still delivered")
        public void shouldReturnEmptyLoggingCopyWhenCapIsZero_Stream() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 0);

            // When
            wrapper.getOutputStream().write("data".getBytes(StandardCharsets.UTF_8));

            // Then: logging copy empty
            assertThat(wrapper.getContentForLogging().length, is(equalTo(0)));

            // And: client receives full body
            wrapper.copyBodyToResponse();
            assertThat(underlying.getContentAsByteArray().length, is(equalTo(4)));
        }

        @Test
        @DisplayName("Body exactly at cap: no truncation")
        public void shouldNotTruncateWhenBodyLengthEqualsCapExactly_Stream() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 10);

            // When
            wrapper.getOutputStream().write("ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8));

            // Then
            assertThat(wrapper.getContentForLogging().length, is(equalTo(10)));
        }

        @Test
        @DisplayName("Redirect flag preserved through cap-aware constructor")
        public void shouldPreserveRedirectFlagThroughCapAwareConstructor() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying, 50);

            // When
            wrapper.sendRedirect("/other");

            // Then
            assertThat(wrapper.isRedirect(), is(true));
        }
    }

    // =========================================================================
    // No-cap constructor (single-arg)
    // =========================================================================


    @Nested
    @DisplayName("No-cap constructor")
    class NoCap {

        @Test
        @DisplayName("getContentForLogging() returns full body when cap is -1")
        public void shouldReturnFullBodyForNoCap() throws IOException {
            // Given
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);

            // When
            wrapper.getOutputStream().write("HELLO".getBytes(StandardCharsets.UTF_8));

            // Then
            assertThat(wrapper.getContentForLogging().length, is(equalTo(5)));
        }
    }
}
