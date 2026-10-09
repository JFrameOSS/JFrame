package io.github.jframe.tests.spring;

import io.github.jframe.exception.JFrameErrorCode;
import io.github.support.ProblemJson;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Every errorCode emitted for Spring MVC / security / jFrame errors is a {@link JFrameErrorCode} constant.
 */
@DisplayName("Spring Integration - Emitted errorCodes are JFrameErrorCodes")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(TestSecurityConfiguration.class)
class JFrameErrorCodeContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    static Stream<Arguments> scenarios() {
        return Stream.of(
            Arguments.of("framework 404", get("/test/does-not-exist"), 404, "NOT_FOUND"),
            Arguments.of("framework 405", put("/test/bad-request"), 405, "METHOD_NOT_ALLOWED"),
            Arguments.of(
                "framework 406",
                post("/test/json-body").contentType(MediaType.APPLICATION_JSON).accept("text/csv").content("{}"),
                406,
                "NOT_ACCEPTABLE"
            ),
            Arguments.of(
                "framework 415",
                post("/test/json-body").contentType(MediaType.TEXT_PLAIN).content("hello"),
                415,
                "UNSUPPORTED_MEDIA_TYPE"
            ),
            Arguments.of(
                "unreadable body",
                post("/test/json-body").contentType(MediaType.APPLICATION_JSON).content("{not json"),
                400,
                "BAD_REQUEST"
            ),
            Arguments.of("security 401", get("/test/bad-credentials"), 401, "UNAUTHORIZED"),
            Arguments.of("security 403", get("/test/access-denied"), 403, "FORBIDDEN"),
            Arguments.of("validation", get("/test/validation-error"), 400, "VALIDATION_ERROR"),
            Arguments.of("bean validation", get("/test/bean-validation"), 400, "VALIDATION_ERROR"),
            Arguments.of("invalid sort", get("/test/invalid-sort"), 400, "INVALID_SORT"),
            Arguments.of("invalid search", get("/test/invalid-search"), 400, "INVALID_SEARCH"),
            Arguments.of("invalid page", get("/test/invalid-page"), 400, "INVALID_PAGE"),
            Arguments.of("rate limit", get("/test/rate-limit"), 429, "RATE_LIMIT_EXCEEDED"),
            Arguments.of("unhandled", get("/test/unexpected"), 500, "INTERNAL_SERVER_ERROR")
        );
    }

    @ParameterizedTest(name = "{0} -> {3}")
    @MethodSource("scenarios")
    @DisplayName("Should emit a JFrameErrorCode name matching the status")
    void shouldEmitJFrameErrorCode(final String scenario, final RequestBuilder request, final int status, final String errorCode)
        throws Exception {
        // Given: A request producing a framework, security or jFrame error
        // When: Performing it
        final MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: errorCode is the expected JFrameErrorCode with matching status
        final String[] names = Arrays.stream(JFrameErrorCode.values()).map(Enum::name).toArray(String[]::new);
        assertThat(response.getStatus(), is(status));
        assertThat(body, hasEntry("errorCode", errorCode));
        assertThat(errorCode, is(oneOf(names)));
        assertThat(JFrameErrorCode.valueOf(errorCode).getHttpStatus().getStatusCode(), is(status));
    }
}
