package io.github.jframe.exception.assembler;

import lombok.experimental.UtilityClass;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.SerializationConfig;

/**
 * Utility for translating field names using a Jackson {@link PropertyNamingStrategy}.
 */
@UtilityClass
final class PropertyNamingUtil {

    /**
     * Translates the given name using the provided naming strategy.
     *
     * <p>Returns the original name unchanged if {@code name} or {@code namingStrategy} is null.
     *
     * @param namingStrategy the Jackson property naming strategy, may be null
     * @param config         the Jackson serialization config
     * @param name           the name to translate, may be null
     * @return the translated name, or the original name if translation is not applicable
     */
    static String translateName(
        final PropertyNamingStrategy namingStrategy,
        final SerializationConfig config,
        final String name) {

        if (name == null || namingStrategy == null) {
            return name;
        }
        return namingStrategy.nameForField(config, null, name);
    }
}
