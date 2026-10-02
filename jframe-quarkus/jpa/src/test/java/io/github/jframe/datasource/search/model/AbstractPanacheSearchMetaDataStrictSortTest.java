package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.support.UnitTest;
import io.quarkus.panache.common.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AbstractPanacheSearchMetaData Strict Sort Tests")
class AbstractPanacheSearchMetaDataStrictSortTest extends UnitTest {

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
        final var columns = List.of(new SortableColumn("name", "SIDEWAYS"));

        // When / Then
        assertThrows(InvalidSortException.class, () -> meta.toSort(columns));
    }

    @Test
    @DisplayName("Should report rejected field and allowed fields in exception")
    void shouldReportRejectedAndAllowedFields() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("unknown", "ASC"));

        // When
        final var ex = assertThrows(InvalidSortException.class, () -> meta.toSort(columns));

        // Then
        assertThat(ex.getRejectedField(), is("unknown"));
        assertThat(ex.getSortableFields(), containsInAnyOrder("name", "email", "virtual"));
    }

    @Test
    @DisplayName("Should accept lowercase 'asc' direction")
    void shouldAcceptLowercaseAscDirection() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("name", "asc"));

        // When / Then — no exception
        final Sort sort = meta.toSort(columns);
        assertThat(sort, is(notNullValue()));
    }

    @Test
    @DisplayName("Should accept mixed-case 'Desc' direction")
    void shouldAcceptMixedCaseDescDirection() {
        // Given
        final var meta = new TestMetaData();
        final var columns = List.of(new SortableColumn("email", "Desc"));

        // When / Then — no exception
        final Sort sort = meta.toSort(columns);
        assertThat(sort.getColumns().get(0).getDirection(), is(Sort.Direction.Descending));
    }

    // =========================================================================
    // defaultSort
    // =========================================================================

    @Test
    @DisplayName("Should apply defaultSort when sortOrder is null and mark isDefault=true")
    void shouldApplyDefaultSortWhenNull() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(0, 10, null, Collections.emptyList());

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getAppliedSort().isDefault(), is(true));
        assertThat(resolved.getAppliedSort().getField(), is("name"));
    }

    @Test
    @DisplayName("Should apply defaultSort when sortOrder is empty and mark isDefault=true")
    void shouldApplyDefaultSortWhenEmpty() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(0, 10, Collections.emptyList(), Collections.emptyList());

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getAppliedSort().isDefault(), is(true));
        assertThat(resolved.getAppliedSort().getField(), is("name"));
    }

    @Test
    @DisplayName("Should mark isDefault=false when explicit sort provided")
    void shouldMarkIsDefaultFalseForExplicitSort() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(
            0,
            10,
            List.of(new SortableColumn("email", "DESC")),
            Collections.emptyList()
        );

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getAppliedSort().isDefault(), is(false));
        assertThat(resolved.getAppliedSort().getField(), is("email"));
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
        final var sort = meta.toSort(columns);

        // Then: tiebreaker 'id' appended
        final var colNames = sort.getColumns().stream()
            .map(Sort.Column::getName).toList();
        assertThat(colNames.contains("id"), is(true));
        final var tiebreakerCol = sort.getColumns().stream()
            .filter(c -> "id".equals(c.getName())).findFirst().orElseThrow();
        assertThat(tiebreakerCol.getDirection(), is(Sort.Direction.Ascending));
    }

    @Test
    @DisplayName("Should not append duplicate tiebreaker when already present")
    void shouldNotAppendDuplicateTiebreaker() {
        // Given: 'id' is sortable and explicitly requested
        final var meta = new WithIdSortableMetaData();
        final var columns = List.of(new SortableColumn("id", "DESC"));

        // When
        final var sort = meta.toSort(columns);

        // Then: only one 'id' column
        final long idCount = sort.getColumns().stream()
            .filter(c -> "id".equals(c.getName())).count();
        assertThat(idCount, is(1L));
    }

    @Test
    @DisplayName("Should skip tiebreaker when tiebreakerProperty returns null")
    void shouldSkipTiebreakerWhenNull() {
        // Given
        final var meta = new NoTiebreakerMetaData();
        final var columns = List.of(new SortableColumn("name", "ASC"));

        // When
        final var sort = meta.toSort(columns);

        // Then: only the requested field, no extra column
        assertThat(sort.getColumns(), notNullValue());
        final long idCount = sort.getColumns().stream()
            .filter(c -> "id".equals(c.getName())).count();
        assertThat(idCount, is(0L));
    }

    // =========================================================================
    // Virtual fields
    // =========================================================================

    @Test
    @DisplayName("Should resolve to empty Sort when virtual sort field used")
    void shouldResolveToEmptySortForVirtualField() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(
            0,
            10,
            List.of(new SortableColumn("virtual", "ASC")),
            Collections.emptyList()
        );

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.isVirtual(), is(true));
        assertThat(resolved.getSort().getColumns(), notNullValue());
    }

    @Test
    @DisplayName("Should include virtual fields in getAllowedSortFields")
    void shouldIncludeVirtualFieldsInAllowed() {
        // Given
        final var meta = new TestMetaData();

        // When
        final var allowed = meta.getAllowedSortFields();

        // Then
        assertThat(allowed.contains("virtual"), is(true));
        assertThat(allowed.contains("name"), is(true));
        assertThat(allowed.contains("email"), is(true));
    }

    // =========================================================================
    // rejectAnySort
    // =========================================================================

    @Test
    @DisplayName("Should throw InvalidSortException from rejectAnySort when sort is provided")
    void shouldThrowFromRejectAnySortWhenProvided() {
        // Given
        final var sortOrder = List.of(new SortableColumn("name", "ASC"));

        // When / Then
        assertThrows(
            InvalidSortException.class,
            () -> AbstractPanacheSearchMetaData.rejectAnySort(sortOrder)
        );
    }

    @Test
    @DisplayName("Should not throw from rejectAnySort when sort is empty")
    void shouldNotThrowFromRejectAnySortWhenEmpty() {
        // Given / When / Then
        AbstractPanacheSearchMetaData.rejectAnySort(Collections.emptyList());
    }

    @Test
    @DisplayName("Should not throw from rejectAnySort when sort is null")
    void shouldNotThrowFromRejectAnySortWhenNull() {
        // Given / When / Then
        AbstractPanacheSearchMetaData.rejectAnySort(null);
    }

    // =========================================================================
    // resolveSort — page metadata
    // =========================================================================

    @Test
    @DisplayName("Should set correct pageNumber and pageSize in resolveSort")
    void shouldSetPageMetadata() {
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
        assertThat(resolved.getPage().index, is(2));
        assertThat(resolved.getPage().size, is(15));
    }

    @Test
    @DisplayName("Should use defaultPageSize when input pageSize is 0")
    void shouldUseDefaultPageSizeWhenZero() {
        // Given
        final var meta = new TestMetaData();
        final var input = new SortablePageInput(0, 0, Collections.emptyList(), Collections.emptyList());

        // When
        final var resolved = meta.resolveSort(input);

        // Then
        assertThat(resolved.getPage().size, is(20)); // default
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    static class TestMetaData extends AbstractPanacheSearchMetaData {

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


    static class WithIdSortableMetaData extends AbstractPanacheSearchMetaData {

        WithIdSortableMetaData() {
            addField("id", "id", SearchType.NUMERIC, true);
        }

        @Override
        protected List<SortableColumn> defaultSort() {
            return List.of(new SortableColumn("id", "ASC"));
        }
    }


    static class NoTiebreakerMetaData extends AbstractPanacheSearchMetaData {

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
}
