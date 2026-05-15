package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * Indicates the search criterium is a numeric range field with optional from/to boundaries.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NumericRangeField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 7496928048956001967L;

    private Integer fromValue;

    private Integer toValue;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     * @param fromValue  lower bound (inclusive), may be null.
     * @param toValue    upper bound (inclusive), may be null.
     */
    public NumericRangeField(final String columnName, final Integer fromValue, final Integer toValue) {
        super(columnName, SearchType.NUMERIC_RANGE);
        this.fromValue = fromValue;
        this.toValue = toValue;
    }
}
