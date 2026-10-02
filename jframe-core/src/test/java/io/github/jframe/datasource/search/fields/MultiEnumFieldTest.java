package io.github.jframe.datasource.search.fields;

import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - MultiEnumField")
class MultiEnumFieldTest extends UnitTest {

    @Test
    @DisplayName("Should resolve all enum constants when every value matches")
    void shouldResolveAllEnumConstantsWhenEveryValueMatches() {
        // Given: Valid constant names
        final List<String> values = List.of("ACTIVE", "INACTIVE");

        // When: Creating the field
        final MultiEnumField field = new MultiEnumField("status", Status.class, values);

        // Then: All constants resolved
        assertThat(field.getEnums(), contains(Status.ACTIVE, Status.INACTIVE));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when any value matches no constant")
    void shouldThrowWhenAnyValueMatchesNoConstant() {
        // Given: One valid and one invalid value
        final List<String> values = List.of("ACTIVE", "BOGUS");

        // When / Then: Construction fails fast
        final InvalidFieldValueException exception = assertThrows(
            InvalidFieldValueException.class,
            () -> new MultiEnumField("status", Status.class, values)
        );
        assertThat(exception.getRejectedValue(), is(equalTo("BOGUS")));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when value differs only in case")
    void shouldThrowWhenValueDiffersOnlyInCase() {
        // Given: A lower-case constant name
        final List<String> values = List.of("active");

        // When / Then: Exact match is required
        assertThrows(IllegalArgumentException.class, () -> new MultiEnumField("status", Status.class, values));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should not throw when values are null or empty")
    void shouldNotThrowWhenValuesAreNullOrEmpty(final List<String> values) {
        // Given: Absent values

        // When: Creating the field
        final MultiEnumField field = new MultiEnumField("status", Status.class, values);

        // Then: No filter values
        assertThat(field.getValues(), is(empty()));
    }

    private enum Status {
        ACTIVE,
        INACTIVE
    }
}
