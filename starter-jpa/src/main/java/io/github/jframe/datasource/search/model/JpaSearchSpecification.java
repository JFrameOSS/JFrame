package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.fields.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.*;

import org.springframework.data.jpa.domain.Specification;

import static io.github.jframe.datasource.search.model.SearchConstants.Character.PERCENTAGE;

/**
 * Defines a search specification for a given domain model object.
 *
 * @param <T> Domain model type for which the instantiated Search Specification can be used.
 */
@Slf4j
@RequiredArgsConstructor
public class JpaSearchSpecification<T> implements Specification<T> {

    @Serial
    private static final long serialVersionUID = 4048263278292967348L;

    private final List<SearchCriterium> searchCriteria;

    private final boolean includeNullDates;

    /**
     * Constructor.
     *
     * @param searchCriteria list of search criteria to create predicates for.
     */
    public JpaSearchSpecification(final List<SearchCriterium> searchCriteria) {
        this(searchCriteria, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param root            must not be {@literal null}.
     * @param query           can be {@literal null} to allow overrides that accept {@link jakarta.persistence.criteria.CriteriaDelete}
     *                        which is an {@link jakarta.persistence.criteria.AbstractQuery} but no {@link CriteriaQuery}.
     * @param criteriaBuilder must not be {@literal null}.
     * @return a {@link Predicate} that can be used to filter the results of a query based on the provided search criteria.
     */
    @Override
    public Predicate toPredicate(@Nonnull final Root<T> root,
        final CriteriaQuery<?> query,
        @Nonnull final CriteriaBuilder criteriaBuilder) {
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return null;
        }

        final List<Predicate> searchPredicates = new ArrayList<>();
        for (final SearchCriterium crit : searchCriteria) {
            addPredicates(root, criteriaBuilder, searchPredicates, crit);
        }

        return criteriaBuilder.and(searchPredicates.toArray(new Predicate[0]));
    }

    @SuppressWarnings("CyclomaticComplexity")
    private void addPredicates(final Root<T> root, final CriteriaBuilder criteriaBuilder, final List<Predicate> searchPredicates,
        final SearchCriterium crit) {

        final Path<String> columnPath = getColumnPath(root, crit);
        switch (crit.getSearchType()) {
            case TEXT -> {
                final TextSearchField textCrit = (TextSearchField) crit;
                searchPredicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(columnPath),
                        PERCENTAGE + textCrit.getValue().toLowerCase() + PERCENTAGE
                    )
                );
            }
            case NUMBER -> {
                final NumberSearchField numberCrit = (NumberSearchField) crit;
                searchPredicates.add(criteriaBuilder.equal(columnPath, numberCrit.getValue()));
            }
            case DROPDOWN_BOOLEAN -> {
                final DropdownBooleanSearchField dropdownBooleanCriteria = (DropdownBooleanSearchField) crit;
                searchPredicates.add(criteriaBuilder.equal(columnPath, dropdownBooleanCriteria.isValue()));
            }
            case DROPDOWN_STRING -> {
                final DropdownStringSearchField dropdownStringCriteria = (DropdownStringSearchField) crit;
                searchPredicates.add(criteriaBuilder.equal(columnPath, dropdownStringCriteria.getValue()));
            }
            case MULTIPLE_SELECT -> {
                final MultipleSelectSearchField multipleSelectCrit = (MultipleSelectSearchField) crit;
                searchPredicates.add(columnPath.in(multipleSelectCrit.getValues()));
            }
            case DATE -> {
                final DateSearchField dateField = (DateSearchField) crit;
                addDateCriteria(root, criteriaBuilder, searchPredicates, dateField);
            }
            case MULTI_WORD -> {
                final MultiWordSearchField multiWordField = (MultiWordSearchField) crit;
                for (final String word : multiWordField.getValues()) {
                    searchPredicates.add(
                        criteriaBuilder.like(
                            criteriaBuilder.lower(columnPath),
                            PERCENTAGE + word.toLowerCase() + PERCENTAGE
                        )
                    );
                }
            }
            case ENUM -> {
                final EnumSearchField enumField = (EnumSearchField) crit;
                searchPredicates.add(criteriaBuilder.equal(columnPath, enumField.getEnum()));
            }
            case MULTIPLE_ENUM -> {
                final MultipleEnumSearchField enumField = (MultipleEnumSearchField) crit;
                searchPredicates.add(columnPath.in(enumField.getEnums()));
            }
            default -> log.error("Cannot search for criterium without valid search type");
        }
    }

    /**
     * Method that performs the necessary joins when searching in related entities.
     */
    private <Y> Path<Y> getColumnPath(final Root<T> root, final SearchCriterium crit) {
        final String columnName = crit.getColumnName();
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

    private void addDateCriteria(final Root<T> root, final CriteriaBuilder criteriaBuilder, final List<Predicate> searchPredicates,
        final DateSearchField dateField) {
        final Path<LocalDateTime> columnPath = getColumnPath(root, dateField);
        if (dateField.getFromDate() != null) {
            Predicate predicate = criteriaBuilder.greaterThanOrEqualTo(columnPath, dateField.getFromDate());
            if (includeNullDates) {
                predicate = criteriaBuilder.or(predicate, criteriaBuilder.isNull(columnPath));
            }
            searchPredicates.add(predicate);
        }
        if (dateField.getToDate() != null) {
            Predicate predicate = criteriaBuilder.lessThanOrEqualTo(columnPath, dateField.getToDate());
            if (includeNullDates) {
                predicate = criteriaBuilder.or(predicate, criteriaBuilder.isNull(columnPath));
            }
            searchPredicates.add(predicate);
        }
    }
}
