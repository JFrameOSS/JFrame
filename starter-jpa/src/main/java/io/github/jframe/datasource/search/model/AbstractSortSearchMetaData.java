package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.*;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import static java.util.Objects.nonNull;

/**
 * Abstract metadata class defining search and sorting capabilities for domain model objects.
 *
 * <p>Maps frontend field names to database columns and defines search behavior for each field.
 * Concrete implementations should extend this class and configure fields using the {@code addField} methods.
 *
 * <p>Thread-safe: Uses concurrent collections for field mappings.
 *
 * @see SearchType
 * @see SearchCriterium
 * @see SearchInput
 */
@Slf4j
@Getter
@SuppressWarnings(
    {
        "ClassDataAbstractionCoupling",
        "PMD.CouplingBetweenObjects"
    }
)
public abstract class AbstractSortSearchMetaData {

    private final Map<String, SearchType> searchTypes = new ConcurrentHashMap<>();
    private final Map<String, String> columnNames = new ConcurrentHashMap<>();
    private final List<String> sortableFields = new ArrayList<>();
    private final Map<String, Class<?>> enumClasses = new ConcurrentHashMap<>();
    private final Map<SearchType, SearchCriteriumFactory> factories = Map.of(
        SearchType.NONE,
        (c, i) -> null,
        SearchType.DATE,
        this::toDateSearchField,
        SearchType.NUMERIC,
        (c, i) -> {
            if (StringUtils.isBlank(i.getTextValue())) {
                return null;
            }
            final NumericField f = new NumericField(c);
            f.setValue(Integer.parseInt(i.getTextValue()));
            return f;
        },
        SearchType.BOOLEAN,
        (c, i) -> {
            if (StringUtils.isBlank(i.getTextValue())) {
                return null;
            }
            final BooleanField f = new BooleanField(c);
            f.setValue(Boolean.parseBoolean(i.getTextValue()));
            return f;
        },
        SearchType.ENUM,
        (c, i) -> {
            if (StringUtils.isBlank(i.getTextValue())) {
                return null;
            }
            final EnumField f = new EnumField(c, enumClasses.get(i.getFieldName()));
            f.setValue(i.getTextValue());
            return f;
        },
        SearchType.MULTI_ENUM,
        (c, i) -> {
            if (CollectionUtils.isEmpty(i.getTextValueList())) {
                return null;
            }
            final MultiEnumField f = new MultiEnumField(c, enumClasses.get(i.getFieldName()));
            f.setValues(i.getTextValueList());
            return f;
        },
        SearchType.TEXT,
        (c, i) -> {
            if (StringUtils.isBlank(i.getTextValue())) {
                return null;
            }
            final TextField f = new TextField(c);
            f.setValue(i.getTextValue());
            return f;
        },
        SearchType.MULTI_TEXT,
        (c, i) -> {
            if (CollectionUtils.isEmpty(i.getTextValueList())) {
                return null;
            }
            final MultiTextField f = new MultiTextField(c);
            f.setValues(i.getTextValueList());
            return f;
        },
        SearchType.FUZZY_TEXT,
        (c, i) -> {
            if (StringUtils.isBlank(i.getTextValue())) {
                return null;
            }
            final FuzzyTextField f = new FuzzyTextField(c);
            f.setValue(i.getTextValue());
            return f;
        },
        SearchType.MULTI_FUZZY,
        (c, i) -> {
            if (StringUtils.isBlank(i.getTextValue())) {
                return null;
            }
            final MultiFuzzyField f = new MultiFuzzyField(c);
            f.setValue(i.getTextValue());
            f.setOperator(i.getOperator());
            return f;
        }
    );

    /* -------------------------------------------------
     *  Search & sorting helpers
     * ------------------------------------------------- */

