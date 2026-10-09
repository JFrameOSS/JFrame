package io.github.jframe.exception.resource;

import io.github.support.Extensions;
import io.github.support.ProblemJson;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;

import java.util.Map;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Unit Test - ErrorResponseResource (RFC 9457)")
class ErrorResponseResourceTest extends UnitTest {

    @Test
    @DisplayName("Should not expose legacy fields or throwable message")
    void shouldNotExposeLegacyFieldsOrThrowableMessage() {
        // Given: A resource created for an exception with an internal message
        final ErrorResponseResource resource = new ErrorResponseResource(new IllegalStateException("db password=hunter2"));

        // When: Serialising it
        final String json = ProblemJson.toMap(resource).toString();
        final Map<String, Object> body = ProblemJson.toMap(resource);

        // Then: No legacy members, no message, no null values
        for (final String legacy : new String[] {
            "statusCode",
            "errorReason",
            "cause",
            "method",
            "uri",
            "query",
            "contentType"
        }) {
            assertThat(body, not(hasKey(legacy)));
        }
        assertThat(json, not(containsString("hunter2")));
        assertThat(body.values(), not(hasItem(nullValue())));
    }

    @Test
    @DisplayName("Should map ApiError to errorCode extension and detail")
    void shouldMapApiErrorToErrorCodeAndDetail() {
        // Given: A resource and an ApiError
        final ErrorResponseResource resource = new ErrorResponseResource();

        // When: Setting the error
        resource.setError(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT));
        final Map<String, Object> body = ProblemJson.toMap(resource);

        // Then: errorCode and detail carry the ApiError values
        assertThat(body, hasEntry("errorCode", "ORDER_CLOSED"));
        assertThat(body, hasEntry("detail", "Order is already closed"));
    }

    @Test
    @DisplayName("Should serialise added extensions at top level and omit null ones")
    void shouldSerialiseExtensionsAtTopLevelAndOmitNull() {
        // Given: A resource with a custom and a null extension
        final ErrorResponseResource resource = new ErrorResponseResource();
        Extensions.add(resource, "tenant", "acme");
        Extensions.add(resource, "empty", null);

        // When: Serialising it
        final Map<String, Object> body = ProblemJson.toMap(resource);

        // Then: Custom member is top-level, null one omitted
        assertThat(body, hasEntry("tenant", "acme"));
        assertThat(body, not(hasKey("empty")));
        assertThat(body, not(hasKey("extensions")));
    }

    @Test
    @DisplayName("Should serialise correlation ids as top-level members only when set")
    void shouldSerialiseCorrelationIdsOnlyWhenSet() {
        // Given: A resource with txId but without trace information
        final ErrorResponseResource resource = new ErrorResponseResource();
        resource.setTxId("tx-1");

        // When: Serialising it
        final Map<String, Object> body = ProblemJson.toMap(resource);

        // Then: txId present, traceId/spanId omitted
        assertThat(body, hasEntry("txId", "tx-1"));
        assertThat(body, not(hasKey("traceId")));
        assertThat(body, not(hasKey("spanId")));
    }
}
