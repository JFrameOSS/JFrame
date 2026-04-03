package io.github.jframe.datasource.search.model.input;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Class that defines a sortable column.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortableColumn implements Serializable {

    @Serial
    private static final long serialVersionUID = 101291045332418919L;

    @Schema(
        description = "Name of the field to sort on",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "name"
    )
    private String name;

    @Schema(
        description = "Direction to sort, either ASC or DESC",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "ASC"
    )
    private String direction;

    /**
     * A constructor that copies another SortableColumn.
     */
    public SortableColumn(final SortableColumn other) {
        this.name = other.name;
        this.direction = other.direction;
    }
}
