package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.resource.AppliedSort;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resolved result of {@link AbstractPanacheSearchMetaData#resolveSort}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanacheResolvedSort {

    private Page page;

    private Sort sort;

    private AppliedSort appliedSort;

    private List<SortableColumn> columns;

    private boolean virtual;
}
