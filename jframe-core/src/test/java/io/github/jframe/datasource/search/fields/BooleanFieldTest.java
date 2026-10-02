package io.github.jframe.datasource.search.fields;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - BooleanField")
class BooleanFieldTest extends UnitTest {

    @ParameterizedTest
    @CsvSource(
        {
            "true,true",
            "TRUE,true",
            "True,true",
            "false,false",
            "FALSE,false"
        }
    )
    @DisplayName("Should parse true/false case-insensitively")
    void shouldParseTrueFalseCaseInsensitively(final String value, final boolean expected) {
        // Given: A boolean literal in any case

        // When: Creating the field
        final BooleanField field = new BooleanField("active", value);

        // Then: Parsed accordingly
        assertThat(field.isValue(), is(equalTo(expected)));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "yes",
            "1",
            "0",
            "on",
            "truee"
        }
    )
    @DisplayName("Should throw IllegalArgumentException when value is not true or false")
    void shouldThrowWhenValueIsNotTrueOrFalse(final String value) {
        // Given: A non-boolean literal

        // When / Then: Construction fails fast
        final InvalidFieldValueException exception = assertThrows(
            InvalidFieldValueException.class,
            () -> new BooleanField("active", value)
        );
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
        final BooleanField field = new BooleanField("active", value);

        // Then: Defaults to false
        assertThat(field.isValue(), is(false));
    }
}
