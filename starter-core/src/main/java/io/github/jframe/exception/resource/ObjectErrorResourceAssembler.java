package io.github.jframe.exception.resource;

import io.github.jframe.util.converter.AbstractModelConverter;

import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import static java.util.Objects.requireNonNull;

/**
 * Assembler for object error resources.
 */
@Component
public class ObjectErrorResourceAssembler extends AbstractModelConverter<ObjectError, ValidationErrorResource> {

    private final ObjectMapper objectMapper;

    /** Constructor with an {@code objectMapper}. */
    public ObjectErrorResourceAssembler(final ObjectMapper objectMapper) {
        super(ValidationErrorResource.class);
        this.objectMapper = requireNonNull(objectMapper);
    }

    @Override
    public void convert(final ObjectError objectError, final ValidationErrorResource resource) {
        requireNonNull(objectError);
        final String field;
        if (objectError instanceof final FieldError fieldError) {
            field = convertProperty(fieldError.getField());
        } else {
            field = objectError.getObjectName();
        }
        final String code = convertProperty(objectError.getCode());
        resource.setField(field);
        resource.setCode(code);
    }

    /**
     * Converts the given property name (field name or error code) using the application defined {@link PropertyNamingStrategy} for
     * consistent output in responses. The naming strategy is defined in {@code application.yml} via the
     * {@code spring.jackson.property-naming-strategy} property.
     *
     * <p>For example, if the {@link SnakeCaseStrategy} is defined, the following field names and
     * error codes will be translated as following:
     *
     * <ul>
     * <li>description -&gt; description
     * <li>price -&gt; price
     * <li>discountPrice -&gt; discount_price
     * <li>Required -&gt; required
     * <li>InvalidLength -&gt; invalid_length
     * </ul>
     */
    protected String convertProperty(final String propertyName) {
        final String name;
        if (objectMapper == null || propertyName == null || propertyName.isEmpty()) {
            name = propertyName;
        } else {
            // retrieve the application defined property naming strategy from the object mapper's
            // serialization config
            final PropertyNamingStrategy propertyNamingStrategy =
                objectMapper.getSerializationConfig().getPropertyNamingStrategy();
            if (propertyNamingStrategy == null) {
                name = propertyName;
            } else {
                name = propertyNamingStrategy.nameForField(null, null, propertyName);
            }
        }
        return name;
    }
}
