package io.github.jframe.logging.logger;

import io.github.jframe.autoconfigure.properties.LoggingProperties;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.wrapper.BufferedClientHttpResponse;
import io.github.jframe.logging.wrapper.WrappedContentCachingResponse;
import io.github.support.UnitTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for body-size cap enforcement in the four body paths of
 * {@link HttpRequestResponseBodyLogger}:
 * <ol>
 * <li>{@code getTxRequestBody} — inbound servlet request body</li>
 * <li>{@code getTxResponseBody} — inbound servlet response body</li>
 * <li>{@code getCallRequestBody} — outbound client request body</li>
 * <li>{@code getCallResponseBody} — outbound client response body (already correct)</li>
 * </ol>
 *
 * <p><strong>Defect:</strong> only {@code getCallResponseBody} calls
 * {@code HttpBodyUtil.compressBody}; the other three paths mask but never cap.
 *
 * <p>Tests labelled "EXISTING BEHAVIOUR" pass today.
 * Tests labelled "DRIVES FIX" will FAIL (at runtime) because the cap is not applied.
 * Tests referencing the new {@code WrappedContentCachingResponse(response, int)} or
 * {@code ResettableHttpServletRequest(request, response, int)} constructors will also
 * FAIL TO COMPILE until those are added.
 */
@DisplayName("Unit Test - Spring HttpRequestResponseBodyLogger body-cap enforcement")
public class HttpRequestResponseBodyLoggerBodyCapTest extends UnitTest {

    @Mock
    private LoggingProperties loggingProperties;

    @Mock
    private PasswordMasker passwordMasker;

    @Mock
    private HttpServletRequest servletRequest;

    private HttpRequestResponseBodyLogger logger;

