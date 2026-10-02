package io.github.jframe.exception.factory;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.page.InvalidPageErrorResponseResource;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.jframe.exception.sort.InvalidSortErrorResponseResource;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Spring Core - DefaultExceptionResponseFactory")
public class DefaultExceptionResponseFactoryTest extends UnitTest {

    private DefaultExceptionResponseFactory factory;

    @BeforeEach
    @Override
    public void setUp() {
        factory = new DefaultExceptionResponseFactory();
    }

    @Test
    @DisplayName("Should return InvalidSortErrorResponseResource for InvalidSortException")
    public void shouldReturnInvalidSortErrorResponseResourceForInvalidSortException() {
        // Given
        final var ex = new InvalidSortException("badField", List.of("name", "createdAt"));

        // When
        final ErrorResponseResource result = factory.create(ex);

        // Then
        assertThat(result, is(instanceOf(InvalidSortErrorResponseResource.class)));
        final InvalidSortErrorResponseResource sortError = (InvalidSortErrorResponseResource) result;
        assertThat(sortError.getRejectedField(), is(equalTo("badField")));
        assertThat(sortError.getSortableFields(), containsInAnyOrder("name", "createdAt"));
    }

    @Test
    @DisplayName("Should return InvalidSortErrorResponseResource when InvalidSortException is in cause chain")
    public void shouldReturnInvalidSortErrorResponseResourceFromCauseChain() {
        // Given
        final var sortEx = new InvalidSortException("x", List.of("a"));
        final var wrapper = new RuntimeException("wrapper", sortEx);

        // When
        final ErrorResponseResource result = factory.create(wrapper);

        // Then
        assertThat(result, is(instanceOf(InvalidSortErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should return InvalidSearchErrorResponseResource for InvalidSearchException")
    public void shouldReturnInvalidSearchErrorResponseResourceForInvalidSearchException() {
        // Given: An invalid search exception
        final InvalidSearchException exception = new InvalidSearchException("age", "abc", List.of("age", "name"));

        // When: Creating the response
        final ErrorResponseResource result = factory.create(exception);

        // Then: Search-specific resource with details
        assertThat(result, is(instanceOf(InvalidSearchErrorResponseResource.class)));
        final InvalidSearchErrorResponseResource searchError = (InvalidSearchErrorResponseResource) result;
        assertThat(searchError.getRejectedField(), is(equalTo("age")));
        assertThat(searchError.getRejectedValue(), is(equalTo("abc")));
        assertThat(searchError.getSearchableFields(), containsInAnyOrder("age", "name"));
    }

    @Test
    @DisplayName("Should return InvalidSearchErrorResponseResource when InvalidSearchException is in cause chain")
    public void shouldReturnInvalidSearchErrorResponseResourceFromCauseChain() {
        // Given: A wrapped invalid search exception
        final RuntimeException wrapper = new RuntimeException("wrapper", new InvalidSearchException("x", null, List.of("a")));

        // When: Creating the response
        final ErrorResponseResource result = factory.create(wrapper);

        // Then: Search-specific resource
        assertThat(result, is(instanceOf(InvalidSearchErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should return InvalidPageErrorResponseResource for InvalidPageException")
    public void shouldReturnInvalidPageErrorResponseResourceForInvalidPageException() {
        // Given: An invalid page exception
        final InvalidPageException exception = new InvalidPageException("pageNumber", -5);

        // When: Creating the response
        final ErrorResponseResource result = factory.create(exception);

        // Then: Page-specific resource with details
        assertThat(result, is(instanceOf(InvalidPageErrorResponseResource.class)));
        final InvalidPageErrorResponseResource pageError = (InvalidPageErrorResponseResource) result;
        assertThat(pageError.getRejectedParameter(), is(equalTo("pageNumber")));
        assertThat(pageError.getRejectedValue(), is(equalTo(-5)));
    }

    @Test
    @DisplayName("Should return InvalidPageErrorResponseResource when InvalidPageException is in cause chain")
    public void shouldReturnInvalidPageErrorResponseResourceFromCauseChain() {
        // Given: A wrapped invalid page exception
        final RuntimeException wrapper = new RuntimeException("wrapper", new InvalidPageException("pageNumber", -1));

        // When: Creating the response
        final ErrorResponseResource result = factory.create(wrapper);

        // Then: Page-specific resource
        assertThat(result, is(instanceOf(InvalidPageErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should return plain ErrorResponseResource for generic HttpException")
    public void shouldReturnPlainErrorResponseResourceForHttpException() {
        // Given
        final var ex = new HttpException(JFrameErrorCode.BAD_REQUEST);

        // When
        final ErrorResponseResource result = factory.create(ex);

        // Then: should be plain resource (not sort subtype)
        assertThat(result, is(notNullValue()));
        assertThat(result, is(not(instanceOf(InvalidSortErrorResponseResource.class))));
    }

    @Test
    @DisplayName("Should return non-null resource for null throwable")
    public void shouldReturnNonNullResourceForNullThrowable() {
        // When
        final ErrorResponseResource result = factory.create(null);

        // Then
        assertThat(result, is(notNullValue()));
    }
}
