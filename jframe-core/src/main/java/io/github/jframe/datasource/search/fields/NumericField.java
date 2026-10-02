package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;

/**
 * Indicates the search criterium is a numeric field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class NumericField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 2309426883656091433L;

    private int value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     * @param value      the integer value, optionally prefixed with {@code !}.
     * @throws InvalidFieldValueException when the value is present but not an integer.
     */
    public NumericField(final String columnName, final String value) {
        super(columnName, SearchType.NUMERIC);
        if (StringUtils.isNotBlank(value)) {
            final boolean inverse = value.startsWith("!");
            setInverse(inverse);
            this.value = parse(inverse ? value.substring(1) : value, value);
        }
    }

    private static int parse(final String number, final String raw) {
        try {
            return Integer.parseInt(number);
        } catch (final NumberFormatException exception) {
            throw new InvalidFieldValueException("Invalid integer value", raw, exception);
        }
    }

}
