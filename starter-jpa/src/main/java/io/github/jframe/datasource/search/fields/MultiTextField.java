package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

/**
 * Indicates the search criterium is a multiple select field with a list of fixed strings value to match exactly.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MultiTextField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 4790820671565424226L;

    private List<String> values;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public MultiTextField(final String columnName) {
        super(columnName, SearchType.MULTI_TEXT);
    }
}
