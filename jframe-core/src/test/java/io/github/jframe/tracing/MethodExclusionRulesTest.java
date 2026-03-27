package io.github.jframe.tracing;

import io.github.support.UnitTest;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link MethodExclusionRules}.
 *
 * <p>Verifies the isExcluded method for all exclusion categories.
 */
@DisplayName("Tracing - MethodExclusionRules")
class MethodExclusionRulesTest extends UnitTest {

    @Nested
    @DisplayName("isExcluded - excluded names")
    class ExcludedNames {

        @Test
        @DisplayName("Should exclude toString method")
        void shouldExcludeWhenMethodNameIsToString() {
            // Given: method name is toString, no config exclusions
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("toString", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Should exclude hashCode method")
        void shouldExcludeWhenMethodNameIsHashCode() {
            // Given: method name is hashCode
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("hashCode", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Should exclude equals method")
        void shouldExcludeWhenMethodNameIsEquals() {
            // Given: method name is equals
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("equals", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Should exclude clone method")
        void shouldExcludeWhenMethodNameIsClone() {
            // Given: method name is clone
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("clone", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }
    }


    @Nested
    @DisplayName("isExcluded - excluded prefixes")
    class ExcludedPrefixes {

        @Test
        @DisplayName("Should exclude method starting with get prefix")
        void shouldExcludeWhenMethodStartsWithGet() {
            // Given: method name starts with get
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("getName", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Should exclude method starting with set prefix")
        void shouldExcludeWhenMethodStartsWithSet() {
            // Given: method name starts with set
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("setFoo", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Should exclude method starting with is prefix")
        void shouldExcludeWhenMethodStartsWithIs() {
            // Given: method name starts with is
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("isActive", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }
    }


    @Nested
    @DisplayName("isExcluded - config excluded methods")
    class ConfigExcludedMethods {

        @Test
        @DisplayName("Should exclude method present in config exclusions (case-insensitive)")
        void shouldExcludeWhenMethodIsInConfigExclusions() {
            // Given: config exclusions contain health (lowercase)
            final Set<String> configExcluded = Set.of("health", "ping");

            // When: checking exclusion with uppercase variant
            final boolean result = MethodExclusionRules.isExcluded("HEALTH", configExcluded);

            // Then: method is excluded via case-insensitive match
            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Should exclude method matching config exclusion exact case")
        void shouldExcludeWhenMethodMatchesConfigExclusionExactCase() {
            // Given: config exclusions contain ping
            final Set<String> configExcluded = Set.of("ping");

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("ping", configExcluded);

            // Then: method is excluded
            assertThat(result, is(true));
        }
    }


    @Nested
    @DisplayName("isExcluded - non-excluded methods")
    class NonExcludedMethods {

        @Test
        @DisplayName("Should not exclude regular business method")
        void shouldNotExcludeWhenMethodIsRegularBusinessMethod() {
            // Given: a business method not in any exclusion list
            final Set<String> configExcluded = Set.of("health");

            // When: checking exclusion
            final boolean result = MethodExclusionRules.isExcluded("processOrder", configExcluded);

            // Then: method is not excluded
            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Should not exclude method with prefix-like name that is not a prefix match")
        void shouldNotExcludeWhenMethodDoesNotMatchAnyRule() {
            // Given: a method that looks similar to prefix but does not match
            final Set<String> configExcluded = Set.of();

            // When: checking exclusion for a method without get/set/is prefix
            final boolean result = MethodExclusionRules.isExcluded("fetchData", configExcluded);

            // Then: method is not excluded
            assertThat(result, is(false));
        }
    }
}
