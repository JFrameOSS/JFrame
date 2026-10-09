package io.github.jframe.exception.search;

import io.github.support.ProblemJson;
import io.github.support.UnitTest;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

@DisplayName("Unit Test - InvalidSearchErrorResponseResource")
class InvalidSearchErrorResponseResourceTest extends UnitTest {

    @Test
    @DisplayName("Should serialise rejected input as top-level extension members")
    void shouldSerialiseRejectedInputAsExtensions() {
        // Given: An invalid search exception
        final InvalidSearchException exception = new InvalidSearchException("status", "BOGUS", List.of("status", "name"));

        // When: Serialising its resource
        final Map<String, Object> body = ProblemJson.toMap(new InvalidSearchErrorResponseResource(exception));

        // Then: Rejected input is top-level; legacy cause is gone
        assertThat(body, hasEntry("rejectedField", "status"));
        assertThat(body, hasEntry("rejectedValue", "BOGUS"));
        assertThat(body, hasEntry("searchableFields", List.of("status", "name")));
        assertThat(body, not(hasKey("cause")));
    }

    @Test
    @DisplayName("Should omit rejectedValue when null")
    void shouldOmitRejectedValueWhenNull() {
        // Given: An invalid search exception without a rejected value
        final InvalidSearchException exception = new InvalidSearchException("unknown", null, List.of("name"));

        // When: Serialising its resource
        final Map<String, Object> body = ProblemJson.toMap(new InvalidSearchErrorResponseResource(exception));

        // Then: The member is omitted, not null
        assertThat(body, not(hasKey("rejectedValue")));
    }
}