    @BeforeEach
    public void setUp() {
        logger = new HttpRequestResponseBodyLogger(passwordMasker, loggingProperties);
        // Default: pass-through masker
        when(passwordMasker.maskPasswordsIn(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ServletInputStream servletInputStreamOf(final byte[] bytes) {
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return new ServletInputStream() {

            @Override
            public boolean isFinished() { return bais.available() == 0; }

            @Override
            public boolean isReady() { return true; }

            @Override
            public void setReadListener(final ReadListener l) { /* no-op */ }

            @Override
            public int read() throws IOException { return bais.read(); }
        };
    }

    // =========================================================================
    // Path 1: getTxRequestBody
    // =========================================================================

    @Nested
    @DisplayName("getTxRequestBody — inbound servlet request")
    class TxRequestBody {

        @Test
        @DisplayName("EXISTING BEHAVIOUR — should return full body when cap is -1 (unlimited)")
        public void shouldReturnFullBodyWhenCapIsUnlimited() throws IOException {
            // Given: 100-byte body and unlimited cap
            final String fullBody = "R".repeat(100);
            when(loggingProperties.getResponseLength()).thenReturn(-1);
            when(servletRequest.getInputStream())
                .thenReturn(servletInputStreamOf(fullBody.getBytes(StandardCharsets.UTF_8)));
            when(servletRequest.getCharacterEncoding()).thenReturn("UTF-8");

            // When: Getting the logged request body
            final String result = logger.getTxRequestBody(servletRequest);

            // Then: Full body returned — unlimited cap must never truncate
            assertThat(result, is(notNullValue()));
            assertThat(result.length(), is(equalTo(100)));
        }

        @Test
        @DisplayName("DRIVES FIX — should truncate to cap when body exceeds configured limit")
        public void shouldTruncateToCapWhenBodyExceedsConfiguredLimit() throws IOException {
            // Given: 200-byte body and cap = 50
            final int cap = 50;
            final String fullBody = "S".repeat(200);
            when(loggingProperties.getResponseLength()).thenReturn(cap);
            when(servletRequest.getInputStream())
                .thenReturn(servletInputStreamOf(fullBody.getBytes(StandardCharsets.UTF_8)));
            when(servletRequest.getCharacterEncoding()).thenReturn("UTF-8");

            // When: Getting the logged request body
            final String result = logger.getTxRequestBody(servletRequest);

            // Then: Result is capped — starts with cap chars, no more
            assertThat(result, is(notNullValue()));
            assertThat(result.startsWith("S".repeat(cap)), is(true));
        }

        @Test
        @DisplayName("DRIVES FIX — should not truncate when body is smaller than cap")
        public void shouldNotTruncateWhenBodyIsSmallerThanCap() throws IOException {
            // Given: 10-byte body and cap = 100
            final String shortBody = "short body";
            when(loggingProperties.getResponseLength()).thenReturn(100);
            when(servletRequest.getInputStream())
                .thenReturn(servletInputStreamOf(shortBody.getBytes(StandardCharsets.UTF_8)));
            when(servletRequest.getCharacterEncoding()).thenReturn("UTF-8");

            // When: Getting the logged request body
            final String result = logger.getTxRequestBody(servletRequest);

            // Then: Full body returned, no padding, no truncation marker
            assertThat(result, is(equalTo("short body")));
        }

        @Test
        @DisplayName("DRIVES FIX — truncation before masking: password beyond cap absent from log")
        public void shouldNotSeePasswordFieldBeyondCapInLoggedOutput() throws IOException {
            // Given: body = 20 'A's followed by a password field; cap = 20
            final int cap = 20;
            final String fullBody = "A".repeat(cap) + "{\"password\":\"secret\"}";
            when(loggingProperties.getResponseLength()).thenReturn(cap);
            when(servletRequest.getInputStream())
                .thenReturn(servletInputStreamOf(fullBody.getBytes(StandardCharsets.UTF_8)));
            when(servletRequest.getCharacterEncoding()).thenReturn("UTF-8");

            // When: Getting the logged request body
            final String result = logger.getTxRequestBody(servletRequest);

            // Then: "password" key does not appear (it was beyond the cap)
            assertThat(result, not(containsString("password")));
        }
    }

    // =========================================================================
    // Path 2: getTxResponseBody
    // Uses existing WrappedContentCachingResponse constructor (no cap in wrapper yet).
    // Tests prove the logger itself must apply the cap via compressBody.
    // =========================================================================


    @Nested
    @DisplayName("getTxResponseBody — inbound servlet response")
    class TxResponseBody {

        @Test
        @DisplayName("EXISTING BEHAVIOUR — should return full body when cap is -1 (unlimited)")
        public void shouldReturnFullBodyWhenCapIsUnlimited() throws IOException {
            // Given: A WrappedContentCachingResponse caching 200 bytes, cap = -1
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            underlying.setContentType("application/json");
            // Existing single-arg constructor (no cap at wrapper level)
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);
            wrapper.getOutputStream().write("T".repeat(200).getBytes(StandardCharsets.UTF_8));

            when(loggingProperties.getResponseLength()).thenReturn(-1);

            // When: Getting the logged response body
            final String result = logger.getTxResponseBody(wrapper);

            // Then: Full 200 chars returned — unlimited must not truncate
            assertThat(result, is(notNullValue()));
            assertThat(result.length(), is(equalTo(200)));
        }

        @Test
        @DisplayName("DRIVES FIX — should truncate to cap when response body exceeds cap")
        public void shouldTruncateToCapWhenResponseBodyExceedsCap() throws IOException {
            // Given: 200-byte body cached in wrapper, cap = 50 applied at logger level
            final int cap = 50;
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            underlying.setContentType("application/json");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);
            wrapper.getOutputStream().write("U".repeat(200).getBytes(StandardCharsets.UTF_8));

            when(loggingProperties.getResponseLength()).thenReturn(cap);

            // When: Getting the logged response body
            final String result = logger.getTxResponseBody(wrapper);

            // Then: Result is capped at 50 chars (via compressBody in logger)
            assertThat(result, is(notNullValue()));
            assertThat(result.startsWith("U".repeat(cap)), is(true));
        }

        @Test
        @DisplayName("DRIVES FIX — should not truncate when response body is smaller than cap")
        public void shouldNotTruncateWhenResponseBodyIsSmallerThanCap() throws IOException {
            // Given: 10-byte body, cap = 500
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            underlying.setContentType("application/json");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);
            wrapper.getOutputStream().write("V".repeat(10).getBytes(StandardCharsets.UTF_8));

            when(loggingProperties.getResponseLength()).thenReturn(500);

            // When: Getting the logged response body
            final String result = logger.getTxResponseBody(wrapper);

            // Then: Full 10 chars returned
            assertThat(result.length(), is(equalTo(10)));
        }

        @Test
        @DisplayName("DRIVES FIX — truncation before masking: password beyond cap absent from log")
        public void shouldNotSeePasswordBeyondCapInTxResponseLog() throws IOException {
            // Given: Response body where password is beyond cap = 5
            final int cap = 5;
            final String body = "AAAAA{\"password\":\"s3cr3t\"}";
            final MockHttpServletResponse underlying = new MockHttpServletResponse();
            underlying.setCharacterEncoding("UTF-8");
            underlying.setContentType("application/json");
            final WrappedContentCachingResponse wrapper = new WrappedContentCachingResponse(underlying);
            wrapper.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            when(loggingProperties.getResponseLength()).thenReturn(cap);
            when(passwordMasker.maskPasswordsIn(anyString())).thenAnswer(inv -> inv.getArgument(0));

            // When: Getting the logged response body
            final String result = logger.getTxResponseBody(wrapper);

            // Then: "password" field absent — it was beyond the cap
            assertThat(result, not(containsString("password")));
        }
    }

    // =========================================================================
    // Path 3: getCallRequestBody
    // =========================================================================


    @Nested
    @DisplayName("getCallRequestBody — outbound client request")
    class CallRequestBody {

