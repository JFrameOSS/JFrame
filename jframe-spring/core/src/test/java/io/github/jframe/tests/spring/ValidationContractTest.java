package io.github.jframe.tests.spring;

import io.github.support.ProblemJson;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Validation Problem Details with default {@code type} base URI and Spring's own Problem Details switched on.
 */
@DisplayName("Spring Integration - Validation Contract (default type URI, spring.mvc.problemdetails.enabled)")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.mvc.problemdetails.enabled=true"
)
@Import(TestSecurityConfiguration.class)
class ValidationContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Should render validation errors with default type URI")
    void shouldRenderValidationErrorsWithDefaultTypeUri() throws Exception {
        // Given: No type base URI configured and Spring Problem Details enabled
        // When: A ValidationException is thrown
        final MockHttpServletResponse response = mockMvc.perform(get("/test/validation-error")).andReturn().getResponse();
        ProblemJson.assertRfc9457(response.getContentAsString(), response.getStatus());
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: jFrame output unchanged, type omitted (no base URI configured)
        assertThat(response.getStatus(), is(400));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, not(hasKey("type")));
        assertThat(body, hasEntry("errorCode", "VALIDATION_ERROR"));
        assertThat((List<?>) body.get("errors"), hasSize(2));
    }

    @Test
    @DisplayName("Should render MVC framework error as jFrame Problem Details when Spring Problem Details enabled")
    void shouldRenderMvcErrorAsJFrameProblemWhenSpringProblemDetailsEnabled() throws Exception {
        // Given: spring.mvc.problemdetails.enabled=true
        // When: Calling an unknown route
        final MockHttpServletResponse response = mockMvc.perform(get("/test/does-not-exist")).andReturn().getResponse();
        ProblemJson.assertRfc9457(response.getContentAsString(), response.getStatus());
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: jFrame's handler wins (errorCode present)
        assertThat(response.getStatus(), is(404));
        assertThat(body, hasKey("errorCode"));
        assertThat(body, hasEntry("instance", "/test/does-not-exist"));
    }
}
