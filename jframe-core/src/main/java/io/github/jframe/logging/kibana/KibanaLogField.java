package io.github.jframe.logging.kibana;

import static java.util.Objects.nonNull;

/**
 * Interface that allows client projects to use their own log fields.
 */
@FunctionalInterface
public interface KibanaLogField {

    /**
     * Get the name with which this field will appear in the log.
     *
     * @return the log name of the field
     */
    String getLogName();

    /**
     * Check if a key matches.
     *
     * @param key the key to match
     * @return true if not null and if the supplied key equals this key.
     */
    default boolean matches(final String key) {
        return nonNull(key) && key.equalsIgnoreCase(getLogName());
    }
}
