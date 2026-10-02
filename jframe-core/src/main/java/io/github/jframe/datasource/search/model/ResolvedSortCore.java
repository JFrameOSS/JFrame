package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.resource.AppliedSort;

import java.util.List;

/**
 * Framework-agnostic result of {@link AbstractSearchMetaData#resolveCore}.
 */
public final class ResolvedSortCore {

    private final List<SortableColumn> columns;
    private final AppliedSort appliedSort;
    private final boolean virtual;
    private final int pageNumber;
    private final int pageSize;

    /** Creates a new resolved core result. */
    public ResolvedSortCore(
                            final List<SortableColumn> columns,
                            final AppliedSort appliedSort,
                            final boolean virtual,
                            final int pageNumber,
                            final int pageSize) {
        this.columns = columns;
        this.appliedSort = appliedSort;
        this.virtual = virtual;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public List<SortableColumn> getColumns() {
        return columns;
    }

    public AppliedSort getAppliedSort() {
        return appliedSort;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }
}
