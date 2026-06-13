package io.github.jframe.exception.handler;

import io.github.jframe.exception.ApiError;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;

import java.io.IOException;
import java.util.Map;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests for {@link ErrorResponseWriter}.
 *
 * <p>Verifies the ErrorResponseWriter functionality including:
 * <ul>
 * <li>JSON error response body written to HttpServletResponse</li>
 * <li>Content-Type set to application/json</li>
 * <li>HTTP status code set from Response.Status</li>
 * <li>ErrorResponseResource fields populated from request and arguments</li>
 * <li>ApiError overload extracts status, errorCode, and errorReason</li>
 * <li>errorCode always serialized when provided; absent query string handled correctly</li>
 * </ul>
 */
@DisplayName("Exception Handler - Error Response Writer")
public class ErrorResponseWriterTest extends UnitTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ======================== write(req, res, status, errorCode, errorReason) ========================

    @Test
    @DisplayName("Should write JSON error response with status code and error code")
    public void shouldWriteJsonErrorResponseWithStatusCodeAndErrorCode() throws IOException {
        // Given: A GET request and a writable response with a specific status, code, and reason
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final Response.Status status = Response.Status.BAD_REQUEST;
        final String errorCode = "JFRAME_BAD_REQUEST";
        final String errorReason = "Input validation failed";

        // When: Writing the error response
        ErrorResponseWriter.write(request, response, status, errorCode, errorReason);

        // Then: Response body contains statusCode, errorCode and errorReason; cause is absent
        assertThat(response.getContentType(), containsString(MediaType.APPLICATION_JSON_VALUE));
        final Map<String, Object> body = parseBody(response);
        assertThat(body.get("statusCode"), is(equalTo(400)));
        assertThat(body.get("errorCode"), is(equalTo(errorCode)));
        assertThat(body.get("errorReason"), is(equalTo(errorReason)));
        assertThat(body.get("cause"), is(nullValue()));
    }

    @Test
    @DisplayName("Should always serialize errorCode when provided")
    public void shouldAlwaysSerializeErrorCodeWhenProvided() throws IOException {
        // Given: A request with an error code (errorCode is always populated — never null in the new design)
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String errorCode = "JFRAME_INTERNAL_ERROR";

        // When: Writing error response with a non-null error code
        ErrorResponseWriter.write(request, response, Response.Status.INTERNAL_SERVER_ERROR, errorCode, "Unexpected error");

        // Then: errorCode is present in JSON body (always populated, never absent)
        final Map<String, Object> body = parseBody(response);
        assertThat(body.get("errorCode"), is(equalTo(errorCode)));
        assertThat(body.get("errorReason"), is(equalTo("Unexpected error")));
    }

    @Test
    @DisplayName("Should set HTTP status on response")
    public void shouldSetHttpStatusOnResponse() throws IOException {
        // Given: A request and a writable response
        final MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/resource/1");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // When: Writing error response with NOT_FOUND status
        ErrorResponseWriter.write(request, response, Response.Status.NOT_FOUND, "JFRAME_NOT_FOUND", "Resource not found");

        // Then: Response HTTP status code matches the provided status
        assertThat(response.getStatus(), is(equalTo(404)));
    }

    // ======================== write(req, res, apiError) ========================

    @Test
    @DisplayName("Should write JSON error response from ApiError")
    public void shouldWriteJsonErrorResponseFromApiError() throws IOException {
        // Given: A request and an ApiError carrying status, errorCode, and errorReason
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final ApiError apiError = new TestApiError("ITEM_NOT_FOUND", "The requested item does not exist", Response.Status.NOT_FOUND);

        // When: Writing error response from ApiError
        ErrorResponseWriter.write(request, response, apiError);

        // Then: Status, errorCode, and errorReason are all extracted from ApiError
        assertThat(response.getStatus(), is(equalTo(404)));
        final Map<String, Object> body = parseBody(response);
        assertThat(body.get("errorCode"), is(equalTo("ITEM_NOT_FOUND")));
        assertThat(body.get("errorReason"), is(equalTo("The requested item does not exist")));
    }

    @Test
    @DisplayName("Should populate request info from HttpServletRequest")
    public void shouldPopulateRequestInfoFromHttpServletRequest() throws IOException {
        // Given: A request with URI, method, query string, and content type
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/search");
        request.setQueryString("page=1&size=10");
        request.setContentType("application/json");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // When: Writing error response
        ErrorResponseWriter.write(request, response, Response.Status.BAD_REQUEST, "JFRAME_BAD_REQUEST", "Search failed");

        // Then: Request metadata is populated in the response body
        final Map<String, Object> body = parseBody(response);
        assertThat(body.get("uri"), is(equalTo("/api/search")));
        assertThat(body.get("method"), is(equalTo("POST")));
        assertThat(body.get("query"), is(equalTo("page=1&size=10")));
        assertThat(body.get("contentType"), is(equalTo("application/json")));
    }

    @Test
    @DisplayName("Should set query to null when request has no query string")
    public void shouldSetQueryToNullWhenRequestHasNoQueryString() throws IOException {
        // Given: A request without a query string
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // When: Writing error response
        ErrorResponseWriter.write(request, response, Response.Status.SERVICE_UNAVAILABLE, "JFRAME_INTERNAL_ERROR", "Service unavailable");

        // Then: Query field is absent from JSON body (NON_NULL serialization)
        final Map<String, Object> body = parseBody(response);
        assertThat(body.get("query"), is(nullValue()));
    }

    // ======================== HELPERS ========================

    private Map<String, Object> parseBody(final MockHttpServletResponse response) throws IOException {
        return MAPPER.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {});
    }
}
