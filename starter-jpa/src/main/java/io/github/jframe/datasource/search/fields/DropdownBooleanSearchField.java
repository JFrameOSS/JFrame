package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * describes a search from a dropdown i.e. with a fixed string value to match exactly.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DropdownBooleanSearchField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 482074504831496597L;

    private Boolean value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public DropdownBooleanSearchField(final String columnName) {
        super(columnName, SearchType.DROPDOWN_BOOLEAN);
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
