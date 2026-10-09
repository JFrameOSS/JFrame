package io.github.jframe.tests.spring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.jframe.logging.model.TransactionId;
import io.github.support.ProblemJson;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static io.github.jframe.tests.spring.TestController.SECRET_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * End-to-end RFC 9457 Problem Details contract for the Spring adapter.
 */
@DisplayName("Spring Integration - Problem Details Contract")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        ExceptionHandlingContractTest.TYPE_BASE_URI_PROPERTY,
        "jframe.logging.filters.transaction-id.enabled=true"
    }
)
@Import(
    {
        TestSecurityConfiguration.class,
        TestEnricherConfiguration.class
    }
)
class ExceptionHandlingContractTest {

    static final String TYPE_BASE_URI = "https://errors.example.com/";
    static final String TYPE_BASE_URI_PROPERTY = "jframe.exception.type-base-uri=" + TYPE_BASE_URI;

    private static final List<String> LEGACY_FIELDS =
        List.of("statusCode", "errorReason", "cause", "method", "uri", "query", "contentType");

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

    @Test
    @DisplayName("Should render business HttpException as Problem Details")
    void shouldRenderBusinessHttpExceptionAsProblemDetails() throws Exception {
        // Given: An endpoint throwing HttpException(ORDER_CLOSED, 409)
        // When: Calling it
        final MockHttpServletResponse response = perform(get("/test/business"));
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: Standard members and errorCode are set, legacy fields gone
        assertThat(response.getStatus(), is(409));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("type", TYPE_BASE_URI + "ORDER_CLOSED"));
        assertThat(body, hasEntry("title", "Conflict"));
        assertThat(body, hasEntry("status", 409));
        assertThat(body, hasEntry("detail", "Order is already closed"));
        assertThat(body, hasEntry("instance", "/test/business"));
        assertThat(body, hasEntry("errorCode", "ORDER_CLOSED"));
        LEGACY_FIELDS.forEach(field -> assertThat(body, not(hasKey(field))));
    }

    @Test
    @DisplayName("Should not leak wrapped cause message of HttpException")
    void shouldNotLeakWrappedCauseMessageWhenHttpExceptionHasCause() throws Exception {
        // Given: An HttpException wrapping an internal cause
        // When: Calling the endpoint
        final MockHttpServletResponse response = perform(get("/test/business-with-cause"));

        // Then: The cause message is absent from the body
        assertThat(response.getStatus(), is(409));
        assertThat(response.getContentAsString(), not(containsString(SECRET_MESSAGE)));
        assertThat(ProblemJson.parse(response.getContentAsString()), hasEntry("detail", "Order is already closed"));
    }

    @Test
    @DisplayName("Should URI-encode error code in type")
    void shouldUriEncodeErrorCodeInTypeWhenCodeIsNotUriSafe() throws Exception {
        // Given: An error code containing a space and a slash
        // When: Calling the endpoint
        final Map<String, Object> body = ProblemJson.parse(perform(get("/test/unsafe-code")).getContentAsString());

        // Then: type is a valid URI with the code percent-encoded; errorCode is unchanged
        assertThat(body, hasEntry("type", TYPE_BASE_URI + "BAD%20CODE%2F1"));
        assertThat(body, hasEntry("errorCode", "BAD CODE/1"));
    }

    @Test
    @DisplayName("Should add txId when transaction id is known and omit absent extensions")
    void shouldAddTxIdAndOmitAbsentExtensionsWhenTransactionIdKnown() throws Exception {
        // Given: A known transaction id
        final UUID txId = UUID.randomUUID();
        TransactionId.set(txId);

        // When: An error occurs
        final Map<String, Object> body = ProblemJson.parse(perform(get("/test/bad-request")).getContentAsString());

        // Then: txId present, OTLP and unrelated extensions omitted (not null)
        assertThat(body, hasEntry("txId", txId.toString()));
        assertThat(body, not(hasKey("traceId")));
        assertThat(body, not(hasKey("spanId")));
        assertThat(body, not(hasKey("errors")));
        assertThat(body, not(hasKey("limit")));
        assertThat(body.values(), not(hasItem(nullValue())));
    }

    @Test
    @DisplayName("Should omit txId when no transaction id is known")
    void shouldOmitTxIdWhenNoTransactionIdKnown() throws Exception {
        // Given: No transaction id
        // When: An error occurs
        final Map<String, Object> body = ProblemJson.parse(perform(get("/test/bad-request")).getContentAsString());

        // Then: txId is omitted
        assertThat(body, not(hasKey("txId")));
    }

    @Test
    @DisplayName("Should list each violation of a ValidationException in errors extension")
    void shouldListViolationsWhenValidationExceptionThrown() throws Exception {
        // Given: A ValidationException with two violations
        // When: Calling the endpoint
        final MockHttpServletResponse response = perform(get("/test/validation-error"));
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: 400 with VALIDATION_ERROR and both violations
        assertThat(response.getStatus(), is(400));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("errorCode", "VALIDATION_ERROR"));
        assertThat(body, hasEntry("type", TYPE_BASE_URI + "VALIDATION_ERROR"));
        assertThat(
            (List<?>) body.get("errors"),
            containsInAnyOrder(
                Map.of("field", "name", "code", "name.required"),
                Map.of("field", "email", "code", "email.required")
            )
        );
    }

    @Test
    @DisplayName("Should render bean-validation failure as VALIDATION_ERROR with errors extension")
    void shouldRenderBeanValidationFailureWithErrors() throws Exception {
        // Given: A MethodArgumentNotValidException on field 'name'
        // When: Calling the endpoint
        final MockHttpServletResponse response = perform(get("/test/bean-validation"));
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: 400 VALIDATION_ERROR listing the field
        assertThat(response.getStatus(), is(400));
        assertThat(body, hasEntry("errorCode", "VALIDATION_ERROR"));
        final List<?> errors = (List<?>) body.get("errors");
        assertThat(errors, hasSize(1));
        assertThat(((Map<?, ?>) errors.get(0)).get("field"), is("name"));
    }

    @Test
    @DisplayName("Should render rate limit as Problem Details and keep rate-limit headers")
    void shouldRenderRateLimitAndKeepHeaders() throws Exception {
        // Given: A RateLimitExceededException(100, 0, reset)
        // When: Calling the endpoint
        final MockHttpServletResponse response = perform(get("/test/rate-limit"));
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: 429 with limit extensions and headers
        assertThat(response.getStatus(), is(429));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("status", 429));
        assertThat(body, hasEntry("limit", 100));
        assertThat(body, hasEntry("remaining", 0));
        assertThat(body, hasKey("resetDate"));
        assertThat(response.getHeader("X-RateLimit-Limit"), is("100"));
        assertThat(response.getHeader("X-RateLimit-Remaining"), is("0"));
        assertThat(response.getHeader("X-RateLimit-Reset"), is(notNullValue()));
    }

    @Test
    @DisplayName("Should render invalid sort with rejected input extensions")
    void shouldRenderInvalidSortWithRejectedInput() throws Exception {
        // Given / When: Invalid sort field
        final Map<String, Object> body = ProblemJson.parse(perform(get("/test/invalid-sort")).getContentAsString());

        // Then: INVALID_SORT with rejected field and sortable fields
        assertThat(body, hasEntry("status", 400));
        assertThat(body, hasEntry("errorCode", "INVALID_SORT"));
        assertThat(body, hasEntry("rejectedField", "password"));
        assertThat(body, hasEntry("sortableFields", List.of("name", "createdAt")));
    }

    @Test
    @DisplayName("Should render invalid search with rejected input extensions")
    void shouldRenderInvalidSearchWithRejectedInput() throws Exception {
        // Given / When: Invalid search value
        final Map<String, Object> body = ProblemJson.parse(perform(get("/test/invalid-search")).getContentAsString());

        // Then: INVALID_SEARCH with rejected input
        assertThat(body, hasEntry("status", 400));
        assertThat(body, hasEntry("errorCode", "INVALID_SEARCH"));
        assertThat(body, hasEntry("rejectedField", "age"));
        assertThat(body, hasEntry("rejectedValue", "abc"));
        assertThat(body, hasEntry("searchableFields", List.of("name", "age")));
    }

    @Test
    @DisplayName("Should render invalid page with rejected input extensions")
    void shouldRenderInvalidPageWithRejectedInput() throws Exception {
        // Given / When: Invalid page size
        final Map<String, Object> body = ProblemJson.parse(perform(get("/test/invalid-page")).getContentAsString());

        // Then: INVALID_PAGE with rejected parameter
        assertThat(body, hasEntry("status", 400));
        assertThat(body, hasEntry("errorCode", "INVALID_PAGE"));
        assertThat(body, hasEntry("rejectedParameter", "size"));
        assertThat(body, hasEntry("rejectedValue", -1));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        {
            "/test/bad-credentials, 401",
            "/test/access-denied, 403"
        }
    )
    @DisplayName("Should render security exceptions as Problem Details with errorCode")
    void shouldRenderSecurityExceptionsWithErrorCode(final String path, final int status) throws Exception {
        // Given / When: A security exception reaches the handler
        final MockHttpServletResponse response = perform(get(path));
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: Status unchanged, errorCode present, message not leaked
        assertThat(response.getStatus(), is(status));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("status", status));
        assertThat(body, hasKey("errorCode"));
        assertThat(response.getContentAsString(), not(containsString(SECRET_MESSAGE)));
    }

    @Test
    @DisplayName("Should render unexpected failure as generic 500 without internals")
    void shouldRenderGeneric500WithoutInternalsWhenUnexpectedFailure() throws Exception {
        // Given / When: An unhandled IllegalStateException
        final MockHttpServletResponse response = perform(get("/test/unexpected"));
        final String json = response.getContentAsString();
        final Map<String, Object> body = ProblemJson.parse(json);

        // Then: Generic 500, no message, class name or stack trace
        assertThat(response.getStatus(), is(500));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("errorCode", "INTERNAL_SERVER_ERROR"));
        assertThat(body, hasEntry("title", "Internal Server Error"));
        assertThat(body, hasKey("detail"));
        assertThat(json, not(containsString(SECRET_MESSAGE)));
        assertThat(json, not(containsString("IllegalStateException")));
        assertThat(json, not(containsString("at io.github")));
    }

    @Test
    @DisplayName("Should log unexpected failure at ERROR with throwable")
    void shouldLogUnexpectedFailureAtErrorWithThrowable() throws Exception {
        // Given: A list appender on the root logger
        final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);

        // When: An unhandled exception occurs
        try {
            perform(get("/test/unexpected"));
        } finally {
            root.detachAppender(appender);
        }

        // Then: One ERROR event carries the original throwable
        final List<ILoggingEvent> errors = appender.list.stream()
            .filter(event -> event.getLevel() == Level.ERROR && event.getThrowableProxy() != null)
            .filter(event -> IllegalStateException.class.getName().equals(event.getThrowableProxy().getClassName()))
            .toList();
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getThrowableProxy().getMessage(), is(SECRET_MESSAGE));
    }

    @Test
    @DisplayName("Should render unknown route as jFrame Problem Details")
    void shouldRenderUnknownRouteAsProblemDetails() throws Exception {
        // Given: A known transaction id and an unknown route
        TransactionId.set(UUID.randomUUID());

        // When: Calling the unknown route
        final MockHttpServletResponse response = perform(get("/test/does-not-exist"));

        // Then: 404 enriched by jFrame
        assertJFrameProblem(response, 404, "/test/does-not-exist");
    }

    @Test
    @DisplayName("Should render unsupported method as jFrame Problem Details")
    void shouldRenderUnsupportedMethodAsProblemDetails() throws Exception {
        // Given: A known transaction id
        TransactionId.set(UUID.randomUUID());

        // When: Calling a GET endpoint with PUT
        final MockHttpServletResponse response = perform(put("/test/bad-request"));

        // Then: 405 enriched by jFrame
        assertJFrameProblem(response, 405, "/test/bad-request");
    }

    @Test
    @DisplayName("Should render unsupported media type as jFrame Problem Details")
    void shouldRenderUnsupportedMediaTypeAsProblemDetails() throws Exception {
        // Given: A known transaction id
        TransactionId.set(UUID.randomUUID());

        // When: Posting text/plain to a JSON-only endpoint
        final MockHttpServletResponse response =
            perform(post("/test/json-body").contentType(MediaType.TEXT_PLAIN).content("hello"));

        // Then: 415 enriched by jFrame
        assertJFrameProblem(response, 415, "/test/json-body");
    }

    @Test
    @DisplayName("Should render unreadable body as jFrame Problem Details")
    void shouldRenderUnreadableBodyAsProblemDetails() throws Exception {
        // Given: A known transaction id
        TransactionId.set(UUID.randomUUID());

        // When: Posting malformed JSON
        final MockHttpServletResponse response =
            perform(post("/test/json-body").contentType(MediaType.APPLICATION_JSON).content("{not json"));

        // Then: 400 enriched by jFrame
        assertJFrameProblem(response, 400, "/test/json-body");
    }

    @Test
    @DisplayName("Should return Problem Details JSON, not 406, when client accepts application/json only")
    void shouldReturnProblemJsonWhenClientAcceptsApplicationJsonOnly() throws Exception {
        // Given: Accept: application/json only
        // When: An error occurs
        final MockHttpServletResponse response = perform(get("/test/business").accept(MediaType.APPLICATION_JSON));

        // Then: Original status with parseable Problem Details
        assertThat(response.getStatus(), is(409));
        assertThat(ProblemJson.parse(response.getContentAsString()), hasEntry("errorCode", "ORDER_CLOSED"));
    }

    @Test
    @DisplayName("Should apply application enrichers after built-ins in deterministic order")
    void shouldApplyApplicationEnrichersInOrder() throws Exception {
        // Given: An early (@Order) and a late (unordered) application enricher
        // When: An error occurs
        final Map<String, Object> body = ProblemJson.parse(perform(get("/test/bad-request")).getContentAsString());

        // Then: Custom extension present, late enricher wins
        assertThat(body, hasEntry("tenant", "acme"));
        assertThat(body, hasEntry("enricherOrder", "late"));
    }

    @Test
    @DisplayName("Should let application enricher override errorCode and detail")
    void shouldLetApplicationEnricherOverrideErrorCodeAndDetail() throws Exception {
        // Given: An enricher overriding the error for the not-found path
        // When: Calling the endpoint
        final MockHttpServletResponse response = perform(get(TestEnricherConfiguration.OVERRIDE_PATH));
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());

        // Then: Override is applied, status unchanged
        assertThat(response.getStatus(), is(404));
        assertThat(body, hasEntry("errorCode", "ORDER_NOT_FOUND"));
        assertThat(body, hasEntry("detail", "Order does not exist"));
    }

    /** Performs the request; every error body must conform to RFC 9457. */
    private MockHttpServletResponse perform(final RequestBuilder request) throws Exception {
        final MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
        ProblemJson.assertRfc9457(response.getContentAsString(), response.getStatus());
        return response;
    }

    private static void assertJFrameProblem(final MockHttpServletResponse response, final int status, final String instance)
        throws Exception {
        final Map<String, Object> body = ProblemJson.parse(response.getContentAsString());
        assertThat(response.getStatus(), is(status));
        assertThat(response.getContentType(), startsWith(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("status", status));
        assertThat(body, hasEntry("instance", instance));
        assertThat(body, hasKey("type"));
        assertThat(body, hasKey("title"));
        assertThat(body, hasKey("errorCode"));
        assertThat(body, hasKey("txId"));
        assertThat(body, hasEntry("tenant", "acme"));
    }
}
