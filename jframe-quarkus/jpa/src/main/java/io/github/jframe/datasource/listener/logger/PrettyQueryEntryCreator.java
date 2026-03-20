package io.github.jframe.datasource.listener.logger;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;

/**
 * A custom query log entry creator that formats SQL queries using a configurable
 * Hibernate {@link FormatStyle}.
 */
public class PrettyQueryEntryCreator extends DefaultQueryLogEntryCreator {

    private final Formatter formatter;

    /** Creates a new entry creator with {@link FormatStyle#NONE} formatting. */
    public PrettyQueryEntryCreator() {
        this(FormatStyle.NONE);
    }

    /**
     * Creates a new entry creator with the specified format style.
     *
     * @param formatStyle the Hibernate format style to use for SQL formatting
     */
    public PrettyQueryEntryCreator(final FormatStyle formatStyle) {
        this.formatter = formatStyle.getFormatter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatQuery(final String query) {
        return this.formatter.format(query);
    }
}
