package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.PageableItem;
import io.github.jframe.datasource.search.model.resource.PageResource;
import io.github.jframe.exception.page.InvalidPageException;
import io.quarkus.panache.common.Sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
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
     * When {@code sort} is {@code null}, no ORDER BY clause is applied. Ordering matches Spring: dotted paths are
     * LEFT joined, String attributes are compared case-insensitively and nulls sort last.
     *
     * @param spec       the search specification to apply as a WHERE predicate, may be {@code null}
     * @param pageNumber the 0-based page number
     * @param pageSize   the maximum number of results per page
     * @param sort       the Panache sort descriptor, may be {@code null} for unsorted results
     * @return a {@link PageResource} containing the matching entities and pagination metadata
     * @throws InvalidPageException when {@code pageNumber} is negative or the resulting offset overflows {@code int}
     */
    public PageResource<T> searchPage(final SearchSpecification<T> spec,
        final int pageNumber,
        final int pageSize,
        final Sort sort) {

        validatePage(pageNumber, pageSize);

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
            final Map<String, From<?, ?>> joins = new HashMap<>();
            final List<Order> orders = sort.getColumns().stream()
                .map(col -> toOrder(cb, root, joins, col))
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

    /** Rejects negative page numbers and offsets beyond {@code int} range. */
    private static void validatePage(final int pageNumber, final int pageSize) {
        if (pageNumber < 0 || (long) pageNumber * pageSize > Integer.MAX_VALUE) {
            throw new InvalidPageException("pageNumber", pageNumber);
        }
    }

    private static Order toOrder(
        final CriteriaBuilder cb,
        final Root<?> root,
        final Map<String, From<?, ?>> joins,
        final Sort.Column column) {
        final Expression<?> expression = sortExpression(cb, resolvePath(root, joins, column.getName()));
        return column.getDirection() == Sort.Direction.Descending
            ? cb.desc(expression, Nulls.LAST)
            : cb.asc(expression, Nulls.LAST);
    }

    /** Navigates a dotted property path, LEFT joining (and reusing) every intermediate segment. */
    private static Path<?> resolvePath(final Root<?> root, final Map<String, From<?, ?>> joins, final String property) {
        final String[] segments = property.split("\\.");
        From<?, ?> current = root;
        final StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < segments.length - 1; i++) {
            prefix.append(segments[i]).append('.');
            final From<?, ?> parent = current;
            final String segment = segments[i];
            current = joins.computeIfAbsent(prefix.toString(), key -> parent.join(segment, JoinType.LEFT));
        }
        return current.get(segments[segments.length - 1]);
    }

    @SuppressWarnings("unchecked")
    private static Expression<?> sortExpression(final CriteriaBuilder cb, final Path<?> path) {
        return String.class.equals(path.getJavaType()) ? cb.lower((Expression<String>) path) : path;
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
