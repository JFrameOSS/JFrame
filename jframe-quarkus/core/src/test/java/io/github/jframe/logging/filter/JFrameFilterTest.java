package io.github.jframe.logging.filter;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link JFrameFilter} — marker interface for JFrame JAX-RS filters.
 *
 * <p>Verifies via reflection that:
 * <ul>
 * <li>The type is an interface (not a class or annotation)</li>
 * <li>No methods are declared (pure marker contract)</li>
 * </ul>
 */
@DisplayName("Quarkus Core - JFrameFilter Marker Interface")
public class JFrameFilterTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Interface type contract
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be an interface")
    public void shouldBeAnInterface() {
        // Given: The JFrameFilter type

        // When: Checking if it is an interface
        final boolean isInterface = JFrameFilter.class.isInterface();

        // Then: It must be an interface
        assertThat(isInterface, is(true));
    }

    // -------------------------------------------------------------------------
    // Marker contract — no methods
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have no declared methods (marker interface)")
    public void shouldHaveNoDeclaredMethods() {
        // Given: The JFrameFilter interface

        // When: Counting declared methods
        final int methodCount = JFrameFilter.class.getDeclaredMethods().length;

        // Then: No methods — this is a pure marker interface
        assertThat(methodCount, is(0));
    }
}
