package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.fields.InvalidFieldValueException;
import io.github.jframe.datasource.search.model.input.SearchInput;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.support.TestStatus;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.github.jframe.exception.search.InvalidSearchException.MAX_REJECTED_VALUE_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Strict search-value and paging contract (1.7.0) for {@link AbstractPanacheSearchMetaData}.
 */
@DisplayName("Quarkus JPA - AbstractPanacheSearchMetaData strict input")
public class AbstractPanacheSearchMetaDataStrictInputTest extends UnitTest {

    private static final int LARGE_PAGE_SIZE = 10_000;

    private final TestMetaData meta = new TestMetaData();

    // =========================================================================
    // toSearchCriteria — unknown field
    // =========================================================================

    @Test
    @DisplayName("Should reject unknown search field with null rejected value and searchable field list")
    public void shouldRejectUnknownSearchFieldWithNullRejectedValueAndSearchableFieldList() {
        // Given: A search input for a field that is not registered
        final SearchInput input = aSearchInput("unknown", "value");

        // When: Converting to search criteria
        final InvalidSearchException exception = assertThrows(
            InvalidSearchException.class,
            () -> meta.toSearchCriteria(List.of(input))
        );

        // Then: The field is rejected, value is null and all searchable fields are listed
        assertThat(exception.getRejectedField(), is("unknown"));
        assertThat(exception.getRejectedValue(), is(nullValue()));
        assertThat(
            exception.getSearchableFields(),
            containsInAnyOrder("age", "ids", "active", "status", "roles", "createdAt")
        );
    }

    // =========================================================================
    // toSearchCriteria — present but invalid values
    // =========================================================================

    @Test
    @DisplayName("Should reject non-integer NUMERIC value")
    public void shouldRejectNonIntegerNumericValue() {
        // Given: A NUMERIC field with a non-integer value
        final SearchInput input = aSearchInput("age", "12.5");

        // When / Then: The raw value is rejected
        assertRejected(input, "age", "12.5");
    }

    @Test
    @DisplayName("Should reject alphabetic NUMERIC value")
    public void shouldRejectAlphabeticNumericValue() {
        // Given: A NUMERIC field with letters
        final SearchInput input = aSearchInput("age", "abc");

        // When / Then: The raw value is rejected
        assertRejected(input, "age", "abc");
    }

    @Test
    @DisplayName("Should reject MULTI_NUMERIC list containing a non-integer entry")
    public void shouldRejectMultiNumericListContainingNonIntegerEntry() {
        // Given: A MULTI_NUMERIC field with one invalid entry among valid ones
        final SearchInput input = aMultiValueSearchInput("ids", List.of("1", "x", "3"));

        // When / Then: The invalid entry is rejected
        assertRejected(input, "ids", "x");
    }

    @Test
    @DisplayName("Should reject unknown ENUM constant")
    public void shouldRejectUnknownEnumConstant() {
        // Given: An ENUM field with a value that is not a TestStatus constant
        final SearchInput input = aSearchInput("status", "BOGUS");

        // When / Then: The raw value is rejected
        assertRejected(input, "status", "BOGUS");
    }

    @Test
    @DisplayName("Should reject MULTI_ENUM list containing an unknown constant")
    public void shouldRejectMultiEnumListContainingUnknownConstant() {
        // Given: A MULTI_ENUM field with one unknown constant
        final SearchInput input = aMultiValueSearchInput("roles", List.of("ACTIVE", "BOGUS"));

        // When / Then: The unknown constant is rejected
        assertRejected(input, "roles", "BOGUS");
    }

    @Test
    @DisplayName("Should reject non-ISO from date")
    public void shouldRejectNonIsoFromDate() {
        // Given: A DATE field with a non-ISO from date
        final SearchInput input = aDateSearchInput("createdAt", "01-01-2024", null);

        // When / Then: The raw date is rejected (not a DateTimeParseException)
        assertRejected(input, "createdAt", "01-01-2024");
    }

    @Test
    @DisplayName("Should reject non-ISO to date")
    public void shouldRejectNonIsoToDate() {
        // Given: A DATE field with a valid from date and an invalid to date
        final SearchInput input = aDateSearchInput("createdAt", "2024-01-01T00:00:00", "tomorrow");

        // When / Then: The raw to date is rejected
        assertRejected(input, "createdAt", "tomorrow");
    }

    @Test
    @DisplayName("Should reject BOOLEAN value other than true or false")
    public void shouldRejectBooleanValueOtherThanTrueOrFalse() {
        // Given: A BOOLEAN field with value 'yes'
        final SearchInput input = aSearchInput("active", "yes");

        // When / Then: The raw value is rejected instead of silently becoming false
        assertRejected(input, "active", "yes");
    }

    // =========================================================================
    // toSearchCriteria — absent values remain "no filter"
    // =========================================================================

    @Test
    @DisplayName("Should accept null and blank values without throwing")
    public void shouldAcceptNullAndBlankValuesWithoutThrowing() {
        // Given: Inputs for every validated type with null or blank values
        final List<SearchInput> inputs = List.of(
            aSearchInput("age", null),
            aSearchInput("active", " "),
            aSearchInput("status", ""),
            aDateSearchInput("createdAt", null, null)
        );

        // When: Converting to search criteria
        final List<SearchCriterium> result = meta.toSearchCriteria(inputs);

        // Then: No exception is thrown — absent values are not rejected
        assertThat(result.size() <= inputs.size(), is(true));
    }

