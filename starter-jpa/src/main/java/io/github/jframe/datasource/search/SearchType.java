package io.github.jframe.datasource.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum to describe different types of searchable fields.
 */
@Getter
@AllArgsConstructor
@Schema(description = "SearchType")
public enum SearchType {

    NONE("Does not use any operator"),
    DATE("For use by an equal-operator on a date value"),
    NUMERIC("For use by an equal-operator on a numeric value"),
    BOOLEAN("For use by an equal-operator on a boolean value"),
    ENUM("For use by an equal-operator on a single enum value"),
    MULTI_ENUM("For use by an in-operator on a set of enum values"),
    TEXT("For use by an equal-operator on a single string value"),
    MULTI_TEXT("For use by an in-operator on a set of string values"),
    FUZZY_TEXT("For use by a like-operator on a single string value"),
    MULTI_FUZZY("For use by multiple like-operators on strings, which are combined depending on operator");

    private final String description;

}
