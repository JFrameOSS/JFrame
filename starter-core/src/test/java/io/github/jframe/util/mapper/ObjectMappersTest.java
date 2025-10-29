package io.github.jframe.util.mapper;

import io.github.support.UnitTest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ObjectMappers}.
 *
 * <p>Verifies the ObjectMappers functionality including:
 * <ul>
 * <li>JSON serialization with toJson()</li>
 * <li>JSON deserialization with fromJson()</li>
 * <li>Handling of null values and unknown properties</li>
 * <li>DateTime serialization/deserialization</li>
 * <li>Generic type handling with TypeReference</li>
 * </ul>
 */
@DisplayName("Utility - ObjectMappers")
class ObjectMappersTest extends UnitTest {

    @Test
    @DisplayName("Should serialize valid object to JSON")
    void toJson_withValidObject_shouldSerializeToJson() {
        // Given: A test object with name, age, and email
        final TestData testData = new TestData("John Doe", 30, "john@example.com");

        // When: Serializing the object to JSON
        final String json = ObjectMappers.toJson(testData);

        // Then: JSON string contains all field values
        assertThat(json)
            .isNotEmpty()
            .contains("\"name\":\"John Doe\"")
            .contains("\"age\":30")
            .contains("\"email\":\"john@example.com\"");
    }

    @Test
    @DisplayName("Should return empty string when serializing null object")
    void toJson_withNullObject_shouldReturnEmptyString() {
        // Given: A null object
        // When: Serializing null to JSON
        final String json = ObjectMappers.toJson(null);

        // Then: Empty string is returned
        assertThat(json).isEmpty();
    }

    @Test
    @DisplayName("Should serialize object with null fields successfully")
    void toJson_withObjectContainingNullFields_shouldSerializeSuccessfully() {
        // Given: A test object with some null fields
        final TestData testData = new TestData("Jane Doe", null, null);

        // When: Serializing the object to JSON
        final String json = ObjectMappers.toJson(testData);

        // Then: JSON contains non-null fields and includes null values
        assertThat(json)
            .isNotEmpty()
            .contains("\"name\":\"Jane Doe\"")
            .contains("\"age\":null")
            .contains("\"email\":null");
    }

    @Test
    @DisplayName("Should serialize LocalDateTime as ISO-8601 format")
    void toJson_withDateTime_shouldSerializeAsIso8601() {
        // Given: A test object with LocalDateTime
        final LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        final TestDataWithDateTime testData = new TestDataWithDateTime("Test", dateTime);

        // When: Serializing the object to JSON
        final String json = ObjectMappers.toJson(testData);

        // Then: DateTime is serialized as ISO-8601 string, not as timestamp
        assertThat(json)
            .isNotEmpty()
            .contains("\"dateTime\":\"2024-01-15T10:30:45\"")
            .doesNotContain("timestamp");
    }

    @Test
    @DisplayName("Should serialize OffsetDateTime as ISO-8601 format with offset")
    void toJson_withOffsetDateTime_shouldSerializeAsIso8601WithOffset() {
        // Given: A test object with OffsetDateTime in UTC
        final OffsetDateTime dateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        final TestDataWithOffsetDateTime testData = new TestDataWithOffsetDateTime("Test", dateTime);

        // When: Serializing the object to JSON
        final String json = ObjectMappers.toJson(testData);

        // Then: DateTime is serialized as ISO-8601 string with Z offset
        assertThat(json)
            .isNotEmpty()
            .contains("\"dateTime\":\"2024-01-15T10:30:45Z\"");
    }

    @Test
    @DisplayName("Should deserialize valid JSON to object")
    void fromJson_withValidJson_shouldDeserializeToObject() {
        // Given: A valid JSON string
        final String json = "{\"name\":\"Alice\",\"age\":25,\"email\":\"alice@example.com\"}";

        // When: Deserializing JSON to TestData object
        final TestData result = ObjectMappers.fromJson(json, TestData.class);

        // Then: Object is created with all field values
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.age()).isEqualTo(25);
        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Should deserialize JSON with missing fields to object with null fields")
    void fromJson_withJsonMissingFields_shouldDeserializeWithNullFields() {
        // Given: A JSON string with only one field
        final String json = "{\"name\":\"Bob\"}";

        // When: Deserializing JSON to TestData object
        final TestData result = ObjectMappers.fromJson(json, TestData.class);

        // Then: Object is created with non-null name and null other fields
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Bob");
        assertThat(result.age()).isNull();
        assertThat(result.email()).isNull();
    }

    @Test
    @DisplayName("Should ignore unknown properties during deserialization")
    void fromJson_withJsonContainingUnknownProperties_shouldIgnoreUnknownProperties() {
        // Given: A JSON string with an unknown field
        final String json = "{\"name\":\"Charlie\",\"age\":35,\"email\":\"charlie@example.com\",\"unknownField\":\"value\"}";

        // When: Deserializing JSON to TestData object
        final TestData result = ObjectMappers.fromJson(json, TestData.class);

        // Then: Object is created with known fields, unknown field is ignored
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Charlie");
        assertThat(result.age()).isEqualTo(35);
        assertThat(result.email()).isEqualTo("charlie@example.com");
    }

