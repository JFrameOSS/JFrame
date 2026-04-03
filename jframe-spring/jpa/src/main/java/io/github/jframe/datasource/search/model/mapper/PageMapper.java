package io.github.jframe.datasource.search.model.mapper;

import io.github.jframe.datasource.search.model.PageableItem;
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
     * convert PageableItem to PageableItemResource.
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

        source.getContent().forEach(item -> {
            pageResource.add(toResourceObject(item));

        });
        return pageResource;
    }
}
