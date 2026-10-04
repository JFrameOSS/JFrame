package io.github.jframe.datasource.search.model.mapper;

import io.github.jframe.datasource.search.model.PageableItem;
import io.github.jframe.datasource.search.model.ResolvedSort;
import io.github.jframe.datasource.search.model.resource.AppliedSort;
import io.github.jframe.datasource.search.model.resource.PageResource;
import io.github.jframe.datasource.search.model.resource.PageableItemResource;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Unit Test - PageMapper")
class PageMapperTest extends UnitTest {

    private TestPageMapper mapper;

    @BeforeEach
    @Override
    public void setUp() {
        mapper = new TestPageMapper();
    }

    @Test
    @DisplayName("Should return null when source Page is null")
    void shouldReturnNullWhenSourceIsNull() {
        // Given: A null input page

        // When: Mapping to PageResource
        final PageResource<TestItemResource> result = mapper.toPageResource(null);

        // Then: Result should be null
        assertThat(result, is(nullValue()));
    }

    @Test
    @DisplayName("Should return PageResource with empty list — NOT null — when Page has no content")
    void shouldReturnEmptyListNotNullWhenPageHasNoContent() {
        // Given: A Page with no items
        final Page<TestItem> emptyPage = new PageImpl<>(
            Collections.emptyList(),
            PageRequest.of(0, 25),
            0L
        );

        // When: Mapping to PageResource
        final PageResource<TestItemResource> result = mapper.toPageResource(emptyPage);

        // Then: content should be an empty list, not null
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), is(notNullValue()));
        assertThat(result.getContent(), is(empty()));
    }

    @Test
    @DisplayName("Should return PageResource with content when Page has items")
    void shouldReturnPageResourceWithContentWhenPageHasItems() {
        // Given: A Page with two items
        final List<TestItem> items = List.of(new TestItem("alpha"), new TestItem("beta"));
        final Page<TestItem> page = new PageImpl<>(items, PageRequest.of(0, 25), 2L);

        // When: Mapping to PageResource
        final PageResource<TestItemResource> result = mapper.toPageResource(page);

        // Then: Content list should have two entries
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), is(notNullValue()));
        assertThat(result.getContent(), hasSize(2));
    }

    @Test
    @DisplayName("Should correctly map page metadata (totalElements, totalPages, pageSize, pageNumber)")
    void shouldCorrectlyMapPageMetadata() {
        // Given: A Page with known metadata
        final Page<TestItem> page = new PageImpl<>(
            List.of(new TestItem("x")),
            PageRequest.of(2, 10),
            55L
        );

        // When: Mapping to PageResource
        final PageResource<TestItemResource> result = mapper.toPageResource(page);

        // Then: Metadata fields should match the source Page
        assertThat(result.getTotalElements(), is(equalTo(55L)));
        assertThat(result.getPageSize(), is(equalTo(10)));
        assertThat(result.getPageNumber(), is(equalTo(2)));
        assertThat(result.getTotalPages(), is(not(equalTo(0))));
    }

    @Test
    @DisplayName("Should be safe to iterate over PageResource content when page is empty")
    void shouldBeSafeToIterateOverEmptyPageResourceContent() {
        // Given: A Page with no items
        final Page<TestItem> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 25), 0L);

        // When: Mapping and iterating
        final PageResource<TestItemResource> result = mapper.toPageResource(emptyPage);

        // Then: Iterating content should not throw NullPointerException
        int count = 0;
        for (final TestItemResource ignored : result) {
            count++;
        }
        assertThat(count, is(equalTo(0)));
    }

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    static class TestItem implements PageableItem {

        private final String value;

        TestItem(final String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }


    static class TestItemResource implements PageableItemResource {

        private final String value;

        TestItemResource(final String value) {
            this.value = value;
        }
    }


    static class TestPageMapper extends PageMapper<TestItemResource, TestItem> {

        @Override
        public TestItemResource toResourceObject(final TestItem source) {
            return new TestItemResource(source.getValue());
        }
    }

    @Test
    @DisplayName("Should set appliedSort when overload is used")
    public void shouldSetAppliedSortWhenOverloadIsUsed() {
        // Given
        final Page<TestItem> page = new PageImpl<>(List.of(new TestItem("a")), PageRequest.of(0, 25), 1L);
        final var appliedSort = new AppliedSort("name", "ASC", false);

        // When
        final PageResource<TestItemResource> result = mapper.toPageResource(page, appliedSort);

        // Then
        assertThat(result.getAppliedSort(), is(notNullValue()));
        assertThat(result.getAppliedSort().getField(), is(equalTo("name")));
        assertThat(result.getAppliedSort().getDirection(), is(equalTo("ASC")));
        assertThat(result.getAppliedSort().isDefault(), is(false));
    }

    @Test
    @DisplayName("Should set appliedSort from ResolvedSort and return null for null page")
    public void shouldSetAppliedSortFromResolvedSort() {
        // Given: A page and a resolved sort
        final Page<TestItem> page = new PageImpl<>(List.of(new TestItem("a")), PageRequest.of(0, 25), 1L);
        final AppliedSort appliedSort = new AppliedSort("score", "DESC", true);
        final ResolvedSort resolvedSort = new ResolvedSort(PageRequest.of(0, 25), appliedSort, List.of(), true);

        // When: Mapping with the ResolvedSort overload
        final PageResource<TestItemResource> result = mapper.toPageResource(page, resolvedSort);
        final PageResource<TestItemResource> nullResult = mapper.toPageResource(null, resolvedSort);

        // Then: appliedSort is copied; null page maps to null
        assertThat(result.getAppliedSort(), is(equalTo(appliedSort)));
        assertThat(result.getContent(), hasSize(1));
        assertThat(nullResult, is(nullValue()));
    }

    @Test
    @DisplayName("Should return null appliedSort when null passed to overload")
    public void shouldReturnNullAppliedSortWhenNullPassedToOverload() {
        // Given
        final Page<TestItem> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0L);

        // When
        final PageResource<TestItemResource> result = mapper.toPageResource(page, (AppliedSort) null);

        // Then
        assertThat(result.getAppliedSort(), is(nullValue()));
    }
}
