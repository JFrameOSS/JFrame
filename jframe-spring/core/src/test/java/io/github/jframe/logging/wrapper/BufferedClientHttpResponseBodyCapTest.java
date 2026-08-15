package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * DRIVES FIX — tests for the new {@code getCappedBodyAsString(int)} method on
 * {@link BufferedClientHttpResponse}.
 *
 * <p><strong>These tests WILL FAIL TO COMPILE</strong> until the backend agent adds:
 * {@code String getCappedBodyAsString(int maxBytes) throws IOException}
 *
 * <p>See {@link BufferedClientHttpResponseExistingBehaviourTest} for tests that
 * compile and pass today.
 */
@DisplayName("DRIVES FIX - BufferedClientHttpResponse getCappedBodyAsString")
public class BufferedClientHttpResponseBodyCapTest extends UnitTest {

    private static ClientHttpResponse delegateWith(final byte[] body) {
        return new ClientHttpResponse() {

            @Override
            public HttpStatusCode getStatusCode() {
                return HttpStatus.OK;
            }

            @Override
            public String getStatusText() {
                return "OK";
            }

            @Override
            public HttpHeaders getHeaders() {
                return HttpHeaders.EMPTY;
            }

            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public void close() {
                // no-op
            }
        };
    }

    @Test
    @DisplayName("Should return full body when cap is -1 (unlimited)")
    public void shouldReturnFullBodyWhenCapIsUnlimited() throws IOException {
        // Given: A 1 KB body and unlimited cap
        final String expected = "D".repeat(1_000);
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(expected.getBytes(StandardCharsets.UTF_8)));

        // When: Reading body with cap = -1 (unlimited)
        final String result = response.getCappedBodyAsString(-1);

        // Then: Full body returned — unlimited must never truncate
        assertThat(result, is(equalTo(expected)));
    }

    @Test
    @DisplayName("Should return only cap bytes decoded when body exceeds cap")
    public void shouldReturnOnlyCapBytesDecodedWhenBodyExceedsCap() throws IOException {
        // Given: A 500-byte body and cap = 50
        final int cap = 50;
        final String fullBody = "E".repeat(500);
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(fullBody.getBytes(StandardCharsets.UTF_8)));

        // When: Reading capped body
        final String result = response.getCappedBodyAsString(cap);

        // Then: Exactly cap chars returned
        assertThat(result, is(equalTo("E".repeat(cap))));
    }

    @Test
    @DisplayName("Should not truncate when body is smaller than cap")
    public void shouldNotTruncateWhenBodyIsSmallerThanCap() throws IOException {
        // Given: A 10-byte body and cap = 500
        final String shortBody = "F".repeat(10);
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(shortBody.getBytes(StandardCharsets.UTF_8)));

        // When: Reading capped body
        final String result = response.getCappedBodyAsString(500);

        // Then: Full 10-char body returned, no padding
        assertThat(result, is(equalTo(shortBody)));
    }

    @Test
    @DisplayName("Should not truncate when body length equals cap exactly")
    public void shouldNotTruncateWhenBodyLengthEqualsCapExactly() throws IOException {
        // Given: A 5-byte body and cap = 5
        final String body = "EXACT";
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(body.getBytes(StandardCharsets.UTF_8)));

        // When: Reading capped body
        final String result = response.getCappedBodyAsString(5);

        // Then: All 5 chars included
        assertThat(result, is(equalTo("EXACT")));
    }

    @Test
    @DisplayName("Should produce empty string when cap is 0")
    public void shouldProduceEmptyStringWhenCapIsZero() throws IOException {
        // Given: A non-empty body and cap = 0
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith("data".getBytes(StandardCharsets.UTF_8)));

        // When: Reading capped body
        final String result = response.getCappedBodyAsString(0);

        // Then: Empty string returned (cap=0 logs nothing)
        assertThat(result.length(), is(equalTo(0)));
    }

    @Test
    @DisplayName("Should still deliver full body via getBody() when capped string read occurs")
    public void shouldDeliverFullBodyViaGetBodyWhenCappedStringReadOccurs() throws IOException {
        // Given: A 200-byte body with cap = 10 on the string read
        final byte[] fullBytes = "G".repeat(200).getBytes(StandardCharsets.UTF_8);
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(fullBytes));

        // When: Capped string read (logging), then stream read (interceptor return path)
        response.getCappedBodyAsString(10);
        final byte[] streamResult = response.getBody().readAllBytes();

        // Then: Client still receives all 200 bytes
        assertThat(streamResult.length, is(equalTo(200)));
    }

    @Test
    @DisplayName("Should not throw when cap splits a multi-byte UTF-8 character")
    public void shouldNotThrowWhenCapSplitsMultiByteUtf8Character() throws IOException {
        // Given: Body "AB€CD" (7 bytes), cap = 4 (cuts through 3-byte '€')
        final byte[] euroSign = "\u20AC".getBytes(StandardCharsets.UTF_8); // 3 bytes
        final byte[] bodyBytes = new byte[7];
        bodyBytes[0] = 'A';
        bodyBytes[1] = 'B';
        System.arraycopy(euroSign, 0, bodyBytes, 2, 3);
        bodyBytes[5] = 'C';
        bodyBytes[6] = 'D';

        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(bodyBytes));

        // When: Capped read where cap splits a multi-byte char
        final String result = response.getCappedBodyAsString(4);

        // Then: No exception; result starts with "AB"; partial char handled gracefully
        assertThat(result, is(notNullValue()));
        assertThat(result.startsWith("AB"), is(true));
    }
}
