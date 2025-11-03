package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.*;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
@SuppressWarnings("ClassDataAbstractionCoupling")
public abstract class AbstractSortSearchMetaData {

    /**
     * Sort order ascending.
     */
    protected static final String ASC = "ASC";

    /**
     * Sort order descending.
     */
    protected static final String DESC = "DESC";

    /**
     * Map of SearchType per fieldname.
     */
    private final Map<String, SearchType> searchTypes = new ConcurrentHashMap<>();

    /**
     * Map of database column name per fieldname.
     */
    private final Map<String, String> columnNames = new ConcurrentHashMap<>();

    /**
     * List of fields on which sorting is allowed.
     */
    private final List<String> sortableFields = new ArrayList<>();

    /**
     * Map of Enum classes per field name.
     */
    private final Map<String, Class<?>> enumClasses = new ConcurrentHashMap<>();

    /**
     * Adds a field to the searchable/sortable field configuration.
     *
     * @param field      frontend field name
     * @param column     associated database column name
     * @param searchType type of search operation for this field
     * @param sortable   whether this field supports sorting
     */
    protected void addField(final String field, final String column, final SearchType searchType, final boolean sortable) {
        addField(field, column, searchType, sortable, false);
    }

    /**
     * Adds an enum field to the searchable/sortable field configuration.
     *
     * @param field          frontend field name
     * @param column         associated database column name
     * @param searchType     must be {@link SearchType#ENUM} or {@link SearchType#MULTIPLE_ENUM}
     * @param sortable       whether this field supports sorting
     * @param enumClass      enum class for value validation and conversion
     * @param isCustomSearch whether this field uses custom search logic
     * @throws IllegalArgumentException if searchType is not ENUM or MULTIPLE_ENUM
     */
    protected void addField(final String field, final String column, final SearchType searchType,
        final boolean sortable, final Class<?> enumClass, final boolean isCustomSearch) {
        if (searchType != SearchType.ENUM && searchType != SearchType.MULTIPLE_ENUM) {
            throw new IllegalArgumentException("SearchType must be ENUM in order to use this method");
        }
        enumClasses.put(field, enumClass);
        addField(field, column, searchType, sortable, isCustomSearch);
    }

