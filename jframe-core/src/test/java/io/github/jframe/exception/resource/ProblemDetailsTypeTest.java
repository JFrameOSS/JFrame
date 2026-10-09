package io.github.jframe.exception.resource;

import io.github.support.ProblemJson;
import io.github.support.UnitTest;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Unit Test - ProblemDetails type")
class ProblemDetailsTypeTest extends UnitTest {

    private static final String BASE = "https://errors.example.com/";

    @ParameterizedTest
    @NullSource
    @ValueSource(
        strings = {
            "",
            "   "
        }
    )
    @DisplayName("Should omit type when base URI is not configured")
    void shouldReturnNullTypeWhenBaseUriNotConfigured(final String baseUri) {
        // When
        final String type = ProblemDetails.type(baseUri, "ORDER_CLOSED");

        // Then: about:blank semantics, member omitted
        assertThat(type, is(nullValue()));
    }

    @Test
    @DisplayName("Should build type from configured base and percent-encoded error code")
    void shouldBuildEncodedTypeWhenBaseUriConfigured() {
        // When
        final String type = ProblemDetails.type(BASE, "BAD CODE/1");

        // Then
        assertThat(type, is(BASE + "BAD%20CODE%2F1"));
    }

    @Test
    @DisplayName("Should not serialise type and stay RFC 9457 conformant when base URI is not configured")
    void shouldOmitTypeMemberWhenBaseUriNotConfigured() {
        // Given
        final ErrorResponseResource resource = new ErrorResponseResource();
        resource.setErrorCode("ORDER_CLOSED");

        // When
        ProblemDetails.apply(resource, 409, "/orders/1", null);
        final String json = ProblemJson.toJson(resource);
        final Map<String, Object> body = ProblemJson.parse(json);

        // Then
        assertThat(body, not(hasKey("type")));
        assertThat(body, hasEntry("status", 409));
        ProblemJson.assertRfc9457(json, 409);
    }
}
