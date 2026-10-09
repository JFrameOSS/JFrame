package io.github.jframe.tests.spring;

import io.github.jframe.logging.model.TransactionId;
import io.github.support.ProblemJson;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Cross-runtime parity: Spring bodies must equal the shared expected bodies in jframe-core test fixtures.
 */
@DisplayName("Spring Integration - Problem Details cross-runtime parity")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(TestSecurityConfiguration.class)
class ProblemDetailsParityTest {

    private static final String CONTEXT_PATH = "/app";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @AfterEach
    void clearTransactionId() {
        TransactionId.remove();
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource(
        {
            "GET, /test/business, business-http-exception",
            "GET, /test/validation-error, validation-error",
            "GET, /test/rate-limit, rate-limit",
            "GET, /test/invalid-sort, invalid-sort",
            "GET, /test/unexpected, unhandled-500",
            "GET, /test/does-not-exist, framework-404",
            "PUT, /test/bad-request, framework-405",
            "GET, /test/bad-credentials, security-401",
            "GET, /test/access-denied, security-403"
        }
    )
    @DisplayName("Should render body equal to the shared expected body")
    void shouldRenderSharedExpectedBody(final String method, final String path, final String expected) throws Exception {
        // Given: No type base URI configured, built-in enrichers only
        // When: Calling the endpoint
        final MockHttpServletResponse response = perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path));
        final String json = response.getContentAsString();

        // Then: RFC 9457 conformant and identical to the Quarkus expectation
        ProblemJson.assertRfc9457(json, response.getStatus());
        ProblemJson.assertMatchesExpected(json, expected);
    }

    @Test
    @DisplayName("Should render malformed JSON body equal to the shared unreadable-body expectation")
    void shouldRenderSharedExpectedBodyWhenRequestBodyIsMalformed() throws Exception {
        // Given: A malformed JSON request body
        final MockHttpServletRequestBuilder request = post("/test/json-body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":");

        // When: Posting it
        final MockHttpServletResponse response = perform(request);
        final String json = response.getContentAsString();

        // Then: RFC 9457 conformant and identical to the Quarkus expectation
        ProblemJson.assertRfc9457(json, response.getStatus());
        ProblemJson.assertMatchesExpected(json, "unreadable-body");
    }

    @Test
    @DisplayName("Should include servlet context path in instance")
    void shouldIncludeContextPathInInstanceWhenDeployedUnderContextPath() throws Exception {
        // Given: The application served under a context path
        // When: Calling a failing endpoint through it
        final MockHttpServletResponse response = perform(get(CONTEXT_PATH + "/test/business").contextPath(CONTEXT_PATH));
        final String json = response.getContentAsString();

        // Then: instance is the full path the client requested
        ProblemJson.assertRfc9457(json, response.getStatus());
        assertThat(ProblemJson.parse(json), hasEntry("instance", CONTEXT_PATH + "/test/business"));
    }

    private MockHttpServletResponse perform(final MockHttpServletRequestBuilder request) throws Exception {
        final MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
        assertThat(response.getContentType().startsWith(ProblemJson.PROBLEM_JSON), is(true));
        return response;
    }
}
