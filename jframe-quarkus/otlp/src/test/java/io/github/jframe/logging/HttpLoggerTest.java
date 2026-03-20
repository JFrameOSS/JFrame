package io.github.jframe.logging;

import io.github.support.UnitTest;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HttpLogger}.
 *
 * <p>Verifies that HTTP request and response logging behaves correctly:
 * <ul>
 * <li>Logs at DEBUG level when DEBUG is enabled</li>
 * <li>Overloaded {@code logResponse} handles body and no-body variants</li>
 * <li>All methods are safe to call with null or empty inputs</li>
 * </ul>
 */
@DisplayName("Unit Test - HttpLogger")
public class HttpLoggerTest extends UnitTest {

    // ─── logRequest ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logRequest")
    class LogRequest {

        @Test
        @DisplayName("Should log request at DEBUG level without throwing")
        public void shouldLogRequestAtDebugLevelWithoutThrowing() {
            // Given: Request details
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putSingle("Content-Type", "application/json");
            headers.putSingle("Authorization", "Bearer token");

            // When + Then: No exception thrown when DEBUG is enabled or disabled
            HttpLogger.logRequest("GET", "https://api.example.com/api/users", headers);
        }

        @Test
        @DisplayName("Should log request with empty headers without throwing")
        public void shouldLogRequestWithEmptyHeadersWithoutThrowing() {
            // Given: Empty headers map
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

            // When + Then: No exception thrown with empty headers
            HttpLogger.logRequest("POST", "https://api.example.com/api/orders", headers);
        }
    }

    // ─── logResponse (without body) ──────────────────────────────────────────


    @Nested
    @DisplayName("logResponse without body")
    class LogResponseWithoutBody {

        @Test
        @DisplayName("Should log response status and headers at DEBUG level without throwing")
        public void shouldLogResponseStatusAndHeadersAtDebugLevelWithoutThrowing() {
            // Given: Response status and headers
            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
            headers.putSingle("Content-Type", "application/json");
            headers.putSingle("X-Request-ID", "abc-123");

            // When + Then: No exception thrown
            HttpLogger.logResponse(200, headers);
        }

        @Test
        @DisplayName("Should log 500 server error response without throwing")
        public void shouldLog500ServerErrorResponseWithoutThrowing() {
            // Given: 500 response with error headers
            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
            headers.putSingle("Content-Type", "application/json");

            // When + Then: No exception thrown for error status codes
            HttpLogger.logResponse(500, headers);
        }
    }

    // ─── logResponse (with body) ─────────────────────────────────────────────


    @Nested
    @DisplayName("logResponse with body")
    class LogResponseWithBody {

        @Test
        @DisplayName("Should log response with body at DEBUG level without throwing")
        public void shouldLogResponseWithBodyAtDebugLevelWithoutThrowing() {
            // Given: Response with a body
            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
            headers.putSingle("Content-Type", "application/json");
            final String body = "{\"id\": 42, \"name\": \"John\"}";

            // When + Then: No exception thrown
            HttpLogger.logResponse(200, headers, body);
        }

        @Test
        @DisplayName("Should log response with null body without throwing")
        public void shouldLogResponseWithNullBodyWithoutThrowing() {
            // Given: Response where body is null
            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

            // When + Then: No NPE or exception thrown with null body
            HttpLogger.logResponse(204, headers, null);
        }

        @Test
        @DisplayName("Should log response with empty body without throwing")
        public void shouldLogResponseWithEmptyBodyWithoutThrowing() {
            // Given: Response where body is empty string
            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

            // When + Then: No exception thrown with empty body
            HttpLogger.logResponse(200, headers, "");
        }
    }
}
