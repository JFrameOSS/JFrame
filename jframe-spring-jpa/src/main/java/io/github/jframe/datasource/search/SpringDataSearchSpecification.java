package io.github.jframe.datasource.search;

import java.util.Objects;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

/**
 * Adapter wrapping jframe-core's {@link SearchSpecification} as a Spring Data {@link Specification}.
 *
 * <p>Bridges the framework-agnostic {@link SearchSpecification} to the Spring Data JPA
 * {@link Specification} contract, enabling use in Spring Data repositories.
 *
 * @param <T> the entity type
 */
public final class SpringDataSearchSpecification<T> implements Specification<T> {

    private final SearchSpecification<T> delegate;

    /**
     * Creates a new {@code SpringDataSearchSpecification} wrapping the given delegate.
     *
     * @param delegate the jframe-core specification to wrap; must not be {@code null}
     * @throws NullPointerException if {@code delegate} is {@code null}
     */
    public SpringDataSearchSpecification(final SearchSpecification<T> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Static factory method for creating a {@code SpringDataSearchSpecification}.
     *
     * @param delegate the jframe-core specification to wrap; must not be {@code null}
     * @param <T>      the entity type
     * @return a new {@code SpringDataSearchSpecification} wrapping the delegate
     * @throws NullPointerException if {@code delegate} is {@code null}
     */
    public static <T> SpringDataSearchSpecification<T> of(final SearchSpecification<T> delegate) {
        return new SpringDataSearchSpecification<>(delegate);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the underlying {@link SearchSpecification#toPredicate(Root, CriteriaQuery, CriteriaBuilder)}.
     */
    @Override
    public Predicate toPredicate(
        final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder criteriaBuilder) {
        return delegate.toPredicate(root, query, criteriaBuilder);
    }
}
