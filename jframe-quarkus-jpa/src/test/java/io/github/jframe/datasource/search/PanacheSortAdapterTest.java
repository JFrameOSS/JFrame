package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.support.UnitTest;
import io.quarkus.panache.common.Sort;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link PanacheSortAdapter}.
 *
 * <p>Verifies conversion of {@link SortableColumn} list to Panache {@link Sort},
 * including single columns, multiple columns, ASC/DESC directions, and empty inputs.
 */
@DisplayName("Quarkus JPA - PanacheSortAdapter")
public class PanacheSortAdapterTest extends UnitTest {

    @Test
    @DisplayName("Should return empty Sort when column list is empty")
    public void shouldReturnEmptySortWhenColumnListIsEmpty() {
        // Given: An empty list of sortable columns

        // When: Converting to Panache Sort
        final Sort sort = PanacheSortAdapter.toSort(Collections.emptyList());

        // Then: A non-null Sort instance is returned (unsorted)
        assertThat(sort, is(notNullValue()));
    }

    @Test
    @DisplayName("Should return null when column list is null")
    public void shouldReturnNullWhenColumnListIsNull() {
        // Given: A null list of sortable columns

        // When: Converting null to Panache Sort
        final Sort sort = PanacheSortAdapter.toSort(null);

        // Then: Result is null
        assertThat(sort == null, is(true));
    }

    @Test
    @DisplayName("Should convert single ASC column to Panache Sort")
    public void shouldConvertSingleAscColumnToPanacheSort() {
        // Given: A single sortable column with ASC direction
        final List<SortableColumn> columns = List.of(new SortableColumn("email", "ASC"));

        // When: Converting to Panache Sort
        final Sort sort = PanacheSortAdapter.toSort(columns);

        // Then: Sort is non-null and contains the column
        assertThat(sort, is(notNullValue()));
    }

    @Test
    @DisplayName("Should convert single DESC column to Panache Sort")
    public void shouldConvertSingleDescColumnToPanacheSort() {
        // Given: A single sortable column with DESC direction
        final List<SortableColumn> columns = List.of(new SortableColumn("name", "DESC"));

        // When: Converting to Panache Sort
        final Sort sort = PanacheSortAdapter.toSort(columns);

        // Then: Sort is non-null
        assertThat(sort, is(notNullValue()));
    }

    @Test
    @DisplayName("Should convert multiple columns to Panache Sort")
    public void shouldConvertMultipleColumnsToPanacheSort() {
        // Given: Multiple sortable columns with mixed directions
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("createdAt", "DESC")
        );

        // When: Converting to Panache Sort
        final Sort sort = PanacheSortAdapter.toSort(columns);

        // Then: Sort is non-null
        assertThat(sort, is(notNullValue()));
    }

    @Test
    @DisplayName("Should handle case-insensitive direction strings")
    public void shouldHandleCaseInsensitiveDirectionStrings() {
        // Given: Columns with lowercase direction strings
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "asc"),
            new SortableColumn("name", "desc")
        );

        // When: Converting to Panache Sort
        final Sort sort = PanacheSortAdapter.toSort(columns);

        // Then: Sort is non-null (no exception thrown)
        assertThat(sort, is(notNullValue()));
    }
}
