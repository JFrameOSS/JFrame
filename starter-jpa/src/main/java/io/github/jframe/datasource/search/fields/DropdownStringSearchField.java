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
public class DropdownStringSearchField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 482074504831496597L;

    private String value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public DropdownStringSearchField(final String columnName) {
        super(columnName, SearchType.DROPDOWN_STRING);
    }
}
