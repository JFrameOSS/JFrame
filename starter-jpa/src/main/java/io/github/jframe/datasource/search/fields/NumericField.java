package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * Indicates the search criterium is a numeric field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NumericField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 2309426883656091433L;

    private int value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public NumericField(final String columnName) {
        super(columnName, SearchType.NUMERIC);
    }

}
