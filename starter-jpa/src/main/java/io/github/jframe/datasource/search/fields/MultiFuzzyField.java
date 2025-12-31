package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchOperator;
import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

/**
 * Indicates the search criterium is a space separated multiple words fuzzy and field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MultiFuzzyField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 4790820671565424226L;

    private String value;

    private SearchOperator operator = SearchOperator.AND;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public MultiFuzzyField(final String columnName) {
        super(columnName, SearchType.MULTI_FUZZY);
    }

    /**
     * Retrieve the search terms by splitting the value by spaces.
     *
     * @return list of search terms.
     */
    public List<String> getSearchTerms() {
        return Arrays.asList(this.value.split("\\s+"));
    }
}
