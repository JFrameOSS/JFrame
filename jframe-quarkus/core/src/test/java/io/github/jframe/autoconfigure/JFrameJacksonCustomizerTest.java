package io.github.jframe.autoconfigure;

import io.github.support.UnitTest;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for {@link JFrameJacksonCustomizer}.
 *
 * <p>Verifies that the produced {@link ObjectMapper} is configured with:
 * <ul>
 * <li>ISO-8601 date serialization (no timestamps)</li>
 * <li>Null fields always included in output</li>
 * <li>Unknown properties silently ignored on deserialization</li>
 * <li>lowerCamelCase property naming strategy</li>
 * <li>Compact (non-pretty-printed) output</li>
 * </ul>
 */
@DisplayName("Quarkus Core - JFrameJacksonCustomizer")
public class JFrameJacksonCustomizerTest extends UnitTest {

    private ObjectMapper objectMapper;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        final JFrameJacksonCustomizer customizer = new JFrameJacksonCustomizer();
        objectMapper = customizer.objectMapper();
    }

    // -------------------------------------------------------------------------
    // Test fixture
    // -------------------------------------------------------------------------

    /** Simple test payload covering all configuration axes. */
    static class TestPayload {

        public String name;
        public Integer value;
        public LocalDateTime dateTime;

        TestPayload() {}

        TestPayload(final String name, final Integer value, final LocalDateTime dateTime) {
            this.name = name;
            this.value = value;
            this.dateTime = dateTime;
        }
    }

    // -------------------------------------------------------------------------
    // Dates — ISO-8601
    // -------------------------------------------------------------------------


    @Nested
    @DisplayName("when serializing dates")
    class WhenSerializingDates {

        @Test
        @DisplayName("Should serialize LocalDateTime as ISO-8601 string, not numeric timestamp")
        public void shouldSerializeLocalDateTimeAsIso8601() throws Exception {
            // Given: A payload with a known LocalDateTime
            final LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
            final TestPayload payload = new TestPayload("test", 1, dateTime);

            // When: Serializing to JSON
            final String json = objectMapper.writeValueAsString(payload);

            // Then: Date appears as ISO-8601 text, not a number
            assertThat(json, containsString("2024-01-15T10:30:45"));
            assertThat(json, not(containsString("1705312245")));
        }
    }

    // -------------------------------------------------------------------------
    // Null inclusion
    // -------------------------------------------------------------------------


    @Nested
    @DisplayName("when serializing null fields")
    class WhenSerializingNullFields {

        @Test
        @DisplayName("Should include null fields as explicit null in JSON output")
        public void shouldIncludeNullFieldsInOutput() throws Exception {
            // Given: A payload where value and dateTime are null
            final TestPayload payload = new TestPayload("only-name", null, null);

            // When: Serializing to JSON
            final String json = objectMapper.writeValueAsString(payload);

            // Then: Null fields are present in the output
            assertThat(json, containsString("\"value\":null"));
            assertThat(json, containsString("\"dateTime\":null"));
        }
    }

    // -------------------------------------------------------------------------
    // Unknown properties
    // -------------------------------------------------------------------------


    @Nested
    @DisplayName("when deserializing unknown properties")
    class WhenDeserializingUnknownProperties {

        @Test
        @DisplayName("Should not throw when JSON contains unknown properties")
        public void shouldNotThrowOnUnknownProperties() throws Exception {
            // Given: JSON with an extra field not present in TestPayload
            final String json = "{\"name\":\"test\",\"value\":42,\"dateTime\":null,\"unknownField\":\"ignored\"}";

            // When: Deserializing (should not throw)
            final TestPayload result = objectMapper.readValue(json, TestPayload.class);

            // Then: Known fields are mapped correctly
            assertThat(result, is(notNullValue()));
            assertThat(result.name, is("test"));
        }
    }

    // -------------------------------------------------------------------------
    // Naming strategy — lowerCamelCase
    // -------------------------------------------------------------------------


    @Nested
    @DisplayName("when applying naming strategy")
    class WhenApplyingNamingStrategy {

        @Test
        @DisplayName("Should use lowerCamelCase for property names")
        public void shouldUseLowerCamelCasePropertyNames() throws Exception {
            // Given: A payload with a camelCase field (dateTime)
            final TestPayload payload = new TestPayload("test", 1, null);

            // When: Serializing to JSON
            final String json = objectMapper.writeValueAsString(payload);

            // Then: Field appears as camelCase (dateTime, not date_time or DateTime)
            assertThat(json, containsString("\"dateTime\""));
            assertThat(json, not(containsString("\"date_time\"")));
            assertThat(json, not(containsString("\"DateTime\"")));
        }
    }

    // -------------------------------------------------------------------------
    // Compact output — no pretty-printing
    // -------------------------------------------------------------------------


    @Nested
    @DisplayName("when writing output format")
    class WhenWritingOutputFormat {

        @Test
        @DisplayName("Should produce compact JSON without indentation or newlines")
        public void shouldProduceCompactJsonWithoutIndentation() throws Exception {
            // Given: A simple payload
            final TestPayload payload = new TestPayload("compact", 7, null);

            // When: Serializing to JSON
            final String json = objectMapper.writeValueAsString(payload);

            // Then: Output contains no newline or indentation characters
            assertThat(json, not(containsString("\n")));
            assertThat(json, not(containsString("  ")));
        }
    }

    // -------------------------------------------------------------------------
    // ObjectMapper bootstrap
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should produce a non-null ObjectMapper")
    public void shouldProduceNonNullObjectMapper() {
        // Given: A fresh customizer
        final JFrameJacksonCustomizer customizer = new JFrameJacksonCustomizer();

        // When: Calling objectMapper()
        final ObjectMapper mapper = customizer.objectMapper();

        // Then: Mapper is not null
        assertThat(mapper, is(notNullValue()));
    }
}
