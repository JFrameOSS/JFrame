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
import io.github.jframe.datasource.search.model.resource.AppliedSort;
import io.github.jframe.exception.sort.InvalidSortException;
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
        "PMD.ExcessiveImports",
        "PMD.GodClass",
        "PMD.CouplingBetweenObjects",
        "PMD.TooManyMethods"
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
     * When the effective sort uses one of these, the pageable is left unsorted
     * and {@link ResolvedSort#isVirtual()} returns {@code true}.
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
     * <p>When {@code input.getSortOrder()} is null or empty, {@link #defaultSort()} is applied
     * and the resulting {@link AppliedSort} is marked {@code isDefault=true}.
     * Unknown or non-sortable field names and invalid directions throw {@link InvalidSortException}.
     *
     * @param input the sortable page input
     * @return the resolved sort result
     * @throws InvalidSortException when a sort field or direction is rejected
     */
    public ResolvedSort resolveSort(final SortablePageInput input) {
        final boolean isDefault = CollectionUtils.isEmpty(input.getSortOrder());
        final List<SortableColumn> requested = isDefault ? defaultSort() : input.getSortOrder();

        if (CollectionUtils.isEmpty(requested)) {
            final int pageSize = input.getPageSize() <= 0 ? getDefaultPageSize() : input.getPageSize();
            final Pageable pageable = PageRequest.of(input.getPageNumber(), pageSize, Sort.unsorted());
            return new ResolvedSort(pageable, null, Collections.emptyList(), false);
        }

        final SortableColumn leading = requested.getFirst();
        final boolean isVirtual = virtualSortFields().contains(normaliseFieldName(leading));

        final List<SortableColumn> validated = normalise(requested);
        final Sort jpaSort = isVirtual ? Sort.unsorted() : toJpaSort(validated);

        final int pageSize = input.getPageSize() <= 0 ? getDefaultPageSize() : input.getPageSize();
        final Pageable pageable = PageRequest.of(input.getPageNumber(), pageSize, jpaSort);

        final String leadingDir = normaliseDirection(leading.getDirection());
        final AppliedSort appliedSort = new AppliedSort(leading.getName(), leadingDir, isDefault);

        return new ResolvedSort(pageable, appliedSort, validated, isVirtual);
    }

    /**
     * Convert a list of {@link SortableColumn} objects into a Spring Data {@link Sort}.
     *
     * <p><strong>Strict:</strong> unknown/non-sortable field or invalid direction → {@link InvalidSortException}.
     * Returns {@link Sort#unsorted()} for null/empty input.
     *
     * @param sortOrders sort columns requested by the client
     * @return Spring Data Sort
     * @throws InvalidSortException when a column is rejected
     */
    public Sort toSort(final List<SortableColumn> sortOrders) {
        if (CollectionUtils.isEmpty(sortOrders)) {
            return Sort.unsorted();
        }
        return toJpaSort(normalise(sortOrders));
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
     * Delegates to {@link #resolveSort(SortablePageInput)} — strict validation applies.
     *
     * @param input the sortable page input.
     * @return a configured {@link Pageable}.
     */
    public Pageable toPageable(final SortablePageInput input) {
        return resolveSort(input).getPageable();
    }


    /** Validates each column and returns the normalised list; throws on first invalid entry. */
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

    /** Normalises direction string: ASC/DESC (case-insensitive) → uppercase; anything else → {@code null}. */
    private static String normaliseDirection(final String direction) {
        if (direction == null || direction.isBlank()) {
            return null;
        }
        final String upper = direction.strip().toUpperCase();
        return ("ASC".equals(upper) || "DESC".equals(upper)) ? upper : null;
    }

    /** Returns the field name as-is (no normalisation — strict case-sensitive match). */
    private static String normaliseFieldName(final SortableColumn col) {
        return col.getName();
    }

    /** Builds a {@link Sort} with ignoreCase + nullsLast on every order, plus the tiebreaker. */
    private Sort toJpaSort(final List<SortableColumn> columns) {
        final List<Sort.Order> orders = new ArrayList<>();
        for (final SortableColumn col : columns) {
            final String dir = normaliseDirection(col.getDirection());
            final List<String> dbCols = columnNames.get(col.getName());
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
