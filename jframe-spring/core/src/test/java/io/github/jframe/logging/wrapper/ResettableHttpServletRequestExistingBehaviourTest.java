package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * EXISTING BEHAVIOUR tests for {@link ResettableHttpServletRequest}.
 *
 * <p>These tests characterise the current correct behaviour that must be preserved
 * by the fix. They compile and pass today.
 *
 * <p>See {@code ResettableHttpServletRequestBodyCapTest} for the DRIVES FIX tests
 * (compilation blockers that define the new API the backend agent must add).
 */
@DisplayName("Unit Test - ResettableHttpServletRequest existing correctness")
public class ResettableHttpServletRequestExistingBehaviourTest extends UnitTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private static ServletInputStream servletInputStreamOf(final byte[] bytes) {
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return new ServletInputStream() {

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(final ReadListener listener) {
                // no-op
            }

            @Override
            public int read() throws IOException {
                return bais.read();
            }
        };
    }

    @Test
    @DisplayName("Should return non-null input stream after wrapping request with body")
    public void shouldReturnNonNullInputStreamAfterWrappingRequestWithBody() throws IOException {
        // Given: A request containing a JSON body
        final byte[] bodyBytes = "{\"name\":\"alice\"}".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        final ResettableHttpServletRequest wrapper = new ResettableHttpServletRequest(request, response);

        // When: Reading the input stream
        final byte[] read = wrapper.getInputStream().readAllBytes();

        // Then: Full body is available
        assertThat(read, is(notNullValue()));
        assertThat(new String(read, StandardCharsets.UTF_8), is(equalTo("{\"name\":\"alice\"}")));
    }

    @Test
    @DisplayName("Should allow stream to be reset and re-read in full")
    public void shouldAllowStreamToBeResetAndReReadInFull() throws IOException {
        // Given: A request with a small body
        final byte[] bodyBytes = "hello".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        final ResettableHttpServletRequest wrapper = new ResettableHttpServletRequest(request, response);

        // When: Reading, resetting, reading again
        final byte[] firstRead = wrapper.getInputStream().readAllBytes();
        wrapper.reset();
        final byte[] secondRead = wrapper.getInputStream().readAllBytes();

        // Then: Both reads return the complete body
        assertThat(new String(firstRead, StandardCharsets.UTF_8), is(equalTo("hello")));
        assertThat(new String(secondRead, StandardCharsets.UTF_8), is(equalTo("hello")));
    }

    @Test
    @DisplayName("Should handle empty body without throwing")
    public void shouldHandleEmptyBodyWithoutThrowing() throws IOException {
        // Given: A request with an empty body
        when(request.getInputStream()).thenReturn(servletInputStreamOf(new byte[0]));

        final ResettableHttpServletRequest wrapper = new ResettableHttpServletRequest(request, response);

        // When: Reading the stream
        final byte[] read = wrapper.getInputStream().readAllBytes();

        // Then: No exception, result is empty
        assertThat(read.length, is(equalTo(0)));
    }
}
