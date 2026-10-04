package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.JpaSearchSpecification;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spring-specific search metadata adapter. Extends {@link AbstractSearchMetaData} with
 * Spring Data {@link Sort}, {@link Pageable}, and JPA {@link Specification} support.
 *
 * @see AbstractSearchMetaData
 */
@SuppressWarnings(
    {
        "ClassDataAbstractionCoupling",
        "PMD.CouplingBetweenObjects"
    }
)
public abstract class AbstractSortSearchMetaData extends AbstractSearchMetaData {

    private final Map<String, SortExpression> sortExpressions = new ConcurrentHashMap<>();

    /**
     * Registers a virtual sort key ordered by the given expression inside {@link #toSearchSpecification}.
     *
     * <p><b>Warning:</b> avoid joining to-many associations — duplicate rows corrupt pagination counts.
     * Reuse existing joins from the root where possible.
     *
     * @param name       frontend sort key
     * @param expression builds the order expressions
     */
    protected void addSortExpression(final String name, final SortExpression expression) {
        sortExpressions.put(name, expression);
    }

    @Override
    protected Set<String> effectiveVirtualSortFields() {
        final Set<String> all = new HashSet<>(virtualSortFields());
        all.addAll(sortExpressions.keySet());
        return all;
    }

    /**
     * Resolves the effective sort and paging from the input.
     *
     * @param input the sortable page input
     * @return the resolved sort result
     * @throws InvalidSortException when a sort field or direction is rejected
     * @throws InvalidPageException when the page number is negative
     */
    public ResolvedSort resolveSort(final SortablePageInput input) {
        final ResolvedSortCore core = resolveCore(input);

        if (core.getColumns().isEmpty()) {
            final Pageable pageable = PageRequest.of(core.getPageNumber(), core.getPageSize(), Sort.unsorted());
            return new ResolvedSort(pageable, null, Collections.emptyList(), false);
        }

        final Sort jpaSort = core.isVirtual() ? Sort.unsorted() : toJpaSort(core.getColumns());
        final Pageable pageable = PageRequest.of(core.getPageNumber(), core.getPageSize(), jpaSort);

        return new ResolvedSort(pageable, core.getAppliedSort(), core.getColumns(), core.isVirtual());
    }

    /**
     * Convert sort columns into a Spring Data {@link Sort}.
     *
     * <p>Returns {@link Sort#unsorted()} for null/empty input.
     * Unknown/non-sortable fields or invalid directions throw {@link InvalidSortException}.
     *
     * @param sortOrders sort columns requested by the client
     * @return Spring Data Sort
     * @throws InvalidSortException when a column is rejected
     */
    public Sort toSort(final List<SortableColumn> sortOrders) {
        if (sortOrders == null || sortOrders.isEmpty()) {
            return Sort.unsorted();
        }
        return toJpaSort(normalise(sortOrders));
    }

    /**
     * Convert a {@link SortablePageInput} to a Spring Data {@link Pageable}.
     *
     * @param input the sortable page input
     * @return configured {@link Pageable}
     */
    public Pageable toPageable(final SortablePageInput input) {
        return resolveSort(input).getPageable();
    }

    /**
     * Build a {@link JpaSearchSpecification} from the search inputs in a {@link SortablePageInput}.
     *
     * <p><b>Warning (virtual sorts):</b> when the effective sort contains a virtual key, ordering is applied
     * inside the specification. Pass an unsorted {@link org.springframework.data.domain.Pageable} (e.g. from
     * {@link #resolveSort}) — a sorted {@code Pageable} overrides the in-spec order.
     *
     * @param input the sortable page input
     * @param <T>   the entity type
     * @return a {@link JpaSearchSpecification} based on the input's search criteria
     */
    public <T> JpaSearchSpecification<T> toSearchSpecification(final SortablePageInput input) {
        final List<SearchCriterium> criteria = toSearchCriteria(input.getSearchInputs());
        final ResolvedSortCore core = resolveCore(input);
        if (!core.isVirtual()) {
            return new JpaSearchSpecification<>(criteria);
        }
        final List<SortableColumn> columns = core.getColumns();
        return new JpaSearchSpecification<>(criteria) {

            @Override
            public Predicate toPredicate(final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
                if (query != null && !isCountQuery(query)) {
                    query.orderBy(toOrders(columns, root, query, cb));
                }
                return super.toPredicate(root, query, cb);
            }
        };
    }

