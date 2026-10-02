package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.InvalidFieldValueException;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.support.TestStatus;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.github.jframe.exception.search.InvalidSearchException.MAX_REJECTED_VALUE_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - AbstractSortSearchMetaData Strict Search")
class AbstractSortSearchMetaDataStrictSearchTest extends UnitTest {

    private final TestMetaData metaData = new TestMetaData();

    @Test
    @DisplayName("Should throw InvalidSearchException with null value when search field is unknown")
    void shouldThrowWhenSearchFieldIsUnknown() {
        // Given: A search on an unregistered field
        final SearchInput input = aTextSearch("unknown", "x");

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Field rejected, value null, searchable fields listed
        assertThat(exception.getRejectedField(), is(equalTo("unknown")));
        assertThat(exception.getRejectedValue(), is(nullValue()));
        assertThat(exception.getSearchableFields(), containsInAnyOrder("name", "age", "status", "active", "ids", "roles", "createdAt"));
    }

    @Test
    @DisplayName("Should throw InvalidSearchException when numeric value is not an integer")
    void shouldThrowWhenNumericValueIsInvalid() {
        // Given: A non-integer numeric search
        final SearchInput input = aTextSearch("age", "abc");

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Field and raw value rejected
        assertThat(exception.getRejectedField(), is(equalTo("age")));
        assertThat(exception.getRejectedValue(), is(equalTo("abc")));
        assertThat(exception.getSearchableFields(), containsInAnyOrder("name", "age", "status", "active", "ids", "roles", "createdAt"));
    }

    @Test
    @DisplayName("Should throw InvalidSearchException when enum value matches no constant")
    void shouldThrowWhenEnumValueIsInvalid() {
        // Given: An unknown enum constant
        final SearchInput input = aTextSearch("status", "BOGUS");

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Field and raw value rejected
        assertThat(exception.getRejectedField(), is(equalTo("status")));
        assertThat(exception.getRejectedValue(), is(equalTo("BOGUS")));
    }

    @Test
    @DisplayName("Should throw InvalidSearchException when boolean value is not true or false")
    void shouldThrowWhenBooleanValueIsInvalid() {
        // Given: A non-boolean literal
        final SearchInput input = aTextSearch("active", "yes");

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Field and raw value rejected
        assertThat(exception.getRejectedField(), is(equalTo("active")));
        assertThat(exception.getRejectedValue(), is(equalTo("yes")));
    }

    @Test
    @DisplayName("Should throw when an invalid input follows a valid one")
    void shouldThrowWhenInvalidInputFollowsValidOne() {
        // Given: One valid and one unknown search
        final List<SearchInput> inputs = List.of(aTextSearch("name", "Alice"), aTextSearch("unknown", "x"));

        // When / Then: The whole request is rejected
        assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(inputs));
    }

    @Test
    @DisplayName("Should build criteria for valid searches")
    void shouldBuildCriteriaForValidSearches() {
        // Given: Valid searches on all registered fields
        final List<SearchInput> inputs = List.of(
            aTextSearch("name", "Alice"),
            aTextSearch("age", "!30"),
            aTextSearch("status", "ACTIVE"),
            aTextSearch("active", "TRUE")
        );

        // When: Converting to criteria
        final List<SearchCriterium> criteria = metaData.toSearchCriteria(inputs);

        // Then: One criterium per input
        assertThat(criteria, hasSize(4));
    }

    @Test
    @DisplayName("Should not throw when a registered field has a blank value")
    void shouldNotThrowWhenRegisteredFieldHasBlankValue() {
        // Given: A numeric search without a value
        final SearchInput input = aTextSearch("age", "");

        // When: Converting to criteria
        final List<SearchCriterium> criteria = metaData.toSearchCriteria(List.of(input));

        // Then: No exception, criterium still created
        assertThat(criteria, hasSize(1));
    }

    @Test
    @DisplayName("Should report only the offending entry when a multi-numeric value is invalid")
    void shouldReportOffendingEntryWhenMultiNumericValueIsInvalid() {
        // Given: A list with one non-integer entry
        final SearchInput input = aListSearch("ids", List.of("1", "x", "3"));

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Only the bad entry is reported
        assertThat(exception.getRejectedField(), is(equalTo("ids")));
        assertThat(exception.getRejectedValue(), is(equalTo("x")));
    }

    @Test
    @DisplayName("Should report only the offending entry when a multi-enum value is invalid")
    void shouldReportOffendingEntryWhenMultiEnumValueIsInvalid() {
        // Given: A list with one unknown constant
        final SearchInput input = aListSearch("roles", List.of("ACTIVE", "BOGUS"));

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Only the unknown constant is reported
        assertThat(exception.getRejectedField(), is(equalTo("roles")));
        assertThat(exception.getRejectedValue(), is(equalTo("BOGUS")));
    }

    @Test
    @DisplayName("Should report only the offending bound when a date bound is invalid")
    void shouldReportOffendingBoundWhenDateBoundIsInvalid() {
        // Given: A valid from and an invalid to date
        final SearchInput input = new SearchInput();
        input.setFieldName("createdAt");
        input.setFromDateValue("2024-01-01T00:00:00");
        input.setToDateValue("tomorrow");

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Only the bad bound is reported
        assertThat(exception.getRejectedField(), is(equalTo("createdAt")));
        assertThat(exception.getRejectedValue(), is(equalTo("tomorrow")));
    }


    @Test
    @DisplayName("Should preserve cause with a bounded message when a huge numeric value is invalid")
    void shouldPreserveBoundedCauseWhenHugeNumericValueIsInvalid() {
        // Given: A 500-char non-numeric value
        final String value = "x".repeat(500);
        final SearchInput input = aTextSearch("age", value);

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> metaData.toSearchCriteria(List.of(input)));

        // Then: Cause is kept but its message never echoes the untruncated input
        assertThat(exception.getCause(), is(instanceOf(InvalidFieldValueException.class)));
        assertThat(exception.getCause().getMessage(), not(containsString(value)));
        assertThat(exception.getCause().getMessage().length(), is(lessThanOrEqualTo(MAX_REJECTED_VALUE_LENGTH + 50)));
        assertThat(exception.getRejectedValue().length(), is(MAX_REJECTED_VALUE_LENGTH));
    }

    private static SearchInput aListSearch(final String field, final List<String> values) {
        final SearchInput input = new SearchInput();
        input.setFieldName(field);
        input.setTextValueList(values);
        return input;
    }

    private static SearchInput aTextSearch(final String field, final String value) {
        final SearchInput input = new SearchInput();
        input.setFieldName(field);
        input.setTextValue(value);
        return input;
    }

    static class TestMetaData extends AbstractSortSearchMetaData {

        TestMetaData() {
            super();
            addField("name", "name", SearchType.TEXT, true);
            addField("age", "age", SearchType.NUMERIC, true);
            addField("status", "status", SearchType.ENUM, TestStatus.class, false);
            addField("active", "active", SearchType.BOOLEAN, false);
            addField("ids", "id", SearchType.MULTI_NUMERIC, false);
            addField("roles", "role", SearchType.MULTI_ENUM, TestStatus.class, false);
            addField("createdAt", "created_at", SearchType.DATE, false);
        }
    }
}
