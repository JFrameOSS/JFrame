package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.BaseSearchSpecification;
import io.github.jframe.datasource.search.model.SearchCriterium;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PanacheSearchSpecification} in jframe-quarkus-jpa.
 *
 * <p>Verifies that the Quarkus {@link PanacheSearchSpecification} correctly extends
 * {@link BaseSearchSpecification}, implements the core {@link SearchSpecification},
 * and delegates predicate construction to the base class.
 */
@DisplayName("Quarkus - PanacheSearchSpecification")
public class PanacheSearchSpecificationTest extends UnitTest {

    @Mock
    private Root<Object> root;

    @Mock
    private CriteriaQuery<?> criteriaQuery;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate predicate;

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    private static PanacheSearchSpecification<Object> aSpecWithEmptyCriteria() {
        return new PanacheSearchSpecification<>(Collections.emptyList());
    }

    private static PanacheSearchSpecification<Object> aSpecWithNullCriteria() {
        return new PanacheSearchSpecification<>(null);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create instance with empty criteria list")
    public void shouldCreateInstanceWithEmptyCriteriaList() {
        // Given: An empty criteria list

        // When: Creating a PanacheSearchSpecification with empty criteria
        final PanacheSearchSpecification<Object> spec = new PanacheSearchSpecification<>(Collections.emptyList());

        // Then: Instance should be created successfully
        assertThat(spec, is(notNullValue()));
    }

    @Test
    @DisplayName("Should create instance with null criteria list")
    public void shouldCreateInstanceWithNullCriteriaList() {
        // Given: A null criteria list

        // When: Creating a PanacheSearchSpecification with null criteria
        final PanacheSearchSpecification<Object> spec = new PanacheSearchSpecification<>(null);

        // Then: Instance should be created successfully
        assertThat(spec, is(notNullValue()));
    }

    @Test
    @DisplayName("Should create instance with non-empty criteria list")
    public void shouldCreateInstanceWithNonEmptyCriteriaList() {
        // Given: A criteria list with one element
        final List<SearchCriterium> criteria = Collections.singletonList(new SearchCriterium());

        // When: Creating a PanacheSearchSpecification
        final PanacheSearchSpecification<Object> spec = new PanacheSearchSpecification<>(criteria);

        // Then: Instance should be created successfully
        assertThat(spec, is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // Class hierarchy
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should extend BaseSearchSpecification")
    public void shouldExtendBaseSearchSpecification() {
        // Given: A PanacheSearchSpecification instance
        final PanacheSearchSpecification<Object> spec = aSpecWithEmptyCriteria();

        // When: Checking the type hierarchy

        // Then: Must be an instance of BaseSearchSpecification
        assertThat(spec, is(instanceOf(BaseSearchSpecification.class)));
    }

    @Test
    @DisplayName("Should implement core SearchSpecification interface via inheritance")
    public void shouldImplementCoreSearchSpecificationInterfaceViaInheritance() {
        // Given: A PanacheSearchSpecification instance
        final PanacheSearchSpecification<Object> spec = aSpecWithEmptyCriteria();

        // When: Checking the type hierarchy

        // Then: Must be assignable to the core SearchSpecification interface (via BaseSearchSpecification)
        assertThat(spec, is(instanceOf(SearchSpecification.class)));
    }

    @Test
    @DisplayName("Should be assignable where SearchSpecification is expected")
    public void shouldBeAssignableWhereSearchSpecificationIsExpected() {
        // Given: A PanacheSearchSpecification
        final PanacheSearchSpecification<Object> spec = aSpecWithEmptyCriteria();

        // When: Assigning to a SearchSpecification reference
        final SearchSpecification<Object> searchSpec = spec;

        // Then: Assignment succeeds and reference is not null
        assertThat(searchSpec, is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // toPredicate delegation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return predicate when toPredicate is called with empty criteria")
    public void shouldReturnPredicateWhenToPredicateIsCalledWithEmptyCriteria() {
        // Given: A spec with empty criteria and a mocked conjunction predicate
        final PanacheSearchSpecification<Object> spec = aSpecWithEmptyCriteria();
        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        // When: Calling toPredicate
        final Predicate result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

        // Then: A predicate is returned (from base class logic for empty criteria)
        assertThat(result, is(notNullValue()));
    }

    @Test
    @DisplayName("Should delegate toPredicate to base class logic")
    public void shouldDelegateToPredicateToBaseClassLogic() {
        // Given: A spec with empty criteria
        final PanacheSearchSpecification<Object> spec = aSpecWithEmptyCriteria();
        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        // When: Calling toPredicate via the SearchSpecification reference
        final SearchSpecification<Object> searchSpec = spec;
        final Predicate result = searchSpec.toPredicate(root, criteriaQuery, criteriaBuilder);

        // Then: The predicate is produced by base class delegation
        assertThat(result, is(notNullValue()));
    }

    @Test
    @DisplayName("Should return predicate when toPredicate is called with null criteria")
    public void shouldReturnPredicateWhenToPredicateIsCalledWithNullCriteria() {
        // Given: A spec constructed with null criteria
        final PanacheSearchSpecification<Object> spec = aSpecWithNullCriteria();
        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        // When: Calling toPredicate
        final Predicate result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

        // Then: A predicate is returned without throwing
        assertThat(result, is(notNullValue()));
    }
}
