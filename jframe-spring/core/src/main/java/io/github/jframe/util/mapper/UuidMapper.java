package io.github.jframe.util.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link UUID} to {@link String} conversion.
 *
 * <p>Registered in {@link io.github.jframe.util.mapper.config.SharedMapperConfig} via
 * {@code uses}, so any consumer mapper that declares {@code config = SharedMapperConfig.class}
 * automatically gains null-safe {@code UUID → String} conversion without additional configuration.
 *
 * <p>Uses {@code componentModel = "spring"} directly (not via {@link
 * io.github.jframe.util.mapper.config.SharedMapperConfig}) to avoid a MapStruct compile-time
 * warning about self-reference in the {@code uses} registry.
 */
@Mapper(componentModel = "spring")
public class UuidMapper {

    /**
     * Converts a {@link UUID} to its standard hyphenated string representation.
     *
     * <p>Returns {@code null} when the input is {@code null}, matching the null-safety
     * contract of every other method in this package.
     *
     * @param uuid the UUID to convert, may be {@code null}
     * @return the standard {@code 8-4-4-4-12} string representation, or {@code null}
     */
    public String toString(final UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return uuid.toString();
    }
}