    /**
     * Convert a list of SearchInput objects into a list of SearchCriterium objects based on the defined metadata.
     *
     * @param inputs List of SearchInput objects representing user search criteria.
     * @return List of SearchCriterium objects for querying the database.
     */
    public List<SearchCriterium> toSearchCriteria(final List<SearchInput> inputs) {
        if (CollectionUtils.isEmpty(inputs)) {
            return Collections.emptyList();
        }

        return inputs.stream()
            .map(this::getSearchCriterium)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Convert a list of SortableColumn objects into a Spring Data Sort object based on the defined sortable fields.
     *
     * @param sortOrders List of SortableColumn objects representing user-defined sort orders.
     * @return Spring Data Sort object for querying the database.
     * @throws IllegalArgumentException if any requested sort field is not defined as sortable.
     */
    public Sort toSort(final List<SortableColumn> sortOrders) {
        if (CollectionUtils.isEmpty(sortOrders)) {
            return Sort.unsorted();
        }

        final List<Sort.Order> orders = sortOrders.stream()
            .filter(o -> sortableFields.contains(o.getName()))
            .map(
                o -> new Sort.Order(
                    Sort.Direction.fromString(o.getDirection()),
                    o.getName()
                )
            )
            .toList();

        if (orders.size() != sortOrders.size()) {
            throw new IllegalArgumentException("Attempted to sort on non-sortable fields: " + sortOrders);
        }

        return Sort.by(orders);
    }

    /**
     * Check if a given Predicate is empty (null or has no expressions).
     *
     * @param predicate the Predicate to check.
     * @return true if the predicate is null or has no expressions; false otherwise.
     */
    public static boolean isEmptyPredicate(final Predicate predicate) {
        return predicate == null
            || predicate.getExpressions() == null
            || predicate.getExpressions().isEmpty();
    }

    /* -------------------------------------------------
     *  Search field registration
     * ------------------------------------------------- */

    /**
     * Register a searchable and/or sortable field with the metadata.
     *
     * @param field      the frontend field name.
     * @param column     the database column name.
     * @param searchType the type of search to be performed on this field.
     * @param sortable   whether the field is sortable.
     */
    protected void addField(
        final String field,
        final String column,
        final SearchType searchType,
        final boolean sortable) {
        addField(field, column, searchType, sortable, false);
    }

    /**
     * Register a searchable and/or sortable enum field with the metadata.
     *
     * @param field      the frontend field name.
     * @param column     the database column name.
     * @param searchType the type of search to be performed on this field (must be ENUM or MULTI_ENUM).
     * @param enumClass  the enum class associated with this field.
     * @param sortable   whether the field is sortable.
     */
    protected void addField(
        final String field,
        final String column,
        final SearchType searchType,
        final Class<?> enumClass,
        final boolean sortable,
        final boolean isCustomSearch) {
        if (searchType != SearchType.ENUM && searchType != SearchType.MULTI_ENUM) {
            throw new IllegalArgumentException("SearchType must be ENUM or MULTI_ENUM");
        }
        enumClasses.put(field, enumClass);
        addField(field, column, searchType, sortable, isCustomSearch);
    }

    /**
     * Internal method to register a searchable and/or sortable field with the metadata.
     *
     * @param field          the frontend field name.
     * @param column         the database column name.
     * @param searchType     the type of search to be performed on this field.
     * @param sortable       whether the field is sortable.
     * @param isCustomSearch whether the field uses custom search logic (not registered in searchTypes).
     */
    protected void addField(
        final String field,
        final String column,
        final SearchType searchType,
        final boolean sortable,
        final boolean isCustomSearch) {
        if (!isCustomSearch) {
            searchTypes.put(field, searchType);
        }
        if (nonNull(column)) {
            columnNames.put(field, column);
        }
        if (sortable) {
            sortableFields.add(field);
        }
    }

    /* -------------------------------------------------
     *  Search criterium creation
     * ------------------------------------------------- */

    /**
     * Create a SearchCriterium based on the SearchInput and defined metadata.
     *
     * @param input the SearchInput containing user search criteria.
     * @return the corresponding SearchCriterium, or null if no definition exists.
     */
    protected SearchCriterium getSearchCriterium(final SearchInput input) {
        final String field = input.getFieldName();
        final String column = columnNames.get(field);
        final SearchType type = searchTypes.get(field);

        if (StringUtils.isBlank(column) || type == null) {
            log.info("No definition for search field '{}'", field);
            return null;
        }

        return factories.getOrDefault(type, (c, i) -> null).create(column, input);
    }

    /**
     * Convert SearchInput into a DateField SearchCriterium.
     *
     * @param column the database column name.
     * @param input  the SearchInput containing date range values.
     * @return the DateField SearchCriterium.
     */
    protected DateField toDateSearchField(final String column, final SearchInput input) {
        final DateField field = new DateField(column);
        if (nonNull(input.getFromDateValue())) {
            field.setFromDate(
                LocalDateTime.parse(
                    input.getFromDateValue(),
                    DateTimeFormatter.ISO_DATE_TIME
                )
            );
        }

        if (nonNull(input.getToDateValue())) {
            field.setToDate(
                LocalDateTime.parse(
                    input.getToDateValue(),
                    DateTimeFormatter.ISO_DATE_TIME
                )
            );
        }

        return field;
    }
}
