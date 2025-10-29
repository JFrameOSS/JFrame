package io.github.jframe.datasource.search.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.lang.NonNull;

/**
 * Resource file describing a Page of elements and associated Page metadata.
 *
 * @param <T> type of items allowed in this PageResource instance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResource<T> implements Iterable<T> {

    @Schema(
        description = "Total number of elements available",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "125"
    )
    private long totalElements;

    @Schema(
        description = "Total number of pages available",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "5"
    )
    private int totalPages;

    @Schema(
        description = "Number of items per page",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "25"
    )
    private int pageSize;

    @Schema(
        description = "Current page number (0-based)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "0"
    )
    private int pageNumber;

    @Schema(
        description = "List of items on the current page",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<T> content;

    /**
     * Constructor without content.
     */
    public PageResource(final long totalElements, final int totalPages, final int pageSize, final int pageNumber) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    /**
     * {@inheritDoc}
     *
     * @return an iterator over the content list.
     */
    @NonNull
    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }

    /**
     * add an item to the content list.
     *
     * @param element content element.
     */
    public void add(final T element) {
        if (content == null) {
            content = new ArrayList<>();
        }
        content.add(element);
    }
}
