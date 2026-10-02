package io.github.jframe.exception.sort;

import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Unit Test - InvalidSortErrorResponseResource")
class InvalidSortErrorResponseResourceTest extends UnitTest {

    @Test
    @DisplayName("Should carry rejectedField from exception")
    void shouldCarryRejectedField() {
        // Given
        final var ex = new InvalidSortException("badField", List.of("name", "email"));

        // When
        final var resource = new InvalidSortErrorResponseResource(ex);

        // Then
        assertThat(resource.getRejectedField(), is(equalTo("badField")));
    }

    @Test
    @DisplayName("Should carry sortableFields from exception")
    void shouldCarrySortableFields() {
        // Given
        final var ex = new InvalidSortException("x", List.of("name", "email", "status"));

        // When
        final var resource = new InvalidSortErrorResponseResource(ex);

        // Then
        assertThat(resource.getSortableFields(), containsInAnyOrder("name", "email", "status"));
    }

    @Test
    @DisplayName("Should be non-null when constructed")
    void shouldBeNonNull() {
        // Given
        final var ex = new InvalidSortException("f", List.of());

        // When
        final var resource = new InvalidSortErrorResponseResource(ex);

        // Then
        assertThat(resource, is(notNullValue()));
    }

    @Test
    @DisplayName("Should propagate null sortableFields without NPE")
    void shouldPropagateNullSortableFieldsWithoutNpe() {
        // Given
        final var ex = new InvalidSortException("x", null);

        // When / Then — no NPE during construction
        final var resource = new InvalidSortErrorResponseResource(ex);
        assertThat(resource, is(notNullValue()));
    }
}
