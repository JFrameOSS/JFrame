package io.github.jframe.datasource.listener.logger;

import lombok.RequiredArgsConstructor;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;

/**
 * A custom query log entry creator that formats the query in a pretty way.
 */
@RequiredArgsConstructor
public class PrettyQueryEntryCreator extends DefaultQueryLogEntryCreator {

    private final Formatter formatter = FormatStyle.HIGHLIGHT.getFormatter();

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatQuery(final String query) {
        return this.formatter.format(query);
    }
}
