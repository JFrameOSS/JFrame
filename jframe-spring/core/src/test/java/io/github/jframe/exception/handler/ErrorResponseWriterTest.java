package io.github.jframe.exception.handler;

import io.github.jframe.logging.model.TransactionId;
import io.github.jframe.tests.spring.TestApplication;
import io.github.jframe.tests.spring.TestEnricherConfiguration;
import io.github.jframe.tests.spring.TestSecurityConfiguration;
import io.github.support.ProblemJson;
import io.github.support.fixtures.TestApiError;

import java.util.Map;
import java.util.UUID;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests for {@link ErrorResponseWriter} used from servlet filters.
 *
 * <p>Runs inside an application context: the writer must use the application's JSON mapper and enrichers.
 */
@DisplayName("Spring Integration - Filter-level Error Response Writer")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "jframe.exception.type-base-uri=https://errors.example.com/",
        "jframe.logging.filters.transaction-id.enabled=true"
    }
)
@Import(
    {
        TestSecurityConfiguration.class,
        TestEnricherConfiguration.class
    }
)
public class ErrorResponseWriterTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @AfterEach
    public void clearTransactionId() {
        TransactionId.remove();
    }

    @Test
    @DisplayName("Should write same Problem Details as the handler, including enrichers")
    public void shouldWriteProblemDetailsWithEnrichersWhenCalledFromFilter() throws Exception {
        // Given: A filter request inside the application and a known transaction id
        final UUID txId = UUID.randomUUID();
        TransactionId.set(txId);
        final MockHttpServletRequest request =
            new MockHttpServletRequest(webApplicationContext.getServletContext(), "GET", "/api/secure");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // When: Writing an UNAUTHORIZED ApiError
        ErrorResponseWriter.write(
            request,
            response,
            new TestApiError("TOKEN_EXPIRED", "Token has expired", Response.Status.UNAUTHORIZED)
        );

        // Then: Problem Details body, content type and enriched extensions
        ProblemJson.assertRfc9457(response.getContentAsString(), response.getStatus());
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());
        assertThat(response.getStatus(), is(401));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("type", "https://errors.example.com/TOKEN_EXPIRED"));
        assertThat(body, hasEntry("title", "Unauthorized"));
        assertThat(body, hasEntry("status", 401));
        assertThat(body, hasEntry("detail", "Token has expired"));
        assertThat(body, hasEntry("instance", "/api/secure"));
        assertThat(body, hasEntry("errorCode", "TOKEN_EXPIRED"));
        assertThat(body, hasEntry("txId", txId.toString()));
        assertThat(body, hasEntry("tenant", "acme"));
        assertThat(body, not(hasKey("statusCode")));
        assertThat(body, not(hasKey("method")));
    }

    @Test
    @DisplayName("Should write RFC 9457 conformant body without application context")
    public void shouldWriteConformantBodyWhenNoApplicationContext() throws Exception {
        // Given: A request outside any web application context
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // When: Writing a FORBIDDEN error via the fallback path
        ErrorResponseWriter.write(request, response, Response.Status.FORBIDDEN, "ACCESS_DENIED", "Access denied");

        // Then: Still a valid problem with default type URI
        ProblemJson.assertRfc9457(response.getContentAsString(), 403);
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());
        assertThat(body, hasEntry("type", ProblemJson.DEFAULT_TYPE_BASE_URI + "ACCESS_DENIED"));
        assertThat(body, hasEntry("instance", "/api/secure"));
    }

    @Test
    @DisplayName("Should write RFC 9457 conformant body when request has no URI")
    public void shouldWriteConformantBodyWhenRequestUriEmpty() throws Exception {
        // Given: A request without URI and no application context
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(null);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // When: Writing an error
        ErrorResponseWriter.write(request, response, Response.Status.UNAUTHORIZED, null, null);

        // Then: Optional members may be absent, body still conforms
        ProblemJson.assertRfc9457(response.getContentAsString(), 401);
    }
}
