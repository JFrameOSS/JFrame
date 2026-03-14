package io.github.jframe.util.mapper.config;

import org.mapstruct.*;

/**
 * Shared MapStruct configuration.
 */
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SharedMapperConfig {
    // Just an empty interface for the mapper configuration.
}
