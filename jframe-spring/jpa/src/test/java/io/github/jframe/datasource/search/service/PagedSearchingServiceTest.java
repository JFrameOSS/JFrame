package io.github.jframe.datasource.search.service;

import io.github.jframe.datasource.search.JpaSearchSpecification;
import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.AbstractSortSearchMetaData;
import io.github.jframe.datasource.search.model.PageableItem;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Unit Test - PagedSearchingService")
class PagedSearchingServiceTest extends UnitTest {

    @Mock
    private JpaSpecificationExecutor<Item> repository;

    private final TestService service = new TestService();
    private final TestMetaData metaData = new TestMetaData();

    @Test
    @DisplayName("Should apply default sort with tiebreaker when sortOrder is empty")
    void shouldApplyDefaultSortWithTiebreakerWhenSortOrderIsEmpty() {
        // Given: No sort order, explicit page size
        final SortablePageInput input = aPageInput(1, 10, new ArrayList<>());
        final Page<Item> expected = aPage();
        when(repository.findAll(ArgumentMatchers.<Specification<Item>>any(), any(Pageable.class))).thenReturn(expected);

        // When: Searching
        final Page<Item> result = service.search(input, metaData, repository);

        // Then: Default sort + tiebreaker applied, page preserved
        final Pageable pageable = capturePageable();
        assertThat(result, is(sameInstance(expected)));
        assertThat(pageable.getPageNumber(), is(equalTo(1)));
        assertThat(pageable.getPageSize(), is(equalTo(10)));
        assertThat(propertiesOf(pageable.getSort()), contains("name", "id"));
        assertThat(pageable.getSort().getOrderFor("name").getDirection(), is(equalTo(Sort.Direction.DESC)));
    }

    @Test
    @DisplayName("Should use default page size when pageSize is 0")
    void shouldUseDefaultPageSizeWhenPageSizeIsZero() {
        // Given: Zero page size
        final SortablePageInput input = aPageInput(0, 0, new ArrayList<>());
        when(repository.findAll(ArgumentMatchers.<Specification<Item>>any(), any(Pageable.class))).thenReturn(aPage());

        // When: Searching
        service.search(input, metaData, repository);

        // Then: Default page size used
        assertThat(capturePageable().getPageSize(), is(equalTo(20)));
    }

    @Test
    @DisplayName("Should append tiebreaker to requested sort")
    void shouldAppendTiebreakerToRequestedSort() {
        // Given: Explicit sort on a sortable field
        final SortablePageInput input = aPageInput(0, 10, List.of(new SortableColumn("email", "ASC")));
        when(repository.findAll(ArgumentMatchers.<Specification<Item>>any(), any(Pageable.class))).thenReturn(aPage());

        // When: Searching
        service.search(input, metaData, repository);

        // Then: Requested sort followed by tiebreaker
        assertThat(propertiesOf(capturePageable().getSort()), contains("email", "id"));
    }

    @Test
    @DisplayName("Should resolve sort and paging via metadata when specification is supplied")
    void shouldResolveSortViaMetadataWhenSpecificationIsSupplied() {
        // Given: A pre-built specification and empty sort
        final SortablePageInput input = aPageInput(0, 0, new ArrayList<>());
        final JpaSearchSpecification<Item> specification = new JpaSearchSpecification<>(List.of());
        when(repository.findAll(ArgumentMatchers.<Specification<Item>>any(), any(Pageable.class))).thenReturn(aPage());

        // When: Searching with the specification overload
        service.search(input, metaData, specification, repository);

        // Then: Default sort, tiebreaker and default page size applied
        final Pageable pageable = capturePageable();
        assertThat(pageable.getPageSize(), is(equalTo(20)));
        assertThat(propertiesOf(pageable.getSort()), contains("name", "id"));
    }

    @Test
    @DisplayName("Should propagate InvalidPageException without querying repository")
    void shouldPropagateInvalidPageException() {
        // Given: Negative page number
        final SortablePageInput input = aPageInput(-1, 10, new ArrayList<>());

        // When / Then: Rejected before query
        assertThrows(InvalidPageException.class, () -> service.search(input, metaData, repository));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should pass large page size through to repository")
    void shouldPassLargePageSizeThroughToRepository() {
        // Given: A very large page size
        final SortablePageInput input = aPageInput(0, 10_000, new ArrayList<>());
        when(repository.findAll(ArgumentMatchers.<Specification<Item>>any(), any(Pageable.class))).thenReturn(aPage());

        // When: Searching
        service.search(input, metaData, repository);

        // Then: Page size not capped
        assertThat(capturePageable().getPageSize(), is(equalTo(10_000)));
    }

    @Test
    @DisplayName("Should propagate InvalidPageException for negative page number with specification overload")
    void shouldPropagateInvalidPageExceptionWithSpecificationOverload() {
        // Given: Negative page number
        final SortablePageInput input = aPageInput(-1, 10, new ArrayList<>());
        final JpaSearchSpecification<Item> specification = new JpaSearchSpecification<>(List.of());

        // When / Then: Rejected before query
        assertThrows(InvalidPageException.class, () -> service.search(input, metaData, specification, repository));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should propagate InvalidSortException without querying repository")
    void shouldPropagateInvalidSortException() {
        // Given: Sort on unknown field
        final SortablePageInput input = aPageInput(0, 10, List.of(new SortableColumn("unknown", "ASC")));

        // When / Then: Rejected before query
        assertThrows(InvalidSortException.class, () -> service.search(input, metaData, repository));
        verifyNoInteractions(repository);
    }

    private Pageable capturePageable() {
        final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(ArgumentMatchers.<Specification<Item>>any(), captor.capture());
        final Pageable pageable = captor.getValue();
        assertThat(pageable, is(notNullValue()));
        return pageable;
    }

    private static List<String> propertiesOf(final Sort sort) {
        return sort.stream().map(Sort.Order::getProperty).toList();
    }

    private static SortablePageInput aPageInput(final int pageNumber, final int pageSize, final List<SortableColumn> sortOrder) {
        return new SortablePageInput(pageNumber, pageSize, sortOrder, new ArrayList<>());
    }

    private static Page<Item> aPage() {
        return new PageImpl<>(List.of());
    }

    static final class Item implements PageableItem {
    }


    static final class TestService extends PagedSearchingService {

        Page<Item> search(final SortablePageInput input,
            final AbstractSortSearchMetaData metaData,
            final JpaSpecificationExecutor<Item> repository) {
            return searchPage(input, metaData, repository);
        }

        Page<Item> search(final SortablePageInput input,
            final AbstractSortSearchMetaData metaData,
            final JpaSearchSpecification<Item> specification,
            final JpaSpecificationExecutor<Item> repository) {
            return searchPage(input, metaData, specification, repository);
        }
    }


    static final class TestMetaData extends AbstractSortSearchMetaData {

        TestMetaData() {
            super();
            addField("name", "name", SearchType.TEXT, true);
            addField("email", "email", SearchType.TEXT, true);
        }

        @Override
        protected List<SortableColumn> defaultSort() {
            return List.of(new SortableColumn("name", "DESC"));
        }
    }
}
