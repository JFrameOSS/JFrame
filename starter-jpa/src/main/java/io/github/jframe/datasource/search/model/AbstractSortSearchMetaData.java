package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.*;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;
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
@SuppressWarnings("ClassDataAbstractionCoupling")
public abstract class AbstractSortSearchMetaData {

    private final Map<String, SearchType> searchTypes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> columnNames = new ConcurrentHashMap<>();
    private final List<String> sortableFields = new ArrayList<>();
    private final Map<String, Class<?>> enumClasses = new ConcurrentHashMap<>();
    private final EnumMap<SearchType, SearchCriteriumFactory> factories = new EnumMap<>(SearchType.class);

    /**
     * Constructor initializes default search criterium factories for each SearchType.
     */
    protected AbstractSortSearchMetaData() {
        factories.put(
            SearchType.NONE,
            (c, i) -> null
        );
        factories.put(
            SearchType.DATE,
            (c, i) -> new DateField(c.getFirst(), i.getFromDateValue(), i.getToDateValue())
        );
        factories.put(
            SearchType.NUMERIC,
            (c, i) -> new NumericField(c.getFirst(), i.getTextValue())
        );
        factories.put(
            SearchType.BOOLEAN,
            (c, i) -> new BooleanField(c.getFirst(), i.getTextValue())
        );
        factories.put(
            SearchType.ENUM,
            (c, i) -> new EnumField(c.getFirst(), enumClasses.get(i.getFieldName()), i.getTextValue())
        );
        factories.put(
            SearchType.MULTI_ENUM,
            (c, i) -> new MultiEnumField(c.getFirst(), enumClasses.get(i.getFieldName()), i.getTextValueList())
        );
        factories.put(
            SearchType.TEXT,
            (c, i) -> new TextField(c.getFirst(), i.getTextValue())
        );
        factories.put(
            SearchType.MULTI_TEXT,
            (c, i) -> new MultiTextField(c.getFirst(), i.getTextValueList())
        );
        factories.put(
            SearchType.FUZZY_TEXT,
            (c, i) -> new FuzzyTextField(c.getFirst(), i.getTextValue())
        );
        factories.put(
            SearchType.MULTI_FUZZY,
            (c, i) -> new MultiFuzzyField(c.getFirst(), i.getOperator(), i.getTextValue())
        );
        factories.put(
            SearchType.MULTI_COLUMN_FUZZY,
            (c, i) -> new MultiColumnFuzzyField(c, i.getTextValue())
        );
    }

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
        addField(field, List.of(column), searchType, sortable, false);
    }

    /**
     * Register a searchable and/or sortable field with the custom search logic.
     *
     * @param field    the frontend field name.
     * @param column   the database column name.
     * @param sortable whether the field is sortable.
     */
    protected void addField(
        final String field,
        final String column,
        final boolean sortable) {
        addField(field, List.of(column), SearchType.NONE, sortable, true);
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
        final boolean sortable) {
        if (searchType != SearchType.ENUM && searchType != SearchType.MULTI_ENUM) {
            throw new IllegalArgumentException("SearchType must be ENUM or MULTI_ENUM");
        }
        enumClasses.put(field, enumClass);
        addField(field, List.of(column), searchType, sortable, false);
    }

    /**
     * Register a searchable and/or sortable enum field with the metadata.
     *
     * @param field      the frontend field name.
     * @param columns    the database column names.
     * @param searchType the type of search to be performed on this field (must be ENUM or MULTI_ENUM).
     * @param sortable   whether the field is sortable.
     */
    protected void addField(
        final String field,
        final List<String> columns,
        final SearchType searchType,
        final boolean sortable) {
        if (searchType != SearchType.MULTI_COLUMN_FUZZY) {
            throw new IllegalArgumentException("SearchType must be MULTI_COLUMN_FUZZY");
        }
        addField(field, columns, searchType, sortable, false);
    }

    /**
     * Internal method to register a searchable and/or sortable field with the metadata.
     *
     * @param field      the frontend field name.
     * @param columns    the database column names.
     * @param searchType the type of search to be performed on this field.
     * @param sortable   whether the field is sortable.
     */
    protected void addField(
        final String field,
        final List<String> columns,
        final SearchType searchType,
        final boolean sortable,
        final boolean isCustomSearch) {
        if (!isCustomSearch) {
            searchTypes.put(field, searchType);
        }
        if (nonNull(columns)) {
            columnNames.put(field, columns);
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
        final List<String> columns = columnNames.get(field);
        final SearchType type = searchTypes.get(field);

        if (CollectionUtils.isEmpty(columns)) {
            log.info("No definition for search field '{}'", field);
            return null;
        }

        return factories.getOrDefault(type, (c, i) -> null).create(columns, input);
    }
}
