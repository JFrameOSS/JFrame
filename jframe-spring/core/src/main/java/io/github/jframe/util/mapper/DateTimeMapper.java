package io.github.jframe.util.mapper;

import io.github.jframe.util.mapper.config.SharedMapperConfig;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.mapstruct.Mapper;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.nonNull;

/**
 * Mapper that converts a local date time to a zoned date time.
 */
@Mapper(config = SharedMapperConfig.class)
public class DateTimeMapper {

    /**
     * Converts a LocalDateTime to a ZonedDateTime, assuming zone UTC.
     */
    public ZonedDateTime toZonedDateTime(final LocalDateTime localDateTime) {
        return nonNull(localDateTime)
            ? ZonedDateTime.of(localDateTime, UTC)
            : null;
    }

    /**
     * Converts a OffsetDateTime to a ZonedDateTime.
     */
    public ZonedDateTime toZonedDateTime(final OffsetDateTime offsetDateTime) {
        return nonNull(offsetDateTime)
            ? offsetDateTime.toZonedDateTime()
            : null;
    }

    /**
     * Converts a string to a OffsetDateTime.
     */
    public OffsetDateTime toOffsetDateTime(final String timestamp) {
        final LocalDateTime localDateTime = LocalDateTime.parse(timestamp);
        return OffsetDateTime.of(localDateTime, UTC);
    }

    /**
     * Converts the given ZonedDateTime to a LocalDateTime at the same instant.
     * <p>
     * If {@link ZonedDateTime} is null, returns null.
     */
    public static LocalDateTime toLocalDateTime(final ZonedDateTime zonedDateTime) {
        return nonNull(zonedDateTime)
            ? zonedDateTime.withZoneSameInstant(UTC).toLocalDateTime()
            : null;
    }
}
