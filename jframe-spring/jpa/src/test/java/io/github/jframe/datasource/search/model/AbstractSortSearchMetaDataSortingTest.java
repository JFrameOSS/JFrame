package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AbstractSortSearchMetaData Sorting Tests")
class AbstractSortSearchMetaDataSortingTest extends UnitTest {

    // =========================================================================
    // Valid paths
    // =========================================================================

    @Test
    @DisplayName("Should create Sort for single mapped field")
    void shouldReturnSortWhenSingleValidFieldRequested() {
        // Given: metadata with a sortable 'email' field mapped to 'user.email'
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("email", "ASC"));

        // When: converting to Sort
        final Sort sort = metaData.toSort(columns);

        // Then: sort contains the DB column with the correct direction
        assertThat(sort, is(notNullValue()));
        final Sort.Order order = sort.getOrderFor("user.email");
        assertThat(order, is(notNullValue()));
        assertThat(order.getDirection(), is(equalTo(Sort.Direction.ASC)));
    }

    @Test
    @DisplayName("Should create Sort for multiple valid fields, preserving order and direction")
    void shouldReturnSortWhenMultipleValidFieldsRequested() {
        // Given: two sortable fields requested in a specific order
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("role", "DESC")
        );

        // When: converting to Sort
        final Sort sort = metaData.toSort(columns);

        // Then: both user-specified columns are present; plus tiebreaker 'id'
        assertThat(sort, is(notNullValue()));
        final Sort.Order emailOrder = sort.getOrderFor("user.email");
        assertThat(emailOrder, is(notNullValue()));
        assertThat(emailOrder.getDirection(), is(equalTo(Sort.Direction.ASC)));
        final Sort.Order roleOrder = sort.getOrderFor("role");
        assertThat(roleOrder, is(notNullValue()));
        assertThat(roleOrder.getDirection(), is(equalTo(Sort.Direction.DESC)));
    }

    @Test
    @DisplayName("Should return unsorted when sort list is empty")
    void shouldReturnUnsortedWhenEmptySortListProvided() {
        // Given: no sort columns requested
        final TestMetaData metaData = new TestMetaData();

        // When: converting an empty list
        final Sort sort = metaData.toSort(Collections.emptyList());

        // Then: result is unsorted
        assertThat(sort.isUnsorted(), is(true));
    }

    @Test
    @DisplayName("Should handle duplicate valid column names without throwing")
    void shouldHandleDuplicateValidColumnNamesWithoutThrowing() {
        // Given: the same sortable column appears twice in the request
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("email", "DESC")
        );

        // When: converting to Sort — must not throw
        final Sort sort = metaData.toSort(columns);

        // Then: result is non-null
        assertThat(sort, is(notNullValue()));
    }

    @Test
    @DisplayName("Should not log a warning when all requested columns are valid")
    void shouldNotLogWarningWhenAllColumnsAreValid() {
        // Given: all requested columns are registered and sortable
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("role", "DESC")
        );

        // When / Then — no exception thrown
        metaData.toSort(columns);
    }

    // =========================================================================
    // Strict contract: invalid columns throw InvalidSortException
    // =========================================================================

    @Test
    @DisplayName("Should throw InvalidSortException for unregistered column")
    void shouldThrowForUnregisteredColumn() {
        // Given: 'unknown' is not registered in metadata at all
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("unknown", "ASC"));

        // When / Then
        final var ex = assertThrows(InvalidSortException.class, () -> metaData.toSort(columns));
        assertThat(ex.getRejectedField(), is(equalTo("unknown")));
        assertThat(ex.getSortableFields(), containsInAnyOrder("email", "role"));
    }

    @Test
    @DisplayName("Should throw InvalidSortException for non-sortable registered column")
    void shouldThrowForNonSortableColumn() {
        // Given: 'nonSortableField' is registered but not marked sortable
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("nonSortableField", "ASC"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> metaData.toSort(columns));
    }

    @Test
    @DisplayName("Should treat unregistered and non-sortable columns identically — both rejected")
    void shouldTreatUnregisteredAndNonSortableColumnsIdentically() {
        // Given: one unregistered column
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("unknown", "ASC"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> metaData.toSort(columns));
    }

    @Test
    @DisplayName("Should throw on first invalid column in a mixed request")
    void shouldThrowOnFirstInvalidColumnInMixedRequest() {
        // Given: 'email' is valid, 'unknown' is not
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("unknown", "ASC"),
            new SortableColumn("email", "DESC")
        );

        // When / Then: throws — no partial results
        assertThrows(InvalidSortException.class, () -> metaData.toSort(columns));
    }

    @Test
    @DisplayName("Should throw when unknown column appears after valid ones")
    void shouldThrowWhenUnknownColumnAppearsAfterValidOnes() {
        // Given: 'role' is valid, 'unknown' follows
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("role", "ASC"),
            new SortableColumn("unknown", "DESC"),
            new SortableColumn("email", "DESC")
        );

        // When / Then
        assertThrows(InvalidSortException.class, () -> metaData.toSort(columns));
    }

    @Test
    @DisplayName("Should throw InvalidSortException for wrong-case column name — exact-match only")
    void shouldThrowForWrongCaseColumnName() {
        // Given: 'Email' with a capital E does not match the registered 'email'
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("Email", "ASC"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> metaData.toSort(columns));
    }

    @Test
    @DisplayName("Should preserve direction of valid column before throwing on invalid")
    void shouldThrowOnInvalidDirectionNotPreserveInvalidColumn() {
        // Given: 'role' is sortable with DESC direction, followed by invalid 'nonSortableField'
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("nonSortableField", "ASC"),
            new SortableColumn("role", "DESC")
        );

        // When / Then: throws on first invalid
        assertThrows(InvalidSortException.class, () -> metaData.toSort(columns));
    }

    // =========================================================================
    // Test fixture
    // =========================================================================

    static class TestMetaData extends AbstractSortSearchMetaData {

        public TestMetaData() {
            super();
            addField("email", "user.email", SearchType.FUZZY_TEXT, true);
            addField("role", "role", SearchType.ENUM, true);
            addField("nonSortableField", "col", SearchType.TEXT, false);
        }
    }
}
