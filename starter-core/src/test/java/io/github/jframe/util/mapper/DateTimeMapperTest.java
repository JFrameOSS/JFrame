package io.github.jframe.util.mapper;

import io.github.support.UnitTest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DateTimeMapper}.
 *
 * <p>Verifies the DateTimeMapper functionality including:
 * <ul>
 * <li>LocalDateTime to ZonedDateTime conversion (UTC)</li>
 * <li>OffsetDateTime to ZonedDateTime conversion</li>
 * <li>String to OffsetDateTime parsing</li>
 * <li>ZonedDateTime to LocalDateTime conversion (UTC)</li>
 * <li>Null value handling</li>
 * </ul>
 */
@DisplayName("Utility - DateTimeMapper")
class DateTimeMapperTest extends UnitTest {

    private DateTimeMapper dateTimeMapper;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        dateTimeMapper = new DateTimeMapper();
    }

    @Test
    @DisplayName("Should convert LocalDateTime to ZonedDateTime in UTC")
    void toZonedDateTime_withLocalDateTime_shouldConvertToUtc() {
        // Given: A LocalDateTime instance
        final LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);

        // When: Converting LocalDateTime to ZonedDateTime
        final ZonedDateTime result = dateTimeMapper.toZonedDateTime(localDateTime);

        // Then: ZonedDateTime is created in UTC zone
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getMinute()).isEqualTo(30);
        assertThat(result.getSecond()).isEqualTo(45);
        assertThat(result.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Should return null when converting null LocalDateTime")
    void toZonedDateTime_withNullLocalDateTime_shouldReturnNull() {
        // Given: A null LocalDateTime
        final LocalDateTime localDateTime = null;

        // When: Converting null LocalDateTime to ZonedDateTime
        final ZonedDateTime result = dateTimeMapper.toZonedDateTime(localDateTime);

        // Then: Null is returned
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should convert OffsetDateTime to ZonedDateTime")
    void toZonedDateTime_withOffsetDateTime_shouldConvertToZonedDateTime() {
        // Given: An OffsetDateTime instance in UTC
        final OffsetDateTime offsetDateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC);

        // When: Converting OffsetDateTime to ZonedDateTime
        final ZonedDateTime result = dateTimeMapper.toZonedDateTime(offsetDateTime);

        // Then: ZonedDateTime is created with correct values
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getMinute()).isEqualTo(30);
        assertThat(result.getSecond()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should convert OffsetDateTime with non-UTC offset to ZonedDateTime")
    void toZonedDateTime_withOffsetDateTimeNonUtc_shouldConvertWithOffset() {
        // Given: An OffsetDateTime with +02:00 offset
        final OffsetDateTime offsetDateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.ofHours(2));

        // When: Converting OffsetDateTime to ZonedDateTime
        final ZonedDateTime result = dateTimeMapper.toZonedDateTime(offsetDateTime);

        // Then: ZonedDateTime preserves the offset
        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    @DisplayName("Should return null when converting null OffsetDateTime")
    void toZonedDateTime_withNullOffsetDateTime_shouldReturnNull() {
        // Given: A null OffsetDateTime
        final OffsetDateTime offsetDateTime = null;

        // When: Converting null OffsetDateTime to ZonedDateTime
        final ZonedDateTime result = dateTimeMapper.toZonedDateTime(offsetDateTime);

        // Then: Null is returned
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should parse ISO-8601 string to OffsetDateTime in UTC")
    void toOffsetDateTime_withValidString_shouldParseToUtc() {
        // Given: An ISO-8601 formatted datetime string
        final String timestamp = "2024-01-15T10:30:45";

        // When: Parsing string to OffsetDateTime
        final OffsetDateTime result = dateTimeMapper.toOffsetDateTime(timestamp);

        // Then: OffsetDateTime is created in UTC with correct values
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getMinute()).isEqualTo(30);
        assertThat(result.getSecond()).isEqualTo(45);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Should throw exception when parsing invalid datetime string")
    void toOffsetDateTime_withInvalidString_shouldThrowException() {
        // Given: An invalid datetime string
        final String invalidTimestamp = "invalid-date";

        // When/Then: Parsing invalid string throws DateTimeParseException
        assertThatThrownBy(() -> dateTimeMapper.toOffsetDateTime(invalidTimestamp))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should convert ZonedDateTime to LocalDateTime in UTC")
    void toLocalDateTime_withZonedDateTime_shouldConvertToUtc() {
        // Given: A ZonedDateTime in UTC
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC);

        // When: Converting ZonedDateTime to LocalDateTime
        final LocalDateTime result = DateTimeMapper.toLocalDateTime(zonedDateTime);

        // Then: LocalDateTime is created with UTC time values
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getMinute()).isEqualTo(30);
        assertThat(result.getSecond()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should convert ZonedDateTime with different timezone to LocalDateTime in UTC")
    void toLocalDateTime_withNonUtcZonedDateTime_shouldConvertToUtc() {
        // Given: A ZonedDateTime in Europe/Paris timezone (UTC+1 in winter)
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(2024, 1, 15, 11, 30, 45, 0, ZoneId.of("Europe/Paris"));

        // When: Converting ZonedDateTime to LocalDateTime
        final LocalDateTime result = DateTimeMapper.toLocalDateTime(zonedDateTime);

        // Then: LocalDateTime shows UTC time (1 hour earlier than Paris time in winter)
        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getMinute()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should return null when converting null ZonedDateTime")
    void toLocalDateTime_withNullZonedDateTime_shouldReturnNull() {
        // Given: A null ZonedDateTime
        final ZonedDateTime zonedDateTime = null;

        // When: Converting null ZonedDateTime to LocalDateTime
        final LocalDateTime result = DateTimeMapper.toLocalDateTime(zonedDateTime);

        // Then: Null is returned
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle midnight time correctly")
    void toZonedDateTime_withMidnight_shouldConvertCorrectly() {
        // Given: A LocalDateTime at midnight
        final LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 0, 0, 0);

        // When: Converting midnight LocalDateTime to ZonedDateTime
        final ZonedDateTime result = dateTimeMapper.toZonedDateTime(localDateTime);

        // Then: ZonedDateTime is created with midnight time
        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(0);
        assertThat(result.getMinute()).isEqualTo(0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Should handle end of day time correctly")
    void toZonedDateTime_withEndOfDay_shouldConvertCorrectly() {
        // Given: A LocalDateTime at 23:59:59
        final LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        // When: Converting end-of-day LocalDateTime to ZonedDateTime
        final ZonedDateTime result = dateTimeMapper.toZonedDateTime(localDateTime);

        // Then: ZonedDateTime is created with correct time
        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(23);
        assertThat(result.getMinute()).isEqualTo(59);
        assertThat(result.getSecond()).isEqualTo(59);
    }

    @Test
    @DisplayName("Should round-trip convert LocalDateTime to ZonedDateTime and back")
    void roundTripConversion_shouldPreserveValues() {
        // Given: A LocalDateTime instance
        final LocalDateTime original = LocalDateTime.of(2024, 1, 15, 14, 30, 45);

        // When: Converting LocalDateTime to ZonedDateTime and back
        final ZonedDateTime zonedDateTime = dateTimeMapper.toZonedDateTime(original);
        final LocalDateTime result = DateTimeMapper.toLocalDateTime(zonedDateTime);

        // Then: Original LocalDateTime is preserved
        assertThat(result).isEqualTo(original);
    }
}
