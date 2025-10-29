package io.github.jframe.datasource.search.model.input;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Input parameters to handle sorting and paging.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortablePageInput {

    @Schema(
        description = "Page number to return",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "0"
    )
    private int pageNumber;

    @Schema(
        description = "Number of items per page to return",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "25"
    )
    private int pageSize;

    @Schema(
        description = "List of properties to sort on, with direction",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<SortableColumn> sortOrder = new ArrayList<>();

    @Schema(
        description = "List of search criteria to filter results",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<SearchInput> searchInputs = new ArrayList<>();

    /**
     * A constructor that copies another SortablePageInput.
     */
    public SortablePageInput(final SortablePageInput other) {
        this.pageNumber = other.pageNumber;
        this.pageSize = other.pageSize;

        for (final SortableColumn column : other.sortOrder) {
            this.sortOrder.add(new SortableColumn(column));
        }

        for (final SearchInput searchInput : other.searchInputs) {
            this.searchInputs.add(new SearchInput(searchInput));
        }
    }

    /**
     * Adds a search input to the list of search inputs.
     */
    public void addSearchInput(final SearchInput searchInput) {
        this.searchInputs.add(searchInput);
    }

}
