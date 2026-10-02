package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.resource.AppliedSort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Pageable;

/**
 * Resolved result of {@link AbstractSortSearchMetaData#resolveSort}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedSort {

    private Pageable pageable;

    private AppliedSort appliedSort;

    private List<SortableColumn> columns;

    private boolean virtual;
}
