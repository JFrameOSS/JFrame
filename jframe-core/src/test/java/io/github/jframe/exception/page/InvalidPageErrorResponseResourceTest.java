package io.github.jframe.exception.page;

import io.github.support.ProblemJson;
import io.github.support.UnitTest;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

@DisplayName("Unit Test - InvalidPageErrorResponseResource")
class InvalidPageErrorResponseResourceTest extends UnitTest {

    @Test
    @DisplayName("Should serialise rejected input as top-level extension members")
    void shouldSerialiseRejectedInputAsExtensions() {
        // Given: An invalid page exception
        final InvalidPageException exception = new InvalidPageException("pageNumber", -2);

        // When: Serialising its resource
        final Map<String, Object> body = ProblemJson.toMap(new InvalidPageErrorResponseResource(exception));

        // Then: Rejected input is top-level; legacy cause is gone
        assertThat(body, hasEntry("rejectedParameter", "pageNumber"));
        assertThat(body, hasEntry("rejectedValue", -2));
        assertThat(body, not(hasKey("cause")));
    }
}
