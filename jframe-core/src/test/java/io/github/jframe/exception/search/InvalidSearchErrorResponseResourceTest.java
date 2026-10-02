package io.github.jframe.exception.search;

import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Unit Test - InvalidSearchErrorResponseResource")
class InvalidSearchErrorResponseResourceTest extends UnitTest {

    @Test
    @DisplayName("Should carry rejected field, value and searchable fields from exception")
    void shouldCarryDetailsFromException() {
        // Given: An invalid search exception
        final InvalidSearchException exception = new InvalidSearchException("status", "BOGUS", List.of("status", "name"));

        // When: Creating the response resource
        final InvalidSearchErrorResponseResource resource = new InvalidSearchErrorResponseResource(exception);

        // Then: Details are copied
        assertThat(resource.getRejectedField(), is(equalTo("status")));
        assertThat(resource.getRejectedValue(), is(equalTo("BOGUS")));
        assertThat(resource.getSearchableFields(), contains("status", "name"));
    }

    @Test
    @DisplayName("Should keep rejected value null when field is unknown")
    void shouldKeepRejectedValueNullWhenFieldUnknown() {
        // Given: An unknown-field exception
        final InvalidSearchException exception = new InvalidSearchException("unknown", null, List.of("name"));

        // When: Creating the response resource
        final InvalidSearchErrorResponseResource resource = new InvalidSearchErrorResponseResource(exception);

        // Then: Rejected value is null
        assertThat(resource.getRejectedValue(), is(nullValue()));
    }
}
