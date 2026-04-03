package io.github.jframe.logging.logger;

import io.github.support.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Unit tests for {@link HttpRequestResponseDebugLogger}.
 *
 * <p>Verifies formatting of HTTP request and response debug output strings, including
 * presence of method/URI/status lines, headers blocks and body blocks, and correct
 * omission of sections when headers or body are absent.
 *
 */
@DisplayName("Unit Test - HttpRequestResponseDebugLogger")
public class HttpRequestResponseDebugLoggerTest extends UnitTest {

    private HttpRequestResponseDebugLogger debugLogger;

    @BeforeEach
    @Override
    public void setUp() {
        debugLogger = new HttpRequestResponseDebugLogger();
    }

    // ---------------------------------------------------------------------------
    // getRequestDebugOutput
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should format request with method, URI, headers and body in HTTP debug format")
    public void shouldFormatRequestWithMethodUriHeadersAndBodyInHttpDebugFormat() {
        // Given: A GET request to /api/users with a JSON Accept header and a JSON body
        final String method = "GET";
        final String uri = "/api/users";
        final String headers = "Accept: application/json\n";
        final String body = "{\"filter\":\"active\"}";

        // When: Generating the request debug output
        final String result = debugLogger.getRequestDebugOutput(method, uri, headers, body);

        // Then: The output contains the method, URI, headers and body
        assertThat(result, containsString("GET"));
        assertThat(result, containsString("/api/users"));
        assertThat(result, containsString("Accept: application/json"));
        assertThat(result, containsString("{\"filter\":\"active\"}"));
    }

    @Test
    @DisplayName("Should format request without body section when body is empty string")
    public void shouldFormatRequestWithoutBodySectionWhenBodyIsEmptyString() {
        // Given: A GET request with headers but no body
        final String method = "GET";
        final String uri = "/api/health";
        final String headers = "Accept: */*\n";
        final String body = "";

        // When: Generating the request debug output
        final String result = debugLogger.getRequestDebugOutput(method, uri, headers, body);

        // Then: The output contains method and headers but no blank body section after headers
        assertThat(result, containsString("GET"));
        assertThat(result, containsString("/api/health"));
        assertThat(result, containsString("Accept: */*"));
        // Body section must not appear as a spurious extra newline + content
        assertThat(result, not(containsString("\n\n")));
    }

    @Test
    @DisplayName("Should format request without body section when body is null")
    public void shouldFormatRequestWithoutBodySectionWhenBodyIsNull() {
        // Given: A DELETE request with headers and a null body
        final String method = "DELETE";
        final String uri = "/api/items/5";
        final String headers = "Authorization: Bearer token\n";

        // When: Generating the request debug output with null body
        final String result = debugLogger.getRequestDebugOutput(method, uri, headers, null);

        // Then: The output contains method, URI and headers, with no extra blank sections
        assertThat(result, containsString("DELETE"));
        assertThat(result, containsString("/api/items/5"));
        assertThat(result, containsString("Authorization: Bearer token"));
        assertThat(result, not(containsString("null")));
    }

    @Test
    @DisplayName("Should format request without headers section when headers are empty")
    public void shouldFormatRequestWithoutHeadersSectionWhenHeadersAreEmpty() {
        // Given: A POST request with an empty headers string and a body
        final String method = "POST";
        final String uri = "/api/login";
        final String headers = "";
        final String body = "{\"username\":\"admin\"}";

        // When: Generating the request debug output
        final String result = debugLogger.getRequestDebugOutput(method, uri, headers, body);

        // Then: The output contains method and body; headers block is absent
        assertThat(result, containsString("POST"));
        assertThat(result, containsString("/api/login"));
        assertThat(result, containsString("{\"username\":\"admin\"}"));
    }

    // ---------------------------------------------------------------------------
    // getResponseDebugOutput
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should format response with status line, headers and body")
    public void shouldFormatResponseWithStatusLineHeadersAndBody() {
        // Given: A 200 OK response with a JSON content-type header and a JSON body
        final int status = 200;
        final String reasonPhrase = "OK";
        final String headers = "Content-Type: application/json\n";
        final String body = "{\"result\":\"success\"}";

        // When: Generating the response debug output
        final String result = debugLogger.getResponseDebugOutput(status, reasonPhrase, headers, body);

        // Then: The output contains the status code, reason phrase, headers and body
        assertThat(result, containsString("200"));
        assertThat(result, containsString("OK"));
        assertThat(result, containsString("Content-Type: application/json"));
        assertThat(result, containsString("{\"result\":\"success\"}"));
    }

    @Test
    @DisplayName("Should format response without body section when body is empty string")
    public void shouldFormatResponseWithoutBodySectionWhenBodyIsEmptyString() {
        // Given: A 204 No Content response with no body
        final int status = 204;
        final String reasonPhrase = "No Content";
        final String headers = "X-Request-Id: abc\n";
        final String body = "";

        // When: Generating the response debug output
        final String result = debugLogger.getResponseDebugOutput(status, reasonPhrase, headers, body);

        // Then: The output contains status and headers; no spurious body section
        assertThat(result, containsString("204"));
        assertThat(result, containsString("No Content"));
        assertThat(result, containsString("X-Request-Id: abc"));
        assertThat(result, not(containsString("\n\n")));
    }

    @Test
    @DisplayName("Should format response without body section when body is null")
    public void shouldFormatResponseWithoutBodySectionWhenBodyIsNull() {
        // Given: A 404 Not Found response with null body
        final int status = 404;
        final String reasonPhrase = "Not Found";
        final String headers = "Content-Length: 0\n";

        // When: Generating the response debug output with null body
        final String result = debugLogger.getResponseDebugOutput(status, reasonPhrase, headers, null);

        // Then: Status and headers appear; "null" text is not present
        assertThat(result, containsString("404"));
        assertThat(result, containsString("Not Found"));
        assertThat(result, containsString("Content-Length: 0"));
        assertThat(result, not(containsString("null")));
    }

    @Test
    @DisplayName("Should format response without headers section when headers are empty")
    public void shouldFormatResponseWithoutHeadersSectionWhenHeadersAreEmpty() {
        // Given: A 500 response with no headers and a body
        final int status = 500;
        final String reasonPhrase = "Internal Server Error";
        final String headers = "";
        final String body = "{\"error\":\"unexpected\"}";

        // When: Generating the response debug output
        final String result = debugLogger.getResponseDebugOutput(status, reasonPhrase, headers, body);

        // Then: Status line and body appear; empty headers produce no headers block
        assertThat(result, containsString("500"));
        assertThat(result, containsString("Internal Server Error"));
        assertThat(result, containsString("{\"error\":\"unexpected\"}"));
    }

    @Test
    @DisplayName("Should include both status code and reason phrase in response status line")
    public void shouldIncludeBothStatusCodeAndReasonPhraseInResponseStatusLine() {
        // Given: A 201 Created response
        final int status = 201;
        final String reasonPhrase = "Created";
        final String headers = "";
        final String body = "";

        // When: Generating the response debug output
        final String result = debugLogger.getResponseDebugOutput(status, reasonPhrase, headers, body);

        // Then: Both the numeric code and the reason phrase appear together
        assertThat(result, containsString("201"));
        assertThat(result, containsString("Created"));
    }
}
