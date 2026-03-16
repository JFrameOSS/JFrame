package io.github.jframe.logging.logger;

import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.support.UnitTest;

import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HttpRequestResponseHeadersLogger}.
 *
 * <p>Verifies header formatting (sorted, "Name: value\n"), multi-value joining, empty-header
 * handling, and password masking for both request and response contexts.
 *
 */
@DisplayName("Unit Test - HttpRequestResponseHeadersLogger")
public class HttpRequestResponseHeadersLoggerTest extends UnitTest {

    /** Real PasswordMasker that masks the "password" field. */
    private PasswordMasker passwordMasker;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    private HttpRequestResponseHeadersLogger headersLogger;

    @BeforeEach
    @Override
    public void setUp() {
        passwordMasker = new PasswordMasker(List.of("password"));
        headersLogger = new HttpRequestResponseHeadersLogger(passwordMasker);
    }

    // ---------------------------------------------------------------------------
    // getRequestHeaders
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should format single request header as 'Name: value\\n'")
    public void shouldFormatSingleRequestHeaderAsNameValueNewline() {
        // Given: A request context containing a single Accept header
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Accept", "application/json");
        when(requestContext.getHeaders()).thenReturn(headers);

        // When: Extracting request headers as a string
        final String result = headersLogger.getRequestHeaders(requestContext);

        // Then: The header is formatted as "Name: value\n"
        assertThat(result, is(equalTo("Accept: application/json\n")));
    }

    @Test
    @DisplayName("Should sort request headers alphabetically by name")
    public void shouldSortRequestHeadersAlphabeticallyByName() {
        // Given: A request context with headers that are not in alphabetical order
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", "text/plain");
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Bearer token");
        when(requestContext.getHeaders()).thenReturn(headers);

        // When: Extracting request headers as a string
        final String result = headersLogger.getRequestHeaders(requestContext);

        // Then: Accept comes before Authorization, which comes before Content-Type
        final int acceptIndex = result.indexOf("Accept:");
        final int authIndex = result.indexOf("Authorization:");
        final int contentTypeIndex = result.indexOf("Content-Type:");
        assertThat(acceptIndex < authIndex, is(true));
        assertThat(authIndex < contentTypeIndex, is(true));
    }

    @Test
    @DisplayName("Should join multi-valued request headers with comma separator")
    public void shouldJoinMultiValuedRequestHeadersWithCommaSeparator() {
        // Given: A request context with a header having multiple values
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Accept", "application/json");
        headers.add("Accept", "text/plain");
        when(requestContext.getHeaders()).thenReturn(headers);

        // When: Extracting request headers
        final String result = headersLogger.getRequestHeaders(requestContext);

        // Then: Multiple values are joined with ", " on one line
        assertThat(result, containsString("Accept: application/json, text/plain"));
    }

    @Test
    @DisplayName("Should return empty string when request has no headers")
    public void shouldReturnEmptyStringWhenRequestHasNoHeaders() {
        // Given: A request context with an empty headers map
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        when(requestContext.getHeaders()).thenReturn(headers);

        // When: Extracting request headers
        final String result = headersLogger.getRequestHeaders(requestContext);

        // Then: An empty string is returned
        assertThat(result, is(equalTo("")));
    }

    @Test
    @DisplayName("Should mask password values in request headers")
    public void shouldMaskPasswordValuesInRequestHeaders() {
        // Given: A request context with a header whose value contains a password
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("X-Auth", "user=admin&password=secret");
        when(requestContext.getHeaders()).thenReturn(headers);

        // When: Extracting request headers
        final String result = headersLogger.getRequestHeaders(requestContext);

        // Then: The password value is masked
        assertThat(result, containsString("password=***"));
    }

    @Test
    @DisplayName("Should preserve original header name case in request headers output")
    public void shouldPreserveOriginalHeaderNameCaseInRequestHeadersOutput() {
        // Given: A request context with mixed-case header names
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("X-Custom-Header", "value");
        headers.add("content-type", "application/json");
        when(requestContext.getHeaders()).thenReturn(headers);

        // When: Extracting request headers
        final String result = headersLogger.getRequestHeaders(requestContext);

        // Then: Original casing is preserved in the output
        assertThat(result, containsString("X-Custom-Header: value"));
        assertThat(result, containsString("content-type: application/json"));
    }

    // ---------------------------------------------------------------------------
    // getResponseHeaders
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should format single response header as 'Name: value\\n'")
    public void shouldFormatSingleResponseHeaderAsNameValueNewline() {
        // Given: A response context containing a single Content-Type header
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", "application/json");
        when(responseContext.getStringHeaders()).thenReturn(headers);

        // When: Extracting response headers as a string
        final String result = headersLogger.getResponseHeaders(responseContext);

        // Then: The header is formatted as "Name: value\n"
        assertThat(result, is(equalTo("Content-Type: application/json\n")));
    }

    @Test
    @DisplayName("Should sort response headers alphabetically by name")
    public void shouldSortResponseHeadersAlphabeticallyByName() {
        // Given: A response context with headers in random order
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("X-Rate-Limit", "100");
        headers.add("Content-Type", "application/json");
        headers.add("Cache-Control", "no-cache");
        when(responseContext.getStringHeaders()).thenReturn(headers);

        // When: Extracting response headers
        final String result = headersLogger.getResponseHeaders(responseContext);

        // Then: Cache-Control comes before Content-Type, which comes before X-Rate-Limit
        final int cacheIndex = result.indexOf("Cache-Control:");
        final int contentTypeIndex = result.indexOf("Content-Type:");
        final int xRateIndex = result.indexOf("X-Rate-Limit:");
        assertThat(cacheIndex < contentTypeIndex, is(true));
        assertThat(contentTypeIndex < xRateIndex, is(true));
    }

    @Test
    @DisplayName("Should join multi-valued response headers with comma separator")
    public void shouldJoinMultiValuedResponseHeadersWithCommaSeparator() {
        // Given: A response context with a Set-Cookie header having multiple values
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Set-Cookie", "session=abc");
        headers.add("Set-Cookie", "lang=en");
        when(responseContext.getStringHeaders()).thenReturn(headers);

        // When: Extracting response headers
        final String result = headersLogger.getResponseHeaders(responseContext);

        // Then: Multiple cookie values are joined with ", " on one line
        assertThat(result, containsString("Set-Cookie: session=abc, lang=en"));
    }

    @Test
    @DisplayName("Should return empty string when response has no headers")
    public void shouldReturnEmptyStringWhenResponseHasNoHeaders() {
        // Given: A response context with an empty headers map
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        when(responseContext.getStringHeaders()).thenReturn(headers);

        // When: Extracting response headers
        final String result = headersLogger.getResponseHeaders(responseContext);

        // Then: An empty string is returned
        assertThat(result, is(equalTo("")));
    }

    @Test
    @DisplayName("Should mask password values in response headers")
    public void shouldMaskPasswordValuesInResponseHeaders() {
        // Given: A response context with a header whose value contains a password
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("X-Debug", "password=exposed&token=abc");
        when(responseContext.getStringHeaders()).thenReturn(headers);

        // When: Extracting response headers
        final String result = headersLogger.getResponseHeaders(responseContext);

        // Then: The password value is masked in the output
        assertThat(result, containsString("password=***"));
    }
}
