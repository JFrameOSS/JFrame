package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - NullCheckField")
class NullCheckFieldTest extends UnitTest {

    @ParameterizedTest
    @CsvSource(
        {
            "true,true",
            "TRUE,true",
            "True,true",
            "false,false",
            "FALSE,false",
            "False,false"
        }
    )
    @DisplayName("Should parse true/false case-insensitively")
    void shouldParseTrueFalseCaseInsensitively(final String value, final boolean expected) {
        // Given: A boolean literal in any case

        // When: Creating the field
        final NullCheckField field = new NullCheckField("deletedAt", value);

        // Then: Parsed accordingly
        assertThat(field.isValue(), is(equalTo(expected)));
    }

    @Test
    @DisplayName("Should expose NULL_CHECK search type and column name")
    void shouldExposeNullCheckSearchTypeAndColumnName() {
        // Given: A value

        // When: Creating the field
        final NullCheckField field = new NullCheckField("deletedAt", "true");

        // Then: Search type and column are set
        assertThat(field.getSearchType(), is(SearchType.NULL_CHECK));
        assertThat(field.getColumnName(), is(equalTo("deletedAt")));
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
    @DisplayName("Should throw InvalidFieldValueException when value is not true or false")
    void shouldThrowWhenValueIsNotTrueOrFalse(final String value) {
        // Given: A non-boolean literal

        // When / Then: Construction fails fast
        final InvalidFieldValueException exception = assertThrows(
            InvalidFieldValueException.class,
            () -> new NullCheckField("deletedAt", value)
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
    @DisplayName("Should default to false when value is null or blank")
    void shouldDefaultToFalseWhenValueIsNullOrBlank(final String value) {
        // Given: An absent value

        // When: Creating the field
        final NullCheckField field = new NullCheckField("deletedAt", value);

        // Then: Defaults to false, same as BooleanField
        assertThat(field.isValue(), is(false));
    }
}
