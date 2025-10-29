package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Describes a database column name and the type of search to be performed on this column.
 */
@Data
@EqualsAndHashCode
@RequiredArgsConstructor
public class SearchCriterium implements Serializable {

    @Serial
    private static final long serialVersionUID = 101291045332418919L;

    private final String columnName;

    private final SearchType searchType;

}
