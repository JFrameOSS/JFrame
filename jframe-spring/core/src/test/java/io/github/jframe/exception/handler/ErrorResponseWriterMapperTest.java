package io.github.jframe.exception.handler;

import io.github.jframe.tests.spring.TestApplication;
import io.github.jframe.tests.spring.TestSecurityConfiguration;
import io.github.support.ProblemJson;
import io.github.support.fixtures.TestApiError;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * {@link ErrorResponseWriter} must serialise with the application's configured JSON mapper.
 */
@DisplayName("Spring Integration - Error Response Writer uses application JSON mapper")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(
    {
        TestSecurityConfiguration.class,
        ErrorResponseWriterMapperTest.IndentingMapperConfiguration.class
    }
)
public class ErrorResponseWriterMapperTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @DisplayName("Should serialise with application mapper configuration")
    public void shouldUseApplicationMapperWhenAvailable() throws Exception {
        // Given: An application mapper with indented output
        final MockHttpServletRequest request =
            new MockHttpServletRequest(webApplicationContext.getServletContext(), "GET", "/api/secure");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // When: Writing an error from a filter
        ErrorResponseWriter.write(request, response, new TestApiError("TOKEN_EXPIRED", "Token has expired", Response.Status.UNAUTHORIZED));
        final String json = response.getContentAsString();

        // Then: Output is indented (application mapper) and still conforms
        assertThat(json, containsString("\n"));
        ProblemJson.assertRfc9457(json, response.getStatus());
    }

    /** Application mapper with a distinctive, visible configuration. */
    @TestConfiguration
    static class IndentingMapperConfiguration {

        @Bean
        @Primary
        JsonMapper indentingJsonMapper() {
            return JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        }
    }
}
