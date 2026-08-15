package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * DRIVES FIX — tests for the new cap-aware constructor of {@link CachingResponseContext}.
 *
 * <p><strong>These tests WILL FAIL TO COMPILE</strong> until the backend agent adds:
 * {@code CachingResponseContext(ContainerResponseContext delegate, int byteCap)}
 *
 * <p>Contract: cap is enforced inside {@code setCachedBody(byte[])} — only up to {@code byteCap}
 * bytes are stored. The caller is still responsible for streaming the full body to the client.
 *
 * <p>See {@link CachingResponseContextExistingBehaviourTest} for tests that compile today.
 */
@DisplayName("DRIVES FIX - CachingResponseContext cap-aware constructor")
public class CachingResponseContextBodyCapTest extends UnitTest {

    @Test
    @DisplayName("Should store full body when cap is -1 (unlimited)")
    public void shouldStoreFullBodyWhenCapIsUnlimited() {
        // Given: A 500-byte body and unlimited cap
        final byte[] body = "D".repeat(500).getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context =
            new CachingResponseContext(new CachingResponseContextExistingBehaviourTest.StubContainerResponseContext(), -1);

        // When: Storing and retrieving
        context.setCachedBody(body);

        // Then: Full 500 bytes stored — unlimited must not truncate
        assertThat(context.getCachedBody().length, is(equalTo(500)));
    }

    @Test
    @DisplayName("Should store only cap bytes when full body exceeds cap")
    public void shouldStoreOnlyCapBytesWhenFullBodyExceedsCap() {
        // Given: A 200-byte body and cap = 50
        final int cap = 50;
        final byte[] body = "E".repeat(200).getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context =
            new CachingResponseContext(new CachingResponseContextExistingBehaviourTest.StubContainerResponseContext(), cap);

        // When: Full body passed to setCachedBody
        context.setCachedBody(body);

        // Then: Only cap bytes stored
        assertThat(context.getCachedBody().length, is(equalTo(cap)));
    }

    @Test
    @DisplayName("Should not truncate when stored body is smaller than cap")
    public void shouldNotTruncateWhenStoredBodyIsSmallerThanCap() {
        // Given: A 10-byte body and cap = 500
        final byte[] body = "F".repeat(10).getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context =
            new CachingResponseContext(new CachingResponseContextExistingBehaviourTest.StubContainerResponseContext(), 500);

        // When: Storing and retrieving
        context.setCachedBody(body);

        // Then: Full 10 bytes stored
        assertThat(context.getCachedBody().length, is(equalTo(10)));
    }

    @Test
    @DisplayName("Should not truncate when body length equals cap exactly")
    public void shouldNotTruncateWhenBodyLengthEqualsCapExactly() {
        // Given: A 5-byte body and cap = 5
        final byte[] body = "exact".getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context =
            new CachingResponseContext(new CachingResponseContextExistingBehaviourTest.StubContainerResponseContext(), 5);

        // When: Storing and retrieving
        context.setCachedBody(body);

        // Then: All 5 bytes stored
        assertThat(context.getCachedBody().length, is(equalTo(5)));
    }

    @Test
    @DisplayName("Should store empty body when cap is 0")
    public void shouldStoreEmptyBodyWhenCapIsZero() {
        // Given: A non-empty body and cap = 0
        final byte[] body = "secret".getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context =
            new CachingResponseContext(new CachingResponseContextExistingBehaviourTest.StubContainerResponseContext(), 0);

        // When: Storing the body
        context.setCachedBody(body);

        // Then: Zero bytes stored (cap=0 logs nothing)
        assertThat(context.getCachedBody().length, is(equalTo(0)));
    }

    @Test
    @DisplayName("Should decode capped bytes to a valid string for multi-byte character splits")
    public void shouldDecodeCappedBytesWithoutThrowingForMultiByteSplits() {
        // Given: "AB€" (5 bytes), cap = 4 (splits 3-byte '€')
        final byte[] euroSign = "\u20AC".getBytes(StandardCharsets.UTF_8);
        final byte[] body = new byte[5];
        body[0] = 'A';
        body[1] = 'B';
        System.arraycopy(euroSign, 0, body, 2, 3);

        final CachingResponseContext context =
            new CachingResponseContext(new CachingResponseContextExistingBehaviourTest.StubContainerResponseContext(), 4);
        context.setCachedBody(body);

        // When: Decoding stored bytes to a string
        final String decoded = context.getCachedBodyAsString();

        // Then: No exception; starts with "AB"
        assertThat(decoded, is(notNullValue()));
        assertThat(decoded.startsWith("AB"), is(true));
    }
}
