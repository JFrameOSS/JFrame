package io.github.jframe.datasource.search.fields;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - NumericField")
class NumericFieldTest extends UnitTest {

    @Test
    @DisplayName("Should parse inverse numeric value when prefixed with '!'")
    void shouldParseInverseNumericValueWhenPrefixed() {
        // Given: An inverse numeric value
        final String value = "!42";

        // When: Creating the field
        final NumericField field = new NumericField("age", value);

        // Then: Value parsed and inverse set
        assertThat(field.getValue(), is(equalTo(42)));
        assertThat(field.isInverse(), is(true));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "abc",
            "!abc",
            "1.5",
            "!",
            "12a"
        }
    )
    @DisplayName("Should throw IllegalArgumentException when value is not an integer")
    void shouldThrowWhenValueIsNotAnInteger(final String value) {
        // Given: A non-integer value

        // When / Then: Construction fails fast
        final InvalidFieldValueException exception = assertThrows(InvalidFieldValueException.class, () -> new NumericField("age", value));
        assertThat(exception.getRejectedValue(), is(equalTo(value)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
            "  "
        }
    )
    @DisplayName("Should not throw when value is null or blank")
    void shouldNotThrowWhenValueIsNullOrBlank(final String value) {
        // Given: An absent value

        // When: Creating the field
        final NumericField field = new NumericField("age", value);

        // Then: No filter value, not inverse
        assertThat(field.getValue(), is(equalTo(0)));
        assertThat(field.isInverse(), is(false));
    }
}
