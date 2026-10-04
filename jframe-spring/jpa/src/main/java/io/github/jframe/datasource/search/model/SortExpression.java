package io.github.jframe.datasource.search.model;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

/**
 * Builds the order expressions for a virtual sort key. Returns a list so one key can order by several
 * expressions; the requested direction applies to all.
 *
 * <p><b>Warning:</b> avoid joining to-many associations — duplicate rows corrupt pagination counts.
 * Reuse existing joins from the root where possible.
 */
@FunctionalInterface
public interface SortExpression {

    /**
     * Builds the expressions to order by.
     *
     * @param root  the query root
     * @param query the query being built
     * @param cb    the criteria builder
     * @return the order expressions, in order
     */
    List<Expression<?>> build(From<?, ?> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
