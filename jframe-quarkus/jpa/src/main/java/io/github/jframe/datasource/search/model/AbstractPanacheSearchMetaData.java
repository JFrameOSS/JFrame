package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.PanacheSearchSpecification;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.jframe.exception.page.InvalidPageException;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Quarkus/Panache search metadata adapter. Extends {@link AbstractSearchMetaData} with
 * Panache {@link Sort}, {@link Page}, and {@link PanacheSearchSpecification} support.
 *
 * @see AbstractSearchMetaData
 */
@SuppressWarnings("ClassDataAbstractionCoupling")
public abstract class AbstractPanacheSearchMetaData extends AbstractSearchMetaData {

    private static final String DESCENDING = "DESC";
    private static final String PAGE_NUMBER = "pageNumber";

    /**
     * Resolves the effective sort and paging from the input.
     *
     * @param input the sortable page input
     * @return the resolved sort result
     * @throws InvalidSortException when a sort field or direction is rejected
     * @throws InvalidPageException when the page number is negative or offset overflows {@code int}
     */
    public PanacheResolvedSort resolveSort(final SortablePageInput input) {
        final ResolvedSortCore core = resolveCore(input);

        if ((long) core.getPageNumber() * core.getPageSize() > Integer.MAX_VALUE) {
            throw new InvalidPageException(PAGE_NUMBER, core.getPageNumber());
        }
        final Page page = Page.of(core.getPageNumber(), core.getPageSize());

        if (core.getColumns().isEmpty()) {
            return new PanacheResolvedSort(page, Sort.empty(), null, Collections.emptyList(), false);
        }

        final Sort panacheSort = core.isVirtual() ? Sort.empty() : buildPanacheSort(core.getColumns());

        return new PanacheResolvedSort(page, panacheSort, core.getAppliedSort(), core.getColumns(), core.isVirtual());
    }

    /**
     * Convert sort columns into a Panache {@link Sort}.
     *
     * <p>Returns {@link Sort#empty()} for null/empty input.
     * Unknown/non-sortable fields or invalid directions throw {@link InvalidSortException}.
     *
     * @param sortOrders sort columns requested by the client
     * @return Panache Sort
     * @throws InvalidSortException when a column is rejected
     */
    public Sort toSort(final List<SortableColumn> sortOrders) {
        if (sortOrders == null || sortOrders.isEmpty()) {
            return Sort.empty();
        }
        return buildPanacheSort(normalise(sortOrders));
    }

    /**
     * Build a {@link PanacheSearchSpecification} from the search inputs in a {@link SortablePageInput}.
     *
     * <p><b>Warning:</b> virtual sort keys are not ordered by jFrame on Quarkus; the consumer must order them.
     *
     * @param <T>   the entity type
     * @param input the sortable page input containing search inputs
     * @return a new PanacheSearchSpecification wrapping the derived criteria
     */
    public <T> PanacheSearchSpecification<T> toSearchSpecification(final SortablePageInput input) {
        return new PanacheSearchSpecification<>(toSearchCriteria(input.getSearchInputs()));
    }

    /** Builds a Panache {@link Sort} with tiebreaker appended unless already present. */
    private Sort buildPanacheSort(final List<SortableColumn> columns) {
        if (columns.isEmpty()) {
            return Sort.empty();
        }

        Sort result = Sort.empty();
        final Set<String> addedColumns = new LinkedHashSet<>();

        for (final SortableColumn col : columns) {
            final List<String> dbCols = getColumnNames().get(col.getName());
            if (dbCols == null) {
                continue;
            }
            final Sort.Direction dir = toDirection(col.getDirection());
            for (final String dbCol : dbCols) {
                result = result.and(dbCol, dir);
                addedColumns.add(dbCol);
            }
        }

        for (final SortableColumn tiebreaker : tiebreaker()) {
            if (addedColumns.add(tiebreaker.getName())) {
                result = result.and(tiebreaker.getName(), toDirection(tiebreaker.getDirection()));
            }
        }

        return result;
    }

    private static Sort.Direction toDirection(final String direction) {
        return DESCENDING.equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending;
    }
}
