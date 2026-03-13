package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;

/**
 * Indicates the search criterium is free text field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TextField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 482074504831496597L;

    private String value;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public TextField(final String columnName, final String value) {
        super(columnName, SearchType.TEXT);
        if (StringUtils.isNotBlank(value)) {
            if (value.startsWith("!")) {
                setInverse(true);
                this.value = value.substring(1);
            } else {
                this.value = value;
            }
        } else {
            this.value = null;
        }
    }
}