        @Test
        @DisplayName("EXISTING BEHAVIOUR — should return full body when cap is -1 (unlimited)")
        public void shouldReturnFullBodyWhenCapIsUnlimited() {
            // Given: 100-byte body, cap = -1
            final byte[] body = "W".repeat(100).getBytes(StandardCharsets.UTF_8);
            when(loggingProperties.getResponseLength()).thenReturn(-1);

            // When: Getting the logged outbound request body
            final String result = logger.getCallRequestBody(body);

            // Then: Full 100 chars returned
            assertThat(result.length(), is(equalTo(100)));
        }

        @Test
        @DisplayName("DRIVES FIX — should truncate to cap when outbound request body exceeds cap")
        public void shouldTruncateToCapWhenOutboundRequestBodyExceedsCap() {
            // Given: 200-byte body, cap = 30
            final int cap = 30;
            final byte[] body = "X".repeat(200).getBytes(StandardCharsets.UTF_8);
            when(loggingProperties.getResponseLength()).thenReturn(cap);

            // When: Getting the logged outbound request body
            final String result = logger.getCallRequestBody(body);

            // Then: Result is capped at 30 chars
            assertThat(result.startsWith("X".repeat(cap)), is(true));
        }

        @Test
        @DisplayName("DRIVES FIX — should not truncate when outbound request body is smaller than cap")
        public void shouldNotTruncateWhenOutboundRequestBodyIsSmallerThanCap() {
            // Given: 5-byte body, cap = 100
            final byte[] body = "HELLO".getBytes(StandardCharsets.UTF_8);
            when(loggingProperties.getResponseLength()).thenReturn(100);

            // When: Getting the logged outbound request body
            final String result = logger.getCallRequestBody(body);

            // Then: Full body returned
            assertThat(result, is(equalTo("HELLO")));
        }

        @Test
        @DisplayName("DRIVES FIX — truncation before masking: password beyond cap absent from log")
        public void shouldNotSeePasswordBeyondCapInCallRequestLog() {
            // Given: Body where password field is beyond cap = 5
            final int cap = 5;
            final String bodyStr = "AAAAA{\"password\":\"topsecret\"}";
            final byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);
            when(loggingProperties.getResponseLength()).thenReturn(cap);
            when(passwordMasker.maskPasswordsIn(anyString())).thenAnswer(inv -> inv.getArgument(0));

            // When: Getting the logged outbound request body
            final String result = logger.getCallRequestBody(body);

            // Then: Password field absent from logged output
            assertThat(result, not(containsString("password")));
        }
    }

    // =========================================================================
    // Path 4: getCallResponseBody — already uses compressAndMaskBody (ALREADY CORRECT)
    // =========================================================================


    @Nested
    @DisplayName("getCallResponseBody — outbound client response (ALREADY CORRECT)")
    class CallResponseBody {

        @Test
        @DisplayName("EXISTING BEHAVIOUR — should return full body when cap is -1 (unlimited)")
        public void shouldReturnFullBodyWhenCapIsUnlimited() throws IOException {
            // Given: A BufferedClientHttpResponse with 100-byte body, cap = -1
            final byte[] body = "Y".repeat(100).getBytes(StandardCharsets.UTF_8);
            final BufferedClientHttpResponse clientResponse = aBufferedResponse(body);
            when(loggingProperties.getResponseLength()).thenReturn(-1);

            // When: Getting the logged outbound response body
            final String result = logger.getCallResponseBody(clientResponse);

            // Then: Full body returned
            assertThat(result.length(), is(equalTo(100)));
        }

        @Test
        @DisplayName("EXISTING BEHAVIOUR — should truncate when body exceeds cap")
        public void shouldTruncateWhenBodyExceedsCap() throws IOException {
            // Given: 200-byte body, cap = 50
            final int cap = 50;
            final byte[] body = "Z".repeat(200).getBytes(StandardCharsets.UTF_8);
            final BufferedClientHttpResponse clientResponse = aBufferedResponse(body);
            when(loggingProperties.getResponseLength()).thenReturn(cap);

            // When: Getting the logged outbound response body
            final String result = logger.getCallResponseBody(clientResponse);

            // Then: Result is capped
            assertThat(result.startsWith("Z".repeat(cap)), is(true));
        }

        private BufferedClientHttpResponse aBufferedResponse(final byte[] body) {
            return new BufferedClientHttpResponse(new ClientHttpResponse() {

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
            });
        }
    }

    // =========================================================================
    // Null / empty edge cases
    // =========================================================================


    @Nested
    @DisplayName("Edge cases — empty bodies")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty byte array in getCallRequestBody")
        public void shouldHandleEmptyByteArrayInCallRequestBody() {
            // Given: empty body, any cap
            final byte[] body = new byte[0];
            when(loggingProperties.getResponseLength()).thenReturn(50);

            // When: Getting logged body
            final String result = logger.getCallRequestBody(body);

            // Then: Result is not null, length is 0
            assertThat(result, is(notNullValue()));
            assertThat(result.length(), is(equalTo(0)));
        }
    }
}
