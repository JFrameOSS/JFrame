package io.github.jframe.tests.spring;

import io.github.support.ProblemJson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** A blank {@code jframe.exception.type-base-uri} counts as not configured. */
@DisplayName("Spring Integration - Blank type base URI")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "jframe.exception.type-base-uri=  "
)
@Import(TestSecurityConfiguration.class)
class BlankTypeBaseUriTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @DisplayName("Should omit type when base URI property is blank")
    void shouldOmitTypeWhenBaseUriBlank() throws Exception {
        // Given: Blank base URI property
        // When
        final MockHttpServletResponse response = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            .perform(get("/test/business")).andReturn().getResponse();

        // Then: Body equals shared expectation without type
        ProblemJson.assertRfc9457(response.getContentAsString(), response.getStatus());
        ProblemJson.assertMatchesExpected(response.getContentAsString(), "business-http-exception");
    }
}
