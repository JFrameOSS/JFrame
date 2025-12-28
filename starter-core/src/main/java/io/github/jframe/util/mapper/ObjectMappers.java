package io.github.jframe.util.mapper;


import io.github.jframe.exception.core.InternalServerErrorException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static java.util.Objects.isNull;

/**
 * Utility class for serializing and deserializing objects to and from JSON.
 */
@Slf4j
@UtilityClass
public class ObjectMappers {

    private static final ObjectMapper MAPPER = createObjectMapper();

    /**
     * Creates a new ObjectMapper instance with the necessary configuration.
     *
     * @return a configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
            // .addModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Not found in Jackson 3
            .build();
    }

    /**
     * Serializes an object to JSON.
     *
     * @param object the object to serialize
     * @return the JSON representation of the object, or an empty string if the object is null
     */
    public static String toJson(final Object object) {
        try {
            return isNull(object)
                ? ""
                : MAPPER.writeValueAsString(object);
        } catch (final JacksonException exception) {
            log.error("Failed to serialize object to JSON: {}", object.getClass().getSimpleName(), exception);
            return "";
        }
    }

    /**
     * Serializes an object to pretty-printed JSON.
     *
     * @param json        the JSON string to pretty-print
     * @param targetClass the class of the object to deserialize
     * @param <T>         the factory of the object
     * @return the deserialized object
     */
    public static <T> T fromJson(final String json, final Class<T> targetClass) {
        try {
            return MAPPER.readValue(json, targetClass);
        } catch (final JacksonException exception) {
            throw new InternalServerErrorException("Failed to deserialize JSON to class " + targetClass.getSimpleName(), exception);
        }
    }

    /**
     * Deserializes a JSON string into an object of the specified factory reference.
     *
     * @param json    the JSON string to deserialize
     * @param typeRef the factory reference representing the target factory
     * @param <T>     the factory of the object
     * @return the deserialized object
     */
    public static <T> T fromJson(final String json, final TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (final JacksonException exception) {
            throw new InternalServerErrorException("Failed to deserialize JSON to reference " + typeRef.getType(), exception);
        }
    }
}
