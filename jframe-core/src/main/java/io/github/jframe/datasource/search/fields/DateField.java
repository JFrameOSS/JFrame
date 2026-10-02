package io.github.jframe.datasource.search.fields;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.SearchCriterium;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;

/**
 * Indicates the search criterium is a date field.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class DateField extends SearchCriterium {

    @Serial
    private static final long serialVersionUID = 8496928048956001967L;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    /**
     * default constructor.
     *
     * @param columnName connected database column name.
     * @param fromDate   ISO date-time lower bound; blank means no bound.
     * @param toDate     ISO date-time upper bound; blank means no bound.
     * @throws InvalidFieldValueException when a bound is present but not an ISO date-time.
     */
    public DateField(final String columnName, final String fromDate, final String toDate) {
        super(columnName, SearchType.DATE);
        this.fromDate = parse(fromDate);
        this.toDate = parse(toDate);
    }

    private static LocalDateTime parse(final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (final DateTimeParseException exception) {
            throw new InvalidFieldValueException("Invalid ISO date-time", value, exception);
        }
    }
}