    @Test
    @DisplayName("Should deserialize ISO-8601 string to LocalDateTime")
    void fromJson_withDateTime_shouldDeserializeFromIso8601() {
        // Given: A JSON string with ISO-8601 formatted datetime
        final String json = "{\"name\":\"Test\",\"dateTime\":\"2024-01-15T10:30:45\"}";

        // When: Deserializing JSON to TestDataWithDateTime object
        final TestDataWithDateTime result = ObjectMappers.fromJson(json, TestDataWithDateTime.class);

        // Then: Object is created with parsed LocalDateTime
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test");
        assertThat(result.dateTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 45));
    }

    @Test
    @DisplayName("Should throw AssertionError when deserializing invalid JSON")
    void fromJson_withInvalidJson_shouldThrowAssertionError() {
        // Given: An invalid JSON string
        final String invalidJson = "{invalid json}";

        // When/Then: Deserializing invalid JSON throws AssertionError with descriptive message
        assertThatThrownBy(() -> ObjectMappers.fromJson(invalidJson, TestData.class))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Failed to deserialize JSON");
    }

    @Test
    @DisplayName("Should deserialize JSON to generic List using TypeReference")
    void fromJson_withTypeReference_shouldDeserializeToGenericType() {
        // Given: A JSON array string and a TypeReference for List<TestData>
        final String json = "[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":30}]";
        final TypeReference<List<TestData>> typeRef = new TypeReference<>() {};

        // When: Deserializing JSON to List<TestData> using TypeReference
        final List<TestData> result = ObjectMappers.fromJson(json, typeRef);

        // Then: List is created with two TestData objects
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Alice");
        assertThat(result.get(0).age()).isEqualTo(25);
        assertThat(result.get(1).name()).isEqualTo("Bob");
        assertThat(result.get(1).age()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should deserialize JSON to Map using TypeReference")
    void fromJson_withMapTypeReference_shouldDeserializeToMap() {
        // Given: A JSON object string and a TypeReference for Map<String, String>
        final String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        final TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};

        // When: Deserializing JSON to Map using TypeReference
        final Map<String, String> result = ObjectMappers.fromJson(json, typeRef);

        // Then: Map is created with correct key-value pairs
        assertThat(result)
            .hasSize(2)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
    }

    @Test
    @DisplayName("Should throw AssertionError when deserializing invalid JSON with TypeReference")
    void fromJson_withInvalidJsonAndTypeReference_shouldThrowAssertionError() {
        // Given: An invalid JSON array string and a TypeReference
        final String invalidJson = "[{invalid}]";
        final TypeReference<List<TestData>> typeRef = new TypeReference<>() {};

        // When/Then: Deserializing invalid JSON with TypeReference throws AssertionError
        assertThatThrownBy(() -> ObjectMappers.fromJson(invalidJson, typeRef))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Failed to deserialize JSON");
    }

    @Test
    @DisplayName("Should serialize nested object structure")
    void toJson_withNestedObject_shouldSerializeNestedStructure() {
        // Given: A test object containing a nested object
        final TestData nestedData = new TestData("Nested", 20, "nested@example.com");
        final TestDataWithNested testData = new TestDataWithNested("Parent", nestedData);

        // When: Serializing the nested object to JSON
        final String json = ObjectMappers.toJson(testData);

        // Then: JSON contains both parent and nested object fields
        assertThat(json)
            .isNotEmpty()
            .contains("\"name\":\"Parent\"")
            .contains("\"nested\":")
            .contains("\"name\":\"Nested\"");
    }

    @Test
    @DisplayName("Should deserialize nested JSON structure")
    void fromJson_withNestedObject_shouldDeserializeNestedStructure() {
        // Given: A JSON string with nested object
        final String json = "{\"name\":\"Parent\",\"nested\":{\"name\":\"Child\",\"age\":10}}";

        // When: Deserializing nested JSON to TestDataWithNested object
        final TestDataWithNested result = ObjectMappers.fromJson(json, TestDataWithNested.class);

        // Then: Object is created with nested object populated
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Parent");
        assertThat(result.nested()).isNotNull();
        assertThat(result.nested().name()).isEqualTo("Child");
        assertThat(result.nested().age()).isEqualTo(10);
    }

    // Test data classes
    record TestData(String name, Integer age, String email) {
    }


    record TestDataWithDateTime(String name, LocalDateTime dateTime) {
    }


    record TestDataWithOffsetDateTime(String name, OffsetDateTime dateTime) {
    }


    record TestDataWithNested(String name, TestData nested) {
    }
}
