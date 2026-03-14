package io.github.jframe.datasource.search;

import io.github.jframe.datasource.search.model.resource.PageResource;

import java.util.List;

/**
 * Abstract base class that maps a list of source entities to a {@link PageResource} of target DTOs.
 *
 * <p>Subclasses implement {@link #mapItem(Object)} to define the per-item conversion.
 *
 * @param <S> the source type (e.g. a JPA entity)
 * @param <T> the target type (e.g. a response DTO)
 */
public abstract class QuarkusPageMapper<S, T> {

    /**
     * Maps a page of source items to a {@link PageResource} of target items.
     *
     * <p>Returns {@code null} when {@code items} is {@code null}.
     *
     * @param items         the source items for this page, may be {@code null}
     * @param totalElements the total element count across all pages
     * @param totalPages    the total page count
     * @param pageSize      the size of each page
     * @param pageNumber    the current page number (0-based)
     * @return a populated {@link PageResource}, or {@code null} if {@code items} is {@code null}
     */
    public PageResource<T> map(final List<S> items,
        final long totalElements,
        final int totalPages,
        final int pageSize,
        final int pageNumber) {
        if (items == null) {
            return null;
        }
        final List<T> mapped = items.stream()
            .map(this::mapItem)
            .toList();
        return QuarkusPageAdapter.toPageResource(mapped, totalElements, totalPages, pageSize, pageNumber);
    }

    /**
     * Converts a single source item to its target representation.
     *
     * @param item the source item
     * @return the converted target item
     */
    protected abstract T mapItem(S item);
}
