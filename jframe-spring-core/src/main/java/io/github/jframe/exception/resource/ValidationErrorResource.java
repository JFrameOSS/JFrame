package io.github.jframe.exception.resource;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Resource for validation errors.
 */
@Getter
@Setter
public class ValidationErrorResource {

    private String code;

    private String field;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
}