    /**
     * Adds a field to the searchable/sortable field configuration with custom search support.
     *
     * @param field          frontend field name
     * @param column         associated database column name. May be {@code null} for custom searches.
     * @param searchType     type of search operation for this field
     * @param sortable       whether this field supports sorting
     * @param isCustomSearch whether this field uses custom search logic (bypasses standard search type mapping)
     */
    protected void addField(final String field, final String column, final SearchType searchType,
        final boolean sortable, final boolean isCustomSearch) {
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

    /**
     * Creates a typed search criterium from the given search input.
     *
     * @param searchInput input containing field name and search value
     * @return typed search criterium or {@code null} if field is not configured or value is empty
     */
    protected SearchCriterium getSearchCriterium(final SearchInput searchInput) {
        final String columnName = columnNames.get(searchInput.getFieldName());
        final SearchType searchType = searchTypes.get(searchInput.getFieldName());

        if (StringUtils.isBlank(columnName) || searchType == null) {
            log.info("No definition for search field '{}'.", searchInput.getFieldName());
            return null;
        }
        return toSearchCriterium(searchInput, columnName, searchType);
    }

    /**
     * Converts search input to appropriate search criterium based on search type.
     *
     * @param searchInput input containing search values
     * @param columnName  database column name
     * @param searchType  type of search operation
     * @return typed search criterium or {@code null} if no valid value provided
     */
    @SuppressWarnings("cyclomaticcomplexity")
    private SearchCriterium toSearchCriterium(final SearchInput searchInput, final String columnName, final SearchType searchType) {
        SearchCriterium result = null;
        switch (searchType) {
            case TEXT -> {
                if (StringUtils.isNotBlank(searchInput.getTextValue())) {
                    result = new TextSearchField(columnName);
                    ((TextSearchField) result).setValue(searchInput.getTextValue());
                }
            }
            case NUMBER -> {
                if (StringUtils.isNotBlank(searchInput.getTextValue())) {
                    result = new NumberSearchField(columnName);
                    ((NumberSearchField) result).setValue(Integer.parseInt(searchInput.getTextValue()));
                }
            }
            case DROPDOWN_BOOLEAN -> {
                if (StringUtils.isNotBlank(searchInput.getTextValue())) {
                    result = new DropdownBooleanSearchField(columnName);
                    ((DropdownBooleanSearchField) result).setValue(Boolean.parseBoolean(searchInput.getTextValue()));
                }
            }
            case DROPDOWN_STRING -> {
                if (StringUtils.isNotBlank(searchInput.getTextValue())) {
                    result = new DropdownStringSearchField(columnName);
                    ((DropdownStringSearchField) result).setValue(searchInput.getTextValue());
                }
            }
            case MULTIPLE_SELECT -> {
                if (!searchInput.getTextValueList().isEmpty()) {
                    result = new MultipleSelectSearchField(columnName);
                    ((MultipleSelectSearchField) result).setValues(searchInput.getTextValueList());
                }
            }
            case MULTI_WORD -> {
                result = new MultiWordSearchField(columnName);
                ((MultiWordSearchField) result).setValue(searchInput.getTextValue());
            }
            case ENUM -> {
                if (StringUtils.isNotBlank(searchInput.getTextValue())) {
                    result = new EnumSearchField(columnName, enumClasses.get(columnName));
                    ((EnumSearchField) result).setValue(searchInput.getTextValue());
                }
            }
            case MULTIPLE_ENUM -> {
                if (CollectionUtils.isNotEmpty(searchInput.getTextValueList())) {
                    result = new MultipleEnumSearchField(columnName, enumClasses.get(columnName));
                    ((MultipleEnumSearchField) result).setValues(searchInput.getTextValueList());
                }
            }
            case DATE -> {
                result = toDateSearchField(columnName, searchInput);
            }
            default -> {
                // do nothing
            }
        }
        return result;
    }

    /**
     * Creates a date search field from search input with null-safe date parsing.
     *
     * @param columnName  database column name
     * @param searchInput input containing date values
     * @return configured date search field
     */
    protected DateSearchField toDateSearchField(final String columnName, final SearchInput searchInput) {
        final DateSearchField result = new DateSearchField(columnName);
        if (nonNull(searchInput.getFromDateValue())) {
            final LocalDateTime fromDate = LocalDateTime.parse(searchInput.getFromDateValue(), DateTimeFormatter.ISO_DATE_TIME);
            result.setFromDate(fromDate);
        }
        if (nonNull(searchInput.getToDateValue())) {
            final LocalDateTime toDate = LocalDateTime.parse(searchInput.getToDateValue(), DateTimeFormatter.ISO_DATE_TIME);
            result.setToDate(toDate);
        }
        return result;
    }

    /**
     * Converts search inputs to search criteria objects based on configured metadata.
     *
     * @param inputs list of search inputs from the client
     * @return list of search criteria for database querying, or {@code null} if inputs are empty
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public List<SearchCriterium> toSearchCriteria(final List<SearchInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }

        final List<SearchCriterium> searchCriteria = new ArrayList<>();
        for (final SearchInput searchInput : inputs) {
            final SearchCriterium searchCriterium = getSearchCriterium(searchInput);
            if (nonNull(searchCriterium)) {
                searchCriteria.add(searchCriterium);
            }
        }
        return searchCriteria;
    }

    /**
     * Converts sorting configuration to Spring Data Sort object.
     *
     * @param sortOrders list of sortable fields and directions
     * @return Sort object for database queries
     * @throws IllegalArgumentException if any field is not configured as sortable
     */
    public Sort toSort(final List<SortableColumn> sortOrders) {

        if (sortOrders == null || sortOrders.isEmpty()) {
            return Sort.unsorted();
        }

        final List<Sort.Order> sortList = sortOrders.stream().filter(sortOrder -> sortableFields.contains(sortOrder.getName()))
            .map(
                sortOrder -> new Sort.Order(
                    Sort.Direction.fromString(sortOrder.getDirection()),
                    sortOrder.getName()
                )
            ).toList();

        if (sortOrders.size() != sortList.size()) {
            throw new IllegalArgumentException(
                "Attempted to sort on " + sortOrders
                    + ", which contain non-sortable fields"
            );
        }

        return Sort.by(sortList);

    }

    /**
     * Checks whether a JPA predicate is null or contains no expressions.
     *
     * @param predicate JPA predicate to check
     * @return {@code true} if predicate is null, has null expressions, or empty expressions
     */
    public static boolean isEmptyPredicate(final Predicate predicate) {
        return predicate == null || predicate.getExpressions() == null || predicate.getExpressions().isEmpty();
    }

    /**
     * Extracts an integer value from search inputs for the specified field name.
     *
     * @param pageInput page input containing search inputs
     * @param fieldName field name to extract value for
     * @return integer value of the field or {@code null} if not found or not parseable
     * @throws NumberFormatException if the field value cannot be parsed as integer
     */
    public Integer extractSearchInputAsInteger(final SortablePageInput pageInput, final String fieldName) {
        return pageInput.getSearchInputs().stream()
            .filter(searchInput -> searchInput.getFieldName().equals(fieldName))
            .findFirst()
            .map(SearchInput::getTextValue)
            .map(Integer::valueOf)
            .orElse(null);
    }


}
