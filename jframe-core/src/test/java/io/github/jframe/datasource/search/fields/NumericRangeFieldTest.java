package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Unit Test - NumericRangeField")
class NumericRangeFieldTest extends UnitTest {

    @Test
    @DisplayName("Should store fromValue and toValue when both are provided")
    void shouldStoreFromValueAndToValueWhenBothAreProvided() {
        // Given: Both from and to values

        // When: Creating a NumericRangeField
        final NumericRangeField field = new NumericRangeField("age", 18, 65);

        // Then: Both values are stored
        assertThat(field.getFromValue(), is(equalTo(18)));
        assertThat(field.getToValue(), is(equalTo(65)));
    }

    @Test
    @DisplayName("Should use NUMERIC_RANGE search type")
    void shouldUseNumericRangeSearchType() {
        // Given: A valid range

        // When: Creating a NumericRangeField
        final NumericRangeField field = new NumericRangeField("age", 0, 100);

        // Then: SearchType is NUMERIC_RANGE
        assertThat(field.getSearchType(), is(equalTo(SearchType.NUMERIC_RANGE)));
    }

    @Test
    @DisplayName("Should store column name")
    void shouldStoreColumnName() {
        // Given: A column name

        // When: Creating a NumericRangeField
        final NumericRangeField field = new NumericRangeField("myColumn", null, null);

        // Then: Column name is stored
        assertThat(field.getColumnName(), is(equalTo("myColumn")));
    }

    @Test
    @DisplayName("Should allow null fromValue")
    void shouldAllowNullFromValue() {
        // Given: Only a toValue is provided

        // When: Creating a NumericRangeField with null fromValue
        final NumericRangeField field = new NumericRangeField("age", null, 65);

        // Then: fromValue is null, toValue is stored
        assertThat(field.getFromValue(), is(nullValue()));
        assertThat(field.getToValue(), is(equalTo(65)));
    }

    @Test
    @DisplayName("Should allow null toValue")
    void shouldAllowNullToValue() {
        // Given: Only a fromValue is provided

        // When: Creating a NumericRangeField with null toValue
        final NumericRangeField field = new NumericRangeField("age", 18, null);

        // Then: fromValue is stored, toValue is null
        assertThat(field.getFromValue(), is(equalTo(18)));
        assertThat(field.getToValue(), is(nullValue()));
    }

    @Test
    @DisplayName("Should allow both fromValue and toValue to be null")
    void shouldAllowBothFromValueAndToValueToBeNull() {
        // Given: No range values provided

        // When: Creating a NumericRangeField with both null
        final NumericRangeField field = new NumericRangeField("age", null, null);

        // Then: Both values are null
        assertThat(field.getFromValue(), is(nullValue()));
        assertThat(field.getToValue(), is(nullValue()));
    }

    @Test
    @DisplayName("Should handle zero as a valid boundary value")
    void shouldHandleZeroAsAValidBoundaryValue() {
        // Given: Zero as boundary values

        // When: Creating a NumericRangeField with zero boundaries
        final NumericRangeField field = new NumericRangeField("score", 0, 0);

        // Then: Zero values are stored correctly
        assertThat(field.getFromValue(), is(notNullValue()));
        assertThat(field.getFromValue(), is(equalTo(0)));
        assertThat(field.getToValue(), is(notNullValue()));
        assertThat(field.getToValue(), is(equalTo(0)));
    }

    @Test
    @DisplayName("Should handle negative range values")
    void shouldHandleNegativeRangeValues() {
        // Given: Negative range values

        // When: Creating a NumericRangeField with negative boundaries
        final NumericRangeField field = new NumericRangeField("temperature", -50, -10);

        // Then: Negative values are stored correctly
        assertThat(field.getFromValue(), is(equalTo(-50)));
        assertThat(field.getToValue(), is(equalTo(-10)));
    }
}
