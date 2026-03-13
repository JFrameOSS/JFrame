package io.github.jframe.datasource.search;

import io.github.support.UnitTest;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.jpa.domain.Specification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SpringDataSearchSpecification}.
 *
 * <p>Verifies that the Spring Data adapter correctly wraps jframe-core's {@link SearchSpecification},
 * delegates predicate construction, and upholds the Spring Data {@link Specification} contract.
 */
@DisplayName("Spring JPA - SpringDataSearchSpecification")
public class SpringDataSearchSpecificationTest extends UnitTest {

    @Mock
    private Root<Object> root;

    @Mock
    private CriteriaQuery<?> criteriaQuery;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate predicate;

    // -------------------------------------------------------------------------
    // Factory / Constructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create instance via constructor with valid delegate")
    public void shouldCreateInstanceViaConstructorWithValidDelegate() {
        // Given: A valid SearchSpecification delegate
        final SearchSpecification<Object> delegate = (r, q, cb) -> predicate;

        // When: Creating a SpringDataSearchSpecification with the delegate
        final SpringDataSearchSpecification<Object> spec = new SpringDataSearchSpecification<>(delegate);

        // Then: Instance should be created successfully
        assertThat(spec, is(notNullValue()));
    }

    @Test
    @DisplayName("Should create instance via static factory method")
    public void shouldCreateInstanceViaStaticFactoryMethod() {
        // Given: A valid SearchSpecification delegate
        final SearchSpecification<Object> delegate = (r, q, cb) -> predicate;

        // When: Creating via static factory method
        final SpringDataSearchSpecification<Object> spec = SpringDataSearchSpecification.of(delegate);

        // Then: Instance should be created successfully
        assertThat(spec, is(notNullValue()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when delegate is null in constructor")
    public void shouldThrowNullPointerExceptionWhenDelegateIsNullInConstructor() {
        // Given: A null delegate

        // When & Then: Constructor should throw NullPointerException
        assertThrows(
            NullPointerException.class,
            () -> new SpringDataSearchSpecification<>(null)
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException when delegate is null in factory method")
    public void shouldThrowNullPointerExceptionWhenDelegateIsNullInFactoryMethod() {
        // Given: A null delegate

        // When & Then: Factory method should throw NullPointerException
        assertThrows(
            NullPointerException.class,
            () -> SpringDataSearchSpecification.of(null)
        );
    }

    // -------------------------------------------------------------------------
    // Interface contract
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should implement Spring Data Specification interface")
    public void shouldImplementSpringDataSpecificationInterface() {
        // Given: A valid delegate
        final SearchSpecification<Object> delegate = (r, q, cb) -> predicate;

        // When: Creating the adapter
        final SpringDataSearchSpecification<Object> spec = SpringDataSearchSpecification.of(delegate);

        // Then: It must be an instance of Spring Data's Specification
        assertThat(spec, is(instanceOf(Specification.class)));
    }

    // -------------------------------------------------------------------------
    // Delegation behaviour
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should delegate toPredicate to underlying SearchSpecification")
    public void shouldDelegateToPredicateToUnderlyingSearchSpecification() {
        // Given: A delegate that returns a known predicate
        final SearchSpecification<Object> delegate = (r, q, cb) -> predicate;
        final SpringDataSearchSpecification<Object> spec = SpringDataSearchSpecification.of(delegate);

        // When: Calling toPredicate on the adapter
        final Predicate result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

        // Then: The predicate returned by the delegate should be returned
        assertThat(result, is(equalTo(predicate)));
    }

    @Test
    @DisplayName("Should pass Root, CriteriaQuery and CriteriaBuilder to delegate unchanged")
    public void shouldPassRootCriteriaQueryAndCriteriaBuilderToDelegateUnchanged() {
        // Given: A delegate that captures the exact arguments it receives
        @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            }
        ) final Root<?>[] capturedRoot = new Root[1];
        @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            }
        ) final CriteriaQuery<?>[] capturedQuery = new CriteriaQuery[1];
        final CriteriaBuilder[] capturedCb = new CriteriaBuilder[1];

        final SearchSpecification<Object> delegate = (r, q, cb) -> {
            capturedRoot[0] = r;
            capturedQuery[0] = q;
            capturedCb[0] = cb;
            return predicate;
        };
        final SpringDataSearchSpecification<Object> spec = SpringDataSearchSpecification.of(delegate);

        // When: Calling toPredicate
        spec.toPredicate(root, criteriaQuery, criteriaBuilder);

        // Then: Exact same references should have been forwarded to the delegate
        assertThat(capturedRoot[0], is(equalTo(root)));
        assertThat(capturedQuery[0], is(equalTo(criteriaQuery)));
        assertThat(capturedCb[0], is(equalTo(criteriaBuilder)));
    }

    @Test
    @DisplayName("Should return null predicate when delegate returns null")
    public void shouldReturnNullPredicateWhenDelegateReturnsNull() {
        // Given: A delegate that returns null
        final SearchSpecification<Object> delegate = (r, q, cb) -> null;
        final SpringDataSearchSpecification<Object> spec = SpringDataSearchSpecification.of(delegate);

        // When: Calling toPredicate
        final Predicate result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

        // Then: Null should be propagated as-is
        assertThat(result, is(equalTo(null)));
    }

    @Test
    @DisplayName("Should delegate with lambda SearchSpecification correctly")
    public void shouldDelegateWithLambdaSearchSpecificationCorrectly() {
        // Given: A lambda-based SearchSpecification that uses the CriteriaBuilder
        final Predicate expectedPredicate = mock(Predicate.class);
        when(criteriaBuilder.conjunction()).thenReturn(expectedPredicate);

        final SearchSpecification<Object> delegate = (r, q, cb) -> cb.conjunction();
        final SpringDataSearchSpecification<Object> spec = SpringDataSearchSpecification.of(delegate);

        // When: Calling toPredicate
        final Predicate result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

        // Then: The predicate from the CriteriaBuilder should be returned
        assertThat(result, is(equalTo(expectedPredicate)));
    }
}
