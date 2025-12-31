package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents the metadata of a column. can it be searched on, and is it sortable.
 */
@Getter
@Setter
@AllArgsConstructor
public class ColumnMetaData {

    private List<String> columnNames;

    private SearchType searchType;

    private boolean sortable;

    private boolean customSearch;

    /**
     * Retrieve the first column name of the list.
     *
     * @return the first column name.
     */
    public String getFirstColumnName() {
        return columnNames.stream()
            .findFirst()
            .orElse(null);
    }
}
