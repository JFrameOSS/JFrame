package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.BaseSearchSpecification;
import io.github.jframe.datasource.search.model.SearchCriterium;

import java.util.List;

/**
 * Quarkus JPA search specification that extends the framework-agnostic
 * {@link BaseSearchSpecification}.
 *
 * <p>Provides the same constructor signature and class name as the Spring variant,
 * allowing consumer code to be portable between frameworks.
 *
 * @param <T> the entity type
 */
public class JpaSearchSpecification<T> extends BaseSearchSpecification<T> {

    /**
     * Creates a new search specification for the given criteria.
     *
     * @param searchCriteria the search criteria to apply
     */
    public JpaSearchSpecification(final List<SearchCriterium> searchCriteria) {
        super(searchCriteria);
    }
}
