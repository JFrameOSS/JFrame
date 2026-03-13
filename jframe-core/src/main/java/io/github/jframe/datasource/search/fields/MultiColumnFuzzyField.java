package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Indicates the search criterium is a multi-column fuzzy text field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MultiColumnFuzzyField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 4790820671565424226L;

    private String value;

    /**
     * default constructor.
     *
     * @param columnNames connected database column names.
     */
    public MultiColumnFuzzyField(final List<String> columnNames, final String value) {
        super(columnNames, SearchType.MULTI_COLUMN_FUZZY);
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
