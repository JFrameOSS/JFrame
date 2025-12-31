package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.*;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

import static io.github.jframe.util.constants.Constants.Characters.PERCENTAGE;

/**
 * Defines a search specification for a given domain model object.
 *
 * @param <T> Domain model type for which the instantiated Search Specification can be used.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class JpaSearchSpecification<T> implements Specification<T> {

    @Serial
    private static final long serialVersionUID = 4048263278292967348L;

    private final List<SearchCriterium> searchCriteria;

    /**
     * {@inheritDoc}
     *
     * @param root    must not be {@literal null}.
     * @param query   can be {@literal null} to allow overrides that accept {@link jakarta.persistence.criteria.CriteriaDelete} which is an
     *                {@link jakarta.persistence.criteria.AbstractQuery} but no {@link CriteriaQuery}.
     * @param builder must not be {@literal null}.
     * @return a {@link Predicate} that can be used to filter the results of a query based on the provided search criteria.
     */
    @Override
    public Predicate toPredicate(@Nonnull final Root<T> root,
        @NonNull final CriteriaQuery<?> query,
        @Nonnull final CriteriaBuilder builder) {
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return null;
        }

        final List<Predicate> searchPredicates = new ArrayList<>();
        for (final SearchCriterium crit : searchCriteria) {
            addPredicates(root, builder, searchPredicates, crit);
        }

        return builder.and(searchPredicates.toArray(new Predicate[0]));
    }

    @SuppressWarnings(
        {
            "CyclomaticComplexity",
            "ExecutableStatementCount",
            "PMD.NcssCount"
        }
    )
    private void addPredicates(final Root<T> root, final CriteriaBuilder cb, final List<Predicate> predicates, final SearchCriterium crit) {
        Path<String> path = null;
        if (crit.getSearchType() != SearchType.MULTI_COLUMN_FUZZY) {
            path = getColumnPath(root, crit.getColumnName());
        }

        switch (crit.getSearchType()) {
            case NONE -> {
                // no-op
            }
            case DATE -> addDateCriteria(root, cb, predicates, (DateField) crit);
            case NUMERIC -> {
                final NumericField f = (NumericField) crit;
                predicates.add(cb.equal(path, f.getValue()));
            }
            case BOOLEAN -> {
                final BooleanField f = (BooleanField) crit;
                predicates.add(cb.equal(path, f.isValue()));
            }
            case ENUM -> {
                final EnumField f = (EnumField) crit;
                predicates.add(cb.equal(path, f.getEnum()));
            }
            case MULTI_ENUM -> {
                final MultiEnumField f = (MultiEnumField) crit;
                predicates.add(path.in(f.getEnums()));
            }
            case TEXT -> {
                final TextField f = (TextField) crit;
                predicates.add(cb.equal(path, f.getValue()));
            }
            case MULTI_TEXT -> {
                final MultiTextField f = (MultiTextField) crit;
                predicates.add(path.in(f.getValues()));
            }
            case FUZZY_TEXT -> {
                final FuzzyTextField f = (FuzzyTextField) crit;
                predicates.add(
                    cb.like(
                        cb.lower(path),
                        PERCENTAGE + f.getValue().toLowerCase() + PERCENTAGE
                    )
                );
            }
            case MULTI_FUZZY -> {
                final MultiFuzzyField f = (MultiFuzzyField) crit;
                final Path<String> finalPath = path;
                final List<Predicate> likes = f.getSearchTerms().stream()
                    .map(
                        term -> cb.like(
                            cb.lower(finalPath),
                            PERCENTAGE + term.toLowerCase() + PERCENTAGE
                        )
                    )
                    .toList();

                final Predicate combined = switch (f.getOperator()) {
                    case AND -> cb.and(likes.toArray(Predicate[]::new));
                    case OR -> cb.or(likes.toArray(Predicate[]::new));
                };
                predicates.add(combined);
            }
            case MULTI_COLUMN_FUZZY -> {
                final MultiColumnFuzzyField f = (MultiColumnFuzzyField) crit;
                final List<String> terms = f.getSearchTerms();
                final List<String> columns = f.getColumnNames();

                final List<Predicate> termPredicates = new ArrayList<>();
                for (final String term : terms) {
                    final List<Predicate> columnPredicates = new ArrayList<>();
                    for (final String colName : columns) {
                        final Path<String> colPath = getColumnPath(root, colName);
                        columnPredicates.add(
                            cb.like(
                                cb.lower(colPath),
                                PERCENTAGE + term.toLowerCase() + PERCENTAGE
                            )
                        );
                    }
                    termPredicates.add(cb.or(columnPredicates.toArray(new Predicate[0])));
                }

                predicates.add(cb.and(termPredicates.toArray(new Predicate[0])));
            }
        }
    }

    /**
     * Method that performs the necessary joins when searching in related entities.
     */
    private <Y> Path<Y> getColumnPath(final Root<T> root, final String columnName) {
        if (columnName.contains(".")) {
            final String[] columnNameParts = columnName.split("\\.");
            Join<Object, Object> joined = root.join(columnNameParts[0], JoinType.LEFT);
            for (int i = 1; i < (columnNameParts.length - 1); i++) {
                joined = joined.join(columnNameParts[i], JoinType.LEFT);
            }
            return joined.get(columnNameParts[columnNameParts.length - 1]);
        } else {
            return root.get(columnName);
        }
    }

    private void addDateCriteria(final Root<T> root, final CriteriaBuilder cb, final List<Predicate> predicates, final DateField date) {
        final Path<LocalDateTime> columnPath = getColumnPath(root, date.getColumnName());
        if (date.getFromDate() != null) {
            final Predicate predicate = cb.greaterThanOrEqualTo(columnPath, date.getFromDate());
            predicates.add(predicate);
        }
        if (date.getToDate() != null) {
            final Predicate predicate = cb.lessThanOrEqualTo(columnPath, date.getToDate());
            predicates.add(predicate);
        }
    }
}
