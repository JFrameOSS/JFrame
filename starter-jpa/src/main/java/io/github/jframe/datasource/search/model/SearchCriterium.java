package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Describes a database column name and the type of search to be performed on this column.
 */
@Data
@EqualsAndHashCode
public class SearchCriterium implements Serializable {

    @Serial
    private static final long serialVersionUID = 101291045332418919L;

    private final String columnName;

    private final List<String> columnNames;

    private final SearchType searchType;

    /**
     * Constructor for a single column search criterium.
     *
     * @param columnName the database column name.
     * @param searchType the type of search to be performed.
     */
    public SearchCriterium(final String columnName, final SearchType searchType) {
        this.columnName = columnName;
        this.searchType = searchType;
        this.columnNames = Collections.singletonList(columnName);
    }

    /**
     * Constructor for a multi-column search criterium.
     *
     * @param columnNames the list of database column names.
     * @param searchType  the type of search to be performed.
     */
    public SearchCriterium(final List<String> columnNames, final SearchType searchType) {
        this.columnName = null;
        this.columnNames = columnNames;
        this.searchType = searchType;
    }
}
