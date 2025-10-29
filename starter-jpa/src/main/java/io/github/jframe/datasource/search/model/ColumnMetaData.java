package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import lombok.Data;

import java.util.List;

/**
 * Represents the metadata of a column. can it be searched on, and is it sortable.
 */
@Data
public class ColumnMetaData {

    private List<String> columnNames;

    private SearchType searchType;

    private boolean sortable;

    private boolean customSearch;

    /**
     * default constructor without custom search.
     */
    public ColumnMetaData(final boolean sortable, final SearchType searchType, final String... columnNames) {
        this.columnNames = List.of(columnNames);
        this.searchType = searchType;
        this.sortable = sortable;
        this.customSearch = false;
    }

    /**
     * The default all-args constructor.
     *
     * @param columnNames The name of the column in the database.
     * @param searchType  The type of search that can be performed on this column.
     * @param sortable    Whether this column can be sorted on.
     */
    public ColumnMetaData(final boolean sortable, final boolean customSearch, final SearchType searchType, final String... columnNames) {
        this.columnNames = List.of(columnNames);
        this.searchType = searchType;
        this.sortable = sortable;
        this.customSearch = customSearch;
    }

    /**
     * Retrieve the first column name of the list.
     *
     * @return the first column name.
     */
    public String getColumnName() {
        return columnNames.stream()
            .findFirst()
            .orElse(null);
    }
}
