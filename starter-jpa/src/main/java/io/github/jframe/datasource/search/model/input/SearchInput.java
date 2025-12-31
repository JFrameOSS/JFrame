package io.github.jframe.datasource.search.model.input;

import io.github.jframe.datasource.search.SearchOperator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
        description = "Indicates the search operation of a multi-value search (e.g., AND, OR) - Default is AND",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "AND"
    )
    private SearchOperator operator = SearchOperator.AND;

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

    /**
     * A copy constructor that copies another SearchInput.
     */
    public SearchInput(final SearchInput other) {
        this.fieldName = other.fieldName;
        this.operator = other.operator;
        this.textValue = other.textValue;
        this.fromDateValue = other.fromDateValue;
        this.toDateValue = other.toDateValue;
        this.textValueList = new ArrayList<>(other.textValueList);
    }

    /**
     * A method to extract the text value as an integer.
     *
     * @return the integer value of textValue, or null if it cannot be parsed.
     */
    public Integer getTextValueAsInteger() {
        try {
            return Integer.valueOf(this.textValue);
        } catch (final NumberFormatException extract) {
            return null;
        }
    }
}
