package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Describes a free text search field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DateSearchField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 8496928048956001967L;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public DateSearchField(final String columnName) {
        super(columnName, SearchType.DATE);
    }
}
