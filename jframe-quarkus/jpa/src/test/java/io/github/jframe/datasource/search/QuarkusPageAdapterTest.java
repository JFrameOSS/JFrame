package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.resource.PageResource;
import io.github.support.UnitTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link QuarkusPageAdapter}.
 *
 * <p>Verifies static conversion of raw page values to jframe-core's {@link PageResource},
 * including metadata mapping, content mapping, empty pages, large pages, and null handling.
 */
@DisplayName("Quarkus JPA - QuarkusPageAdapter")
public class QuarkusPageAdapterTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Utility class structure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be a final class")
    public void shouldBeAFinalClass() {
        // Given: The QuarkusPageAdapter class

        // When: Checking the class modifiers
        final int modifiers = QuarkusPageAdapter.class.getModifiers();

        // Then: The class should be declared final
        assertThat(Modifier.isFinal(modifiers), is(true));
    }

    @Test
    @DisplayName("Should have private constructor preventing instantiation")
    public void shouldHavePrivateConstructorPreventingInstantiation() throws Exception {
        // Given: The single declared constructor of the utility class
        final Constructor<QuarkusPageAdapter> constructor =
            QuarkusPageAdapter.class.getDeclaredConstructor();

        // When: Checking constructor visibility
        final boolean isPrivate = Modifier.isPrivate(constructor.getModifiers());

        // Then: Constructor must be private
        assertThat(isPrivate, is(true));
    }

    @Test
    @DisplayName("Should throw exception when instantiated via reflection")
    public void shouldThrowExceptionWhenInstantiatedViaReflection() throws Exception {
        // Given: The private constructor made accessible via reflection
        final Constructor<QuarkusPageAdapter> constructor =
            QuarkusPageAdapter.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then: Instantiation should be prevented
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    // -------------------------------------------------------------------------
    // Null handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return null when all inputs are null")
    public void shouldReturnNullWhenAllInputsAreNull() {
        // Given: Null inputs for all parameters

        // When: Converting null inputs to a PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(null, 0, 0, 0, 0);

        // Then: Result should be null
        assertThat(result, is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // Metadata mapping
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map totalElements to PageResource")
    public void shouldMapTotalElementsToPageResource() {
        // Given: Raw page data with 42 total elements
        final List<String> content = Collections.emptyList();
        final long totalElements = 42L;
        final int totalPages = 2;
        final int pageSize = 25;
        final int pageNumber = 0;

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            content,
            totalElements,
            totalPages,
            pageSize,
            pageNumber
        );

        // Then: totalElements should be 42
        assertThat(result, is(notNullValue()));
        assertThat(result.getTotalElements(), is(equalTo(42L)));
    }

    @Test
    @DisplayName("Should map totalPages to PageResource")
    public void shouldMapTotalPagesToPageResource() {
        // Given: Raw page data with 5 total pages
        final List<String> content = Collections.emptyList();
        final long totalElements = 125L;
        final int totalPages = 5;
        final int pageSize = 25;
        final int pageNumber = 0;

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            content,
            totalElements,
            totalPages,
            pageSize,
            pageNumber
        );

        // Then: totalPages should be 5
        assertThat(result.getTotalPages(), is(equalTo(5)));
    }

    @Test
    @DisplayName("Should map pageSize to PageResource")
    public void shouldMapPageSizeToPageResource() {
        // Given: Raw page data with size 10
        final List<String> content = Collections.emptyList();
        final long totalElements = 30L;
        final int totalPages = 3;
        final int pageSize = 10;
        final int pageNumber = 1;

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            content,
            totalElements,
            totalPages,
            pageSize,
            pageNumber
        );

        // Then: pageSize should be 10
        assertThat(result.getPageSize(), is(equalTo(10)));
    }

    @Test
    @DisplayName("Should map pageNumber to PageResource")
    public void shouldMapPageNumberToPageResource() {
        // Given: Raw page data on page 2 (0-based)
        final List<String> content = Collections.emptyList();
        final long totalElements = 75L;
        final int totalPages = 3;
        final int pageSize = 25;
        final int pageNumber = 2;

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            content,
            totalElements,
            totalPages,
            pageSize,
            pageNumber
        );

        // Then: pageNumber should be 2
        assertThat(result.getPageNumber(), is(equalTo(2)));
    }

    @Test
    @DisplayName("Should map all metadata fields correctly in single call")
    public void shouldMapAllMetadataFieldsCorrectlyInSingleCall() {
        // Given: Raw page data with known metadata
        final List<String> content = Collections.emptyList();

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            content,
            100L,
            4,
            25,
            1
        );

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
    @DisplayName("Should return PageResource with empty content when list is empty")
    public void shouldReturnPageResourceWithEmptyContentWhenListIsEmpty() {
        // Given: An empty content list
        final List<String> content = Collections.emptyList();

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            content,
            0L,
            0,
            25,
            0
        );

        // Then: PageResource should be non-null with 0 total elements and empty content
        assertThat(result, is(notNullValue()));
        assertThat(result.getTotalElements(), is(equalTo(0L)));
        assertThat(result.getContent(), is(notNullValue()));
        assertThat(result.getContent(), hasSize(0));
    }

    @Test
    @DisplayName("Should include all items from content list in PageResource")
    public void shouldIncludeAllItemsFromContentListInPageResource() {
        // Given: A content list with 3 items
        final List<String> items = List.of("alpha", "beta", "gamma");

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            items,
            3L,
            1,
            25,
            0
        );

        // Then: All 3 items should appear in PageResource content
        assertThat(result.getContent(), is(notNullValue()));
        assertThat(result.getContent(), hasSize(3));
        assertThat(result.getContent(), contains("alpha", "beta", "gamma"));
    }

    @Test
    @DisplayName("Should preserve order of items in PageResource")
    public void shouldPreserveOrderOfItemsInPageResource() {
        // Given: An ordered content list
        final List<String> items = List.of("first", "second", "third", "fourth");

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            items,
            4L,
            1,
            25,
            0
        );

        // Then: Order of items should be preserved
        assertThat(result.getContent().get(0), is(equalTo("first")));
        assertThat(result.getContent().get(1), is(equalTo("second")));
        assertThat(result.getContent().get(2), is(equalTo("third")));
        assertThat(result.getContent().get(3), is(equalTo("fourth")));
    }

    @Test
    @DisplayName("Should map a large page of items to PageResource correctly")
    public void shouldMapALargePageOfItemsToPageResourceCorrectly() {
        // Given: A large content list of 100 items
        final List<String> items = Collections.nCopies(100, "item");

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            items,
            10_000L,
            100,
            100,
            0
        );

        // Then: All metadata and content should be correctly mapped
        assertThat(result.getTotalElements(), is(equalTo(10_000L)));
        assertThat(result.getTotalPages(), is(equalTo(100)));
        assertThat(result.getPageSize(), is(equalTo(100)));
        assertThat(result.getContent(), hasSize(100));
    }

    @Test
    @DisplayName("Should return non-null PageResource for single item")
    public void shouldReturnNonNullPageResourceForSingleItem() {
        // Given: A content list with exactly 1 item
        final List<String> items = List.of("only-item");

        // When: Converting to PageResource
        final PageResource<String> result = QuarkusPageAdapter.toPageResource(
            items,
            1L,
            1,
            25,
            0
        );

        // Then: The single item should be present
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), hasSize(1));
        assertThat(result.getContent().get(0), is(equalTo("only-item")));
    }
}