    /**
     * Build a scoped {@link Specification} by ANDing the base search specification with an equality
     * predicate on {@code fieldPath} == {@code scopeValue}. Supports nested paths (e.g. {@code "tenant.id"}).
     *
     * <p>See {@link #toSearchSpecification(SortablePageInput)} for the virtual-sort {@code Pageable} warning.
     *
     * @param input      the sortable page input
     * @param fieldPath  dot-separated path to the field
     * @param scopeValue the value the field must equal
     * @param <T>        the entity type
     * @return a composed {@link Specification}
     */
    public <T> Specification<T> toSearchSpecification(
        final SortablePageInput input,
        final String fieldPath,
        final Object scopeValue) {
        final JpaSearchSpecification<T> base = toSearchSpecification(input);
        final Specification<T> scope = (root, query, cb) -> cb.equal(resolvePath(root, fieldPath), scopeValue);
        return base.and(scope);
    }

    /** Builds a {@link Sort} with ignoreCase + nullsLast on every order, plus the tiebreaker. */
    private Sort toJpaSort(final List<SortableColumn> columns) {
        final List<Sort.Order> orders = new ArrayList<>();
        for (final SortableColumn col : columns) {
            final String dir = normaliseDirection(col.getDirection());
            final List<String> dbCols = getColumnNames().get(col.getName());
            if (dbCols == null) {
                continue;
            }
            for (final String dbCol : dbCols) {
                orders.add(
                    Sort.Order.by(dbCol)
                        .with(Sort.Direction.fromString(dir))
                        .ignoreCase()
                        .nullsLast()
                );
            }
        }

        for (final SortableColumn tiebreaker : tiebreaker()) {
            final String property = tiebreaker.getName();
            if (orders.stream().noneMatch(o -> property.equals(o.getProperty()))) {
                orders.add(
                    Sort.Order.by(property)
                        .with(Sort.Direction.fromString(normaliseDirection(tiebreaker.getDirection())))
                        .ignoreCase()
                        .nullsLast()
                );
            }
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /** Builds criteria orders for a virtual sort: expressions, mapped columns, then tiebreaker. */
    private List<Order> toOrders(
        final List<SortableColumn> columns,
        final Root<?> root,
        final CriteriaQuery<?> query,
        final CriteriaBuilder cb) {
        final List<Order> orders = new ArrayList<>();
        final Set<String> orderedPaths = new HashSet<>();
        for (final SortableColumn col : columns) {
            final boolean ascending = isAscending(col);
            final SortExpression expression = sortExpressions.get(col.getName());
            if (expression != null) {
                expression.build(root, query, cb).forEach(expr -> orders.add(toOrder(expr, ascending, cb)));
                continue;
            }
            for (final String dbCol : getColumnNames().getOrDefault(col.getName(), Collections.emptyList())) {
                orders.add(toOrder(resolvePath(root, dbCol), ascending, cb));
                orderedPaths.add(dbCol);
            }
        }
        for (final SortableColumn tiebreaker : tiebreaker()) {
            if (orderedPaths.add(tiebreaker.getName())) {
                orders.add(toOrder(resolvePath(root, tiebreaker.getName()), isAscending(tiebreaker), cb));
            }
        }
        return orders;
    }

    @SuppressWarnings("unchecked")
    private static Order toOrder(final Expression<?> expression, final boolean ascending, final CriteriaBuilder cb) {
        final Expression<?> sortable = expression.getJavaType() == String.class
            ? cb.lower((Expression<String>) expression)
            : expression;
        return ascending ? cb.asc(sortable, Nulls.LAST) : cb.desc(sortable, Nulls.LAST);
    }

    private static Path<?> resolvePath(final Root<?> root, final String dotPath) {
        Path<?> path = root;
        for (final String segment : dotPath.split("\\.")) {
            path = path.get(segment);
        }
        return path;
    }

    private static boolean isAscending(final SortableColumn column) {
        return !"DESC".equals(normaliseDirection(column.getDirection()));
    }

    private static boolean isCountQuery(final CriteriaQuery<?> query) {
        final Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }
}
