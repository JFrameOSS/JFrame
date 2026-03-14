package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.resource.PageResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting raw Quarkus/Panache page results to
 * the jframe-core {@link PageResource} model.
 *
 * <p>All methods are static; this class cannot be instantiated.
 */
public final class QuarkusPageAdapter {

    private QuarkusPageAdapter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts raw page data to a {@link PageResource}.
     *
     * <p>Returns {@code null} when {@code content} is {@code null}.
     *
     * @param <T>           the type of content items
     * @param content       the list of items for the current page, may be {@code null}
     * @param totalElements the total number of elements across all pages
     * @param totalPages    the total number of pages
     * @param pageSize      the size of each page
     * @param pageNumber    the current page number (0-based)
     * @return a populated {@link PageResource}, or {@code null} if {@code content} is {@code null}
     */
    public static <T> PageResource<T> toPageResource(final List<T> content,
        final long totalElements,
        final int totalPages,
        final int pageSize,
        final int pageNumber) {
        if (content == null) {
            return null;
        }
        return new PageResource<>(totalElements, totalPages, pageSize, pageNumber, new ArrayList<>(content));
    }
}
