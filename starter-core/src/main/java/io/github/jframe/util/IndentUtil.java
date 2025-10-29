package io.github.jframe.util;

import lombok.experimental.UtilityClass;

import static io.github.jframe.util.constants.Constants.Characters.SYSTEM_NEW_LINE;

/**
 * LogUtil to indent data.
 */
@UtilityClass
public class IndentUtil {

    /** The default indent to use if none specified. */
    public static final String DEFAULT_INDENTATION = "  ";

    /**
     * Indent the {@code value} with the default indent. See {@link IndentUtil#DEFAULT_INDENTATION}.
     *
     * @param value The value to indent.
     * @return An indented string.
     */
    public static String indent(final String value) {
        return indent(value, DEFAULT_INDENTATION);
    }

    /**
     * Indent the {@code value} with the given {@code indent}.
     *
     * @param value       The value to indent.
     * @param indentation The indentation.
     * @return An indented string.
     */
    public static String indent(final String value, final String indentation) {
        return indentation + value.replace(SYSTEM_NEW_LINE, String.format("%n%s", indentation));
    }
}
