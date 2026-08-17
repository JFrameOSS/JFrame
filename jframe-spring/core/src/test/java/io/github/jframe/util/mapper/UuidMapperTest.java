package io.github.jframe.util.mapper;

import io.github.support.UnitTest;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link UuidMapper}.
 *
 * <p>Verifies the null-safe UUID → String conversion.
 */
@DisplayName("Utility - UuidMapper")
class UuidMapperTest extends UnitTest {

    private UuidMapper uuidMapper;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        uuidMapper = new UuidMapper();
    }

    @Test
    @DisplayName("Should return null when UUID input is null")
    void shouldReturnNullWhenUuidIsNull() {
        // Given: A null UUID
        final UUID input = null;

        // When: Converting to string
        final String result = uuidMapper.toString(input);

        // Then: Null is returned without exception
        assertThat(result, is(nullValue()));
    }

    @Test
    @DisplayName("Should return standard UUID string representation for a valid UUID")
    void shouldReturnStandardStringForValidUuid() {
        // Given: A known UUID
        final UUID input = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // When: Converting to string
        final String result = uuidMapper.toString(input);

        // Then: Standard hyphenated lowercase string is returned
        assertThat(result, is(notNullValue()));
        assertThat(result, is("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    @DisplayName("Should round-trip a randomly generated UUID without data loss")
    void shouldRoundTripRandomUuid() {
        // Given: A random UUID
        final UUID input = UUID.randomUUID();

        // When: Converting to string and back
        final String result = uuidMapper.toString(input);

        // Then: Parsing the result reproduces the original UUID
        assertThat(result, is(notNullValue()));
        assertThat(UUID.fromString(result), is(input));
    }

    @Test
    @DisplayName("Should produce a UUID string matching the standard 8-4-4-4-12 format")
    void shouldProduceStringMatchingUuidFormat() {
        // Given: A known UUID
        final UUID input = UUID.randomUUID();

        // When: Converting to string
        final String result = uuidMapper.toString(input);

        // Then: Result matches the standard UUID regex pattern
        assertThat(
            result,
            matchesRegex(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            )
        );
    }
}
