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
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * DRIVES FIX — tests for the new cap-aware constructor of {@link CachingRequestContext}.
 *
 * <p><strong>These tests WILL FAIL TO COMPILE</strong> until the backend agent adds:
 * {@code CachingRequestContext(ContainerRequestContext delegate, int byteCap)}
 *
 * <p>Contract: {@code byteCap == -1} → unlimited; {@code byteCap == 0} → empty cached body;
 * {@code byteCap > 0} → cached body limited to that many bytes.
 * The delegate's entity stream must ALWAYS be restored with the FULL body so the JAX-RS
 * handler still reads the complete request.
 *
 * <p>See {@link CachingRequestContextExistingBehaviourTest} for tests that compile today.
 */
@DisplayName("DRIVES FIX - CachingRequestContext cap-aware constructor")
public class CachingRequestContextBodyCapTest extends UnitTest {

    @Mock
    private ContainerRequestContext delegate;

    @Test
    @DisplayName("Should buffer full body when cap is -1 (unlimited)")
    public void shouldBufferFullBodyWhenCapIsUnlimited() throws IOException {
        // Given: A 500-byte body and unlimited cap
        final byte[] body = "E".repeat(500).getBytes(StandardCharsets.UTF_8);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        // When: Constructing with cap = -1
        final CachingRequestContext context = new CachingRequestContext(delegate, -1);

        // Then: Full body cached — unlimited must not truncate
        assertThat(context.getCachedBodyAsString().length(), is(equalTo(500)));
    }

    @Test
    @DisplayName("Should limit cached body to cap bytes when body exceeds cap")
    public void shouldLimitCachedBodyToCapBytesWhenBodyExceedsCap() throws IOException {
        // Given: 200-byte body, cap = 50
        final int cap = 50;
        final byte[] body = "F".repeat(200).getBytes(StandardCharsets.UTF_8);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        // When: Constructing with a byte cap
        final CachingRequestContext context = new CachingRequestContext(delegate, cap);

        // Then: Logged copy is at most cap bytes
        assertThat(context.getCachedBody().length, is(equalTo(cap)));
    }

    @Test
    @DisplayName("Should not truncate cached body when body is smaller than cap")
    public void shouldNotTruncateCachedBodyWhenBodyIsSmallerThanCap() throws IOException {
        // Given: 10-byte body, cap = 500
        final byte[] body = "G".repeat(10).getBytes(StandardCharsets.UTF_8);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        // When: Constructing with generous cap
        final CachingRequestContext context = new CachingRequestContext(delegate, 500);

        // Then: Full 10 bytes cached
        assertThat(context.getCachedBody().length, is(equalTo(10)));
    }

    @Test
    @DisplayName("Should not truncate when body length equals cap exactly")
    public void shouldNotTruncateWhenBodyLengthEqualsCapExactly() throws IOException {
        // Given: 5-byte body, cap = 5
        final byte[] body = "exact".getBytes(StandardCharsets.UTF_8);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        // When: Constructing with cap = 5
        final CachingRequestContext context = new CachingRequestContext(delegate, 5);

        // Then: All 5 bytes cached
        assertThat(context.getCachedBody().length, is(equalTo(5)));
    }

    @Test
    @DisplayName("Should produce empty cached body when cap is 0")
    public void shouldProduceEmptyCachedBodyWhenCapIsZero() throws IOException {
        // Given: A non-empty body and cap = 0
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream())
            .thenReturn(new ByteArrayInputStream("secret".getBytes(StandardCharsets.UTF_8)));

        // When: Constructing with cap = 0
        final CachingRequestContext context = new CachingRequestContext(delegate, 0);

        // Then: Logged copy is empty (cap=0 logs nothing)
        assertThat(context.getCachedBody().length, is(equalTo(0)));
    }

    @Test
    @DisplayName("Should restore full entity stream to delegate regardless of cap")
    public void shouldRestoreFullEntityStreamToDelegateRegardlessOfCap() throws IOException {
        // Given: A 100-byte body with cap = 10 (logged copy is capped)
        final byte[] body = "H".repeat(100).getBytes(StandardCharsets.UTF_8);
        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        // When: Constructing with a small cap
        final CachingRequestContext context = new CachingRequestContext(delegate, 10);

        // Then: getEntityStream() for the app returns the FULL body
        final byte[] appBody = context.getEntityStream().readAllBytes();
        assertThat(appBody.length, is(equalTo(100)));
    }

    @Test
    @DisplayName("Should not throw when cap splits a multi-byte UTF-8 character")
    public void shouldNotThrowWhenCapSplitsMultiByteUtf8Character() throws IOException {
        // Given: "AB€" (5 bytes), cap = 4 (splits '€')
        final byte[] euroSign = "\u20AC".getBytes(StandardCharsets.UTF_8);
        final byte[] body = new byte[5];
        body[0] = 'A';
        body[1] = 'B';
        System.arraycopy(euroSign, 0, body, 2, 3);

        when(delegate.hasEntity()).thenReturn(true);
        when(delegate.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        // When: Constructing with cap = 4 (splits multi-byte char)
        final CachingRequestContext context = new CachingRequestContext(delegate, 4);
        final String result = context.getCachedBodyAsString();

        // Then: No exception; decoded string starts with "AB"
        assertThat(result, is(notNullValue()));
        assertThat(result.startsWith("AB"), is(true));
    }
}
