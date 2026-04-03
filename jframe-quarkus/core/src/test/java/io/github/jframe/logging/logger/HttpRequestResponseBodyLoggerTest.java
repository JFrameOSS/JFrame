package io.github.jframe.logging.logger;

import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.wrapper.CachingRequestContext;
import io.github.jframe.logging.wrapper.CachingResponseContext;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HttpRequestResponseBodyLogger}.
 *
 * <p>Verifies body extraction and password masking for both incoming requests and outgoing
 * responses, including length-limiting and query-string format masking.
 *
 */
@DisplayName("Unit Test - HttpRequestResponseBodyLogger")
public class HttpRequestResponseBodyLoggerTest extends UnitTest {

    /** Real PasswordMasker configured to mask the "password" field. */
    private PasswordMasker passwordMasker;

    @Mock
    private CachingRequestContext cachingRequest;

    @Mock
    private CachingResponseContext cachingResponse;

    private HttpRequestResponseBodyLogger bodyLogger;

    @BeforeEach
    @Override
    public void setUp() {
        passwordMasker = new PasswordMasker(List.of("password"));
        bodyLogger = new HttpRequestResponseBodyLogger(passwordMasker);
    }

    // ---------------------------------------------------------------------------
    // getRequestBody — masking
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return masked body when request body contains JSON password field")
    public void shouldReturnMaskedBodyWhenRequestBodyContainsJsonPasswordField() {
        // Given: A request whose cached body contains a plain-text password in JSON format
        final String rawBody = "{\"username\":\"admin\",\"password\":\"secret\"}";
        when(cachingRequest.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the request body via the logger
        final String result = bodyLogger.getRequestBody(cachingRequest);

        // Then: The password value is replaced with ***
        assertThat(result, is(equalTo("{\"username\":\"admin\",\"password\":\"***\"}")));
    }

    @Test
    @DisplayName("Should return empty string when request has no body")
    public void shouldReturnEmptyStringWhenRequestHasNoBody() {
        // Given: A request with an empty cached body
        when(cachingRequest.getCachedBodyAsString()).thenReturn("");

        // When: Extracting the request body
        final String result = bodyLogger.getRequestBody(cachingRequest);

        // Then: An empty string is returned
        assertThat(result, is(equalTo("")));
    }

    @Test
    @DisplayName("Should return body unchanged when request body contains no sensitive fields")
    public void shouldReturnBodyUnchangedWhenRequestBodyContainsNoSensitiveFields() {
        // Given: A request body with no password field
        final String rawBody = "{\"username\":\"admin\",\"email\":\"admin@example.com\"}";
        when(cachingRequest.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the request body
        final String result = bodyLogger.getRequestBody(cachingRequest);

        // Then: The body is returned as-is without modification
        assertThat(result, is(equalTo(rawBody)));
    }

    @Test
    @DisplayName("Should mask multiple password fields in JSON request body")
    public void shouldMaskMultiplePasswordFieldsInJsonRequestBody() {
        // Given: A body with two password occurrences
        final String rawBody = "{\"password\":\"secret1\",\"confirmPassword\":\"secret2\"}";
        final PasswordMasker multiMasker = new PasswordMasker(List.of("password", "confirmPassword"));
        final HttpRequestResponseBodyLogger multiFieldLogger = new HttpRequestResponseBodyLogger(multiMasker);
        when(cachingRequest.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the body
        final String result = multiFieldLogger.getRequestBody(cachingRequest);

        // Then: Both password fields are masked
        assertThat(result, is(equalTo("{\"password\":\"***\",\"confirmPassword\":\"***\"}")));
    }

    @Test
    @DisplayName("Should mask password value in URL query string format request body")
    public void shouldMaskPasswordValueInUrlQueryStringFormatRequestBody() {
        // Given: A request body in query-string format containing a password
        final String rawBody = "username=admin&password=secret&remember=true";
        when(cachingRequest.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the request body
        final String result = bodyLogger.getRequestBody(cachingRequest);

        // Then: The password segment is masked
        assertThat(result, is(equalTo("username=admin&password=***&remember=true")));
    }

    // ---------------------------------------------------------------------------
    // getResponseBody(CachingResponseContext) — masking
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return masked body when response body contains JSON password field")
    public void shouldReturnMaskedBodyWhenResponseBodyContainsJsonPasswordField() {
        // Given: A response whose cached body contains a password in JSON format
        final String rawBody = "{\"token\":\"abc\",\"password\":\"hidden\"}";
        when(cachingResponse.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the response body
        final String result = bodyLogger.getResponseBody(cachingResponse);

        // Then: The password value is masked
        assertThat(result, is(equalTo("{\"token\":\"abc\",\"password\":\"***\"}")));
    }

    @Test
    @DisplayName("Should return empty string when response cached body string is null")
    public void shouldReturnEmptyStringWhenResponseCachedBodyStringIsNull() {
        // Given: A response with no cached body (null string returned)
        when(cachingResponse.getCachedBodyAsString()).thenReturn(null);

        // When: Extracting the response body
        final String result = bodyLogger.getResponseBody(cachingResponse);

        // Then: An empty string is returned instead of null
        assertThat(result, is(equalTo("")));
    }

    @Test
    @DisplayName("Should return response body unchanged when no sensitive fields present")
    public void shouldReturnResponseBodyUnchangedWhenNoSensitiveFieldsPresent() {
        // Given: A response body with no password field
        final String rawBody = "{\"status\":\"ok\",\"data\":{\"id\":1}}";
        when(cachingResponse.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the response body
        final String result = bodyLogger.getResponseBody(cachingResponse);

        // Then: The body is returned unchanged
        assertThat(result, is(equalTo(rawBody)));
    }

    @Test
    @DisplayName("Should return empty string when response cached body is empty byte array")
    public void shouldReturnEmptyStringWhenResponseCachedBodyIsEmptyByteArray() {
        // Given: A response with empty bytes stored as a string
        when(cachingResponse.getCachedBodyAsString()).thenReturn("");

        // When: Extracting the response body
        final String result = bodyLogger.getResponseBody(cachingResponse);

        // Then: An empty string is returned
        assertThat(result, is(equalTo("")));
    }

    @Test
    @DisplayName("Should mask password value in URL query string format response body")
    public void shouldMaskPasswordValueInUrlQueryStringFormatResponseBody() {
        // Given: A response body in query-string format with a password
        final String rawBody = "token=abc&password=exposed&user=john";
        when(cachingResponse.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the response body
        final String result = bodyLogger.getResponseBody(cachingResponse);

        // Then: The password segment is masked
        assertThat(result, is(equalTo("token=abc&password=***&user=john")));
    }

    // ---------------------------------------------------------------------------
    // getResponseBody(CachingResponseContext, int maxLength) — truncation
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should truncate response body to maxLength when body exceeds the limit")
    public void shouldTruncateResponseBodyToMaxLengthWhenBodyExceedsTheLimit() {
        // Given: A response body longer than the specified maxLength
        final String rawBody = "{\"data\":\"this is a very long response body that should be truncated\"}";
        when(cachingResponse.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the response body with maxLength of 10
        final String result = bodyLogger.getResponseBody(cachingResponse, 10);

        // Then: The result is limited to 10 characters
        assertThat(result.length(), is(equalTo(10)));
        assertThat(result, is(equalTo(rawBody.substring(0, 10))));
    }

    @Test
    @DisplayName("Should return full response body when maxLength is -1 (unlimited)")
    public void shouldReturnFullResponseBodyWhenMaxLengthIsMinusOne() {
        // Given: A response body with a known password value, maxLength = -1 means unlimited
        final String rawBody = "{\"result\":\"success\",\"password\":\"topsecret\"}";
        when(cachingResponse.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the response body with unlimited length
        final String result = bodyLogger.getResponseBody(cachingResponse, -1);

        // Then: The full body is returned, with password masked
        assertThat(result, is(equalTo("{\"result\":\"success\",\"password\":\"***\"}")));
    }

    @Test
    @DisplayName("Should return full response body when body length is within maxLength")
    public void shouldReturnFullResponseBodyWhenBodyLengthIsWithinMaxLength() {
        // Given: A short response body and a generous maxLength
        final String rawBody = "{\"id\":1}";
        when(cachingResponse.getCachedBodyAsString()).thenReturn(rawBody);

        // When: Extracting the body with maxLength larger than the actual body
        final String result = bodyLogger.getResponseBody(cachingResponse, 1000);

        // Then: The full body is returned unchanged
        assertThat(result, is(equalTo(rawBody)));
    }
}
