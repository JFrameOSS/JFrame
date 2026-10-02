package io.github.jframe.datasource.search.model.mapper;

import io.github.jframe.datasource.search.model.resource.AppliedSort;
import io.github.jframe.datasource.search.model.resource.PageResource;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Static utility class for converting Spring Data's {@link Page} to jframe-core's {@link PageResource}.
 *
 * <p>Handles mapping of all pagination metadata (totalElements, totalPages, pageSize, pageNumber)
 * and copies the page content into the resulting {@link PageResource}.
 */
public final class SpringPageAdapter {

    private SpringPageAdapter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a Spring Data {@link Page} to a jframe-core {@link PageResource}.
     *
     * @param page the Spring Data page to convert; may be {@code null}
     * @param <T>  the type of elements in the page
     * @return the corresponding {@link PageResource}, or {@code null} if {@code page} is {@code null}
     */
    public static <T> PageResource<T> toPageResource(final Page<T> page) {
        if (page == null) {
            return null;
        }

        final List<T> content = new ArrayList<>(page.getContent());

        return new PageResource<>(
            page.getTotalElements(),
            page.getTotalPages(),
            page.getSize(),
            page.getNumber(),
            content
        );
    }

    /**
     * Converts a Spring Data {@link Page} to a jframe-core {@link PageResource} with applied sort metadata.
     *
     * @param page        the Spring Data page to convert; may be {@code null}
     * @param appliedSort the sort that was applied; may be {@code null} (omitted from JSON output)
     * @param <T>         the type of elements in the page
     * @return the corresponding {@link PageResource}, or {@code null} if {@code page} is {@code null}
     */
    public static <T> PageResource<T> toPageResource(final Page<T> page, final AppliedSort appliedSort) {
        final PageResource<T> resource = toPageResource(page);
        if (resource != null) {
            resource.setAppliedSort(appliedSort);
        }
        return resource;
    }
}
