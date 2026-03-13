package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;

/**
 * Indicates the search criterium is a date field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DateField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 8496928048956001967L;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     */
    public DateField(final String columnName, final String fromDate, final String toDate) {
        super(columnName, SearchType.DATE);
        if (nonNull(fromDate)) {
            this.fromDate = LocalDateTime.parse(fromDate, DateTimeFormatter.ISO_DATE_TIME);
        }
        if (nonNull(toDate)) {
            this.toDate = LocalDateTime.parse(toDate, DateTimeFormatter.ISO_DATE_TIME);
        }
    }
}
