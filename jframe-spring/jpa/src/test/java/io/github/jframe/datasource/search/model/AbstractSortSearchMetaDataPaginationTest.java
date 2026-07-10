package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.JpaSearchSpecification;
import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Unit Test - AbstractSortSearchMetaData Pagination & Specification")
class AbstractSortSearchMetaDataPaginationTest extends UnitTest {

    private TestMetaData metaData;

    @BeforeEach
    public void setUp() {
        metaData = new TestMetaData();
    }

    // -------------------------------------------------------------------------
    // toPageable()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create PageRequest using input pageNumber and explicit pageSize")
    void shouldCreatePageRequestUsingExplicitPageSize() {
        // Given: An input with pageNumber=2 and pageSize=10
        final SortablePageInput input = new SortablePageInput(2, 10, Collections.emptyList(), Collections.emptyList());

        // When: Converting to Pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: PageRequest should reflect input values
        assertThat(pageable, is(notNullValue()));
        assertThat(pageable.getPageNumber(), is(equalTo(2)));
        assertThat(pageable.getPageSize(), is(equalTo(10)));
    }

    @Test
    @DisplayName("Should use default page size (20) when pageSize is zero")
    void shouldUseDefaultPageSizeWhenPageSizeIsZero() {
        // Given: An input with pageSize=0
        final SortablePageInput input = new SortablePageInput(0, 0, Collections.emptyList(), Collections.emptyList());

        // When: Converting to Pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: Page size should be the default (20)
        assertThat(pageable.getPageSize(), is(equalTo(20)));
    }

    @Test
    @DisplayName("Should use default page size (20) when pageSize is negative")
    void shouldUseDefaultPageSizeWhenPageSizeIsNegative() {
        // Given: An input with pageSize=-1
        final SortablePageInput input = new SortablePageInput(0, -1, Collections.emptyList(), Collections.emptyList());

        // When: Converting to Pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: Page size should be the default (20)
        assertThat(pageable.getPageSize(), is(equalTo(20)));
    }

    @Test
    @DisplayName("Should use explicit page size (5) when positive pageSize provided")
    void shouldUseExplicitPageSizeWhenPositive() {
        // Given: An input with pageSize=5
        final SortablePageInput input = new SortablePageInput(0, 5, Collections.emptyList(), Collections.emptyList());

        // When: Converting to Pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: Page size should be 5
        assertThat(pageable.getPageSize(), is(equalTo(5)));
    }

    @Test
    @DisplayName("Should include sort from toSort in PageRequest")
    void shouldIncludeSortInPageRequest() {
        // Given: An input with a sort order on a known sortable field
        final SortablePageInput input = new SortablePageInput(
            0,
            25,
            List.of(new io.github.jframe.datasource.search.model.input.SortableColumn("name", "ASC")),
            Collections.emptyList()
        );

        // When: Converting to Pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: Sort should be applied
        assertThat(pageable.getSort().isUnsorted(), is(false));
        assertThat(pageable.getSort().getOrderFor("name"), is(notNullValue()));
    }

    @Test
    @DisplayName("Should return unsorted PageRequest when sortOrder is empty")
    void shouldReturnUnsortedPageRequestWhenNoSortOrder() {
        // Given: An input with no sort order
        final SortablePageInput input = new SortablePageInput(1, 15, Collections.emptyList(), Collections.emptyList());

        // When: Converting to Pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: Sort should be unsorted
        assertThat(pageable.getSort().isUnsorted(), is(true));
    }

    // -------------------------------------------------------------------------
    // getDefaultPageSize()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 20 as default page size from base implementation")
    void shouldReturnTwentyAsDefaultPageSize() {
        // Given: The default metadata implementation (no override)

        // When: Getting the default page size
        final int defaultPageSize = metaData.getDefaultPageSize();

        // Then: Default should be 20
        assertThat(defaultPageSize, is(equalTo(20)));
    }

    @Test
    @DisplayName("Should allow subclass to override getDefaultPageSize")
    void shouldAllowSubclassToOverrideDefaultPageSize() {
        // Given: A metadata subclass with overridden page size
        final AbstractSortSearchMetaData customMeta = new CustomPageSizeMetaData();

        // When: Getting the default page size
        final int defaultPageSize = customMeta.getDefaultPageSize();

        // Then: Overridden value should be returned
        assertThat(defaultPageSize, is(equalTo(50)));
    }

    @Test
    @DisplayName("Should use overridden default page size when pageSize is zero")
    void shouldUseOverriddenDefaultPageSizeWhenPageSizeIsZero() {
        // Given: A metadata subclass with overridden default page size of 50, and input with pageSize=0
        final AbstractSortSearchMetaData customMeta = new CustomPageSizeMetaData();
        final SortablePageInput input = new SortablePageInput(0, 0, Collections.emptyList(), Collections.emptyList());

        // When: Converting to Pageable
        final Pageable pageable = customMeta.toPageable(input);

        // Then: Page size should be the overridden default (50)
        assertThat(pageable.getPageSize(), is(equalTo(50)));
    }

