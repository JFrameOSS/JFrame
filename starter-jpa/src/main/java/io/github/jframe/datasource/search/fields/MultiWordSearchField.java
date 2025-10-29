package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

/**
 * describes a search from a multiword search text.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MultiWordSearchField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 4790820671565424226L;

    private String value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public MultiWordSearchField(final String columnName) {
        super(columnName, SearchType.MULTI_WORD);
    }

    /**
     * return multi words split on space character.
     */
    public List<String> getValues() {
        return Arrays.asList(value.split(" "));
    }

}
