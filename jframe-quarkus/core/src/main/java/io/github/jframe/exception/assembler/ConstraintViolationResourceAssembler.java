package io.github.jframe.exception.assembler;

import io.github.jframe.exception.resource.ValidationErrorResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.SerializationConfig;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;

/**
 * Assembler that converts a {@link Set} of {@link ConstraintViolation} to a list of {@link ValidationErrorResource}.
 *
 * <p>Applies the {@link PropertyNamingStrategy} configured on the {@link ObjectMapper}
 * to field names only. Error codes are taken from the annotation's simple name as-is.
 */
@ApplicationScoped
public class ConstraintViolationResourceAssembler {

    /** The Jackson ObjectMapper (Jackson 3.x). */
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new assembler with the given ObjectMapper.
     *
     * @param objectMapper the Jackson 3.x ObjectMapper
     */
    @Inject
    public ConstraintViolationResourceAssembler(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a set of {@link ConstraintViolation} to a list of {@link ValidationErrorResource}.
     *
     * @param violations the constraint violations to convert
     * @return the converted validation error resources
     */
    public List<ValidationErrorResource> convert(final Set<ConstraintViolation<?>> violations) {
        final SerializationConfig config = objectMapper.serializationConfig();
        final PropertyNamingStrategy namingStrategy = config.getPropertyNamingStrategy();

        final List<ValidationErrorResource> result = new ArrayList<>();
        for (final ConstraintViolation<?> violation : violations) {
            final ValidationErrorResource resource = new ValidationErrorResource();
            final String rawField = violation.getPropertyPath().toString();
            resource.setField(PropertyNamingUtil.translateName(namingStrategy, config, rawField));
            final Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
            resource.setCode(resolveAnnotationSimpleName(annotation));
            result.add(resource);
        }
        return result;
    }

    private String resolveAnnotationSimpleName(final Annotation annotation) {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationType != null) {
            return annotationType.getSimpleName();
        }

        // Fallback for mocks/proxies: use first implemented interface
        final Class<?>[] interfaces = annotation.getClass().getInterfaces();
        return interfaces.length > 0 ? interfaces[0].getSimpleName() : annotation.getClass().getSimpleName();
    }
}
