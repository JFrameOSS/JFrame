package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.PageableItem;
import io.github.jframe.datasource.search.model.resource.PageResource;
import io.github.support.UnitTest;
import io.quarkus.panache.common.Sort;

import java.util.Collections;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PanacheSearchRepository}.
 *
 * <p>Verifies specification-based paginated querying via JPA Criteria API,
 * including predicate application, pagination offsets, null spec/sort handling,
 * and total-pages calculation.
 */
@DisplayName("Quarkus JPA - PanacheSearchRepository")
public class PanacheSearchRepositoryTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Test entity
    // -------------------------------------------------------------------------

    /**
     * Minimal concrete entity used in all test scenarios.
     */
    static class TestEntity implements PageableItem {

        private final String name;

        TestEntity(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // -------------------------------------------------------------------------
    // Concrete subclass under test
    // -------------------------------------------------------------------------


    /**
     * Concrete implementation that exposes the injected mocks to the test harness.
     */
    static class TestPanacheSearchRepository extends PanacheSearchRepository<TestEntity> {

        private final EntityManager em;

        TestPanacheSearchRepository(final EntityManager em) {
            this.em = em;
        }

        @Override
        protected Class<TestEntity> entityClass() {
            return TestEntity.class;
        }

        @Override
        protected EntityManager entityManager() {
            return em;
        }
    }

    // -------------------------------------------------------------------------
    // Mocks & SUT
    // -------------------------------------------------------------------------

    @Mock
    private EntityManager entityManager;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<TestEntity> criteriaQuery;

    @Mock
    private CriteriaQuery<Long> countQuery;

    @Mock
    private Root<TestEntity> root;

    @Mock
    private TypedQuery<TestEntity> typedQuery;

    @Mock
    private TypedQuery<Long> countTypedQuery;

    @Mock
    private Predicate predicate;

    @Mock
    private Expression<Long> countExpression;

    private TestPanacheSearchRepository repository;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Override
    @BeforeEach
    public void setUp() {
        repository = new TestPanacheSearchRepository(entityManager);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);

        // Main result query
        when(criteriaBuilder.createQuery(TestEntity.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(TestEntity.class)).thenReturn(root);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);

        // Count query
        when(criteriaBuilder.createQuery(Long.class)).thenReturn(countQuery);
        when(countQuery.from(TestEntity.class)).thenReturn(root);
        when(entityManager.createQuery(countQuery)).thenReturn(countTypedQuery);
        when(criteriaBuilder.count(root)).thenReturn(countExpression);
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    private static TestEntity aTestEntity(final String name) {
        return new TestEntity(name);
    }

    private static SearchSpecification<TestEntity> aMatchingSpec(final Predicate predicate) {
        return (root, query, cb) -> predicate;
    }

    // -------------------------------------------------------------------------
    // 1. Returns PageResource with correct content when spec matches entities
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return PageResource with content when spec matches entities")
    public void shouldReturnPageResourceWithContentWhenSpecMatchesEntities() {
        // Given: A spec and two matching entities
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        final List<TestEntity> entities = List.of(aTestEntity("alice"), aTestEntity("bob"));
        when(typedQuery.getResultList()).thenReturn(entities);
        when(countTypedQuery.getSingleResult()).thenReturn(2L);

        // When: Searching page 0 with size 10
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, null);

        // Then: PageResource content matches the returned entities
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), hasSize(2));
    }

    @Test
    @DisplayName("Should include all matched entities in PageResource content")
    public void shouldIncludeAllMatchedEntitiesInPageResourceContent() {
        // Given: A spec and known entities
        final TestEntity alice = aTestEntity("alice");
        final TestEntity bob = aTestEntity("bob");
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(List.of(alice, bob));
        when(countTypedQuery.getSingleResult()).thenReturn(2L);

        // When: Searching page 0 with size 10
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, null);

        // Then: Content list contains both entities in order
        assertThat(result.getContent(), contains(alice, bob));
    }

    // -------------------------------------------------------------------------
    // 2. Returns empty PageResource when no entities match
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return PageResource with empty content when no entities match")
    public void shouldReturnPageResourceWithEmptyContentWhenNoEntitiesMatch() {
        // Given: A spec that matches nothing
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, null);

        // Then: Content is empty and totalElements is 0
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), hasSize(0));
        assertThat(result.getTotalElements(), is(equalTo(0L)));
    }

    // -------------------------------------------------------------------------
    // 3. Applies pagination offset correctly (offset = pageNumber * pageSize)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should set first result to pageNumber times pageSize")
    public void shouldSetFirstResultToPageNumberTimesPageSize() {
        // Given: A request for page 2 with size 10 (offset should be 20)
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching page 2
        repository.searchPage(spec, 2, 10, null);

        // Then: firstResult is set to 20
        verify(typedQuery).setFirstResult(20);
    }

    @Test
    @DisplayName("Should set max results to pageSize")
    public void shouldSetMaxResultsToPageSize() {
        // Given: A request with page size 15
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with pageSize 15
        repository.searchPage(spec, 0, 15, null);

        // Then: maxResults is set to 15
        verify(typedQuery).setMaxResults(15);
    }

    @Test
    @DisplayName("Should set zero offset for first page")
    public void shouldSetZeroOffsetForFirstPage() {
        // Given: A request for page 0 (first page)
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching page 0
        repository.searchPage(spec, 0, 25, null);

        // Then: firstResult is 0
        verify(typedQuery).setFirstResult(0);
    }

    // -------------------------------------------------------------------------
    // 4. Handles null specification (returns all entities)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return all entities when specification is null")
    public void shouldReturnAllEntitiesWhenSpecificationIsNull() {
        // Given: Null specification and two entities in the table
        final List<TestEntity> entities = List.of(aTestEntity("alice"), aTestEntity("bob"));
        when(typedQuery.getResultList()).thenReturn(entities);
        when(countTypedQuery.getSingleResult()).thenReturn(2L);

        // When: Searching without a spec
        final PageResource<TestEntity> result = repository.searchPage(null, 0, 10, null);

        // Then: All entities are returned without filtering
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), hasSize(2));
    }

    @Test
    @DisplayName("Should not apply predicate to query when specification is null")
    public void shouldNotApplyPredicateToQueryWhenSpecificationIsNull() {
        // Given: Null specification
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching without a spec
        repository.searchPage(null, 0, 10, null);

        // Then: No where clause predicate is set from spec
        verify(criteriaQuery, never()).where(any(Predicate.class));
    }

    // -------------------------------------------------------------------------
    // 5. Handles null sort (no ORDER BY applied)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return PageResource when sort is null")
    public void shouldReturnPageResourceWhenSortIsNull() {
        // Given: Null sort
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with null sort
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, null);

        // Then: Result is still produced without errors
        assertThat(result, is(notNullValue()));
    }

    @Test
    @DisplayName("Should not apply order by when sort is null")
    public void shouldNotApplyOrderByWhenSortIsNull() {
        // Given: Null sort
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with null sort
        repository.searchPage(spec, 0, 10, null);

        // Then: No order-by clauses are applied to the criteria query
        verify(criteriaQuery, never()).orderBy(anyList());
        verify(criteriaQuery, never()).orderBy(any(Order.class));
    }

    // -------------------------------------------------------------------------
    // 6. Calculates totalPages correctly: ceil(totalElements / pageSize)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should calculate totalPages as ceiling of totalElements divided by pageSize")
    public void shouldCalculateTotalPagesAsCeilingOfTotalElementsDividedByPageSize() {
        // Given: 11 total elements with page size 5 → ceil(11/5) = 3
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(11L);

        // When: Searching page 0 with size 5
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 5, null);

        // Then: totalPages should be 3
        assertThat(result.getTotalPages(), is(equalTo(3)));
    }

    @Test
    @DisplayName("Should calculate totalPages as 1 when elements fit exactly in one page")
    public void shouldCalculateTotalPagesAsOneWhenElementsFitExactlyInOnePage() {
        // Given: 10 total elements with page size 10 → ceil(10/10) = 1
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(10L);

        // When: Searching with pageSize 10
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, null);

        // Then: totalPages should be 1
        assertThat(result.getTotalPages(), is(equalTo(1)));
    }

    @Test
    @DisplayName("Should calculate totalPages as 0 when there are no elements")
    public void shouldCalculateTotalPagesAsZeroWhenThereAreNoElements() {
        // Given: 0 total elements
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with any page size
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, null);

        // Then: totalPages should be 0
        assertThat(result.getTotalPages(), is(equalTo(0)));
    }

    @Test
    @DisplayName("Should expose totalElements in PageResource")
    public void shouldExposeTotalElementsInPageResource() {
        // Given: 42 total elements
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(42L);

        // When: Searching
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, null);

        // Then: totalElements is 42
        assertThat(result.getTotalElements(), is(equalTo(42L)));
    }

    // -------------------------------------------------------------------------
    // 7. Passes spec's predicate to criteria query
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should pass spec predicate to criteria query where clause")
    public void shouldPassSpecPredicateToCriteriaQueryWhereClause() {
        // Given: A spec that produces a known predicate
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with the spec
        repository.searchPage(spec, 0, 10, null);

        // Then: The criteria query has a where clause applied with the spec's predicate
        verify(criteriaQuery).where(eq(predicate));
    }

    // -------------------------------------------------------------------------
    // 8. Abstract contract — entityClass() and entityManager()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return the correct entity class from entityClass")
    public void shouldReturnCorrectEntityClassFromEntityClass() {
        // Given: The concrete test repository
        // When: Calling entityClass
        final Class<TestEntity> entityClass = repository.entityClass();

        // Then: Returns TestEntity.class
        assertThat(entityClass, is(equalTo(TestEntity.class)));
    }

    @Test
    @DisplayName("Should return the injected EntityManager from entityManager")
    public void shouldReturnInjectedEntityManagerFromEntityManager() {
        // Given: The concrete test repository
        // When: Calling entityManager
        final EntityManager result = repository.entityManager();

        // Then: Returns the mocked EntityManager
        assertThat(result, is(equalTo(entityManager)));
    }

    // -------------------------------------------------------------------------
    // 9. PageResource metadata — pageNumber and pageSize are reflected
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should expose pageNumber in PageResource")
    public void shouldExposePageNumberInPageResource() {
        // Given: A request for page 3
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching page 3
        final PageResource<TestEntity> result = repository.searchPage(spec, 3, 10, null);

        // Then: PageResource reports pageNumber 3
        assertThat(result.getPageNumber(), is(equalTo(3)));
    }

    @Test
    @DisplayName("Should expose pageSize in PageResource")
    public void shouldExposePageSizeInPageResource() {
        // Given: A request with page size 20
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with pageSize 20
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 20, null);

        // Then: PageResource reports pageSize 20
        assertThat(result.getPageSize(), is(equalTo(20)));
    }

    // -------------------------------------------------------------------------
    // 10. Sort is applied when provided
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return PageResource when Sort is provided")
    public void shouldReturnPageResourceWhenSortIsProvided() {
        // Given: A non-null Panache Sort
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        final Sort sort = Sort.by("name", Sort.Direction.Ascending);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with an explicit sort
        final PageResource<TestEntity> result = repository.searchPage(spec, 0, 10, sort);

        // Then: Result is produced without errors
        assertThat(result, is(notNullValue()));
    }

    @Test
    @DisplayName("Should apply order by when Sort is provided")
    public void shouldApplyOrderByWhenSortIsProvided() {
        // Given: A non-null Panache Sort by "name" ascending
        final SearchSpecification<TestEntity> spec = aMatchingSpec(predicate);
        final Sort sort = Sort.by("name", Sort.Direction.Ascending);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        // When: Searching with the sort
        repository.searchPage(spec, 0, 10, sort);

        // Then: An orderBy clause is applied to the criteria query
        verify(criteriaQuery).orderBy(anyList());
    }
}
