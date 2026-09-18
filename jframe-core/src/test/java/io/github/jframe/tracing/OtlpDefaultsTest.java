package io.github.jframe.tracing;

import io.github.support.UnitTest;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link OtlpDefaults}.
 *
 * <p>Verifies the parseCommaSeparated utility method behaviour and all shared default constants.
 */
@DisplayName("Tracing - OtlpDefaults")
class OtlpDefaultsTest extends UnitTest {

    @Nested
    @DisplayName("Default constants")
    class DefaultConstants {

        @Test
        @DisplayName("Should default disabled flag to true — telemetry requires explicit opt-in")
        void shouldDefaultDisabledToTrueWhenNoConfiguration() {
            // Given: the shared default constant

            // When: reading DEFAULT_DISABLED

            // Then: telemetry is disabled unless explicitly opted in
            assertThat(OtlpDefaults.DEFAULT_DISABLED, is(true));
        }

        @Test
        @DisplayName("Should provide sampler type default of parentbased_traceidratio")
        void shouldDefaultSamplerTypeToParentBasedTraceIdRatio() {
            // Given: the shared sampler-type default constant

            // When: reading DEFAULT_SAMPLER_TYPE

            // Then: the value is parentbased_traceidratio so sampled parents keep their children
            assertThat(OtlpDefaults.DEFAULT_SAMPLER_TYPE, is("parentbased_traceidratio"));
        }

        @Test
        @DisplayName("Should provide excluded resource attributes default as comma-separated string")
        void shouldDefaultExcludedResourceAttributesToOptInProcessKeys() {
            // Given: the shared excluded resource attributes constant

            // When: parsing the constant
            final Set<String> attributes = OtlpDefaults.parseCommaSeparated(
                OtlpDefaults.DEFAULT_EXCLUDED_RESOURCE_ATTRIBUTES
            );

            // Then: exactly the three OTel spec Opt-In process keys are excluded by default
            assertThat(attributes, hasSize(3));
            assertThat(
                attributes,
                containsInAnyOrder(
                    "process.command_args",
                    "process.command_line",
                    "process.executable.path"
                )
            );
        }

        @Test
        @DisplayName("Should not be instantiable — private constructor throws")
        void shouldThrowWhenInstantiatedViaReflection() throws Exception {
            // Given: the private constructor
            final var constructor = OtlpDefaults.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // When/Then: invoking it wraps the UnsupportedOperationException
            assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        }
    }


    @Nested
    @DisplayName("parseCommaSeparated")
    class ParseCommaSeparated {

        @Test
        @DisplayName("Should parse normal CSV into set of trimmed values")
        void shouldParseNormalCsvWhenValidInput() {
            // Given: a standard CSV string
            final String csv = "health,actuator,ping";

            // When: parsing
            final Set<String> result = OtlpDefaults.parseCommaSeparated(csv);

            // Then: all values are present
            assertThat(result, hasSize(3));
            assertThat(result, containsInAnyOrder("health", "actuator", "ping"));
        }

        @Test
        @DisplayName("Should return set with single value when only one element")
        void shouldReturnSingleElementSetWhenOnlyOneValue() {
            // Given: a single-value CSV
            final String csv = "health";

            // When: parsing
            final Set<String> result = OtlpDefaults.parseCommaSeparated(csv);

            // Then: set contains exactly one element
            assertThat(result, hasSize(1));
            assertThat(result, containsInAnyOrder("health"));
        }

        @Test
        @DisplayName("Should trim whitespace around each value")
        void shouldTrimWhitespaceWhenValuesHaveSpaces() {
            // Given: CSV with surrounding spaces
            final String csv = " health , actuator , ping ";

            // When: parsing
            final Set<String> result = OtlpDefaults.parseCommaSeparated(csv);

            // Then: values are trimmed
            assertThat(result, hasSize(3));
            assertThat(result, containsInAnyOrder("health", "actuator", "ping"));
        }

        @Test
        @DisplayName("Should filter out empty strings produced by trailing commas")
        void shouldFilterEmptyStringsWhenTrailingCommaPresent() {
            // Given: CSV with trailing comma producing empty token
            final String csv = "health,actuator,";

            // When: parsing
            final Set<String> result = OtlpDefaults.parseCommaSeparated(csv);

            // Then: empty token is excluded
            assertThat(result, hasSize(2));
            assertThat(result, containsInAnyOrder("health", "actuator"));
        }

        @Test
        @DisplayName("Should return empty set when input is empty string")
        void shouldReturnEmptySetWhenInputIsEmpty() {
            // Given: an empty string
            final String csv = "";

            // When: parsing
            final Set<String> result = OtlpDefaults.parseCommaSeparated(csv);

            // Then: result is empty
            assertThat(result, is(empty()));
        }

        @Test
        @DisplayName("Should return unmodifiable set")
        void shouldReturnUnmodifiableSetWhenParsed() {
            // Given: a parsed set
            final Set<String> result = OtlpDefaults.parseCommaSeparated("health");

            // When/Then: mutating throws
            assertThrows(UnsupportedOperationException.class, () -> result.add("extra"));
        }
    }
}
