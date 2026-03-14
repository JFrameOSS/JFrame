package io.github.jframe.tests.spring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Spring integration tests that verify validation contract behavior.
 *
 * <p>Verifies that invoking an endpoint that throws a {@link io.github.jframe.exception.core.ValidationException}
 * produces an HTTP 400 response with a non-empty body containing the validation errors.
 */
@DisplayName("Spring Integration - Validation Contract Tests")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(TestSecurityConfiguration.class)
class ValidationContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("Should return 400 with validation errors for invalid input")
    void shouldReturn400WithValidationErrors() throws Exception {
        // Given: A request that triggers validation error
        // When: Calling the validation error endpoint
        final MvcResult result = mockMvc.perform(get("/test/validation-error"))
            .andReturn();

        // Then: Response has 400 status and contains validation errors
        assertThat(result.getResponse().getStatus(), is(400));
        assertThat(result.getResponse().getContentAsString(), is(not(emptyString())));
    }

}
