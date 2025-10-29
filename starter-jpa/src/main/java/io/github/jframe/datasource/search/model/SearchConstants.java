package io.github.jframe.datasource.search.model;

import lombok.experimental.UtilityClass;

/**
 * A utility class containing commonly used constants for the Search module.
 */
public final class SearchConstants {

    private SearchConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Some consistent used constants.
     */
    @UtilityClass
    public static final class Character {

        public static final String ESCAPED_QUOTE = "\"";

        public static final String COMMA = ",";

        public static final String EMPTY = "";

        public static final String SPACE = " ";

        public static final String DASH = "-";

        public static final String SLASH = "/";

        public static final String PERCENTAGE = "%";

    }

}
