package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.mapper.AbstractExceptionMapper;
import io.github.jframe.exception.mapper.HttpExceptionMapper;
import io.github.jframe.exception.mapper.InvalidPageExceptionMapper;
import io.github.jframe.exception.mapper.InvalidSearchExceptionMapper;
import io.github.jframe.exception.mapper.InvalidSortExceptionMapper;
import io.github.jframe.exception.mapper.RateLimitExceededExceptionMapper;
import io.github.jframe.exception.mapper.ThrowableMapper;
import io.github.jframe.exception.mapper.ValidationExceptionMapper;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.jframe.logging.model.TransactionId;
import io.github.jframe.validation.ValidationResult;
import io.github.support.Extensions;
import io.github.support.ProblemJson;
import io.github.support.fixtures.TestApiError;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.github.jframe.exception.factory.ProblemDetailsFixture.aBuilder;
import static io.github.jframe.exception.factory.ProblemDetailsFixture.aRequest;
import static io.github.jframe.exception.factory.ProblemDetailsFixture.builtInEnrichersPlus;
import static io.github.jframe.exception.factory.ProblemDetailsFixture.wire;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * RFC 9457 Problem Details contract for the Quarkus adapter; bodies must equal the Spring adapter's.
 */
@DisplayName("Quarkus Integration - Problem Details Contract")
class ExceptionHandlingContractTest {

    private static final String SECRET_MESSAGE = "jdbc:postgresql://db/secret password=hunter2";
    private static final List<String> LEGACY_FIELDS =
        List.of("statusCode", "errorReason", "cause", "method", "uri", "query", "contentType");

    private final ErrorResponseEntityBuilder builder = aBuilder(builtInEnrichersPlus());

    @AfterEach
    void clearTransactionId() {
        TransactionId.remove();
    }

    private <T extends Throwable> Response map(final AbstractExceptionMapper<T> mapper, final T exception, final String path) {
        return wire(mapper, builder, aRequest(path)).toResponse(exception);
    }

    private static Map<String, Object> body(final Response response) {
        final String json = ProblemJson.toJson(response.getEntity());
        ProblemJson.assertRfc9457(json, response.getStatus());
        return ProblemJson.parse(json);
    }

