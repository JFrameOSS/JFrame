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
 * EXISTING BEHAVIOUR tests for {@link BufferedClientHttpResponse}.
 *
 * <p>Compile and pass today. See {@link BufferedClientHttpResponseBodyCapTest}
 * for DRIVES FIX tests (compilation blockers requiring {@code getCappedBodyAsString(int)}).
 */
@DisplayName("Unit Test - BufferedClientHttpResponse existing correctness")
public class BufferedClientHttpResponseExistingBehaviourTest extends UnitTest {

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
    @DisplayName("Should return full body as string from getBodyAsString")
    public void shouldReturnFullBodyAsStringFromGetBodyAsString() throws IOException {
        // Given: A 1 KB response body
        final String expected = "A".repeat(1_000);
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(expected.getBytes(StandardCharsets.UTF_8)));

        // When: Reading body as string (no cap)
        final String result = response.getBodyAsString();

        // Then: Full body returned
        assertThat(result, is(equalTo(expected)));
    }

    @Test
    @DisplayName("Should return full body from getBody() after getBodyAsString has been called")
    public void shouldReturnFullBodyFromGetBodyAfterStringReadHasBeenCalled() throws IOException {
        // Given: A 50-byte body
        final byte[] bodyBytes = "B".repeat(50).getBytes(StandardCharsets.UTF_8);
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(bodyBytes));

        // When: Body is read as string, then raw stream is read
        response.getBodyAsString();
        final byte[] streamBody = response.getBody().readAllBytes();

        // Then: Full body available via getBody() — wrapper buffers so stream can be re-read
        assertThat(streamBody.length, is(equalTo(50)));
    }

    @Test
    @DisplayName("Should handle empty body without throwing")
    public void shouldHandleEmptyBodyWithoutThrowing() throws IOException {
        // Given: An empty response body
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(new byte[0]));

        // When: Reading body as string
        final String result = response.getBodyAsString();

        // Then: No exception, empty string
        assertThat(result, is(notNullValue()));
        assertThat(result.length(), is(equalTo(0)));
    }

    @Test
    @DisplayName("Should return same string on repeated getBodyAsString calls")
    public void shouldReturnSameStringOnRepeatedGetBodyAsStringCalls() throws IOException {
        // Given: A response with a body
        final String expected = "C".repeat(100);
        final BufferedClientHttpResponse response =
            new BufferedClientHttpResponse(delegateWith(expected.getBytes(StandardCharsets.UTF_8)));

        // When: getBodyAsString called twice
        final String first = response.getBodyAsString();
        final String second = response.getBodyAsString();

        // Then: Both calls return the same string
        assertThat(first, is(equalTo(expected)));
        assertThat(second, is(equalTo(expected)));
    }
}
