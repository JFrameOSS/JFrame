package io.github.jframe.logging.filter;

import io.github.support.UnitTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link FilterPriorities}.
 *
 * <p>Verifies:
 * <ul>
 * <li>Class is a final utility class with private constructor</li>
 * <li>All priority constants have the expected values</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Filters - Filter Priorities")
public class FilterPrioritiesTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Utility class structure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be a final utility class")
    public void shouldBeAFinalUtilityClass() {
        // Given: The FilterPriorities class

        // When: Checking the class modifiers
        final int modifiers = FilterPriorities.class.getModifiers();

        // Then: The class should be declared final
        assertThat(Modifier.isFinal(modifiers), is(true));
    }

    @Test
    @DisplayName("Should have private constructor preventing instantiation via reflection")
    public void shouldHavePrivateConstructorPreventingInstantiation() throws Exception {
        // Given: The single declared constructor of the utility class
        final Constructor<FilterPriorities> constructor =
            FilterPriorities.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then: Instantiation should throw an exception
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    // -------------------------------------------------------------------------
    // Priority constants
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have correct TRANSACTION_ID priority value")
    public void shouldHaveCorrectTransactionIdPriority() {
        // Given: The FilterPriorities constants

        // When: Reading the TRANSACTION_ID constant
        final int priority = FilterPriorities.TRANSACTION_ID;

        // Then: Value is 100
        assertThat(priority, is(100));
    }

    @Test
    @DisplayName("Should have correct REQUEST_ID priority value")
    public void shouldHaveCorrectRequestIdPriority() {
        // Given: The FilterPriorities constants

        // When: Reading the REQUEST_ID constant
        final int priority = FilterPriorities.REQUEST_ID;

        // Then: Value is 200
        assertThat(priority, is(200));
    }

    @Test
    @DisplayName("Should have correct REQUEST_DURATION priority value")
    public void shouldHaveCorrectRequestDurationPriority() {
        // Given: The FilterPriorities constants

        // When: Reading the REQUEST_DURATION constant
        final int priority = FilterPriorities.REQUEST_DURATION;

        // Then: Value is 300
        assertThat(priority, is(300));
    }

    @Test
    @DisplayName("Should have correct REQUEST_RESPONSE_LOG priority value")
    public void shouldHaveCorrectRequestResponseLogPriority() {
        // Given: The FilterPriorities constants

        // When: Reading the REQUEST_RESPONSE_LOG constant
        final int priority = FilterPriorities.REQUEST_RESPONSE_LOG;

        // Then: Value is 400
        assertThat(priority, is(400));
    }

    @Test
    @DisplayName("Should have correct TRACING_RESPONSE priority value")
    public void shouldHaveCorrectTracingResponsePriority() {
        // Given: The FilterPriorities constants

        // When: Reading the TRACING_RESPONSE constant
        final int priority = FilterPriorities.TRACING_RESPONSE;

        // Then: Value is 50 (runs before all others)
        assertThat(priority, is(50));
    }

    @Test
    @DisplayName("Should have TRACING_RESPONSE lower than TRANSACTION_ID")
    public void shouldHaveTracingResponseLowerThanTransactionId() {
        // Given: FilterPriorities constants for TRACING_RESPONSE and TRANSACTION_ID

        // When: Comparing the two priorities

        // Then: TRACING_RESPONSE runs first (lower number = higher priority)
        assertThat(FilterPriorities.TRACING_RESPONSE < FilterPriorities.TRANSACTION_ID, is(true));
    }
}
