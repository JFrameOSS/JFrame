package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.JpaSearchSpecification;
import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.BooleanField;
import io.github.jframe.datasource.search.fields.DateField;
import io.github.jframe.datasource.search.fields.EnumField;
import io.github.jframe.datasource.search.fields.FuzzyTextField;
import io.github.jframe.datasource.search.fields.MultiColumnFuzzyField;
import io.github.jframe.datasource.search.fields.MultiEnumField;
import io.github.jframe.datasource.search.fields.MultiFuzzyField;
import io.github.jframe.datasource.search.fields.MultiNumericField;
import io.github.jframe.datasource.search.fields.MultiTextField;
import io.github.jframe.datasource.search.fields.NumericField;
import io.github.jframe.datasource.search.fields.NumericRangeField;
import io.github.jframe.datasource.search.fields.TextField;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

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
        "ClassFanOutComplexity",
        "PMD.ExcessiveImports"
    }
)
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
        factories.put(SearchType.NONE, (c, i) -> null);
        factories.put(SearchType.DATE, (c, i) -> new DateField(c.getFirst(), i.getFromDateValue(), i.getToDateValue()));
        factories.put(SearchType.NUMERIC, (c, i) -> new NumericField(c.getFirst(), i.getTextValue()));
        factories.put(SearchType.BOOLEAN, (c, i) -> new BooleanField(c.getFirst(), i.getTextValue()));
        factories.put(SearchType.ENUM, (c, i) -> new EnumField(c.getFirst(), enumClasses.get(i.getFieldName()), i.getTextValue()));
        factories.put(
            SearchType.MULTI_ENUM,
            (c, i) -> new MultiEnumField(c.getFirst(), enumClasses.get(i.getFieldName()), i.getTextValueList())
        );
        factories.put(SearchType.TEXT, (c, i) -> new TextField(c.getFirst(), i.getTextValue()));
        factories.put(SearchType.MULTI_TEXT, (c, i) -> new MultiTextField(c.getFirst(), i.getTextValueList()));
        factories.put(SearchType.FUZZY_TEXT, (c, i) -> new FuzzyTextField(c.getFirst(), i.getTextValue()));
        factories.put(SearchType.MULTI_FUZZY, (c, i) -> new MultiFuzzyField(c.getFirst(), i.getOperator(), i.getTextValue()));
        factories.put(SearchType.MULTI_COLUMN_FUZZY, (c, i) -> new MultiColumnFuzzyField(c, i.getTextValue()));
        factories.put(SearchType.MULTI_NUMERIC, (c, i) -> new MultiNumericField(c.getFirst(), i.getTextValueList()));
        factories.put(
            SearchType.NUMERIC_RANGE,
            (c, i) -> new NumericRangeField(c.getFirst(), i.getFromNumericValue(), i.getToNumericValue())
        );
    }

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
            .map(o -> new Sort.Order(Sort.Direction.fromString(o.getDirection()), columnNames.get(o.getName()).getFirst()))
            .toList();

        if (orders.size() != sortOrders.size()) {
            throw new IllegalArgumentException("Attempted to sort on non-sortable fields: " + sortOrders);
        }

        return Sort.by(orders);
    }

    /**
     * Returns the default page size used when the input page size is not positive.
     * Subclasses can override to provide a different default.
     *
     * @return default page size (20).
     */
    protected int getDefaultPageSize() {
        return 20;
    }

    /**
     * Convert a {@link SortablePageInput} to a Spring Data {@link Pageable}.
     * Uses {@link #getDefaultPageSize()} when input page size is not positive.
     *
     * @param input the sortable page input.
     * @return a configured {@link Pageable}.
     */
    public Pageable toPageable(final SortablePageInput input) {
        final int resolvedPageSize = input.getPageSize() <= 0 ? getDefaultPageSize() : input.getPageSize();
        return PageRequest.of(input.getPageNumber(), resolvedPageSize, toSort(input.getSortOrder()));
    }

    /**
     * Build a {@link JpaSearchSpecification} from the search inputs in a {@link SortablePageInput}.
     *
     * @param input the sortable page input.
     * @param <T>   the entity type.
     * @return a {@link JpaSearchSpecification} based on the input's search criteria.
     */
    public <T> JpaSearchSpecification<T> toSearchSpecification(final SortablePageInput input) {
        return new JpaSearchSpecification<>(toSearchCriteria(input.getSearchInputs()));
    }

    /**
     * Build a scoped {@link Specification} by ANDing the base search specification with an equality
     * predicate on {@code fieldPath} == {@code scopeValue}. Supports nested paths (e.g. "tenant.id").
     *
     * @param input      the sortable page input.
     * @param fieldPath  dot-separated path to the field (e.g. "organizationId" or "tenant.id").
     * @param scopeValue the value the field must equal.
     * @param <T>        the entity type.
     * @return a composed {@link Specification}.
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
     * Register a field with custom search logic (no factory-based search type).
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
     * Register a multi-column fuzzy search field with the metadata.
     *
     * @param field      the frontend field name.
     * @param columns    the database column names.
     * @param searchType the type of search to be performed on this field (must be MULTI_COLUMN_FUZZY).
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
     * @param field          the frontend field name.
     * @param columns        the database column names.
     * @param searchType     the type of search to be performed on this field.
     * @param sortable       whether the field is sortable.
     * @param isCustomSearch whether this field uses custom search logic (skips factory registration).
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
