package io.github.jframe.datasource.search.model.resource;

import io.github.support.UnitTest;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Unit Test - PageResource")
class PageResourceTest extends UnitTest {

    // -------------------------------------------------------------------------
    // 4-arg constructor — content should be empty list, not null
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should initialize content to empty list via 4-arg constructor, not null")
    void shouldInitializeContentToEmptyListViaFourArgConstructor() {
        // Given: Construction via the 4-arg (no-content) constructor

        // When: Creating a PageResource without supplying content
        final PageResource<String> pageResource = new PageResource<>(100L, 4, 25, 0);

        // Then: content should be an empty list, not null
        assertThat(pageResource.getContent(), is(notNullValue()));
        assertThat(pageResource.getContent(), is(empty()));
    }

    // -------------------------------------------------------------------------
    // iterator() — must not NPE when content is empty
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should iterate safely over empty PageResource without NPE")
    void shouldIterateSafelyOverEmptyPageResource() {
        // Given: A PageResource created via 4-arg constructor (empty content)
        final PageResource<String> pageResource = new PageResource<>(0L, 0, 25, 0);

        // When: Iterating over it
        int count = 0;
        for (final String ignored : pageResource) {
            count++;
        }

        // Then: No NPE and count should be zero
        assertThat(count, is(equalTo(0)));
    }

    @Test
    @DisplayName("Should return non-null iterator from empty PageResource")
    void shouldReturnNonNullIteratorFromEmptyPageResource() {
        // Given: A PageResource with no content
        final PageResource<String> pageResource = new PageResource<>(0L, 0, 25, 0);

        // When: Calling iterator()
        final Iterator<String> iterator = pageResource.iterator();

        // Then: Iterator should not be null and should have no next element
        assertThat(iterator, is(notNullValue()));
        assertThat(iterator.hasNext(), is(false));
    }

    // -------------------------------------------------------------------------
    // add() — must work correctly on fresh instance
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should add element to content list correctly")
    void shouldAddElementToContentListCorrectly() {
        // Given: A PageResource created via 4-arg constructor
        final PageResource<String> pageResource = new PageResource<>(10L, 1, 25, 0);

        // When: Adding an element
        pageResource.add("hello");

        // Then: Content should contain the added element
        assertThat(pageResource.getContent(), is(notNullValue()));
        assertThat(pageResource.getContent(), hasSize(1));
        assertThat(pageResource.getContent(), contains("hello"));
    }

    @Test
    @DisplayName("Should add multiple elements to content list in order")
    void shouldAddMultipleElementsToContentListInOrder() {
        // Given: A PageResource created via 4-arg constructor
        final PageResource<String> pageResource = new PageResource<>(10L, 1, 25, 0);

        // When: Adding multiple elements
        pageResource.add("first");
        pageResource.add("second");
        pageResource.add("third");

        // Then: Content should contain all elements in insertion order
        assertThat(pageResource.getContent(), hasSize(3));
        assertThat(pageResource.getContent(), contains("first", "second", "third"));
    }

    @Test
    @DisplayName("Should iterate over added elements without NPE")
    void shouldIterateOverAddedElementsWithoutNpe() {
        // Given: A PageResource with elements added via add()
        final PageResource<String> pageResource = new PageResource<>(10L, 1, 25, 0);
        pageResource.add("alpha");
        pageResource.add("beta");

        // When: Iterating
        int count = 0;
        for (final String ignored : pageResource) {
            count++;
        }

        // Then: Both elements should be visited
        assertThat(count, is(equalTo(2)));
    }

    // -------------------------------------------------------------------------
    // 5-arg (all-args) constructor — content supplied explicitly
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should keep supplied content list in all-args constructor")
    void shouldKeepSuppliedContentListInAllArgsConstructor() {
        // Given: An explicit content list
        final List<String> content = List.of("a", "b");

        // When: Creating with all-args constructor
        final PageResource<String> pageResource = new PageResource<>(2L, 1, 25, 0, content);

        // Then: Content should be the supplied list
        assertThat(pageResource.getContent(), is(notNullValue()));
        assertThat(pageResource.getContent(), hasSize(2));
    }

    @Test
    @DisplayName("Should allow null content when explicitly passed to all-args constructor (legacy contract)")
    void shouldAllowNullContentInAllArgsConstructor() {
        // Given: Null content passed explicitly (Lombok @AllArgsConstructor passes through)

        // When: Creating with null content
        final PageResource<String> pageResource = new PageResource<>(0L, 0, 25, 0, null);

        // Then: Content is null — the 5-arg constructor does not auto-initialize
        assertThat(pageResource.getContent(), is(nullValue()));
    }
}
