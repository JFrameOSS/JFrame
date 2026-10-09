package io.github.jframe.tests.spring.advice;

import io.github.jframe.tests.spring.TestApplication;
import io.github.jframe.tests.spring.TestSecurityConfiguration;
import io.github.support.ProblemJson;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Application {@code @RestControllerAdvice} precedence and Spring Security authentication-exception mapping.
 */
@DisplayName("Spring Integration - Application advice and authentication exceptions")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(TestSecurityConfiguration.class)
class ApplicationAdviceContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Should let application advice handle its own exception over jFrame catch-all")
    void shouldUseApplicationAdviceWhenAppHandlesException() throws Exception {
        // Given: App advice handling AppDomainException
        // When
        final MockHttpServletResponse response = mockMvc.perform(get("/test/app/domain")).andReturn().getResponse();

        // Then: App response, not jFrame 500
        assertThat(response.getStatus(), is(402));
        assertThat(ProblemJson.parse(response.getContentAsString()), hasEntry("handledBy", TestApplicationAdvice.HANDLED_BY));
    }

    @Test
    @DisplayName("Should let application advice handle a jFrame HttpException subtype")
    void shouldUseApplicationAdviceWhenAppHandlesHttpExceptionSubtype() throws Exception {
        // Given: App advice handling an HttpException subtype
        // When
        final MockHttpServletResponse response = mockMvc.perform(get("/test/app/http")).andReturn().getResponse();

        // Then: App response, not jFrame 409 problem
        assertThat(response.getStatus(), is(202));
        assertThat(ProblemJson.parse(response.getContentAsString()), hasEntry("handledBy", TestApplicationAdvice.HANDLED_BY));
    }

    @Test
    @DisplayName("Should keep jFrame handling for exceptions the application does not handle")
    void shouldUseJFrameWhenAppDoesNotHandleException() throws Exception {
        // Given / When: Exception without app handler
        final MockHttpServletResponse response = mockMvc.perform(get("/test/app/unhandled")).andReturn().getResponse();
        final String json = response.getContentAsString();

        // Then: jFrame generic 500 problem
        assertThat(response.getStatus(), is(500));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        ProblemJson.assertRfc9457(json, 500);
        assertThat(ProblemJson.parse(json), hasEntry("errorCode", "INTERNAL_SERVER_ERROR"));
        assertThat(json, not(containsString(AppAdviceController.SECRET)));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
            "credentials-not-found",
            "insufficient",
            "bad-credentials"
        }
    )
    @DisplayName("Should render every AuthenticationException subtype as 401 UNAUTHORIZED")
    void shouldRenderUnauthorizedWhenAuthenticationExceptionReachesMvc(final String kind) throws Exception {
        // Given / When: An AuthenticationException subtype thrown from a controller
        final MockHttpServletResponse response = mockMvc.perform(get("/test/app/authentication/" + kind)).andReturn().getResponse();
        final String json = response.getContentAsString();
        final Map<String, Object> body = ProblemJson.parse(json);

        // Then: 401 problem, reason-phrase detail, message not leaked
        assertThat(response.getStatus(), is(401));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        ProblemJson.assertRfc9457(json, 401);
        assertThat(body, hasEntry("errorCode", "UNAUTHORIZED"));
        assertThat(body, hasEntry("detail", "Unauthorized"));
        assertThat(json, not(containsString(AppAdviceController.SECRET)));
    }
}
