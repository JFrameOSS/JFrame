package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;

/**
 * Indicates the search criterium is a boolean field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class BooleanField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 482074504831496597L;

    private boolean value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     * @param value      {@code true} or {@code false}, case-insensitive; blank means false.
     * @throws InvalidFieldValueException when the value is present but not true/false.
     */
    public BooleanField(final String columnName, final String value) {
        super(columnName, SearchType.BOOLEAN);
        if (StringUtils.isBlank(value)) {
            this.value = false;
        } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            this.value = Boolean.parseBoolean(value);
        } else {
            throw new InvalidFieldValueException("Invalid boolean value", value);
        }
    }

    /**
     * Retrieve the boolean value.
     *
     * @return the boolean value.
     */
    public Boolean isValue() {
        return value;
    }
}
