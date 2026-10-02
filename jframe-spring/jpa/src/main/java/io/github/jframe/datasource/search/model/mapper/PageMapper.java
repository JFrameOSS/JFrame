package io.github.jframe.datasource.search.model.mapper;

import io.github.jframe.datasource.search.model.PageableItem;
import io.github.jframe.datasource.search.model.resource.AppliedSort;
import io.github.jframe.datasource.search.model.resource.PageResource;
import io.github.jframe.datasource.search.model.resource.PageableItemResource;

import org.springframework.data.domain.Page;

import static java.util.Objects.isNull;

/**
 * Mapper superclass to convert between Paged resource objects and Paged model objects.
 *
 * @param <T> pageable item resource class.
 * @param <S> pageable item class.
 */
public abstract class PageMapper<T extends PageableItemResource, S extends PageableItem> {

    /**
     * Convert a PageableItem to a PageableItemResource.
     */
    public abstract T toResourceObject(S source);

    /**
     * Map a page of {@code S} objects to a PageResource of {@code T} objects.
     */
    public PageResource<T> toPageResource(final Page<S> source) {
        if (isNull(source)) {
            return null;
        }

        final PageResource<T> pageResource = new PageResource<>(
            source.getTotalElements(),
            source.getTotalPages(),
            source.getSize(),
            source.getNumber()
        );

        source.getContent().forEach(item -> pageResource.add(toResourceObject(item)));
        return pageResource;
    }

    /**
     * Map a page of {@code S} to a {@link PageResource} of {@code T} with sort metadata.
     *
     * @param source      the Spring Data page (may be {@code null})
     * @param appliedSort the sort applied to this page (may be {@code null})
     * @return the mapped page resource, or {@code null} if {@code source} is {@code null}
     */
    public PageResource<T> toPageResource(final Page<S> source, final AppliedSort appliedSort) {
        final PageResource<T> resource = toPageResource(source);
        if (resource != null) {
            resource.setAppliedSort(appliedSort);
        }
        return resource;
    }
}
