package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * Indicates the search criterium is a boolean field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BooleanField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 482074504831496597L;

    private Boolean value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public BooleanField(final String columnName) {
        super(columnName, SearchType.BOOLEAN);
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
