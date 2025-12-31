package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.model.input.SearchInput;

/**
 * Factory interface for creating SearchCriterium instances based on column names and search inputs.
 */
@FunctionalInterface
public interface SearchCriteriumFactory {

    /**
     * Creates a SearchCriterium based on the provided column name and search input.
     *
     * @param column the name of the column to search.
     * @param input  the search input containing criteria details.
     * @return a SearchCriterium instance representing the search criteria.
     */
    SearchCriterium create(String column, SearchInput input);
}
