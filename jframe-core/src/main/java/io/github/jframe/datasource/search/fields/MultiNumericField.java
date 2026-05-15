package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Indicates the search criterium is a multiple select field with a list of numeric values to match exactly.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MultiNumericField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 3920481672565424226L;

    private List<Integer> values;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     * @param values     list of string values to parse as integers; unparseable values are skipped.
     */
    public MultiNumericField(final String columnName, final List<String> values) {
        super(columnName, SearchType.MULTI_NUMERIC);
        if (CollectionUtils.isEmpty(values)) {
            this.values = Collections.emptyList();
        } else {
            final List<Integer> parsed = new ArrayList<>();
            for (final String value : values) {
                try {
                    parsed.add(Integer.valueOf(value));
                } catch (final NumberFormatException ignored) {
                    // skip unparseable values
                }
            }
            this.values = parsed;
        }
    }
}
