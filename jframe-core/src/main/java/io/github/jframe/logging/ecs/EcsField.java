package io.github.jframe.logging.ecs;

import static java.util.Objects.nonNull;

/**
 * Interface that allows client projects to use their own log fields.
 */
@FunctionalInterface
public interface EcsField {

    /**
     * Get the ECS field key for this log field.
     *
     * <p>This key is used as the MDC key for structured logging and as the span attribute key
     * for OpenTelemetry tracing.
     *
     * @return the ECS field key
     */
    String getKey();

    /**
     * Check if a key matches.
     *
     * @param key the key to match
     * @return true if not null and if the supplied key equals this key.
     */
    default boolean matches(final String key) {
        return nonNull(key) && key.equalsIgnoreCase(getKey());
    }
}
