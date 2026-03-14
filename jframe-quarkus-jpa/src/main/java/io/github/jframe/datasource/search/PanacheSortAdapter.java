package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.quarkus.panache.common.Sort;

import java.util.List;

/**
 * Utility class for converting a list of {@link SortableColumn} values to a Panache {@link Sort}.
 *
 * <p>All methods are static; this class cannot be instantiated.
 */
public final class PanacheSortAdapter {

    private static final String DESCENDING = "DESC";

    private PanacheSortAdapter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a direction string ("ASC"/"asc" or "DESC"/"desc") to a Panache {@link Sort.Direction}.
     *
     * @param direction the direction string, case-insensitive
     * @return {@link Sort.Direction#Ascending} or {@link Sort.Direction#Descending}
     */
    private static Sort.Direction toDirection(final String direction) {
        if (DESCENDING.equalsIgnoreCase(direction)) {
            return Sort.Direction.Descending;
        }
        return Sort.Direction.Ascending;
    }

    /**
     * Converts a list of {@link SortableColumn} entries to a Panache {@link Sort}.
     *
     * <p>Returns {@code null} when {@code columns} is {@code null}.
     * Returns an empty {@link Sort} when the list is empty.
     * Direction strings are case-insensitive.
     *
     * @param columns the sort columns, may be {@code null}
     * @return the corresponding Panache {@link Sort}, or {@code null} when {@code columns} is {@code null}
     */
    public static Sort toSort(final List<SortableColumn> columns) {
        if (columns == null) {
            return null;
        }
        return buildSort(columns);
    }

    private static Sort buildSort(final List<SortableColumn> columns) {
        if (columns.isEmpty()) {
            return Sort.empty();
        }

        final SortableColumn first = columns.get(0);
        Sort sort = Sort.by(first.getName(), toDirection(first.getDirection()));

        for (int i = 1; i < columns.size(); i++) {
            final SortableColumn column = columns.get(i);
            sort = sort.and(column.getName(), toDirection(column.getDirection()));
        }

        return sort;
    }
}
