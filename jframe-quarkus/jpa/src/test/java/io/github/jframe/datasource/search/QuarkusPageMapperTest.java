package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.resource.PageResource;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link QuarkusPageMapper}.
 *
 * <p>Verifies the abstract template-method-pattern mapper that subclasses implement to
 * convert domain entities to DTOs while producing a {@link PageResource}.
 */
@DisplayName("Quarkus JPA - QuarkusPageMapper")
public class QuarkusPageMapperTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Concrete subclass for testing
    // -------------------------------------------------------------------------

    /**
     * Concrete test implementation that maps String → String (uppercase).
     */
    static class StringUppercasePageMapper extends QuarkusPageMapper<String, String> {

        @Override
        protected String mapItem(final String item) {
            return item.toUpperCase();
        }
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map each item using mapItem template method")
    public void shouldMapEachItemUsingMapItemTemplateMethod() {
        // Given: A concrete mapper and a content list
        final StringUppercasePageMapper mapper = new StringUppercasePageMapper();
        final List<String> items = List.of("alpha", "beta", "gamma");

        // When: Mapping the page
        final PageResource<String> result = mapper.map(items, 3L, 1, 25, 0);

        // Then: All items are transformed by mapItem
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), hasSize(3));
        assertThat(result.getContent().get(0), is(equalTo("ALPHA")));
        assertThat(result.getContent().get(1), is(equalTo("BETA")));
        assertThat(result.getContent().get(2), is(equalTo("GAMMA")));
    }

    @Test
    @DisplayName("Should preserve page metadata when mapping")
    public void shouldPreservePageMetadataWhenMapping() {
        // Given: A concrete mapper and known page metadata
        final StringUppercasePageMapper mapper = new StringUppercasePageMapper();
        final List<String> items = List.of("one", "two");

        // When: Mapping the page
        final PageResource<String> result = mapper.map(items, 50L, 5, 10, 2);

        // Then: Page metadata is correctly set
        assertThat(result.getTotalElements(), is(equalTo(50L)));
        assertThat(result.getTotalPages(), is(equalTo(5)));
        assertThat(result.getPageSize(), is(equalTo(10)));
        assertThat(result.getPageNumber(), is(equalTo(2)));
    }

    @Test
    @DisplayName("Should return PageResource with empty content when items list is empty")
    public void shouldReturnPageResourceWithEmptyContentWhenItemsListIsEmpty() {
        // Given: A concrete mapper and empty items list
        final StringUppercasePageMapper mapper = new StringUppercasePageMapper();
        final List<String> items = List.of();

        // When: Mapping the page
        final PageResource<String> result = mapper.map(items, 0L, 0, 25, 0);

        // Then: PageResource has no content
        assertThat(result, is(notNullValue()));
        assertThat(result.getContent(), hasSize(0));
        assertThat(result.getTotalElements(), is(equalTo(0L)));
    }

    @Test
    @DisplayName("Should return null when items list is null")
    public void shouldReturnNullWhenItemsListIsNull() {
        // Given: A concrete mapper and null items list

        final StringUppercasePageMapper mapper = new StringUppercasePageMapper();

        // When: Mapping null
        final PageResource<String> result = mapper.map(null, 0L, 0, 0, 0);

        // Then: Result is null
        assertThat(result == null, is(true));
    }

    @Test
    @DisplayName("Should map single item correctly")
    public void shouldMapSingleItemCorrectly() {
        // Given: A concrete mapper and a single item
        final StringUppercasePageMapper mapper = new StringUppercasePageMapper();
        final List<String> items = List.of("hello");

        // When: Mapping the page
        final PageResource<String> result = mapper.map(items, 1L, 1, 25, 0);

        // Then: Single item is transformed
        assertThat(result.getContent(), hasSize(1));
        assertThat(result.getContent().get(0), is(equalTo("HELLO")));
    }
}
