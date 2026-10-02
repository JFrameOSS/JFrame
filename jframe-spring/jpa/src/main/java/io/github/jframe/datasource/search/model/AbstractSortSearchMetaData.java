package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.JpaSearchSpecification;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.criteria.Path;

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
@SuppressWarnings("ClassDataAbstractionCoupling")
public abstract class AbstractSortSearchMetaData extends AbstractSearchMetaData {

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
     * @param input the sortable page input
     * @param <T>   the entity type
     * @return a {@link JpaSearchSpecification} based on the input's search criteria
     */
    public <T> JpaSearchSpecification<T> toSearchSpecification(final SortablePageInput input) {
        return new JpaSearchSpecification<>(toSearchCriteria(input.getSearchInputs()));
    }

    /**
     * Build a scoped {@link Specification} by ANDing the base search specification with an equality
     * predicate on {@code fieldPath} == {@code scopeValue}. Supports nested paths (e.g. {@code "tenant.id"}).
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
        final Specification<T> scope = (root, query, cb) -> {
            Path<?> path = root;
            for (final String segment : fieldPath.split("\\.")) {
                path = path.get(segment);
            }
            return cb.equal(path, scopeValue);
        };
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

        final String tiebreaker = tiebreakerProperty();
        if (tiebreaker != null) {
            final boolean alreadyPresent = orders.stream().anyMatch(o -> tiebreaker.equals(o.getProperty()));
            if (!alreadyPresent) {
                orders.add(Sort.Order.asc(tiebreaker).ignoreCase().nullsLast());
            }
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
