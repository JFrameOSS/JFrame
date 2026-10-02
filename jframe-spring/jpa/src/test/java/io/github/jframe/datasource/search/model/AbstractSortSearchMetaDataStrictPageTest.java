package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Unit Test - AbstractSortSearchMetaData Strict Paging")
class AbstractSortSearchMetaDataStrictPageTest extends UnitTest {

    private static final int LARGE_PAGE_SIZE = 10_000;

    private final TestMetaData metaData = new TestMetaData();

    @Test
    @DisplayName("Should throw InvalidPageException when pageNumber is negative")
    void shouldThrowWhenPageNumberIsNegative() {
        // Given: A negative page number
        final SortablePageInput input = aPageInput(-1, 10);

        // When: Resolving
        final InvalidPageException exception = assertThrows(InvalidPageException.class, () -> metaData.resolveSort(input));

        // Then: pageNumber rejected
        assertThat(exception.getRejectedParameter(), is(equalTo("pageNumber")));
        assertThat(exception.getRejectedValue(), is(equalTo(-1)));
    }

    @Test
    @DisplayName("Should accept large page size without cap")
    void shouldAcceptLargePageSizeWithoutCap() {
        // Given: A very large page size
        final SortablePageInput input = aPageInput(0, LARGE_PAGE_SIZE);

        // When: Converting to pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: Accepted as-is
        assertThat(pageable.getPageNumber(), is(equalTo(0)));
        assertThat(pageable.getPageSize(), is(equalTo(LARGE_PAGE_SIZE)));
    }

    @Test
    @DisplayName("Should accept large page size with explicit sort")
    void shouldAcceptLargePageSizeWithExplicitSort() {
        // Given: A very large page with a sort order
        final SortablePageInput input = aPageInput(0, LARGE_PAGE_SIZE);
        input.setSortOrder(List.of(new SortableColumn("name", "ASC")));

        // When: Resolving
        final Pageable pageable = metaData.resolveSort(input).getPageable();

        // Then: Page size untouched
        assertThat(pageable.getPageSize(), is(equalTo(LARGE_PAGE_SIZE)));
    }

    @Test
    @DisplayName("Should use default page size when pageSize is not positive")
    void shouldUseDefaultPageSizeWhenPageSizeIsNotPositive() {
        // Given: A zero page size
        final SortablePageInput input = aPageInput(0, 0);

        // When: Converting to pageable
        final Pageable pageable = metaData.toPageable(input);

        // Then: Default applied
        assertThat(pageable.getPageSize(), is(equalTo(20)));
    }

    private static SortablePageInput aPageInput(final int pageNumber, final int pageSize) {
        return new SortablePageInput(pageNumber, pageSize, new ArrayList<>(), Collections.emptyList());
    }

    static class TestMetaData extends AbstractSortSearchMetaData {

        TestMetaData() {
            super();
            addField("name", "name", SearchType.TEXT, true);
        }
    }
}
