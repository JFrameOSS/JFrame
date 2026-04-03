package io.github.jframe.datasource.logging;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;

import java.util.List;

/**
 * Default query execution listener that logs SQL queries via SLF4J with suppression support.
 *
 * <p>Accepts a pluggable {@link DefaultQueryLogEntryCreator} for formatting. When no
 * creator is provided, uses the datasource-proxy default.
 */
@Slf4j
public class DefaultQueryExecutionListener extends SLF4JQueryLoggingListener {

    /** Creates a new listener with the default query log entry creator. */
    public DefaultQueryExecutionListener() {
        super();
    }

    /**
     * Creates a new listener with the specified query log entry creator.
     *
     * @param entryCreator the entry creator to use for formatting SQL queries
     */
    public DefaultQueryExecutionListener(final DefaultQueryLogEntryCreator entryCreator) {
        super();
        entryCreator.setMultiline(true);
        this.setQueryLogEntryCreator(entryCreator);
    }

    @Override
    public void beforeQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
        if (SqlStatementLogging.isSuppressed()) {
            log.debug("SQL statement logging is currently suppressed; skipping logging for this query.");
        }
    }
}
