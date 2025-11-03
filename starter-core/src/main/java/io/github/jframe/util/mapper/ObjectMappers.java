package io.github.jframe.util.mapper;


import io.github.jframe.exception.core.InternalServerErrorException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
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
        } catch (final JsonProcessingException exception) {
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
        } catch (final JsonProcessingException exception) {
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
        } catch (final JsonProcessingException exception) {
            throw new InternalServerErrorException("Failed to deserialize JSON to reference " + typeRef.getType(), exception);
        }
    }
}