    @Test
    @DisplayName("Should accept valid values for every validated type")
    public void shouldAcceptValidValuesForEveryValidatedType() {
        // Given: Valid inputs for every validated type
        final List<SearchInput> inputs = List.of(
            aSearchInput("age", "42"),
            aSearchInput("age", "!42"),
            aMultiValueSearchInput("ids", List.of("1", "2")),
            aSearchInput("status", "ACTIVE"),
            aMultiValueSearchInput("roles", List.of("ACTIVE", "PENDING")),
            aSearchInput("active", "false"),
            aDateSearchInput("createdAt", "2024-01-01T00:00:00", "2024-12-31T23:59:59")
        );

        // When: Converting to search criteria
        final List<SearchCriterium> result = meta.toSearchCriteria(inputs);

        // Then: One criterium per input
        assertThat(result, hasSize(inputs.size()));
    }

    // =========================================================================
    // resolveSort — paging bounds
    // =========================================================================

    @Test
    @DisplayName("Should reject negative page number")
    public void shouldRejectNegativePageNumber() {
        // Given: A negative page number
        final SortablePageInput input = aPageInput(-1, 10);

        // When: Resolving
        final InvalidPageException exception = assertThrows(InvalidPageException.class, () -> meta.resolveSort(input));

        // Then: pageNumber is rejected
        assertThat(exception.getRejectedParameter(), is("pageNumber"));
        assertThat(exception.getRejectedValue(), is(-1));
    }

    @Test
    @DisplayName("Should accept large page size and page number zero")
    public void shouldAcceptLargePageSizeAndPageNumberZero() {
        // Given: pageNumber 0 and a very large page size
        final SortablePageInput input = aPageInput(0, LARGE_PAGE_SIZE);

        // When: Resolving
        final PanacheResolvedSort resolved = meta.resolveSort(input);

        // Then: Paging is applied as requested without a cap
        assertThat(resolved.getPage().index, is(0));
        assertThat(resolved.getPage().size, is(LARGE_PAGE_SIZE));
    }

    @Test
    @DisplayName("Should fall back to default page size when page size is negative")
    public void shouldFallBackToDefaultPageSizeWhenPageSizeIsNegative() {
        // Given: A negative page size
        final SortablePageInput input = aPageInput(0, -5);

        // When: Resolving
        final PanacheResolvedSort resolved = meta.resolveSort(input);

        // Then: The default page size is used
        assertThat(resolved.getPage().size, is(20));
    }

    @Test
    @DisplayName("Should reject page number whose offset overflows int")
    public void shouldRejectPageNumberWhoseOffsetOverflowsInt() {
        // Given: pageNumber * pageSize exceeds Integer.MAX_VALUE
        final int pageSize = 50;
        final int pageNumber = Integer.MAX_VALUE / pageSize + 1;
        final SortablePageInput input = aPageInput(pageNumber, pageSize);

        // When: Resolving
        final InvalidPageException exception = assertThrows(InvalidPageException.class, () -> meta.resolveSort(input));

        // Then: pageNumber is rejected
        assertThat(exception.getRejectedParameter(), is("pageNumber"));
        assertThat(exception.getRejectedValue(), is(pageNumber));
    }


    @Test
    @DisplayName("Should preserve cause with a bounded message when a huge numeric value is invalid")
    public void shouldPreserveBoundedCauseWhenHugeNumericValueIsInvalid() {
        // Given: A 500-char non-numeric value
        final String value = "x".repeat(500);
        final SearchInput input = aSearchInput("age", value);

        // When: Converting to criteria
        final InvalidSearchException exception =
            assertThrows(InvalidSearchException.class, () -> meta.toSearchCriteria(List.of(input)));

        // Then: Cause is kept but its message never echoes the untruncated input
        assertThat(exception.getCause(), is(instanceOf(InvalidFieldValueException.class)));
        assertThat(exception.getCause().getMessage(), not(containsString(value)));
        assertThat(exception.getCause().getMessage().length(), is(lessThanOrEqualTo(MAX_REJECTED_VALUE_LENGTH + 50)));
        assertThat(exception.getRejectedValue().length(), is(MAX_REJECTED_VALUE_LENGTH));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void assertRejected(final SearchInput input, final String field, final String value) {
        final InvalidSearchException exception = assertThrows(
            InvalidSearchException.class,
            () -> meta.toSearchCriteria(List.of(input))
        );
        assertThat(exception.getRejectedField(), is(field));
        assertThat(exception.getRejectedValue(), is(value));
    }

    private static SearchInput aSearchInput(final String fieldName, final String textValue) {
        final SearchInput input = new SearchInput();
        input.setFieldName(fieldName);
        input.setTextValue(textValue);
        return input;
    }

    private static SearchInput aMultiValueSearchInput(final String fieldName, final List<String> values) {
        final SearchInput input = new SearchInput();
        input.setFieldName(fieldName);
        input.setTextValueList(values);
        return input;
    }

    private static SearchInput aDateSearchInput(final String fieldName, final String from, final String to) {
        final SearchInput input = new SearchInput();
        input.setFieldName(fieldName);
        input.setFromDateValue(from);
        input.setToDateValue(to);
        return input;
    }

    private static SortablePageInput aPageInput(final int pageNumber, final int pageSize) {
        return new SortablePageInput(pageNumber, pageSize, Collections.emptyList(), Collections.emptyList());
    }

    static class TestMetaData extends AbstractPanacheSearchMetaData {

        TestMetaData() {
            addField("age", "age", SearchType.NUMERIC, false);
            addField("ids", "id", SearchType.MULTI_NUMERIC, false);
            addField("active", "active", SearchType.BOOLEAN, false);
            addField("status", "status", SearchType.ENUM, TestStatus.class, false);
            addField("roles", "role", SearchType.MULTI_ENUM, TestStatus.class, false);
            addField("createdAt", "createdAt", SearchType.DATE, false);
        }
    }
}