    // -------------------------------------------------------------------------
    // toSearchSpecification(SortablePageInput)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return JpaSearchSpecification from toSearchSpecification")
    void shouldReturnJpaSearchSpecificationFromInput() {
        // Given: An input with no search criteria
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), Collections.emptyList());

        // When: Building a search specification
        final JpaSearchSpecification<?> spec = metaData.toSearchSpecification(input);

        // Then: A non-null specification should be returned
        assertThat(spec, is(notNullValue()));
        assertThat(spec, is(instanceOf(JpaSearchSpecification.class)));
    }

    @Test
    @DisplayName("Should return specification with conjunction predicate for empty search inputs")
    void shouldReturnConjunctionSpecForEmptySearchInputs() {
        // Given: An input with empty searchInputs list
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), Collections.emptyList());

        // When: Building a search specification
        final JpaSearchSpecification<?> spec = metaData.toSearchSpecification(input);

        // Then: Specification should be non-null (conjunction = no filtering)
        assertThat(spec, is(notNullValue()));
    }

    @Test
    @DisplayName("Should build specification from search inputs when present")
    void shouldBuildSpecificationFromSearchInputs() {
        // Given: An input with a search criterion for the 'name' field
        final SearchInput nameSearch = new SearchInput();
        nameSearch.setFieldName("name");
        nameSearch.setTextValue("Alice");
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), List.of(nameSearch));

        // When: Building a search specification
        final JpaSearchSpecification<?> spec = metaData.toSearchSpecification(input);

        // Then: Specification should be non-null and reflect the criteria
        assertThat(spec, is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // toSearchSpecification(SortablePageInput, String fieldPath, Object scopeValue)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return non-null scoped specification for simple field path")
    void shouldReturnScopedSpecForSimpleFieldPath() {
        // Given: An input and a simple scope field path
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), Collections.emptyList());
        final String fieldPath = "organizationId";
        final Long scopeValue = 42L;

        // When: Building a scoped specification
        final Specification<?> spec = metaData.toSearchSpecification(input, fieldPath, scopeValue);

        // Then: A non-null specification should be returned
        assertThat(spec, is(notNullValue()));
    }

    @Test
    @DisplayName("Should return non-null scoped specification for nested field path")
    void shouldReturnScopedSpecForNestedFieldPath() {
        // Given: An input and a nested scope field path
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), Collections.emptyList());
        final String fieldPath = "tenant.id";
        final Long scopeValue = 99L;

        // When: Building a scoped specification
        final Specification<?> spec = metaData.toSearchSpecification(input, fieldPath, scopeValue);

        // Then: A non-null specification should be returned
        assertThat(spec, is(notNullValue()));
    }

    @Test
    @DisplayName("Should return a different specification instance from the unscoped one")
    void shouldReturnDifferentSpecFromUnscopedOne() {
        // Given: The same input used for both scoped and unscoped specification creation
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), Collections.emptyList());

        // When: Building both versions
        final JpaSearchSpecification<?> unscoped = metaData.toSearchSpecification(input);
        final Specification<?> scoped = metaData.toSearchSpecification(input, "tenant.id", 1L);

        // Then: They should be different instances (scoped adds an AND predicate)
        assertThat(scoped, is(notNullValue()));
        assertThat(unscoped, is(notNullValue()));
        // scoped is a composed Specification (AND wrapper), not the same object as the raw JpaSearchSpecification
        assertThat(scoped == unscoped, is(false));
    }

    @Test
    @DisplayName("Should include existing search inputs in scoped specification")
    void shouldIncludeSearchInputsInScopedSpec() {
        // Given: An input with a search criterion combined with a scope
        final SearchInput nameSearch = new SearchInput();
        nameSearch.setFieldName("name");
        nameSearch.setTextValue("Bob");
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), List.of(nameSearch));

        // When: Building a scoped specification
        final Specification<?> spec = metaData.toSearchSpecification(input, "organizationId", 7L);

        // Then: Specification is non-null (criteria + scope AND-ed together)
        assertThat(spec, is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // toSort() regression — null/empty returns Sort.unsorted()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return Sort.unsorted() when sortOrder list is null")
    void shouldReturnUnsortedWhenSortOrderIsNull() {
        // Given: Null sort order input

        // When: Calling toSort with null
        final Sort sort = metaData.toSort(null);

        // Then: Should be unsorted, no exception thrown
        assertThat(sort, is(notNullValue()));
        assertThat(sort.isUnsorted(), is(true));
    }

    @Test
    @DisplayName("Should return Sort.unsorted() when sortOrder list is empty")
    void shouldReturnUnsortedWhenSortOrderIsEmpty() {
        // Given: Empty sort order input

        // When: Calling toSort with empty list
        final Sort sort = metaData.toSort(Collections.emptyList());

        // Then: Should be unsorted
        assertThat(sort, is(notNullValue()));
        assertThat(sort.isUnsorted(), is(true));
    }

    // -------------------------------------------------------------------------
    // Inner test fixtures
    // -------------------------------------------------------------------------

    static class TestMetaData extends AbstractSortSearchMetaData {

        TestMetaData() {
            super();
            addField("name", "name", SearchType.TEXT, true);
            addField("status", "status", SearchType.ENUM, false);
        }
    }


    static class CustomPageSizeMetaData extends AbstractSortSearchMetaData {

        CustomPageSizeMetaData() {
            super();
        }

        @Override
        protected int getDefaultPageSize() {
            return 50;
        }
    }
}
