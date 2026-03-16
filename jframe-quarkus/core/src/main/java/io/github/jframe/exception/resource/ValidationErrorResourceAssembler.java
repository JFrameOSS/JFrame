package io.github.jframe.exception.resource;

import io.github.jframe.validation.ValidationError;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.SerializationConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembler that converts {@link ValidationError} list to {@link ValidationErrorResource} list.
 *
 * <p>Applies the {@link PropertyNamingStrategy} configured on the {@link ObjectMapper}
 * to both field names and error codes.
 */
public class ValidationErrorResourceAssembler {

    /** The Jackson ObjectMapper (Jackson 3.x). */
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new assembler with the given ObjectMapper.
     *
     * @param objectMapper the Jackson 3.x ObjectMapper
     */
    public ValidationErrorResourceAssembler(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a list of {@link ValidationError} to a list of {@link ValidationErrorResource}.
     *
     * @param errors the validation errors to convert
     * @return the converted validation error resources
     */
    public List<ValidationErrorResource> convert(final List<ValidationError> errors) {
        final SerializationConfig config = objectMapper.serializationConfig();
        final PropertyNamingStrategy namingStrategy = config.getPropertyNamingStrategy();

        final List<ValidationErrorResource> result = new ArrayList<>();
        for (final ValidationError error : errors) {
            final ValidationErrorResource resource = new ValidationErrorResource();
            resource.setField(PropertyNamingUtil.translateName(namingStrategy, config, error.getField()));
            resource.setCode(PropertyNamingUtil.translateName(namingStrategy, config, error.getCode()));
            result.add(resource);
        }
        return result;
    }
}
