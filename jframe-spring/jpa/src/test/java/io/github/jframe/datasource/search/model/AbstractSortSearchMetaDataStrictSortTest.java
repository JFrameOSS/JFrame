package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AbstractSortSearchMetaData Strict Sort Tests")
class AbstractSortSearchMetaDataStrictSortTest extends UnitTest {

    // =========================================================================
    // InvalidSortException — unknown/non-sortable field
    // =========================================================================

    @Test
    @DisplayName("Should throw InvalidSortException for unknown field")
    void shouldThrowForUnknownField() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("unknown", "ASC"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> meta.toSort(columns));
    }

    @Test
    @DisplayName("Should throw InvalidSortException for non-sortable field")
    void shouldThrowForNonSortableField() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("nonSortable", "ASC"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> meta.toSort(columns));
    }

    @Test
    @DisplayName("Should throw InvalidSortException for null direction")
    void shouldThrowForNullDirection() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", null));

        // When / Then
        assertThrows(InvalidSortException.class, () -> meta.toSort(columns));
    }

    @Test
    @DisplayName("Should throw InvalidSortException for blank direction")
    void shouldThrowForBlankDirection() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", "  "));

        // When / Then
        assertThrows(InvalidSortException.class, () -> meta.toSort(columns));
    }

    @Test
    @DisplayName("Should throw InvalidSortException for invalid direction string")
    void shouldThrowForInvalidDirection() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", "RANDOM"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> meta.toSort(columns));
    }

    @Test
    @DisplayName("Should report rejected field and allowed fields in exception")
    void shouldReportRejectedAndAllowedFields() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("badField", "ASC"));

        // When
        final var ex = assertThrows(InvalidSortException.class, () -> meta.toSort(columns));

        // Then
        assertThat(ex.getRejectedField(), is(equalTo("badField")));
        assertThat(ex.getSortableFields(), containsInAnyOrder("name", "email", "virtual"));
    }

    // =========================================================================
    // Direction case-insensitivity
    // =========================================================================

    @Test
    @DisplayName("Should accept lowercase 'asc' direction")
    void shouldAcceptLowercaseAsc() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", "asc"));

        // When / Then — no exception
        final Sort sort = meta.toSort(columns);
        assertThat(sort.isUnsorted(), is(false));
    }

    @Test
    @DisplayName("Should accept mixed-case 'Desc' direction")
    void shouldAcceptMixedCaseDesc() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", "Desc"));

        // When / Then — no exception
        final Sort sort = meta.toSort(columns);
        assertThat(sort.isUnsorted(), is(false));
    }

    // =========================================================================
    // Default sort — applied when sortOrder is null or empty
    // =========================================================================

    @Test
    @DisplayName("Should apply defaultSort when sortOrder is null and mark isDefault=true")
    void shouldApplyDefaultSortWhenNull() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(0, 25, Collections.emptyList(), null);

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getAppliedSort().isDefault(), is(true));
        assertThat(resolved.getAppliedSort().getField(), is(equalTo("name")));
    }

    @Test
    @DisplayName("Should apply defaultSort when sortOrder is empty and mark isDefault=true")
    void shouldApplyDefaultSortWhenEmpty() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(0, 25, Collections.emptyList(), Collections.emptyList());

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getAppliedSort().isDefault(), is(true));
        assertThat(resolved.getAppliedSort().getField(), is(equalTo("name")));
    }

    @Test
    @DisplayName("Should mark isDefault=false when explicit sort provided")
    void shouldMarkIsDefaultFalseWhenExplicitSort() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(
            0,
            25,
            List.of(new SortableColumn("email", "DESC")),
            Collections.emptyList()
        );

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getAppliedSort().isDefault(), is(false));
        assertThat(resolved.getAppliedSort().getField(), is(equalTo("email")));
        assertThat(resolved.getAppliedSort().getDirection(), is(equalTo("DESC")));
    }

    // =========================================================================
    // Tiebreaker
    // =========================================================================

    @Test
    @DisplayName("Should append tiebreaker ASC when not already present")
    void shouldAppendTiebreaker() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", "ASC"));

        // When
        final Sort sort = meta.toSort(columns);

        // Then: tiebreaker 'id' is appended
        final var orders = sort.toList();
        final boolean hasTiebreaker = orders.stream().anyMatch(o -> "id".equals(o.getProperty()) && o.isAscending());
        assertThat(hasTiebreaker, is(true));
    }

    @Test
    @DisplayName("Should not append duplicate tiebreaker when already present")
    void shouldNotAppendDuplicateTiebreaker() {
        // Given: metadata where 'id' column is sortable
        final var meta = new WithIdSortableMetaData();
        final var columns = List.of(new SortableColumn("id", "DESC"));

        // When
        final Sort sort = meta.toSort(columns);

        // Then: exactly one 'id' order
        final long idCount = sort.toList().stream().filter(o -> "id".equals(o.getProperty())).count();
        assertThat(idCount, is(1L));
    }

    @Test
    @DisplayName("Should skip tiebreaker when tiebreakerProperty returns null")
    void shouldSkipTiebreakerWhenNull() {
        // Given
        final var meta = new NoTiebreakerMetaData();
        final var columns = List.of(new SortableColumn("name", "ASC"));

        // When
        final Sort sort = meta.toSort(columns);

        // Then: no 'id' order appended
        final boolean hasId = sort.toList().stream().anyMatch(o -> "id".equals(o.getProperty()));
        assertThat(hasId, is(false));
    }

    // =========================================================================
    // ignoreCase + nullsLast
    // =========================================================================

    @Test
    @DisplayName("Should apply ignoreCase and nullsLast on all orders")
    void shouldApplyIgnoreCaseAndNullsLast() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", "ASC"));

        // When
        final Sort sort = meta.toSort(columns);

        // Then: the name column order has ignoreCase and nullsLast
        final Sort.Order nameOrder = sort.getOrderFor("name_col");
        assertThat(nameOrder, is(notNullValue()));
        assertThat(nameOrder.isIgnoreCase(), is(true));
        assertThat(nameOrder.getNullHandling(), is(Sort.NullHandling.NULLS_LAST));
    }

    // =========================================================================
    // Multi-column sort
    // =========================================================================

    @Test
    @DisplayName("Should sort by all columns of a multi-column field")
    void shouldSortByAllColumnsOfMultiColumnField() {
        // Given
        final var meta = new MultiColumnMetaData();
        final var columns = List.of(new SortableColumn("fullName", "ASC"));

        // When
        final Sort sort = meta.toSort(columns);

        // Then: both first_name and last_name appear
        final var orders = sort.toList();
        final boolean hasFirst = orders.stream().anyMatch(o -> "first_name".equals(o.getProperty()));
        final boolean hasLast = orders.stream().anyMatch(o -> "last_name".equals(o.getProperty()));
        assertThat(hasFirst, is(true));
        assertThat(hasLast, is(true));
    }

    // =========================================================================
    // Virtual sort fields
    // =========================================================================

    @Test
    @DisplayName("Should resolve to unsorted pageable when virtual sort field used")
    void shouldBeUnsortedWhenVirtualField() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(
            0,
            25,
            List.of(new SortableColumn("virtual", "ASC")),
            Collections.emptyList()
        );

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.isVirtual(), is(true));
        assertThat(resolved.getPageable().getSort().isUnsorted(), is(true));
    }

    @Test
    @DisplayName("Should include virtual fields in getAllowedSortFields")
    void shouldIncludeVirtualFieldsInAllowed() {
        // Given
        final var meta = new TestMetaData();

        // When
        final var allowed = meta.getAllowedSortFields();

        // Then
        assertThat(allowed, containsInAnyOrder("name", "email", "virtual"));
    }

    // =========================================================================
    // rejectAnySort
    // =========================================================================

    @Test
    @DisplayName("Should throw InvalidSortException from rejectAnySort when sort is provided")
    void shouldThrowFromRejectAnySort() {
        // Given
        final var columns = List.of(new SortableColumn("name", "ASC"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> AbstractSortSearchMetaData.rejectAnySort(columns));
    }

    @Test
    @DisplayName("Should not throw from rejectAnySort when sort is empty")
    void shouldNotThrowFromRejectAnySortWhenEmpty() {
        // Given / When / Then — no exception
        AbstractSortSearchMetaData.rejectAnySort(Collections.emptyList());
    }

    @Test
    @DisplayName("Should not throw from rejectAnySort when sort is null")
    void shouldNotThrowFromRejectAnySortWhenNull() {
        // Given / When / Then — no exception
        AbstractSortSearchMetaData.rejectAnySort(null);
    }

    // =========================================================================
    // resolveSort — pageable metadata
    // =========================================================================

    @Test
    @DisplayName("Should set correct pageNumber and pageSize in resolveSort")
    void shouldSetCorrectPageableMetadata() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(
            2,
            15,
            List.of(new SortableColumn("name", "ASC")),
            Collections.emptyList()
        );

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getPageable().getPageNumber(), is(2));
        assertThat(resolved.getPageable().getPageSize(), is(15));
    }

    @Test
    @DisplayName("Should use defaultPageSize when input pageSize is 0")
    void shouldUseDefaultPageSizeWhenZero() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(0, 0, Collections.emptyList(), null);

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getPageable().getPageSize(), is(20)); // default
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    static class TestMetaData extends AbstractSortSearchMetaData {

        TestMetaData() {
            addField("name", "name_col", SearchType.TEXT, true);
            addField("email", "email_col", SearchType.FUZZY_TEXT, true);
            addField("nonSortable", "ns_col", SearchType.TEXT, false);
        }

        @Override
        protected List<SortableColumn> defaultSort() {
            return List.of(new SortableColumn("name", "ASC"));
        }

        @Override
        protected Set<String> virtualSortFields() {
            return Set.of("virtual");
        }
    }


    static class WithIdSortableMetaData extends AbstractSortSearchMetaData {

        WithIdSortableMetaData() {
            addField("id", "id", SearchType.NUMERIC, true);
        }

        @Override
        protected List<SortableColumn> defaultSort() {
            return List.of(new SortableColumn("id", "ASC"));
        }
    }


    static class NoTiebreakerMetaData extends AbstractSortSearchMetaData {

        NoTiebreakerMetaData() {
            addField("name", "name_col", SearchType.TEXT, true);
        }

        @Override
        protected List<SortableColumn> defaultSort() {
            return List.of(new SortableColumn("name", "ASC"));
        }

        @Override
        protected String tiebreakerProperty() {
            return null;
        }
    }


    static class MultiColumnMetaData extends AbstractSortSearchMetaData {

        MultiColumnMetaData() {
            addField("fullName", List.of("first_name", "last_name"), SearchType.MULTI_COLUMN_FUZZY, true);
        }

        @Override
        protected List<SortableColumn> defaultSort() {
            return List.of(new SortableColumn("fullName", "ASC"));
        }
    }
}
