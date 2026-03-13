package io.github.jframe.datasource.search;

import io.github.support.UnitTest;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SearchSpecification}.
 *
 * <p>Verifies the SearchSpecification interface functionality including:
 * <ul>
 * <li>toPredicate() method signature matches Jakarta Persistence API</li>
 * <li>and() default method composes two specifications with AND logic</li>
 * <li>or() default method composes two specifications with OR logic</li>
 * <li>Null predicate handling in composition</li>
 * </ul>
 */
@DisplayName("Search - SearchSpecification")
public class SearchSpecificationTest extends UnitTest {

    @Mock
    private Root<Object> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Predicate predicateA;

    @Mock
    private Predicate predicateB;

    @Mock
    private Predicate combinedPredicate;

    // ---- toPredicate() method signature ---------------------------------

    @Test
    @DisplayName("Should call toPredicate with Root, CriteriaQuery, CriteriaBuilder")
    public void shouldCallToPredicateWithJpaArguments() {
        // Given: A concrete SearchSpecification returning a fixed predicate
        final SearchSpecification<Object> spec = (root1, query1, cb1) -> predicateA;

        // When: Calling toPredicate
        final Predicate result = spec.toPredicate(root, query, cb);

        // Then: Returns the predicate from the implementation
        assertThat(result, is(notNullValue()));
        assertThat(result, is(predicateA));
    }

    @Test
    @DisplayName("Should allow lambda implementation of toPredicate")
    public void shouldAllowLambdaImplementation() {
        // Given: A lambda implementing SearchSpecification
        final SearchSpecification<Object> spec = (r, q, c) -> predicateA;

        // When: Calling toPredicate
        final Predicate result = spec.toPredicate(root, query, cb);

        // Then: Result is the expected predicate
        assertThat(result, is(predicateA));
    }

    // ---- and() composition -----------------------------------------------

    @Test
    @DisplayName("Should compose two specifications with and()")
    public void shouldComposeTwoSpecificationsWithAnd() {
        // Given: Two specifications and an AND combination predicate
        final SearchSpecification<Object> specA = (r, q, c) -> predicateA;
        final SearchSpecification<Object> specB = (r, q, c) -> predicateB;
        when(cb.and(predicateA, predicateB)).thenReturn(combinedPredicate);

        // When: Composing with and()
        final SearchSpecification<Object> combined = specA.and(specB);
        final Predicate result = combined.toPredicate(root, query, cb);

        // Then: Combined predicate is returned
        assertThat(result, is(combinedPredicate));
    }

    @Test
    @DisplayName("Should return non-null composed specification from and()")
    public void shouldReturnNonNullComposedSpecFromAnd() {
        // Given: Two specifications
        final SearchSpecification<Object> specA = (r, q, c) -> predicateA;
        final SearchSpecification<Object> specB = (r, q, c) -> predicateB;

        // When: Composing with and()
        final SearchSpecification<Object> combined = specA.and(specB);

        // Then: Combined specification is non-null
        assertThat(combined, is(notNullValue()));
    }

    // ---- or() composition ------------------------------------------------

    @Test
    @DisplayName("Should compose two specifications with or()")
    public void shouldComposeTwoSpecificationsWithOr() {
        // Given: Two specifications and an OR combination predicate
        final SearchSpecification<Object> specA = (r, q, c) -> predicateA;
        final SearchSpecification<Object> specB = (r, q, c) -> predicateB;
        when(cb.or(predicateA, predicateB)).thenReturn(combinedPredicate);

        // When: Composing with or()
        final SearchSpecification<Object> combined = specA.or(specB);
        final Predicate result = combined.toPredicate(root, query, cb);

        // Then: Combined predicate is returned
        assertThat(result, is(combinedPredicate));
    }

    @Test
    @DisplayName("Should return non-null composed specification from or()")
    public void shouldReturnNonNullComposedSpecFromOr() {
        // Given: Two specifications
        final SearchSpecification<Object> specA = (r, q, c) -> predicateA;
        final SearchSpecification<Object> specB = (r, q, c) -> predicateB;

        // When: Composing with or()
        final SearchSpecification<Object> combined = specA.or(specB);

        // Then: Combined specification is non-null
        assertThat(combined, is(notNullValue()));
    }

    // ---- Null predicate handling ----------------------------------------

    @Test
    @DisplayName("Should handle null predicate from first spec in and() composition")
    public void shouldHandleNullPredicateFromFirstSpecInAndComposition() {
        // Given: First spec returns null, second returns a predicate
        final SearchSpecification<Object> specA = (r, q, c) -> null;
        final SearchSpecification<Object> specB = (r, q, c) -> predicateB;
        when(cb.and(null, predicateB)).thenReturn(predicateB);

        // When: Composing with and() where first predicate is null
        final SearchSpecification<Object> combined = specA.and(specB);

        // Then: Does not throw, delegates to CriteriaBuilder.and()
        final Predicate result = combined.toPredicate(root, query, cb);
        assertThat(result, is(notNullValue()));
    }

    @Test
    @DisplayName("Should handle null predicate from first spec in or() composition")
    public void shouldHandleNullPredicateFromFirstSpecInOrComposition() {
        // Given: First spec returns null, second returns a predicate
        final SearchSpecification<Object> specA = (r, q, c) -> null;
        final SearchSpecification<Object> specB = (r, q, c) -> predicateB;
        when(cb.or(null, predicateB)).thenReturn(predicateB);

        // When: Composing with or() where first predicate is null
        final SearchSpecification<Object> combined = specA.or(specB);

        // Then: Does not throw, delegates to CriteriaBuilder.or()
        final Predicate result = combined.toPredicate(root, query, cb);
        assertThat(result, is(notNullValue()));
    }
}
