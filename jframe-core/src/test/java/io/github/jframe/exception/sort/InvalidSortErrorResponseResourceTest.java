package io.github.jframe.exception.sort;

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

@DisplayName("Unit Test - InvalidSortErrorResponseResource")
class InvalidSortErrorResponseResourceTest extends UnitTest {

    @Test
    @DisplayName("Should serialise rejected input as top-level extension members")
    void shouldSerialiseRejectedInputAsExtensions() {
        // Given: An invalid sort exception
        final InvalidSortException exception = new InvalidSortException("badField", List.of("name", "email"));

        // When: Serialising its resource
        final Map<String, Object> body = ProblemJson.toMap(new InvalidSortErrorResponseResource(exception));

        // Then: Rejected input is top-level; legacy cause is gone
        assertThat(body, hasEntry("rejectedField", "badField"));
        assertThat(body, hasEntry("sortableFields", List.of("name", "email")));
        assertThat(body, not(hasKey("cause")));
    }

    @Test
    @DisplayName("Should serialise sortableFields as empty array when none given")
    void shouldSerialiseEmptySortableFields() {
        // Given: An invalid sort exception without sortable fields
        final InvalidSortException exception = new InvalidSortException("x", null);

        // When: Serialising its resource
        final Map<String, Object> body = ProblemJson.toMap(new InvalidSortErrorResponseResource(exception));

        // Then: The member is an empty array
        assertThat(body, hasEntry("sortableFields", List.of()));
    }
}
