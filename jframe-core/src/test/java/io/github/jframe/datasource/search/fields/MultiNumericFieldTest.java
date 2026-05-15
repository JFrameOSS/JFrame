package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Unit Test - MultiNumericField")
class MultiNumericFieldTest extends UnitTest {

    @Test
    @DisplayName("Should store parsed integer values from valid string list")
    void shouldStoreParsedIntegerValuesFromValidStringList() {
        // Given: A list of valid numeric strings
        final List<String> input = List.of("1", "2", "3");

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("age", input);

        // Then: Values are parsed to integers
        assertThat(field.getValues(), is(notNullValue()));
        assertThat(field.getValues(), hasSize(3));
        assertThat(field.getValues(), contains(1, 2, 3));
    }

    @Test
    @DisplayName("Should use MULTI_NUMERIC search type")
    void shouldUseMultiNumericSearchType() {
        // Given: A valid input list

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("age", List.of("10"));

        // Then: SearchType is MULTI_NUMERIC
        assertThat(field.getSearchType(), is(equalTo(SearchType.MULTI_NUMERIC)));
    }

    @Test
    @DisplayName("Should store column name")
    void shouldStoreColumnName() {
        // Given: A column name

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("myColumn", List.of("5"));

        // Then: Column name is stored
        assertThat(field.getColumnName(), is(equalTo("myColumn")));
    }

    @Test
    @DisplayName("Should return empty list when input list is null")
    void shouldReturnEmptyListWhenInputListIsNull() {
        // Given: A null input list

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("age", null);

        // Then: Values list is empty
        assertThat(field.getValues(), is(notNullValue()));
        assertThat(field.getValues(), is(empty()));
    }

    @Test
    @DisplayName("Should return empty list when input list is empty")
    void shouldReturnEmptyListWhenInputListIsEmpty() {
        // Given: An empty input list

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("age", List.of());

        // Then: Values list is empty
        assertThat(field.getValues(), is(notNullValue()));
        assertThat(field.getValues(), is(empty()));
    }

    @Test
    @DisplayName("Should ignore unparseable strings and keep only valid integers")
    void shouldIgnoreUnparseableStringsAndKeepOnlyValidIntegers() {
        // Given: A mixed list of valid and invalid numeric strings
        final List<String> input = List.of("1", "abc", "3", "not-a-number", "5");

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("age", input);

        // Then: Only valid integers are stored
        assertThat(field.getValues(), hasSize(3));
        assertThat(field.getValues(), contains(1, 3, 5));
    }

    @Test
    @DisplayName("Should return empty list when all strings are unparseable")
    void shouldReturnEmptyListWhenAllStringsAreUnparseable() {
        // Given: A list of all invalid numeric strings
        final List<String> input = List.of("abc", "xyz", "not-a-number");

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("age", input);

        // Then: Values list is empty
        assertThat(field.getValues(), is(empty()));
    }

    @Test
    @DisplayName("Should handle single valid value")
    void shouldHandleSingleValidValue() {
        // Given: A single valid numeric string
        final List<String> input = List.of("42");

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("count", input);

        // Then: Single integer is stored
        assertThat(field.getValues(), hasSize(1));
        assertThat(field.getValues(), contains(42));
    }

    @Test
    @DisplayName("Should handle negative integer values")
    void shouldHandleNegativeIntegerValues() {
        // Given: A list containing negative numbers
        final List<String> input = List.of("-1", "-100", "0");

        // When: Creating a MultiNumericField
        final MultiNumericField field = new MultiNumericField("score", input);

        // Then: Negative integers are parsed correctly
        assertThat(field.getValues(), hasSize(3));
        assertThat(field.getValues(), contains(-1, -100, 0));
    }
}
