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
 * DRIVES FIX — contract tests for {@link ResettableHttpServletRequest} cap-aware API.
 *
 * <p><strong>Status: COMPILE BLOCKER</strong> until the backend agent adds:
 * <ol>
 * <li>{@code ResettableHttpServletRequest(HttpServletRequest, HttpServletResponse, int byteCap)}</li>
 * <li>{@code byte[] getCachedBodyForLogging()}</li>
 * </ol>
 * Once those are added, all tests in this file should pass (none require production logic beyond
 * the new constructor and accessor).
 *
 * <p>The tests are intentionally left in compilation-blocker state. The backend agent should
 * add the new API to make this file compile, and then all tests will pass (green TDD cycle).
 *
 * <p>See {@link ResettableHttpServletRequestExistingBehaviourTest} for tests that compile today.
 *
 * <p><strong>Decisions encoded by these tests:</strong>
 * <ul>
 * <li>cap = -1 → unlimited (no truncation, existing default preserved)</li>
 * <li>cap = 0 → empty logged copy (nothing logged)</li>
 * <li>cap > 0 → logged copy limited to cap bytes</li>
 * <li>getInputStream() ALWAYS returns the full body regardless of cap</li>
 * <li>Multi-byte UTF-8 chars split at cap boundary → partial char is silently dropped; no exception</li>
 * </ul>
 */
@DisplayName("DRIVES FIX - ResettableHttpServletRequest cap-aware API")
public class ResettableHttpServletRequestBodyCapTest extends UnitTest {

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

    // NOTE TO BACKEND AGENT:
    // All tests below use new ResettableHttpServletRequest(request, response, int) and
    // getCachedBodyForLogging(). These will compile once you add:
    //   public ResettableHttpServletRequest(HttpServletRequest request, HttpServletResponse response, int byteCap)
    //   public byte[] getCachedBodyForLogging()
    // The implementation must: read the full body from the delegate stream, store a capped slice
    // for logging, and always return the full body from getInputStream().

    @Test
    @DisplayName("Should cache full body when cap is -1 (unlimited default)")
    public void shouldCacheFullBodyWhenCapIsUnlimited() throws IOException {
        // Given: A 10 KB body and unlimited cap (default -1)
        final int bodySize = 10_000;
        final byte[] bodyBytes = "X".repeat(bodySize).getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Wrapper constructed with cap = -1 (unlimited)
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, -1);
        final byte[] cachedForLogging = wrapper.getCachedBodyForLogging();

        // Then: Complete body available — default must NOT truncate
        assertThat(cachedForLogging.length, is(equalTo(bodySize)));
    }

    @Test
    @DisplayName("Should produce empty logged copy when cap is 0")
    public void shouldProduceEmptyLoggedCopyWhenCapIsZero() throws IOException {
        // Given: A non-empty body and cap = 0
        final byte[] bodyBytes = "secret".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Wrapper with cap = 0
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, 0);
        final byte[] cachedForLogging = wrapper.getCachedBodyForLogging();

        // Then: Logged copy is empty
        assertThat(cachedForLogging.length, is(equalTo(0)));
    }

    @Test
    @DisplayName("Should truncate logged copy to cap when body exceeds cap")
    public void shouldTruncateLoggedCopyToCapWhenBodyExceedsCap() throws IOException {
        // Given: A 20-byte body and cap = 10
        final int cap = 10;
        final byte[] bodyBytes = "ABCDEFGHIJKLMNOPQRST".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Wrapper with cap = 10
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, cap);
        final byte[] cachedForLogging = wrapper.getCachedBodyForLogging();

        // Then: Exactly cap bytes captured
        assertThat(cachedForLogging.length, is(equalTo(cap)));
        assertThat(new String(cachedForLogging, StandardCharsets.UTF_8), is(equalTo("ABCDEFGHIJ")));
    }

    @Test
    @DisplayName("Should not truncate logged copy when body is smaller than cap")
    public void shouldNotTruncateLoggedCopyWhenBodyIsSmallerThanCap() throws IOException {
        // Given: A 5-byte body and cap = 100
        final byte[] bodyBytes = "hello".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Wrapper with generous cap
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, 100);
        final byte[] cachedForLogging = wrapper.getCachedBodyForLogging();

        // Then: Full body, no truncation, no padding
        assertThat(cachedForLogging.length, is(equalTo(5)));
        assertThat(new String(cachedForLogging, StandardCharsets.UTF_8), is(equalTo("hello")));
    }

    @Test
    @DisplayName("Should not truncate when body length equals cap exactly")
    public void shouldNotTruncateWhenBodyLengthEqualsCapExactly() throws IOException {
        // Given: 5-byte body, cap = 5
        final byte[] bodyBytes = "exact".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Wrapper with cap = 5
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, 5);
        final byte[] cachedForLogging = wrapper.getCachedBodyForLogging();

        // Then: All 5 bytes included
        assertThat(cachedForLogging.length, is(equalTo(5)));
    }

    @Test
    @DisplayName("Should truncate when body is cap+1 bytes")
    public void shouldTruncateWhenBodyIsCapPlusOne() throws IOException {
        // Given: 6-byte body, cap = 5
        final byte[] bodyBytes = "sixbyt".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Wrapper with cap = 5
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, 5);
        final byte[] cachedForLogging = wrapper.getCachedBodyForLogging();

        // Then: Only first 5 bytes logged
        assertThat(cachedForLogging.length, is(equalTo(5)));
    }

    @Test
    @DisplayName("Should provide full body to app via getInputStream even when logged copy is capped")
    public void shouldProvideFullBodyToAppViaGetInputStreamEvenWhenLoggedCopyIsCapped() throws IOException {
        // Given: 20-byte body, cap = 5
        final byte[] bodyBytes = "ABCDEFGHIJKLMNOPQRST".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Logging reads capped copy, then app reads stream
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, 5);
        wrapper.getCachedBodyForLogging();
        wrapper.reset();
        final byte[] appBody = wrapper.getInputStream().readAllBytes();

        // Then: App receives the FULL 20 bytes
        assertThat(appBody.length, is(equalTo(20)));
        assertThat(new String(appBody, StandardCharsets.UTF_8), is(equalTo("ABCDEFGHIJKLMNOPQRST")));
    }

    @Test
    @DisplayName("Should not throw when cap splits a multi-byte UTF-8 character")
    public void shouldNotThrowWhenCapSplitsMultiByteUtf8Character() throws IOException {
        // Given: "AB€CD" (7 bytes), cap = 4 (cuts through '€')
        final byte[] euroSign = "\u20AC".getBytes(StandardCharsets.UTF_8);
        final byte[] bodyBytes = new byte[7];
        bodyBytes[0] = 'A';
        bodyBytes[1] = 'B';
        System.arraycopy(euroSign, 0, bodyBytes, 2, 3);
        bodyBytes[5] = 'C';
        bodyBytes[6] = 'D';

        when(request.getInputStream()).thenReturn(servletInputStreamOf(bodyBytes));

        // When: Wrapper with cap = 4
        final ResettableHttpServletRequest wrapper =
            new ResettableHttpServletRequest(request, response, 4);
        final byte[] cachedForLogging = wrapper.getCachedBodyForLogging();
        final String loggedBody = new String(cachedForLogging, StandardCharsets.UTF_8);

        // Then: No exception; decoded string starts with "AB"
        assertThat(loggedBody, is(notNullValue()));
        assertThat(loggedBody.startsWith("AB"), is(true));
    }
}
