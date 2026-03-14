package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.BaseSearchSpecification;
import io.github.jframe.datasource.search.model.SearchCriterium;

import java.util.List;

/**
 * Quarkus/Panache search specification that extends the framework-agnostic
 * {@link BaseSearchSpecification}.
 *
 * @param <T> the entity type
 */
public class PanacheSearchSpecification<T> extends BaseSearchSpecification<T> {

    /**
     * Creates a new search specification for the given criteria.
     *
     * @param searchCriteria the search criteria to apply
     */
    public PanacheSearchSpecification(final List<SearchCriterium> searchCriteria) {
        super(searchCriteria);
    }
}
