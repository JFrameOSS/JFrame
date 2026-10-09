package io.github.jframe.exception.resource;

import io.github.jframe.exception.sort.InvalidSortErrorResponseResource;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.support.Extensions;
import io.github.support.ProblemJson;
import io.github.support.UnitTest;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - ErrorResponseResource reserved member names and RFC 9457 schema")
class ErrorResponseResourceReservedNamesTest extends UnitTest {

    private static final String SHADOW = "shadow";

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
            "type",
            "title",
            "status",
            "detail",
            "instance",
            "errorCode",
            "txId",
            "traceId",
            "spanId"
        }
    )
    @DisplayName("Should ignore extension named like a standard or jFrame member")
    void shouldIgnoreExtensionWhenNameIsReserved(final String reserved) {
        // Given: A fully populated resource
        final ErrorResponseResource resource = aPopulatedResource();
        final Object original = ProblemJson.toMap(resource).get(reserved);

        // When: Adding an extension with a reserved name
        Extensions.add(resource, reserved, SHADOW);
        final String json = ProblemJson.toJson(resource);

        // Then: Exactly one key, original value kept, not exposed as extension
        assertThat(occurrences(json, reserved), is(1));
        assertThat(ProblemJson.parse(json).get(reserved), is(equalTo(original)));
        assertThat(resource.getExtensions(), not(hasKey(reserved)));
    }

    @Test
    @DisplayName("Should ignore extension named like a typed field of the subclass")
    void shouldIgnoreExtensionWhenNameEqualsSubclassField() {
        // Given: An invalid-sort resource with typed members
        final InvalidSortErrorResponseResource resource = new InvalidSortErrorResponseResource(
            new InvalidSortException("password", List.of("name"))
        );

        // When: Adding extensions shadowing its typed fields
        Extensions.add(resource, "rejectedField", SHADOW);
        Extensions.add(resource, "sortableFields", SHADOW);
        final String json = ProblemJson.toJson(resource);

        // Then: Each key once, typed values kept
        assertThat(occurrences(json, "rejectedField"), is(1));
        assertThat(occurrences(json, "sortableFields"), is(1));
        final Map<String, Object> body = ProblemJson.parse(json);
        assertThat(body, hasEntry("rejectedField", "password"));
        assertThat(body, hasEntry("sortableFields", List.of("name")));
    }

    @Test
    @DisplayName("Should satisfy RFC 9457 schema when status is unknown and instance is absent")
    void shouldSatisfySchemaWhenStatusUnknownAndInstanceAbsent() {
        // Given: An unregistered status and no request (instance null)
        final ErrorResponseResource resource = new ErrorResponseResource();
        ProblemDetails.apply(resource, 599, null, null);

        // When: Serialising it
        final String json = ProblemJson.toJson(resource);

        // Then: Valid problem, title still a non-blank string, instance omitted
        ProblemJson.assertRfc9457(json, 599);
        assertThat(ProblemJson.parse(json), not(hasKey("instance")));
        assertThat(resource.getTitle(), not(blankOrNullString()));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
            "{\"status\":\"409\"}",
            "{\"status\":42}",
            "{\"type\":\"http://bad uri\"}",
            "{\"title\":1}"
        }
    )
    @DisplayName("Should reject non-conforming body (schema check is effective)")
    void shouldRejectBodyWhenNotConforming(final String json) {
        // Given: A body violating RFC 9457 Appendix A
        // When: Checking it
        final AssertionError error = assertThrows(
            AssertionError.class,
            () -> ProblemJson.assertRfc9457(json, null)
        );

        // Then: The violation is reported
        assertThat(error, is(notNullValue()));
    }

    private static ErrorResponseResource aPopulatedResource() {
        final ErrorResponseResource resource = new ErrorResponseResource();
        resource.setErrorCode("ORDER_CLOSED");
        resource.setDetail("Order is already closed");
        ProblemDetails.apply(resource, 409, "/orders/1", "https://example.test/problems/");
        resource.setTxId("tx");
        resource.setTraceId("trace");
        resource.setSpanId("span");
        return resource;
    }

    private static int occurrences(final String json, final String key) {
        return (int) Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:").matcher(json).results().count();
    }
}
