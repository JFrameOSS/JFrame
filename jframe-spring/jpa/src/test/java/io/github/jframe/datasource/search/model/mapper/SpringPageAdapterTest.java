package io.github.jframe.datasource.search.model.mapper;

import io.github.jframe.datasource.search.model.resource.PageResource;
import io.github.support.UnitTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SpringPageAdapter}.
 *
 * <p>Verifies static conversion of Spring's {@link Page} to jframe-core's {@link PageResource},
 * including metadata mapping, content mapping, empty pages, large pages, and null handling.
 */
@DisplayName("Spring JPA - SpringPageAdapter")
public class SpringPageAdapterTest extends UnitTest {

    @Mock
    private Page<String> page;

    // -------------------------------------------------------------------------
    // Utility class structure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be a final class")
    public void shouldBeAFinalClass() {
        // Given: The SpringPageAdapter class

        // When: Checking the class modifiers
        final int modifiers = SpringPageAdapter.class.getModifiers();

        // Then: The class should be declared final
        assertThat(Modifier.isFinal(modifiers), is(true));
    }

    @Test
    @DisplayName("Should have private constructor preventing instantiation")
    public void shouldHavePrivateConstructorPreventingInstantiation() throws Exception {
        // Given: The single declared constructor of the utility class
        final Constructor<SpringPageAdapter> constructor =
            SpringPageAdapter.class.getDeclaredConstructor();

        // When: Checking constructor visibility
        final boolean isPrivate = Modifier.isPrivate(constructor.getModifiers());

        // Then: Constructor must be private
        assertThat(isPrivate, is(true));
    }

