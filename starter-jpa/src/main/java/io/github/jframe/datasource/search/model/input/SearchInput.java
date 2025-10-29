package io.github.jframe.datasource.search.model.input;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Input class describing field name and search value associated with it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchInput {

    @Schema(
        description = "Name of the field to search on",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "name"
    )
    private String fieldName;

    @Schema(
        description = "Text value to search for",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "John"
    )
    private String textValue;

    @Schema(
        description = "From date value to search for (inclusive)",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "2023-01-01T00:00:00Z"
    )
    private String fromDateValue;

    @Schema(
        description = "To date value to search for (inclusive)",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "2023-12-31T23:59:59Z"
    )
    private String toDateValue;

    @Schema(
        description = "List of text values to search for",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "[\"Value1\", \"Value2\"]"
    )
    private List<String> textValueList = new ArrayList<>();

    @Schema(
        description = "List of key-value pairs to search for",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<KeyValuePair> objectValueList = new ArrayList<>();

    /**
     * A constructor that copies another SearchInput.
     */
    public SearchInput(final SearchInput other) {
        this.fieldName = other.fieldName;
        this.textValue = other.textValue;
        this.fromDateValue = other.fromDateValue;
        this.toDateValue = other.toDateValue;
        this.textValueList = new ArrayList<>(other.textValueList);
        for (final KeyValuePair pair : other.objectValueList) {
            this.objectValueList.add(new KeyValuePair(pair));
        }
    }

    /**
     * Check if this input is for a given field and is not blank.
     */
    public boolean isFilledSearchField(final String fieldName) {
        return fieldName.equals(getFieldName()) && StringUtils.isNotBlank(getTextValue());
    }
}
