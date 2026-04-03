package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchOperator;
import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.BooleanField;
import io.github.jframe.datasource.search.fields.DateField;
import io.github.jframe.datasource.search.fields.EnumField;
import io.github.jframe.datasource.search.fields.FuzzyTextField;
import io.github.jframe.datasource.search.fields.MultiColumnFuzzyField;
import io.github.jframe.datasource.search.fields.MultiEnumField;
import io.github.jframe.datasource.search.fields.MultiFuzzyField;
import io.github.jframe.datasource.search.fields.MultiTextField;
import io.github.jframe.datasource.search.fields.NumericField;
import io.github.jframe.datasource.search.fields.TextField;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.support.TestStatus;
import io.github.support.UnitTest;
import io.quarkus.panache.common.Sort;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AbstractPanacheSearchMetaData}.
 *
 * <p>Verifies field registration, search criteria conversion, Panache Sort generation,
 * predicate utility, thread-safety, and validation guards.
 */
@DisplayName("Quarkus JPA - AbstractPanacheSearchMetaData")
public class AbstractPanacheSearchMetaDataTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Concrete test subclass
    // -------------------------------------------------------------------------

    /**
     * Minimal concrete subclass that exposes {@code addField} methods for test use.
     */
    private static class TestSearchMetaData extends AbstractPanacheSearchMetaData {

        public void registerField(final String field, final String column,
            final SearchType searchType, final boolean sortable) {
            addField(field, column, searchType, sortable);
        }

        public void registerCustomField(final String field, final String column,
            final boolean sortable) {
            addField(field, column, sortable);
        }

        public void registerEnumField(final String field, final String column,
            final SearchType searchType, final Class<?> enumClass,
            final boolean sortable) {
            addField(field, column, searchType, enumClass, sortable);
        }

        public void registerMultiColumnField(final String field, final List<String> columns,
            final SearchType searchType, final boolean sortable) {
            addField(field, columns, searchType, sortable);
        }
    }


    /**
     * Pre-configured subclass used by most tests.
     */
    private static class FullSearchMetaData extends AbstractPanacheSearchMetaData {

        public FullSearchMetaData() {
            addField("name", "u.name", SearchType.TEXT, true);
            addField("email", "u.email", SearchType.FUZZY_TEXT, true);
            addField("createdAt", "u.created_at", SearchType.DATE, false);
            addField("age", "u.age", SearchType.NUMERIC, false);
            addField("active", "u.active", SearchType.BOOLEAN, false);
            addField("status", "u.status", SearchType.ENUM, TestStatus.class, true);
            addField("tags", "u.tag", SearchType.MULTI_TEXT, false);
            addField("roles", "u.role", SearchType.MULTI_ENUM, TestStatus.class, false);
            addField("fuzzy", "u.fuzzy", SearchType.MULTI_FUZZY, false);
            addField("search", List.of("u.first_name", "u.last_name"), SearchType.MULTI_COLUMN_FUZZY, false);
            addField("custom", "u.custom", false);
        }
    }

    // -------------------------------------------------------------------------
    // Mocks
    // -------------------------------------------------------------------------

    @Mock
    private Predicate predicate;

    @Mock
    private Expression<Boolean> expression;

    // -------------------------------------------------------------------------
    // Subject under test
    // -------------------------------------------------------------------------

    private TestSearchMetaData testMetaData;
    private FullSearchMetaData fullMetaData;

    @BeforeEach
    @Override
    public void setUp() {
        testMetaData = new TestSearchMetaData();
        fullMetaData = new FullSearchMetaData();
    }

    // =========================================================================
    // Factory helpers
    // =========================================================================

    private static SearchInput aSearchInput(final String fieldName, final String textValue) {
        final SearchInput input = new SearchInput();
        input.setFieldName(fieldName);
        input.setTextValue(textValue);
        return input;
    }

    private static SearchInput aSearchInputWithOperator(final String fieldName, final String textValue,
        final SearchOperator operator) {
        final SearchInput input = new SearchInput();
        input.setFieldName(fieldName);
        input.setTextValue(textValue);
        input.setOperator(operator);
        return input;
    }

    private static SearchInput aDateSearchInput(final String fieldName, final String from, final String to) {
        final SearchInput input = new SearchInput();
        input.setFieldName(fieldName);
        input.setFromDateValue(from);
        input.setToDateValue(to);
        return input;
    }

    private static SearchInput aMultiValueSearchInput(final String fieldName, final List<String> values) {
        final SearchInput input = new SearchInput();
        input.setFieldName(fieldName);
        input.setTextValueList(values);
        return input;
    }

    private static SortableColumn aSortableColumn(final String name, final String direction) {
        return new SortableColumn(name, direction);
    }

    // =========================================================================
    // Section 1: Field Registration — addField overloads
    // =========================================================================

    @Test
    @DisplayName("Should register single column field with searchType and sortable flag")
    public void shouldRegisterSingleColumnFieldWithSearchTypeAndSortableFlag() {
        // Given: A fresh metadata instance

        // When: Registering a TEXT field as sortable
        testMetaData.registerField("firstName", "u.first_name", SearchType.TEXT, true);

        // Then: The field is in searchTypes and sortableFields
        assertThat(testMetaData.getSearchTypes().get("firstName"), is(SearchType.TEXT));
        assertThat(testMetaData.getColumnNames().get("firstName"), is(List.of("u.first_name")));
        assertThat(testMetaData.getSortableFields().contains("firstName"), is(true));
    }

    @Test
    @DisplayName("Should register single column field as non-sortable")
    public void shouldRegisterSingleColumnFieldAsNonSortable() {
        // Given: A fresh metadata instance

        // When: Registering a TEXT field as not sortable
        testMetaData.registerField("description", "u.description", SearchType.TEXT, false);

        // Then: The field is in searchTypes but NOT in sortableFields
        assertThat(testMetaData.getSearchTypes().get("description"), is(SearchType.TEXT));
        assertThat(testMetaData.getSortableFields().contains("description"), is(false));
    }

    @Test
    @DisplayName("Should register custom search field with NONE type and mark no searchType entry")
    public void shouldRegisterCustomSearchFieldWithNoneTypeAndMarkNoSearchTypeEntry() {
        // Given: A fresh metadata instance

        // When: Registering a custom (NONE) field
        testMetaData.registerCustomField("customField", "u.custom_col", false);

        // Then: searchTypes does NOT contain the field (custom search is excluded), columnNames does
        assertThat(testMetaData.getSearchTypes().containsKey("customField"), is(false));
        assertThat(testMetaData.getColumnNames().get("customField"), is(List.of("u.custom_col")));
    }

    @Test
    @DisplayName("Should register custom search field as sortable")
    public void shouldRegisterCustomSearchFieldAsSortable() {
        // Given: A fresh metadata instance

        // When: Registering a custom field with sortable = true
        testMetaData.registerCustomField("sortableCustom", "u.custom_col", true);

        // Then: field is in sortableFields but not in searchTypes
        assertThat(testMetaData.getSearchTypes().containsKey("sortableCustom"), is(false));
        assertThat(testMetaData.getSortableFields().contains("sortableCustom"), is(true));
    }

    @Test
    @DisplayName("Should register ENUM field with enum class")
    public void shouldRegisterEnumFieldWithEnumClass() {
        // Given: A fresh metadata instance

        // When: Registering an ENUM field with TestStatus class
        testMetaData.registerEnumField("status", "u.status", SearchType.ENUM, TestStatus.class, false);

        // Then: searchType is ENUM, enumClass is stored
        assertThat(testMetaData.getSearchTypes().get("status"), is(SearchType.ENUM));
        assertThat(testMetaData.getEnumClasses().get("status"), is(equalTo(TestStatus.class)));
    }

    @Test
    @DisplayName("Should register MULTI_ENUM field with enum class")
    public void shouldRegisterMultiEnumFieldWithEnumClass() {
        // Given: A fresh metadata instance

        // When: Registering a MULTI_ENUM field
        testMetaData.registerEnumField("roles", "u.role", SearchType.MULTI_ENUM, TestStatus.class, false);

        // Then: searchType is MULTI_ENUM, enumClass is stored
        assertThat(testMetaData.getSearchTypes().get("roles"), is(SearchType.MULTI_ENUM));
        assertThat(testMetaData.getEnumClasses().get("roles"), is(equalTo(TestStatus.class)));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when registering ENUM field with non-ENUM searchType")
    public void shouldThrowIllegalArgumentExceptionWhenRegisteringEnumFieldWithNonEnumSearchType() {
        // Given: A fresh metadata instance

        // When & Then: Registering ENUM overload with TEXT type should throw
        assertThrows(
            IllegalArgumentException.class,
            () -> testMetaData.registerEnumField("field", "col", SearchType.TEXT, TestStatus.class, false)
        );
    }

    @Test
    @DisplayName("Should register multi-column field with MULTI_COLUMN_FUZZY type")
    public void shouldRegisterMultiColumnFieldWithMultiColumnFuzzyType() {
        // Given: A fresh metadata instance and two column names
        final List<String> columns = List.of("u.first_name", "u.last_name");

        // When: Registering a multi-column field
        testMetaData.registerMultiColumnField("fullName", columns, SearchType.MULTI_COLUMN_FUZZY, false);

        // Then: columnNames stores both columns, searchType is MULTI_COLUMN_FUZZY
        assertThat(testMetaData.getSearchTypes().get("fullName"), is(SearchType.MULTI_COLUMN_FUZZY));
        assertThat(testMetaData.getColumnNames().get("fullName"), is(columns));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when registering multi-column field with non-MULTI_COLUMN_FUZZY type")
    public void shouldThrowIllegalArgumentExceptionWhenRegisteringMultiColumnFieldWithWrongType() {
        // Given: A fresh metadata instance
        final List<String> columns = List.of("col1", "col2");

        // When & Then: Registering multi-column overload with TEXT type should throw
        assertThrows(
            IllegalArgumentException.class,
            () -> testMetaData.registerMultiColumnField("field", columns, SearchType.TEXT, false)
        );
    }

    @Test
    @DisplayName("Should overwrite field registration when same field is registered twice")
    public void shouldOverwriteFieldRegistrationWhenSameFieldIsRegisteredTwice() {
        // Given: A field already registered with TEXT type
        testMetaData.registerField("name", "u.name", SearchType.TEXT, false);

        // When: Registering the same field again with FUZZY_TEXT type
        testMetaData.registerField("name", "u.name_fuzzy", SearchType.FUZZY_TEXT, false);

        // Then: The second registration overwrites the first
        assertThat(testMetaData.getSearchTypes().get("name"), is(SearchType.FUZZY_TEXT));
        assertThat(testMetaData.getColumnNames().get("name"), is(List.of("u.name_fuzzy")));
    }

    // =========================================================================
    // Section 2: toSearchCriteria
    // =========================================================================

    @Test
    @DisplayName("Should return empty list when input list is empty")
    public void shouldReturnEmptyListWhenInputListIsEmpty() {
        // Given: An empty list of search inputs

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(Collections.emptyList());

        // Then: An empty list is returned
        assertThat(result, is(empty()));
    }

    @Test
    @DisplayName("Should return empty list when input list is null")
    public void shouldReturnEmptyListWhenInputListIsNull() {
        // Given: A null input list

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(null);

        // Then: An empty list is returned
        assertThat(result, is(empty()));
    }

    @Test
    @DisplayName("Should return empty list when input field is not registered")
    public void shouldReturnEmptyListWhenInputFieldIsNotRegistered() {
        // Given: A search input referencing an unregistered field
        final SearchInput input = aSearchInput("unknownField", "value");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: The unregistered field is filtered out, empty result
        assertThat(result, is(empty()));
    }

    @Test
    @DisplayName("Should produce TextField criterium for TEXT search type")
    public void shouldProduceTextFieldCriteriumForTextSearchType() {
        // Given: A search input for the 'name' TEXT field
        final SearchInput input = aSearchInput("name", "John");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single TextField criterium is returned
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(TextField.class)));
        assertThat(((TextField) result.get(0)).getValue(), is("John"));
    }

    @Test
    @DisplayName("Should produce FuzzyTextField criterium for FUZZY_TEXT search type")
    public void shouldProduceFuzzyTextFieldCriteriumForFuzzyTextSearchType() {
        // Given: A search input for the 'email' FUZZY_TEXT field
        final SearchInput input = aSearchInput("email", "john@");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single FuzzyTextField criterium is returned
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(FuzzyTextField.class)));
        assertThat(((FuzzyTextField) result.get(0)).getValue(), is("john@"));
    }

    @Test
    @DisplayName("Should produce DateField criterium for DATE search type")
    public void shouldProduceDateFieldCriteriumForDateSearchType() {
        // Given: A search input for the 'createdAt' DATE field with from/to dates
        final SearchInput input = aDateSearchInput(
            "createdAt",
            "2024-01-01T00:00:00",
            "2024-12-31T23:59:59"
        );

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single DateField criterium is returned with from/to dates populated
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(DateField.class)));
        final DateField dateField = (DateField) result.get(0);
        assertThat(dateField.getFromDate(), is(notNullValue()));
        assertThat(dateField.getToDate(), is(notNullValue()));
    }

    @Test
    @DisplayName("Should produce NumericField criterium for NUMERIC search type")
    public void shouldProduceNumericFieldCriteriumForNumericSearchType() {
        // Given: A search input for the 'age' NUMERIC field
        final SearchInput input = aSearchInput("age", "30");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single NumericField criterium is returned
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(NumericField.class)));
        assertThat(((NumericField) result.get(0)).getValue(), is(30));
    }

    @Test
    @DisplayName("Should produce BooleanField criterium for BOOLEAN search type")
    public void shouldProduceBooleanFieldCriteriumForBooleanSearchType() {
        // Given: A search input for the 'active' BOOLEAN field
        final SearchInput input = aSearchInput("active", "true");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single BooleanField criterium is returned
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(BooleanField.class)));
        assertThat(((BooleanField) result.get(0)).isValue(), is(true));
    }

    @Test
    @DisplayName("Should produce EnumField criterium for ENUM search type with correct enum class")
    public void shouldProduceEnumFieldCriteriumForEnumSearchTypeWithCorrectEnumClass() {
        // Given: A search input for the 'status' ENUM field
        final SearchInput input = aSearchInput("status", "ACTIVE");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single EnumField criterium is returned with correct enum class
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(EnumField.class)));
        final EnumField enumField = (EnumField) result.get(0);
        assertThat(enumField.getEnumClass(), is(equalTo(TestStatus.class)));
        assertThat(enumField.getValue(), is("ACTIVE"));
    }

    @Test
    @DisplayName("Should produce MultiEnumField criterium for MULTI_ENUM search type with correct enum class")
    public void shouldProduceMultiEnumFieldCriteriumForMultiEnumSearchTypeWithCorrectEnumClass() {
        // Given: A search input for the 'roles' MULTI_ENUM field
        final SearchInput input = aMultiValueSearchInput("roles", List.of("ACTIVE", "PENDING"));

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single MultiEnumField criterium is returned with correct enum class
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(MultiEnumField.class)));
        final MultiEnumField multiEnumField = (MultiEnumField) result.get(0);
        assertThat(multiEnumField.getEnumClass(), is(equalTo(TestStatus.class)));
        assertThat(multiEnumField.getValues(), hasSize(2));
    }

    @Test
    @DisplayName("Should produce MultiTextField criterium for MULTI_TEXT search type")
    public void shouldProduceMultiTextFieldCriteriumForMultiTextSearchType() {
        // Given: A search input for the 'tags' MULTI_TEXT field
        final SearchInput input = aMultiValueSearchInput("tags", List.of("java", "quarkus"));

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single MultiTextField criterium is returned
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(MultiTextField.class)));
        assertThat(((MultiTextField) result.get(0)).getValues(), hasSize(2));
    }

    @Test
    @DisplayName("Should produce MultiFuzzyField criterium for MULTI_FUZZY search type with operator")
    public void shouldProduceMultiFuzzyFieldCriteriumForMultiFuzzySearchTypeWithOperator() {
        // Given: A search input for the 'fuzzy' MULTI_FUZZY field with OR operator
        final SearchInput input = aSearchInputWithOperator("fuzzy", "hello world", SearchOperator.OR);

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single MultiFuzzyField criterium is returned with the correct operator
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(MultiFuzzyField.class)));
        final MultiFuzzyField multiFuzzyField = (MultiFuzzyField) result.get(0);
        assertThat(multiFuzzyField.getOperator(), is(SearchOperator.OR));
        assertThat(multiFuzzyField.getValue(), is("hello world"));
    }

    @Test
    @DisplayName("Should produce MultiColumnFuzzyField criterium for MULTI_COLUMN_FUZZY search type with multiple columns")
    public void shouldProduceMultiColumnFuzzyFieldCriteriumWithMultipleColumns() {
        // Given: A search input for the 'search' MULTI_COLUMN_FUZZY field
        final SearchInput input = aSearchInput("search", "doe");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A single MultiColumnFuzzyField criterium is returned with two columns
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(MultiColumnFuzzyField.class)));
        final MultiColumnFuzzyField multiColumnField = (MultiColumnFuzzyField) result.get(0);
        assertThat(multiColumnField.getColumnNames(), hasSize(2));
        assertThat(multiColumnField.getValue(), is("doe"));
    }

    @Test
    @DisplayName("Should filter out NONE type criterium from results")
    public void shouldFilterOutNoneTypeCriteriumFromResults() {
        // Given: A custom (NONE) field registered, and a search input for it
        testMetaData.registerCustomField("custom", "u.custom", false);
        final SearchInput input = aSearchInput("custom", "someValue");

        // When: Converting to search criteria
        final List<SearchCriterium> result = testMetaData.toSearchCriteria(List.of(input));

        // Then: Result is empty because NONE factory returns null (filtered)
        assertThat(result, is(empty()));
    }

    @Test
    @DisplayName("Should return multiple criteria for multiple matching inputs")
    public void shouldReturnMultipleCriteriaForMultipleMatchingInputs() {
        // Given: Multiple search inputs for different registered fields
        final SearchInput nameInput = aSearchInput("name", "John");
        final SearchInput emailInput = aSearchInput("email", "john@example.com");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(nameInput, emailInput));

        // Then: Two criteria are returned
        assertThat(result, hasSize(2));
        assertThat(result.get(0), is(instanceOf(TextField.class)));
        assertThat(result.get(1), is(instanceOf(FuzzyTextField.class)));
    }

    @Test
    @DisplayName("Should filter out unregistered fields from mixed input list")
    public void shouldFilterOutUnregisteredFieldsFromMixedInputList() {
        // Given: One registered field and one unregistered field in input
        final SearchInput nameInput = aSearchInput("name", "John");
        final SearchInput unknownInput = aSearchInput("nonExistent", "value");

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(nameInput, unknownInput));

        // Then: Only the registered field produces a criterium
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(TextField.class)));
    }

    // =========================================================================
    // Section 3: toSort(List<SortableColumn>)
    // =========================================================================

    @Test
    @DisplayName("Should return null when sortable column list is null")
    public void shouldReturnNullWhenSortableColumnListIsNull() {
        // Given: A null list of sortable columns

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(null);

        // Then: null is returned (Panache has no Sort.unsorted())
        assertThat(sort, is(nullValue()));
    }

    @Test
    @DisplayName("Should return null when sortable column list is empty")
    public void shouldReturnNullWhenSortableColumnListIsEmpty() {
        // Given: An empty list of sortable columns

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(Collections.emptyList());

        // Then: null is returned for empty input
        assertThat(sort, is(nullValue()));
    }

    @Test
    @DisplayName("Should return Panache Sort for single sortable field with ASC direction")
    public void shouldReturnPanacheSortForSingleSortableFieldWithAscDirection() {
        // Given: A single sortable column for 'name' field
        final List<SortableColumn> columns = List.of(aSortableColumn("name", "ASC"));

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(columns);

        // Then: A non-null Sort is returned
        assertThat(sort, is(notNullValue()));
        assertThat(sort.getColumns(), hasSize(1));
        assertThat(sort.getColumns().get(0).getName(), is("u.name"));
        assertThat(sort.getColumns().get(0).getDirection(), is(Sort.Direction.Ascending));
    }

    @Test
    @DisplayName("Should return Panache Sort for single sortable field with DESC direction")
    public void shouldReturnPanacheSortForSingleSortableFieldWithDescDirection() {
        // Given: A single sortable column for 'email' field
        final List<SortableColumn> columns = List.of(aSortableColumn("email", "DESC"));

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(columns);

        // Then: A non-null Sort with Descending direction is returned
        assertThat(sort, is(notNullValue()));
        assertThat(sort.getColumns(), hasSize(1));
        assertThat(sort.getColumns().get(0).getName(), is("u.email"));
        assertThat(sort.getColumns().get(0).getDirection(), is(Sort.Direction.Descending));
    }

    @Test
    @DisplayName("Should return Panache Sort with multiple fields in correct order")
    public void shouldReturnPanacheSortWithMultipleFieldsInCorrectOrder() {
        // Given: Multiple sortable columns
        final List<SortableColumn> columns = List.of(
            aSortableColumn("name", "ASC"),
            aSortableColumn("status", "DESC")
        );

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(columns);

        // Then: Sort contains two columns mapped to DB column names
        assertThat(sort, is(notNullValue()));
        assertThat(sort.getColumns(), hasSize(2));
        assertThat(sort.getColumns().get(0).getName(), is("u.name"));
        assertThat(sort.getColumns().get(0).getDirection(), is(Sort.Direction.Ascending));
        assertThat(sort.getColumns().get(1).getName(), is("u.status"));
        assertThat(sort.getColumns().get(1).getDirection(), is(Sort.Direction.Descending));
    }

    @Test
    @DisplayName("Should map frontend field name to DB column name in Sort")
    public void shouldMapFrontendFieldNameToDbColumnNameInSort() {
        // Given: Frontend field 'email' maps to DB column 'u.email'
        final List<SortableColumn> columns = List.of(aSortableColumn("email", "ASC"));

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(columns);

        // Then: Sort column uses DB column name, not frontend field name
        assertThat(sort.getColumns().get(0).getName(), is("u.email"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when sorting on non-sortable field")
    public void shouldThrowIllegalArgumentExceptionWhenSortingOnNonSortableField() {
        // Given: 'createdAt' is registered as non-sortable
        final List<SortableColumn> columns = List.of(aSortableColumn("createdAt", "ASC"));

        // When & Then: toSort should throw for non-sortable field
        assertThrows(IllegalArgumentException.class, () -> fullMetaData.toSort(columns));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when sorting on unregistered field")
    public void shouldThrowIllegalArgumentExceptionWhenSortingOnUnregisteredField() {
        // Given: 'unknownField' is not registered in metadata
        final List<SortableColumn> columns = List.of(aSortableColumn("unknownField", "ASC"));

        // When & Then: toSort should throw because field is unknown
        assertThrows(IllegalArgumentException.class, () -> fullMetaData.toSort(columns));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when mix of sortable and non-sortable fields requested")
    public void shouldThrowIllegalArgumentExceptionWhenMixOfSortableAndNonSortableFieldsRequested() {
        // Given: 'name' is sortable, 'createdAt' is not sortable
        final List<SortableColumn> columns = List.of(
            aSortableColumn("name", "ASC"),
            aSortableColumn("createdAt", "DESC")
        );

        // When & Then: toSort should throw because not all requested fields are sortable
        assertThrows(IllegalArgumentException.class, () -> fullMetaData.toSort(columns));
    }

    @Test
    @DisplayName("Should handle case-insensitive ASC direction string in toSort")
    public void shouldHandleCaseInsensitiveAscDirectionStringInToSort() {
        // Given: Lowercase 'asc' direction
        final List<SortableColumn> columns = List.of(aSortableColumn("name", "asc"));

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(columns);

        // Then: Sort is non-null and uses Ascending direction
        assertThat(sort, is(notNullValue()));
        assertThat(sort.getColumns().get(0).getDirection(), is(Sort.Direction.Ascending));
    }

    @Test
    @DisplayName("Should handle case-insensitive DESC direction string in toSort")
    public void shouldHandleCaseInsensitiveDescDirectionStringInToSort() {
        // Given: Lowercase 'desc' direction
        final List<SortableColumn> columns = List.of(aSortableColumn("name", "desc"));

        // When: Converting to Panache Sort
        final Sort sort = fullMetaData.toSort(columns);

        // Then: Sort is non-null and uses Descending direction
        assertThat(sort, is(notNullValue()));
        assertThat(sort.getColumns().get(0).getDirection(), is(Sort.Direction.Descending));
    }

    // =========================================================================
    // Section 4: isEmptyPredicate(Predicate) — static utility
    // =========================================================================

    @Test
    @DisplayName("Should return true for null predicate")
    public void shouldReturnTrueForNullPredicate() {
        // Given: A null predicate

        // When: Checking if it is empty
        final boolean result = AbstractPanacheSearchMetaData.isEmptyPredicate(null);

        // Then: true is returned
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true when predicate has null expressions")
    public void shouldReturnTrueWhenPredicateHasNullExpressions() {
        // Given: A predicate mock returning null expressions
        when(predicate.getExpressions()).thenReturn(null);

        // When: Checking if it is empty
        final boolean result = AbstractPanacheSearchMetaData.isEmptyPredicate(predicate);

        // Then: true is returned
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true when predicate has empty expressions list")
    public void shouldReturnTrueWhenPredicateHasEmptyExpressionsList() {
        // Given: A predicate mock returning empty expressions
        when(predicate.getExpressions()).thenReturn(Collections.emptyList());

        // When: Checking if it is empty
        final boolean result = AbstractPanacheSearchMetaData.isEmptyPredicate(predicate);

        // Then: true is returned
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false when predicate has non-empty expressions list")
    public void shouldReturnFalseWhenPredicateHasNonEmptyExpressionsList() {
        // Given: A predicate mock returning one expression
        when(predicate.getExpressions()).thenReturn(List.of(expression));

        // When: Checking if it is empty
        final boolean result = AbstractPanacheSearchMetaData.isEmptyPredicate(predicate);

        // Then: false is returned
        assertThat(result, is(false));
    }

    // =========================================================================
    // Section 5: Thread-safety
    // =========================================================================

    @Test
    @DisplayName("Should register fields from multiple threads without data corruption")
    public void shouldRegisterFieldsFromMultipleThreadsWithoutDataCorruption() throws InterruptedException {
        // Given: A shared metadata instance and multiple threads registering fields
        final TestSearchMetaData sharedMetaData = new TestSearchMetaData();
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final CopyOnWriteArrayList<Exception> exceptions = new CopyOnWriteArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When: Each thread registers a unique field concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    sharedMetaData.registerField("field" + index, "col" + index, SearchType.TEXT, false);
                } catch (final Exception ex) {
                    exceptions.add(ex);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // Then: No exceptions thrown and all fields are registered without corruption
        assertThat(exceptions, is(empty()));
        assertThat(sharedMetaData.getSearchTypes().size(), is(threadCount));
        assertThat(sharedMetaData.getColumnNames().size(), is(threadCount));
    }

    // =========================================================================
    // Section 6: Validation edge cases
    // =========================================================================

    @Test
    @DisplayName("Should throw IllegalArgumentException for ENUM overload with FUZZY_TEXT type")
    public void shouldThrowIllegalArgumentExceptionForEnumOverloadWithFuzzyTextType() {
        // Given: An attempt to register ENUM overload with FUZZY_TEXT type

        // When & Then: Should throw immediately
        assertThrows(
            IllegalArgumentException.class,
            () -> testMetaData.registerEnumField("field", "col", SearchType.FUZZY_TEXT, TestStatus.class, false)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for ENUM overload with BOOLEAN type")
    public void shouldThrowIllegalArgumentExceptionForEnumOverloadWithBooleanType() {
        // Given: An attempt to register ENUM overload with BOOLEAN type

        // When & Then: Should throw immediately
        assertThrows(
            IllegalArgumentException.class,
            () -> testMetaData.registerEnumField("field", "col", SearchType.BOOLEAN, TestStatus.class, false)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for multi-column overload with FUZZY_TEXT type")
    public void shouldThrowIllegalArgumentExceptionForMultiColumnOverloadWithFuzzyTextType() {
        // Given: An attempt to register multi-column field with FUZZY_TEXT type
        final List<String> columns = List.of("col1", "col2");

        // When & Then: Should throw immediately
        assertThrows(
            IllegalArgumentException.class,
            () -> testMetaData.registerMultiColumnField("field", columns, SearchType.FUZZY_TEXT, false)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for multi-column overload with ENUM type")
    public void shouldThrowIllegalArgumentExceptionForMultiColumnOverloadWithEnumType() {
        // Given: An attempt to register multi-column field with ENUM type
        final List<String> columns = List.of("col1", "col2");

        // When & Then: Should throw immediately
        assertThrows(
            IllegalArgumentException.class,
            () -> testMetaData.registerMultiColumnField("field", columns, SearchType.ENUM, false)
        );
    }

    @Test
    @DisplayName("Should return empty criteria list when custom field is searched with NONE type")
    public void shouldReturnEmptyCriteriaListWhenCustomFieldIsSearchedWithNoneType() {
        // Given: Custom field registered (NONE type, no searchTypes entry)
        testMetaData.registerCustomField("myCustom", "my_col", false);

        // When: Searching on that custom field
        final SearchInput input = aSearchInput("myCustom", "test");
        final List<SearchCriterium> result = testMetaData.toSearchCriteria(List.of(input));

        // Then: Empty result because custom field has no searchType factory mapping (null factory returns null)
        assertThat(result, is(empty()));
    }

    @Test
    @DisplayName("Should allow custom search field registered as sortable to be used in toSort")
    public void shouldAllowCustomSearchFieldRegisteredAsSortableToBeUsedInToSort() {
        // Given: A custom field registered as sortable
        testMetaData.registerCustomField("sortableCustom", "custom_col", true);
        final List<SortableColumn> columns = List.of(aSortableColumn("sortableCustom", "ASC"));

        // When: Converting to Panache Sort
        final Sort sort = testMetaData.toSort(columns);

        // Then: Sort is returned using the DB column name
        assertThat(sort, is(notNullValue()));
        assertThat(sort.getColumns().get(0).getName(), is("custom_col"));
        assertThat(sort.getColumns().get(0).getDirection(), is(Sort.Direction.Ascending));
    }

    @Test
    @DisplayName("Should return correct searchType count after multiple field registrations")
    public void shouldReturnCorrectSearchTypeCountAfterMultipleFieldRegistrations() {
        // Given: Multiple fields registered
        testMetaData.registerField("f1", "col1", SearchType.TEXT, false);
        testMetaData.registerField("f2", "col2", SearchType.FUZZY_TEXT, false);
        testMetaData.registerField("f3", "col3", SearchType.NUMERIC, false);

        // When: Inspecting searchTypes map

        // Then: All three are registered
        assertThat(testMetaData.getSearchTypes().size(), is(3));
    }

    @Test
    @DisplayName("Should produce DateField with null dates when date inputs are null")
    public void shouldProduceDateFieldWithNullDatesWhenDateInputsAreNull() {
        // Given: A date search input with null from/to dates
        final SearchInput input = aDateSearchInput("createdAt", null, null);

        // When: Converting to search criteria
        final List<SearchCriterium> result = fullMetaData.toSearchCriteria(List.of(input));

        // Then: A DateField is produced with null from/to dates
        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(instanceOf(DateField.class)));
        final DateField dateField = (DateField) result.get(0);
        assertThat(dateField.getFromDate(), is(nullValue()));
        assertThat(dateField.getToDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not add field to sortableFields when sortable is false")
    public void shouldNotAddFieldToSortableFieldsWhenSortableIsFalse() {
        // Given: A field registered with sortable=false
        testMetaData.registerField("notSortable", "col", SearchType.TEXT, false);

        // When: Checking sortableFields

        // Then: Field is absent from sortableFields
        assertThat(testMetaData.getSortableFields().contains("notSortable"), is(false));
        assertThat(testMetaData.getSortableFields(), is(empty()));
    }

}
