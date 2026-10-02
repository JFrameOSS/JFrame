package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.PanacheSearchSpecification;
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
import io.github.jframe.datasource.search.model.resource.AppliedSort;
import io.github.jframe.exception.sort.InvalidSortException;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;

import static java.util.Objects.nonNull;

/**
 * Abstract metadata class defining search and sorting capabilities for Panache-based domain model objects.
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
        "PMD.ExcessiveImports",
        "PMD.GodClass",
        "PMD.CouplingBetweenObjects"
    }
)
public abstract class AbstractPanacheSearchMetaData {

    private static final String DESCENDING = "DESC";

    private final Map<String, SearchType> searchTypes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> columnNames = new ConcurrentHashMap<>();
    private final List<String> sortableFields = new ArrayList<>();
    private final Map<String, Class<?>> enumClasses = new ConcurrentHashMap<>();
    private final EnumMap<SearchType, SearchCriteriumFactory> factories = new EnumMap<>(SearchType.class);

    /**
     * Constructor initializes default search criterium factories for each SearchType.
     */
    protected AbstractPanacheSearchMetaData() {
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
        factories.put(
            SearchType.MULTI_NUMERIC,
            (c, i) -> new MultiNumericField(c.getFirst(), i.getTextValueList())
        );
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
     * // =========================================================================
     * // Sort contract — strict (breaking change in 1.7.0)
     * // =========================================================================
     *
     * /**
     * The default sort applied when the request carries no sort order.
     * Override to provide endpoint-specific defaults; return an empty list for unsorted.
     *
     * @return default sort columns (never {@code null})
     */
    protected List<SortableColumn> defaultSort() {
        return Collections.emptyList();
    }

    /**
     * The tiebreaker column appended ASC to every sort unless already present.
     * Override and return {@code null} to disable tiebreaker.
     *
     * @return tiebreaker column name, default {@code "id"}
     */
    protected String tiebreakerProperty() {
        return "id";
    }

    /**
     * Frontend sort keys that are allowed but have no mapped DB column.
     * When the effective sort uses one, the Panache sort is empty and
     * {@link PanacheResolvedSort#isVirtual()} is {@code true}.
     *
     * @return virtual field names (never {@code null})
     */
    protected Set<String> virtualSortFields() {
        return Collections.emptySet();
    }

    /**
     * Returns the union of sortable and virtual sort fields — the full allowed set.
     *
     * @return all accepted sort field names
     */
    public List<String> getAllowedSortFields() {
        final List<String> all = new ArrayList<>(sortableFields);
        all.addAll(virtualSortFields());
        return Collections.unmodifiableList(all);
    }

    /**
     * Resolves the effective sort and paging from the input.
     *
     * @param input the sortable page input
     * @return the resolved sort result
     * @throws InvalidSortException when a sort field or direction is rejected
     */
    public PanacheResolvedSort resolveSort(final SortablePageInput input) {
        final boolean isDefault = CollectionUtils.isEmpty(input.getSortOrder());
        final List<SortableColumn> requested = isDefault ? defaultSort() : input.getSortOrder();

        final int pageSize = input.getPageSize() <= 0 ? getDefaultPageSize() : input.getPageSize();
        final Page page = Page.of(input.getPageNumber(), pageSize);

        if (CollectionUtils.isEmpty(requested)) {
            return new PanacheResolvedSort(page, Sort.empty(), null, Collections.emptyList(), false);
        }

        final SortableColumn leading = requested.getFirst();
        final boolean isVirtual = virtualSortFields().contains(leading.getName());

        final List<SortableColumn> validated = normalise(requested);
        final Sort panacheSort = isVirtual ? Sort.empty() : buildPanacheSort(validated);

        final String leadingDir = normaliseDirection(leading.getDirection());
        final AppliedSort appliedSort = new AppliedSort(leading.getName(), leadingDir, isDefault);

        return new PanacheResolvedSort(page, panacheSort, appliedSort, validated, isVirtual);
    }

    /**
     * Convert a list of {@link SortableColumn} objects into a Panache {@link Sort}.
     *
     * <p><strong>Strict:</strong> unknown/non-sortable field or invalid direction → {@link InvalidSortException}.
     * Returns {@link Sort#empty()} for null/empty input.
     *
     * @param sortOrders sort columns requested by the client
     * @return Panache Sort
     * @throws InvalidSortException when a column is rejected
     */
    public Sort toSort(final List<SortableColumn> sortOrders) {
        if (CollectionUtils.isEmpty(sortOrders)) {
            return Sort.empty();
        }
        return buildPanacheSort(normalise(sortOrders));
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
     * Returns the default page size used when no page size is specified.
     *
     * @return default page size (20)
     */
    protected int getDefaultPageSize() {
        return 20;
    }

    /**
     * Convenience method to build a {@link PanacheSearchSpecification} from a {@link SortablePageInput}.
     *
     * @param <T>   the entity type
     * @param input the sortable page input containing search inputs
     * @return a new PanacheSearchSpecification wrapping the derived criteria
     */
    public <T> PanacheSearchSpecification<T> toSearchSpecification(final SortablePageInput input) {
        return new PanacheSearchSpecification<>(toSearchCriteria(input.getSearchInputs()));
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
     * Register a searchable and/or sortable field with custom search logic.
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
     * @throws IllegalArgumentException if searchType is not ENUM or MULTI_ENUM.
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
     * Register a multi-column searchable field with the metadata.
     *
     * @param field      the frontend field name.
     * @param columns    the database column names.
     * @param searchType the type of search to be performed on this field (must be MULTI_COLUMN_FUZZY).
     * @param sortable   whether the field is sortable.
     * @throws IllegalArgumentException if searchType is not MULTI_COLUMN_FUZZY.
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
     * @param isCustomSearch whether this field uses custom search logic (excludes from searchTypes map).
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


    /** Validates each column and returns them; throws on first invalid entry. */
    private List<SortableColumn> normalise(final List<SortableColumn> columns) {
        final List<String> allowed = getAllowedSortFields();
        for (final SortableColumn col : columns) {
            final String field = col.getName();
            if (field == null || field.isBlank() || !allowed.contains(field)) {
                throw new InvalidSortException(field, allowed);
            }
            final String dir = normaliseDirection(col.getDirection());
            if (dir == null) {
                throw new InvalidSortException(field, allowed);
            }
        }
        return columns;
    }

    /** Normalises direction: ASC/DESC (case-insensitive) → uppercase; anything else → {@code null}. */
    private static String normaliseDirection(final String direction) {
        if (direction == null || direction.isBlank()) {
            return null;
        }
        final String upper = direction.strip().toUpperCase();
        return ("ASC".equals(upper) || DESCENDING.equals(upper)) ? upper : null;
    }

    /** Builds a Panache {@link Sort} with tiebreaker appended unless already present. */
    private Sort buildPanacheSort(final List<SortableColumn> columns) {
        if (columns.isEmpty()) {
            return Sort.empty();
        }

        Sort result = Sort.empty();
        final Set<String> addedColumns = new java.util.LinkedHashSet<>();

        for (final SortableColumn col : columns) {
            final List<String> dbCols = columnNames.get(col.getName());
            if (dbCols == null) {
                continue;
            }
            final Sort.Direction dir = DESCENDING.equalsIgnoreCase(col.getDirection())
                ? Sort.Direction.Descending
                : Sort.Direction.Ascending;
            for (final String dbCol : dbCols) {
                result = result.and(dbCol, dir);
                addedColumns.add(dbCol);
            }
        }

        final String tiebreaker = tiebreakerProperty();
        if (tiebreaker != null && !addedColumns.contains(tiebreaker)) {
            result = result.and(tiebreaker, Sort.Direction.Ascending);
        }

        return result;
    }

}
