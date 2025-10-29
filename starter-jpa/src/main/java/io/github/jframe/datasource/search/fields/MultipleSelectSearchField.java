package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

/**
 * describes a search from a multiple select i.e. with a list of fixed strings value to match exactly.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MultipleSelectSearchField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 4790820671565424226L;

    private List<String> values;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public MultipleSelectSearchField(final String columnName) {
        super(columnName, SearchType.MULTIPLE_SELECT);
    }
}
