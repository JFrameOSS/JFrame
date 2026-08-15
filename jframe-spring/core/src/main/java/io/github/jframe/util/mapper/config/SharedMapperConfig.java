package io.github.jframe.util.mapper.config;

import io.github.jframe.util.mapper.AmountMapper;
import io.github.jframe.util.mapper.UuidMapper;

import org.mapstruct.*;

/**
 * Shared MapStruct configuration — the implicit-conversion registry for all JFrame consumers.
 *
 * <p>Any mapper that declares {@code config = SharedMapperConfig.class} automatically
 * inherits the {@code uses} list below. Admission criterion for {@code uses} is
 * <em>"total and policy-free"</em>: a mapper listed here silently rewrites how every
 * consumer mapper handles its source/target types, with no opt-out.
 *
 * <h2>Why DateTimeMapper is deliberately absent from {@code uses}</h2>
 * <p>{@link io.github.jframe.util.mapper.DateTimeMapper} encodes a UTC assumption in
 * three of its four methods. Auto-applying {@code LocalDateTime → ZonedDateTime} across
 * a non-UTC estate would silently shift timestamps. Consumers opt in explicitly:
 * <pre>{@code
 * @Mapper(config = SharedMapperConfig.class, uses = DateTimeMapper.class)
 * }</pre>
 * Consumer-level {@code uses} merges with the config-level {@code uses} rather than
 * replacing it, so both the date conversion and the UUID conversion work together.
 */
@MapperConfig(
    uses = {
        UuidMapper.class,
        AmountMapper.class
    },
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SharedMapperConfig {
    // Marker interface for shared MapStruct configuration.
}
