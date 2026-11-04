package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;

/**
 * Search criterium for an is-null / is-not-null check on a column.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class NullCheckField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 6193852047718265381L;

    private boolean value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     * @param value      {@code true} (not null) or {@code false} (null), case-insensitive; blank means false.
     * @throws InvalidFieldValueException when the value is present but not true/false.
     */
    public NullCheckField(final String columnName, final String value) {
        super(columnName, SearchType.NULL_CHECK);
        if (StringUtils.isBlank(value)) {
            this.value = false;
        } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            this.value = Boolean.parseBoolean(value);
        } else {
            throw new InvalidFieldValueException("Invalid boolean value", value);
        }
    }

    /**
     * Retrieve the flag.
     *
     * @return {@code true} when the column must be not null.
     */
    public Boolean isValue() {
        return value;
    }
}
