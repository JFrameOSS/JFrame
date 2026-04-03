package io.github.jframe.exception.resource;

import io.github.jframe.util.converter.AbstractModelConverter;
import io.github.jframe.validation.ValidationError;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategy;

import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Assembler for validation resources.
 */
@Component
public class ValidationErrorResourceAssembler extends AbstractModelConverter<ValidationError, ValidationErrorResource> {

    private final ObjectMapper objectMapper;

    /** Constructor with an {@code objectMapper}. */
    public ValidationErrorResourceAssembler(final ObjectMapper objectMapper) {
        super(ValidationErrorResource.class);
        this.objectMapper = requireNonNull(objectMapper);
    }

    @Override
    public void convert(final ValidationError validationError, final ValidationErrorResource resource) {
        requireNonNull(validationError);
        final String field = convertProperty(validationError.getField());
        final String code = convertProperty(validationError.getCode());
        resource.setField(field);
        resource.setCode(code);
    }

    /**
     * Converts the given property name (field name or error code) using the application defined {@link PropertyNamingStrategy} for
     * consistent output in responses. The naming strategy is defined in {@code application.yml} via the
     * {@code spring.jackson.property-naming-strategy} property.
     *
     * <p>For example, if the {@link
     * PropertyNamingStrategy.SnakeCaseStrategy} is defined, the following field names and error codes will be translated as following:
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
                objectMapper.serializationConfig().getPropertyNamingStrategy();
            if (propertyNamingStrategy == null) {
                name = propertyName;
            } else {
                name = propertyNamingStrategy.nameForField(null, null, propertyName);
            }
        }
        return name;
    }
}
