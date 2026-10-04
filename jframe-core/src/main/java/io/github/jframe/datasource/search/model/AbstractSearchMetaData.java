package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.BooleanField;
import io.github.jframe.datasource.search.fields.DateField;
import io.github.jframe.datasource.search.fields.EnumField;
import io.github.jframe.datasource.search.fields.FuzzyTextField;
import io.github.jframe.datasource.search.fields.InvalidFieldValueException;
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
import io.github.jframe.datasource.search.model.resource.AppliedSort;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.jframe.exception.sort.InvalidSortException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;

import static java.util.Objects.nonNull;

/**
 * Framework-agnostic base for search and sort metadata.
 *
 * <p>Maps frontend field names to database columns and defines search/sort behaviour.
 * Extend this class and register fields using the {@code addField} methods.
 *
 * <p>Thread-safe: uses concurrent collections.
 *
 * @see SearchType
 * @see SearchCriterium
 */
@Getter
@SuppressWarnings(
    {
        "ClassDataAbstractionCoupling",
        "ClassFanOutComplexity",
        "PMD.ExcessiveImports",
        "PMD.GodClass"
    }
)
public abstract class AbstractSearchMetaData {

    private static final String ASCENDING = "ASC";
    private static final String DESCENDING = "DESC";
    private static final String PAGE_NUMBER = "pageNumber";

    private final Map<String, SearchType> searchTypes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> columnNames = new ConcurrentHashMap<>();
    private final List<String> sortableFields = new ArrayList<>();
    private final Map<String, Class<?>> enumClasses = new ConcurrentHashMap<>();
    private final EnumMap<SearchType, SearchCriteriumFactory> factories = new EnumMap<>(SearchType.class);

