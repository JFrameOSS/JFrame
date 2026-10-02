package io.github.jframe.datasource.search.fields;

import io.github.support.UnitTest;

import java.time.LocalDateTime;

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

@DisplayName("Unit Test - DateField")
class DateFieldTest extends UnitTest {

    @Test
    @DisplayName("Should parse ISO date-time values")
    void shouldParseIsoDateTimeValues() {
        // Given: ISO date-time bounds
        final String from = "2026-01-01T00:00:00";
        final String to = "2026-12-31T23:59:59";

        // When: Creating the field
        final DateField field = new DateField("createdAt", from, to);

        // Then: Both bounds parsed
        assertThat(field.getFromDate(), is(equalTo(LocalDateTime.of(2026, 1, 1, 0, 0, 0))));
        assertThat(field.getToDate(), is(equalTo(LocalDateTime.of(2026, 12, 31, 23, 59, 59))));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "yesterday",
            "2026-01-01",
            "01-01-2026T00:00:00"
        }
    )
    @DisplayName("Should throw IllegalArgumentException when fromDate is not ISO date-time")
    void shouldThrowWhenFromDateIsNotIsoDateTime(final String value) {
        // Given: A non-ISO date-time

        // When / Then: Construction fails fast
        final InvalidFieldValueException exception = assertThrows(
            InvalidFieldValueException.class,
            () -> new DateField("createdAt", value, null)
        );
        assertThat(exception.getRejectedValue(), is(equalTo(value)));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when toDate is not ISO date-time")
    void shouldThrowWhenToDateIsNotIsoDateTime() {
        // Given: A valid from and invalid to
        final String from = "2026-01-01T00:00:00";

        // When / Then: Construction fails fast
        final InvalidFieldValueException exception = assertThrows(
            InvalidFieldValueException.class,
            () -> new DateField("createdAt", from, "not-a-date")
        );
        assertThat(exception.getRejectedValue(), is(equalTo("not-a-date")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
            "  "
        }
    )
    @DisplayName("Should not throw when bounds are null or blank")
    void shouldNotThrowWhenBoundsAreNullOrBlank(final String value) {
        // Given: Absent bounds

        // When: Creating the field
        final DateField field = new DateField("createdAt", value, value);

        // Then: No filter bounds
        assertThat(field.getFromDate(), is(nullValue()));
        assertThat(field.getToDate(), is(nullValue()));
    }
}