    @Test
    @DisplayName("Should render business HttpException as Problem Details")
    void shouldRenderBusinessHttpExceptionAsProblemDetails() {
        // Given: An HttpException(ORDER_CLOSED, 409)
        final HttpException exception =
            new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT));

        // When: Mapping it
        final Response response = map(new HttpExceptionMapper(), exception, "/test/business");
        final Map<String, Object> body = body(response);

        // Then: Standard members and errorCode, type omitted (no base URI); legacy fields gone
        assertThat(response.getStatus(), is(409));
        assertThat(response.getMediaType().toString(), is(ProblemJson.PROBLEM_JSON));
        assertThat(body, not(hasKey("type")));
        assertThat(body, hasEntry("title", "Conflict"));
        assertThat(body, hasEntry("status", 409));
        assertThat(body, hasEntry("detail", "Order is already closed"));
        assertThat(body, hasEntry("instance", "/test/business"));
        assertThat(body, hasEntry("errorCode", "ORDER_CLOSED"));
        LEGACY_FIELDS.forEach(field -> assertThat(body, not(hasKey(field))));
    }

    @Test
    @DisplayName("Should not leak wrapped cause message of HttpException")
    void shouldNotLeakWrappedCauseMessage() {
        // Given: An HttpException wrapping an internal cause
        final HttpException exception = new HttpException(
            new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT),
            new IllegalStateException(SECRET_MESSAGE)
        );

        // When: Mapping it
        final Map<String, Object> body = body(map(new HttpExceptionMapper(), exception, "/test/business"));

        // Then: Cause message absent
        assertThat(body.toString(), not(containsString(SECRET_MESSAGE)));
        assertThat(body, hasEntry("detail", "Order is already closed"));
    }

    @Test
    @DisplayName("Should keep unsafe error code verbatim and omit type without base URI")
    void shouldKeepErrorCodeVerbatimWhenNoTypeBaseUri() {
        // Given: An error code containing a space and a slash
        final HttpException exception = new HttpException(new TestApiError("BAD CODE/1", "Unsafe code", Response.Status.BAD_REQUEST));

        // When: Mapping it
        final Map<String, Object> body = body(map(new HttpExceptionMapper(), exception, "/test/unsafe-code"));

        // Then: errorCode verbatim, no type
        assertThat(body, not(hasKey("type")));
        assertThat(body, hasEntry("errorCode", "BAD CODE/1"));
    }

    @Test
    @DisplayName("Should add txId when known and omit absent extensions")
    void shouldAddTxIdAndOmitAbsentExtensions() {
        // Given: A known transaction id
        final UUID txId = UUID.randomUUID();
        TransactionId.set(txId);

        // When: Mapping a bad request
        final Map<String, Object> body = body(map(new HttpExceptionMapper(), new BadRequestException(), "/test/bad-request"));

        // Then: txId present; trace, errors and limit members omitted; no nulls
        assertThat(body, hasEntry("txId", txId.toString()));
        assertThat(body, not(hasKey("traceId")));
        assertThat(body, not(hasKey("spanId")));
        assertThat(body, not(hasKey("errors")));
        assertThat(body, not(hasKey("limit")));
        assertThat(body.values(), not(hasItem(nullValue())));
    }

    @Test
    @DisplayName("Should omit txId when no transaction id is known")
    void shouldOmitTxIdWhenUnknown() {
        // Given / When: No transaction id
        final Map<String, Object> body = body(map(new HttpExceptionMapper(), new BadRequestException(), "/test/bad-request"));

        // Then: txId omitted
        assertThat(body, not(hasKey("txId")));
    }

    @Test
    @DisplayName("Should list each violation of a ValidationException in errors extension")
    void shouldListViolationsWhenValidationExceptionThrown() {
        // Given: A ValidationException with two violations
        final ValidationResult result = new ValidationResult();
        result.rejectValue("name", "name.required");
        result.rejectValue("email", "email.required");

        // When: Mapping it
        final Response response = map(new ValidationExceptionMapper(), new ValidationException(result), "/test/validation-error");
        final Map<String, Object> body = body(response);

        // Then: 400 VALIDATION_ERROR with both violations
        assertThat(response.getStatus(), is(400));
        assertThat(response.getMediaType().toString(), is(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("errorCode", "VALIDATION_ERROR"));
        assertThat(
            (List<?>) body.get("errors"),
            containsInAnyOrder(
                Map.of("field", "name", "code", "name.required"),
                Map.of("field", "email", "code", "email.required")
            )
        );
    }

    @Test
    @DisplayName("Should render rate limit as Problem Details and keep rate-limit headers")
    void shouldRenderRateLimitAndKeepHeaders() {
        // Given: A RateLimitExceededException(100, 0, reset)
        final RateLimitExceededException exception =
            new RateLimitExceededException(100, 0, OffsetDateTime.parse("2030-01-01T12:00:00Z"));

        // When: Mapping it
        final Response response = map(new RateLimitExceededExceptionMapper(), exception, "/test/rate-limit");
        final Map<String, Object> body = body(response);

        // Then: 429 with limit extensions and headers
        assertThat(response.getStatus(), is(429));
        assertThat(response.getMediaType().toString(), is(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("status", 429));
        assertThat(body, hasEntry("limit", 100));
        assertThat(body, hasEntry("remaining", 0));
        assertThat(body, hasKey("resetDate"));
        assertThat(response.getHeaderString("X-RateLimit-Limit"), is("100"));
        assertThat(response.getHeaderString("X-RateLimit-Remaining"), is("0"));
        assertThat(response.getHeaderString("X-RateLimit-Reset"), is(notNullValue()));
    }

    @Test
    @DisplayName("Should render invalid sort, search and page with rejected input extensions")
    void shouldRenderInvalidSortSearchPageWithRejectedInput() {
        // Given / When: Invalid sort, search and page inputs
        final Map<String, Object> sort = body(
            map(new InvalidSortExceptionMapper(), new InvalidSortException("password", List.of("name", "createdAt")), "/test/invalid-sort")
        );
        final Map<String, Object> search = body(
            map(
                new InvalidSearchExceptionMapper(),
                new InvalidSearchException("age", "abc", List.of("name", "age")),
                "/test/invalid-search"
            )
        );
        final Map<String, Object> page = body(
            map(new InvalidPageExceptionMapper(), new InvalidPageException("size", -1), "/test/invalid-page")
        );

        // Then: Error codes and rejected input are top-level
        assertThat(sort, hasEntry("status", 400));
        assertThat(sort, hasEntry("errorCode", "INVALID_SORT"));
        assertThat(sort, hasEntry("rejectedField", "password"));
        assertThat(sort, hasEntry("sortableFields", List.of("name", "createdAt")));
        assertThat(search, hasEntry("errorCode", "INVALID_SEARCH"));
        assertThat(search, hasEntry("rejectedField", "age"));
        assertThat(search, hasEntry("rejectedValue", "abc"));
        assertThat(search, hasEntry("searchableFields", List.of("name", "age")));
        assertThat(page, hasEntry("errorCode", "INVALID_PAGE"));
        assertThat(page, hasEntry("rejectedParameter", "size"));
        assertThat(page, hasEntry("rejectedValue", -1));
    }

    @Test
    @DisplayName("Should render unexpected failure as generic 500 without internals")
    void shouldRenderGeneric500WithoutInternals() {
        // Given: An unhandled IllegalStateException
        final IllegalStateException exception = new IllegalStateException(SECRET_MESSAGE);

        // When: Mapping it with the fallback mapper
        final Response response = map(new ThrowableMapper(), exception, "/test/unexpected");
        final Map<String, Object> body = body(response);

        // Then: Generic 500, no message, class name or stack trace
        assertThat(response.getStatus(), is(500));
        assertThat(response.getMediaType().toString(), is(ProblemJson.PROBLEM_JSON));
        assertThat(body, hasEntry("errorCode", "INTERNAL_SERVER_ERROR"));
        assertThat(body, hasEntry("title", "Internal Server Error"));
        assertThat(body, hasKey("detail"));
        assertThat(body.toString(), not(containsString(SECRET_MESSAGE)));
        assertThat(body.toString(), not(containsString("IllegalStateException")));
    }

    @Test
    @DisplayName("Should let application enrichers add extensions, override errorCode/detail, in priority order")
    void shouldApplyApplicationEnrichersInPriorityOrder() {
        // Given: An unannotated (late) enricher registered before a @Priority (early) one
        final ErrorResponseEntityBuilder customBuilder = aBuilder(builtInEnrichersPlus(new LateEnricher(), new EarlyEnricher()));
        final ContainerRequestContext request = aRequest("/test/not-found");

        // When: Mapping an exception
        final Response response = wire(new HttpExceptionMapper(), customBuilder, request).toResponse(new BadRequestException());
        final Map<String, Object> body = body(response);

        // Then: Extension added, override applied after built-ins, late enricher wins
        assertThat(response.getStatus(), is(400));
        assertThat(body, hasEntry("tenant", "acme"));
        assertThat(body, hasEntry("errorCode", "ORDER_NOT_FOUND"));
        assertThat(body, hasEntry("detail", "Order does not exist"));
        assertThat(body, hasEntry("enricherOrder", "late"));
    }

    /** Unordered application enricher: runs after built-ins and prioritised enrichers. */
    static final class LateEnricher implements ErrorResponseEnricher {

        @Override
        public void doEnrich(final ErrorResponseResource resource, final Throwable throwable,
            final ContainerRequestContext requestContext, final int statusCode) {
            Extensions.add(resource, "tenant", "acme");
            Extensions.add(resource, "enricherOrder", "late");
            resource.setError(new TestApiError("ORDER_NOT_FOUND", "Order does not exist", Response.Status.NOT_FOUND));
        }
    }


    /** Prioritised application enricher. */
    @Priority(5000)
    static final class EarlyEnricher implements ErrorResponseEnricher {

        @Override
        public void doEnrich(final ErrorResponseResource resource, final Throwable throwable,
            final ContainerRequestContext requestContext, final int statusCode) {
            Extensions.add(resource, "enricherOrder", "early");
        }
    }
}
