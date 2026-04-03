package io.github.jframe.datasource.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum to describe how multiple search criteria are combined.
 */
@Schema(description = "SearchOperator")
public enum SearchOperator {

    AND,
    OR

}
