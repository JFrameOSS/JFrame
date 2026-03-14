package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.PageableItem;
import io.github.jframe.datasource.search.model.resource.PageResource;
import io.quarkus.panache.common.Sort;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

/**
 * Abstract repository for specification-based paginated querying using JPA Criteria API.
 *
 * <p>This is the Quarkus equivalent of Spring's {@code PagedSearchingService}.
 * Subclasses provide the entity class and {@link EntityManager} via template methods,
 * while this class handles predicate application, sorting, pagination, and count queries.
 *
 * @param <T> the entity type, must implement {@link PageableItem}
 */
public abstract class PanacheSearchRepository<T extends PageableItem> {

    /**
     * Searches for entities matching the given specification with pagination and optional sorting.
     *
     * <p>When {@code spec} is {@code null}, no WHERE clause is applied and all entities are returned.
     * When {@code sort} is {@code null}, no ORDER BY clause is applied.
     *
     * @param spec       the search specification to apply as a WHERE predicate, may be {@code null}
     * @param pageNumber the 0-based page number
     * @param pageSize   the maximum number of results per page
     * @param sort       the Panache sort descriptor, may be {@code null} for unsorted results
     * @return a {@link PageResource} containing the matching entities and pagination metadata
     */
    public PageResource<T> searchPage(final SearchSpecification<T> spec,
        final int pageNumber,
        final int pageSize,
        final Sort sort) {

        final EntityManager em = entityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final Class<T> entity = entityClass();

        // Build data query
        final CriteriaQuery<T> dataQuery = cb.createQuery(entity);
        final Root<T> root = dataQuery.from(entity);

        if (spec != null) {
            dataQuery.where(spec.toPredicate(root, dataQuery, cb));
        }

        if (sort != null && !sort.getColumns().isEmpty()) {
            final List<Order> orders = sort.getColumns().stream()
                .map(
                    col -> col.getDirection() == Sort.Direction.Descending
                        ? cb.desc(root.get(col.getName()))
                        : cb.asc(root.get(col.getName()))
                )
                .toList();
            dataQuery.orderBy(orders);
        }

        final TypedQuery<T> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(pageNumber * pageSize);
        typedQuery.setMaxResults(pageSize);
        final List<T> results = typedQuery.getResultList();

        // Build count query
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        final Root<T> countRoot = countQuery.from(entity);
        countQuery.select(cb.count(countRoot));

        if (spec != null) {
            countQuery.where(spec.toPredicate(countRoot, countQuery, cb));
        }

        final long totalElements = em.createQuery(countQuery).getSingleResult();
        final int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

        return QuarkusPageAdapter.toPageResource(results, totalElements, totalPages, pageSize, pageNumber);
    }

    /**
     * Returns the entity class managed by this repository.
     *
     * @return the entity {@link Class}, never {@code null}
     */
    protected abstract Class<T> entityClass();

    /**
     * Returns the {@link EntityManager} to use for queries.
     *
     * @return the {@link EntityManager}, never {@code null}
     */
    protected abstract EntityManager entityManager();
}
