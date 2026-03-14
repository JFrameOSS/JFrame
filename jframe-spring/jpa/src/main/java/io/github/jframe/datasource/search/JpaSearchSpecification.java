package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.BaseSearchSpecification;
import io.github.jframe.datasource.search.model.SearchCriterium;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

/**
 * Spring Data JPA search specification that extends the framework-agnostic
 * {@link BaseSearchSpecification} and implements Spring's {@link Specification} contract.
 *
 * <p>This allows instances to be used directly with Spring Data's
 * {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor} and
 * composed with {@link Specification#where}, {@link Specification#and},
 * and {@link Specification#or}.
 *
 * <p>The {@code overloads} lint warning is suppressed because the {@code and}/{@code or} overloads
 * with {@link Specification} vs {@link SearchSpecification} parameters are intentional: they serve
 * distinct composition contracts from each framework and are not ambiguous at the call site.
 *
 * @param <T> the entity type
 */
@SuppressWarnings("overloads")
public class JpaSearchSpecification<T> extends BaseSearchSpecification<T> implements Specification<T> {

    /**
     * Creates a new search specification for the given criteria.
     *
     * @param searchCriteria the search criteria to apply
     */
    public JpaSearchSpecification(final List<SearchCriterium> searchCriteria) {
        super(searchCriteria);
    }

    /**
     * Composes this specification with another using a logical AND, using Spring Data's composition rules.
     *
     * @param other the other Spring Data {@link Specification} to AND with
     * @return a new composed {@link Specification}
     */
    @Override
    public Specification<T> and(final Specification<T> other) {
        return Specification.super.and(other);
    }

    /**
     * Composes this specification with another using a logical OR, using Spring Data's composition rules.
     *
     * @param other the other Spring Data {@link Specification} to OR with
     * @return a new composed {@link Specification}
     */
    @Override
    public Specification<T> or(final Specification<T> other) {
        return Specification.super.or(other);
    }
}
