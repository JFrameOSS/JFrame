package io.github.jframe.datasource.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum to describe how multiple search criteria are combined.
 */
@Getter
@AllArgsConstructor
@Schema(description = "SearchOperator")
public enum SearchOperator {

    AND,
    OR

}
