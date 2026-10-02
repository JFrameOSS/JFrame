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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - EnumField")
class EnumFieldTest extends UnitTest {

    @Test
    @DisplayName("Should resolve enum constant when value matches exactly")
    void shouldResolveEnumConstantWhenValueMatches() {
        // Given: A valid inverse constant name
        final String value = "!ACTIVE";

        // When: Creating the field
        final EnumField field = new EnumField("status", Status.class, value);

        // Then: Constant resolved, inverse set
        assertThat(field.getEnum(), is(equalTo(Status.ACTIVE)));
        assertThat(field.isInverse(), is(true));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "BOGUS",
            "!BOGUS",
            "active",
            "Active"
        }
    )
    @DisplayName("Should throw IllegalArgumentException when value matches no constant exactly")
    void shouldThrowWhenValueMatchesNoConstant(final String value) {
        // Given: A value that is not an exact constant name

        // When / Then: Construction fails fast
        final InvalidFieldValueException exception = assertThrows(
            InvalidFieldValueException.class,
            () -> new EnumField("status", Status.class, value)
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
        final EnumField field = new EnumField("status", Status.class, value);

        // Then: No filter value
        assertThat(field.getValue(), is(nullValue()));
    }

    private enum Status {
        ACTIVE,
        INACTIVE
    }
}
