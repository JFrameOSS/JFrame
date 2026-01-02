package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

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
    public MultiTextField(final String columnName, final List<String> values) {
        super(columnName, SearchType.MULTI_TEXT);
        if (CollectionUtils.isNotEmpty(values)) {
            if (values.get(0).startsWith("!")) {
                setInverse(true);
                this.values = values.stream()
                    .map(val -> val.startsWith("!") ? val.substring(1) : val)
                    .toList();
            } else {
                this.values = values;
            }
        } else {
            this.values = Collections.emptyList();
        }
    }
}