    /** Initialises default search criterium factories for each SearchType. */
    protected AbstractSearchMetaData() {
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
     * Convert search inputs to criteria for querying.
     *
     * @throws InvalidSearchException when a field is unknown or its value cannot be parsed
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
     * The default sort applied when the request carries no sort order.
     * Override to provide endpoint-specific defaults; return an empty list for unsorted.
     *
     * @return default sort columns (never {@code null})
     */
    protected List<SortableColumn> defaultSort() {
        return Collections.emptyList();
    }

    /**
     * Tiebreaker columns appended to every sort, each skipped when already present.
     *
     * @return tiebreaker columns, default {@code [tiebreakerProperty() ASC]} or empty when that is {@code null}
     */
    protected List<SortableColumn> tiebreaker() {
        final String property = tiebreakerProperty();
        return property == null ? Collections.emptyList() : List.of(new SortableColumn(property, ASCENDING));
    }

    /**
     * The tiebreaker column appended ASC to every sort unless already present.
     * Override and return {@code null} to disable.
     *
     * @return tiebreaker column name, default {@code "id"}
     */
    protected String tiebreakerProperty() {
        return "id";
    }

    /**
     * Frontend sort keys that are allowed but have no mapped DB column.
     * When the effective sort uses one, the sort is left empty and {@code isVirtual} is {@code true}.
     *
     * @return virtual field names (never {@code null})
     */
    protected Set<String> virtualSortFields() {
        return Collections.emptySet();
    }

    /**
     * All virtual sort keys; adapters override to add framework-registered keys.
     *
     * @return virtual field names (never {@code null})
     */
    protected Set<String> effectiveVirtualSortFields() {
        return virtualSortFields();
    }

    /**
     * Returns the union of sortable and virtual sort fields.
     *
     * @return all accepted sort field names
     */
    public List<String> getAllowedSortFields() {
        final List<String> all = new ArrayList<>(sortableFields);
        all.addAll(effectiveVirtualSortFields());
        return Collections.unmodifiableList(all);
    }

    /**
     * Returns the default page size when input page size is not positive.
     *
     * @return default page size (20)
     */
    protected int getDefaultPageSize() {
        return 20;
    }

    /**
     * Returns all registered searchable field names, sorted.
     *
     * @return searchable field names
     */
    public List<String> getSearchableFields() {
        return columnNames.keySet().stream().sorted().toList();
    }

    /**
     * Guard for fixed-order endpoints. Throws {@link InvalidSortException} when {@code sortOrder} is non-empty.
     *
     * @param sortOrder the sort order from the request (may be {@code null})
     */
    public static void rejectAnySort(final List<SortableColumn> sortOrder) {
        if (!CollectionUtils.isEmpty(sortOrder)) {
            throw new InvalidSortException(sortOrder.getFirst().getName(), Collections.emptyList());
        }
    }

    /**
     * Check if a given Predicate is empty (null or has no expressions).
     *
     * @param predicate the Predicate to check
     * @return {@code true} if the predicate is null or has no expressions
     */
    public static boolean isEmptyPredicate(final Predicate predicate) {
        return predicate == null
            || predicate.getExpressions() == null
            || predicate.getExpressions().isEmpty();
    }

    /**
     * Resolves the effective sort and paging from the input into a framework-agnostic result.
     *
     * @param input the sortable page input
     * @return resolved core result (validated columns, applied sort, virtual flag, page params)
     * @throws InvalidSortException when a sort field or direction is rejected
     * @throws InvalidPageException when the page number is negative
     */
    protected ResolvedSortCore resolveCore(final SortablePageInput input) {
        validatePage(input);
        final boolean isDefault = CollectionUtils.isEmpty(input.getSortOrder());
        final List<SortableColumn> requested = isDefault ? defaultSort() : input.getSortOrder();
        final int pageSize = input.getPageSize() <= 0 ? getDefaultPageSize() : input.getPageSize();

        if (CollectionUtils.isEmpty(requested)) {
            return new ResolvedSortCore(Collections.emptyList(), null, false, input.getPageNumber(), pageSize);
        }

        final SortableColumn leading = requested.getFirst();
        final Set<String> virtualFields = effectiveVirtualSortFields();
        final boolean isVirtual = requested.stream().anyMatch(col -> virtualFields.contains(col.getName()));
        final List<SortableColumn> validated = normalise(requested);
        final String leadingDir = normaliseDirection(leading.getDirection());
        final AppliedSort appliedSort = new AppliedSort(leading.getName(), leadingDir, isDefault);

        return new ResolvedSortCore(validated, appliedSort, isVirtual, input.getPageNumber(), pageSize);
    }

    /**
     * Register a searchable/sortable field.
     *
     * @param field      frontend field name
     * @param column     database column name
     * @param searchType search type for this field
     * @param sortable   whether the field is sortable
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
     * @param field    frontend field name
     * @param column   database column name
     * @param sortable whether the field is sortable
     */
    protected void addField(
        final String field,
        final String column,
        final boolean sortable) {
        addField(field, List.of(column), SearchType.NONE, sortable, true);
    }

    /**
     * Register an enum field (searchType must be ENUM or MULTI_ENUM).
     *
     * @param field      frontend field name
     * @param column     database column name
     * @param searchType must be ENUM or MULTI_ENUM
     * @param enumClass  the enum class
     * @param sortable   whether the field is sortable
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
     * Register a multi-column field (searchType must be MULTI_COLUMN_FUZZY).
     *
     * @param field      frontend field name
     * @param columns    database column names
     * @param searchType must be MULTI_COLUMN_FUZZY
     * @param sortable   whether the field is sortable
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
     * Core field registration method.
     *
     * @param field          frontend field name
     * @param columns        database column names
     * @param searchType     search type for this field
     * @param sortable       whether the field is sortable
     * @param isCustomSearch whether to skip factory-based search (custom search logic)
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
     * Create a SearchCriterium for the given input.
     *
     * @return the corresponding SearchCriterium, or {@code null} for custom-search fields
     * @throws InvalidSearchException when the field is unknown or its value cannot be parsed
     */
    protected SearchCriterium getSearchCriterium(final SearchInput input) {
        final String field = input.getFieldName();
        final List<String> columns = field == null ? null : columnNames.get(field);

        if (CollectionUtils.isEmpty(columns)) {
            throw new InvalidSearchException(field, null, getSearchableFields());
        }

        try {
            return factories.getOrDefault(searchTypes.get(field), (c, i) -> null).create(columns, input);
        } catch (final InvalidFieldValueException exception) {
            throw new InvalidSearchException(field, exception.getRejectedValue(), getSearchableFields(), exception);
        }
    }

    /** Normalises direction: ASC/DESC (case-insensitive) → uppercase; anything else → {@code null}. */
    protected static String normaliseDirection(final String direction) {
        if (direction == null || direction.isBlank()) {
            return null;
        }
        final String upper = direction.strip().toUpperCase(Locale.ROOT);
        return (ASCENDING.equals(upper) || DESCENDING.equals(upper)) ? upper : null;
    }

    /** Validates and returns each column; throws on first invalid entry. */
    protected List<SortableColumn> normalise(final List<SortableColumn> columns) {
        final List<String> allowed = getAllowedSortFields();
        for (final SortableColumn col : columns) {
            final String field = col.getName();
            if (field == null || field.isBlank() || !allowed.contains(field)) {
                throw new InvalidSortException(field, allowed);
            }
            if (normaliseDirection(col.getDirection()) == null) {
                throw new InvalidSortException(field, allowed);
            }
        }
        return columns;
    }

    /** Rejects negative page numbers. */
    private static void validatePage(final SortablePageInput input) {
        if (input.getPageNumber() < 0) {
            throw new InvalidPageException(PAGE_NUMBER, input.getPageNumber());
        }
    }
}
