package io.github.jframe.datasource.listener;

import io.github.jframe.datasource.config.PrettyQueryEntryCreator;
import io.github.jframe.datasource.logging.SqlStatementLogging;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;

import java.util.List;

/** A listener for logging purposes. */
@Slf4j
public class DefaultQueryExecutionListener extends SLF4JQueryLoggingListener {

    /** Creates a new listener with pretty-printed, multiline SQL formatting. */
    public DefaultQueryExecutionListener() {
        super();
        final PrettyQueryEntryCreator prettyQueryEntryCreator = new PrettyQueryEntryCreator();
        prettyQueryEntryCreator.setMultiline(true);
        this.setQueryLogEntryCreator(prettyQueryEntryCreator);
    }

    @Override
    public void beforeQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
        if (SqlStatementLogging.isSuppressed()) {
            log.debug("SQL statement logging is currently suppressed; skipping logging for this query.");
        }
    }
}
