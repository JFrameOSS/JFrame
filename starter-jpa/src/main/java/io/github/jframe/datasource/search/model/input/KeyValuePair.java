package io.github.jframe.datasource.search.model.input;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * key value pair for search input.
 */
@Data
@RequiredArgsConstructor
public class KeyValuePair {

    @Schema(
        description = "Key of the key-value pair",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "status"
    )
    private final String key;

    @Schema(
        description = "Value of the key-value pair",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "active"
    )
    private final String value;

    /**
     * A constructor that copies another KeyValuePair.
     */
    public KeyValuePair(final KeyValuePair other) {
        this.key = other.key;
        this.value = other.value;
    }
}
