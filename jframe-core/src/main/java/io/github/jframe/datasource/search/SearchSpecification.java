package io.github.jframe.datasource.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Framework-agnostic search specification interface for building JPA-style predicates.
 *
 * <p>This interface is analogous to Spring Data's {@code Specification<T>} but without
 * the Spring Data dependency, making it usable in framework-agnostic code.
 *
 * @param <T> the entity type
 */
@FunctionalInterface
public interface SearchSpecification<T> {

    /**
     * Creates a {@link Predicate} for the given root, query and criteria builder.
     *
     * @param root  the root, never {@code null}
     * @param query the criteria query, can be {@code null}
     * @param cb    the criteria builder, never {@code null}
     * @return a {@link Predicate}
     */
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);

    /**
     * Returns a composed specification that represents a logical AND of this specification and another.
     *
     * @param other the other specification
     * @return the composed specification
     */
    default SearchSpecification<T> and(final SearchSpecification<T> other) {
        return (root, query, cb) -> cb.and(this.toPredicate(root, query, cb), other.toPredicate(root, query, cb));
    }

    /**
     * Returns a composed specification that represents a logical OR of this specification and another.
     *
     * @param other the other specification
     * @return the composed specification
     */
    default SearchSpecification<T> or(final SearchSpecification<T> other) {
        return (root, query, cb) -> cb.or(this.toPredicate(root, query, cb), other.toPredicate(root, query, cb));
    }
}
