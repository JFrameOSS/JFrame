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

import org.apache.commons.lang3.StringUtils;

import static java.util.Objects.nonNull;

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

    private SearchOperator operator;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public MultiFuzzyField(final String columnName, final SearchOperator operator, final String value) {
        super(columnName, SearchType.MULTI_FUZZY);
        this.operator = nonNull(operator) ? operator : SearchOperator.AND;
        this.value = StringUtils.isNotBlank(value) ? value : null;
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
