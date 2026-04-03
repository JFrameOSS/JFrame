package io.github.jframe.tests.spring;

import io.github.jframe.tests.contract.ContractFixtures;
import io.github.jframe.tests.contract.ContractFixtures.ExceptionScenario;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Spring integration tests that verify the exception handling contract.
 *
 * <p>Loads exception scenarios from the contract fixture file and verifies that each
 * exception type produces the expected HTTP status when thrown from a controller.
 */
@DisplayName("Spring Integration - Exception Handling Contract Tests")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(TestSecurityConfiguration.class)
class ExceptionHandlingContractTest {

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

    static Stream<ExceptionScenario> exceptionScenarios() {
        return ContractFixtures.loadExceptionScenarios().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exceptionScenarios")
    @DisplayName("Should return correct status code for exception scenario")
    void shouldReturnCorrectStatusForExceptionScenario(final ExceptionScenario scenario) throws Exception {
        // Given: An exception scenario from contract fixtures
        // When: Calling the endpoint that throws this exception
        final MvcResult result = mockMvc.perform(get("/test" + scenario.endpoint()))
            .andReturn();

        // Then: Response status matches expected
        assertThat(result.getResponse().getStatus(), is(scenario.expectedStatusCode()));
    }
}