    @Test
    @DisplayName("Should throw exception when instantiated via reflection")
    public void shouldThrowExceptionWhenInstantiatedViaReflection() throws Exception {
        // Given: The private constructor made accessible via reflection
        final Constructor<SpringPageAdapter> constructor =
            SpringPageAdapter.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then: Instantiation should be prevented
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    // -------------------------------------------------------------------------
    // Null handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return null when page is null")
    public void shouldReturnNullWhenPageIsNull() {
        // Given: A null Page input

        // When: Converting the null page to a PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(null);

        // Then: Result should be null (matching existing PageMapper pattern)
        assertThat(result, is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // Metadata mapping
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map totalElements from Spring Page to PageResource")
    public void shouldMapTotalElementsFromSpringPageToPageResource() {
        // Given: A Spring Page with 42 total elements
        when(page.getTotalElements()).thenReturn(42L);
        when(page.getTotalPages()).thenReturn(2);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(0);
        when(page.getContent()).thenReturn(Collections.emptyList());

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: totalElements should be 42
        assertThat(result, is(notNullValue()));
        assertThat(result.getTotalElements(), is(equalTo(42L)));
    }

    @Test
    @DisplayName("Should map totalPages from Spring Page to PageResource")
    public void shouldMapTotalPagesFromSpringPageToPageResource() {
        // Given: A Spring Page with 5 total pages
        when(page.getTotalElements()).thenReturn(125L);
        when(page.getTotalPages()).thenReturn(5);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(0);
        when(page.getContent()).thenReturn(Collections.emptyList());

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: totalPages should be 5
        assertThat(result.getTotalPages(), is(equalTo(5)));
    }

    @Test
    @DisplayName("Should map pageSize from Spring Page to PageResource")
    public void shouldMapPageSizeFromSpringPageToPageResource() {
        // Given: A Spring Page with size 10
        when(page.getTotalElements()).thenReturn(30L);
        when(page.getTotalPages()).thenReturn(3);
        when(page.getSize()).thenReturn(10);
        when(page.getNumber()).thenReturn(1);
        when(page.getContent()).thenReturn(Collections.emptyList());

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: pageSize should be 10
        assertThat(result.getPageSize(), is(equalTo(10)));
    }

    @Test
    @DisplayName("Should map pageNumber from Spring Page to PageResource")
    public void shouldMapPageNumberFromSpringPageToPageResource() {
        // Given: A Spring Page on page 2 (0-based)
        when(page.getTotalElements()).thenReturn(75L);
        when(page.getTotalPages()).thenReturn(3);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(2);
        when(page.getContent()).thenReturn(Collections.emptyList());

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: pageNumber should be 2
        assertThat(result.getPageNumber(), is(equalTo(2)));
    }

    @Test
    @DisplayName("Should map all metadata fields correctly in single call")
    public void shouldMapAllMetadataFieldsCorrectlyInSingleCall() {
        // Given: A Spring Page with known metadata
        when(page.getTotalElements()).thenReturn(100L);
        when(page.getTotalPages()).thenReturn(4);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(1);
        when(page.getContent()).thenReturn(Collections.emptyList());

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: All metadata fields should be mapped correctly
        assertThat(result.getTotalElements(), is(equalTo(100L)));
        assertThat(result.getTotalPages(), is(equalTo(4)));
        assertThat(result.getPageSize(), is(equalTo(25)));
        assertThat(result.getPageNumber(), is(equalTo(1)));
    }

    // -------------------------------------------------------------------------
    // Content mapping
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return PageResource with empty content when page has no elements")
    public void shouldReturnPageResourceWithEmptyContentWhenPageHasNoElements() {
        // Given: An empty Spring Page
        when(page.getTotalElements()).thenReturn(0L);
        when(page.getTotalPages()).thenReturn(0);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(0);
        when(page.getContent()).thenReturn(Collections.emptyList());

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: PageResource should be non-null with 0 total elements and empty content
        assertThat(result, is(notNullValue()));
        assertThat(result.getTotalElements(), is(equalTo(0L)));
        assertThat(result.getContent(), is(notNullValue()));
        assertThat(result.getContent(), hasSize(0));
    }

    @Test
    @DisplayName("Should include all items from Spring Page content in PageResource")
    public void shouldIncludeAllItemsFromSpringPageContentInPageResource() {
        // Given: A Spring Page with 3 items
        final List<String> items = List.of("alpha", "beta", "gamma");
        when(page.getTotalElements()).thenReturn(3L);
        when(page.getTotalPages()).thenReturn(1);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(0);
        when(page.getContent()).thenReturn(items);

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: All 3 items should appear in PageResource content
        assertThat(result.getContent(), is(notNullValue()));
        assertThat(result.getContent(), hasSize(3));
        assertThat(result.getContent(), contains("alpha", "beta", "gamma"));
    }

    @Test
    @DisplayName("Should preserve order of items from Spring Page in PageResource")
    public void shouldPreserveOrderOfItemsFromSpringPageInPageResource() {
        // Given: A Spring Page with ordered items
        final List<String> items = List.of("first", "second", "third", "fourth");
        when(page.getTotalElements()).thenReturn(4L);
        when(page.getTotalPages()).thenReturn(1);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(0);
        when(page.getContent()).thenReturn(items);

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: Order of items should be preserved
        assertThat(result.getContent().get(0), is(equalTo("first")));
        assertThat(result.getContent().get(1), is(equalTo("second")));
        assertThat(result.getContent().get(2), is(equalTo("third")));
        assertThat(result.getContent().get(3), is(equalTo("fourth")));
    }

    @Test
    @DisplayName("Should map a large page of items to PageResource correctly")
    public void shouldMapALargePageOfItemsToPageResourceCorrectly() {
        // Given: A Spring Page representing a large result set (100 items on page)
        final List<String> items = Collections.nCopies(100, "item");
        when(page.getTotalElements()).thenReturn(10_000L);
        when(page.getTotalPages()).thenReturn(100);
        when(page.getSize()).thenReturn(100);
        when(page.getNumber()).thenReturn(0);
        when(page.getContent()).thenReturn(items);

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: All metadata and content should be correctly mapped
        assertThat(result.getTotalElements(), is(equalTo(10_000L)));
        assertThat(result.getTotalPages(), is(equalTo(100)));
        assertThat(result.getPageSize(), is(equalTo(100)));
        assertThat(result.getContent(), hasSize(100));
    }

    @Test
    @DisplayName("Should return non-null PageResource for page with single item")
    public void shouldReturnNonNullPageResourceForPageWithSingleItem() {
        // Given: A Spring Page with exactly 1 item
        when(page.getTotalElements()).thenReturn(1L);
        when(page.getTotalPages()).thenReturn(1);
        when(page.getSize()).thenReturn(25);
        when(page.getNumber()).thenReturn(0);
        when(page.getContent()).thenReturn(List.of("only-item"));

        // When: Converting to PageResource
        final PageResource<String> result = SpringPageAdapter.toPageResource(page);

        // Then: The single item should be present
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), hasSize(1));
        assertThat(result.getContent().get(0), is(equalTo("only-item")));
    }
}
