package io.github.jframe.datasource.search;

/**
 * Enum to describe different types of searchable fields.
 */
public enum SearchType {

    /* Does not use any operator */
    NONE,
    /* For use by a like-operator on a single string value */
    TEXT,
    /* For use by an in-operator on a set of string values */
    MULTIPLE_SELECT,
    /* Dropdown for a single value as a boolean */
    DROPDOWN_BOOLEAN,
    /* Dropdown for a single value as a string */
    DROPDOWN_STRING,
    /* For use by an equal-operator on a date value */
    DATE,
    /* For use by an equal-operator on a number value */
    NUMBER,
    /* For use by multiple like-operators on strings, which are ANDed together */
    MULTI_WORD,
    /* For use by an equal-operator on a single enum value */
    ENUM,
    /* For use by multiple like-operators on strings, which are ORed together */
    MULTI_FIELD_TEXT,
    /* For use by an in-operator on a set of enum values */
    MULTIPLE_ENUM;

}
