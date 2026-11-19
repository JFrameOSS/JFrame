package io.github.jframe.datasource.logging;

import io.github.support.UnitTest;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Datasource - DefaultQueryExecutionListener")
public class DefaultQueryExecutionListenerTest extends UnitTest {

    @Test
    @DisplayName("Should not write query log entry while SQL logging is suppressed")
    public void shouldNotLogQueryWhenSuppressed() {
        // Given: A listener with a debug-enabled logger and suppressed SQL logging
        final Logger logger = mock(Logger.class);
        lenient().when(logger.isDebugEnabled()).thenReturn(true);
        final DefaultQueryExecutionListener listener = new DefaultQueryExecutionListener();
        listener.setLogger(logger);
        final ExecutionInfo execInfo = new ExecutionInfo();
        final List<QueryInfo> queries = List.of(new QueryInfo("select 1"));

        // When: A query is executed under suppression
        final SqlLoggingState state = SqlStatementLogging.suppress();
        listener.beforeQuery(execInfo, queries);
        listener.afterQuery(execInfo, queries);
        state.close();

        // Then: No query log entry is written
        verify(logger, never()).debug(anyString());
    }

    @Test
    @DisplayName("Should write query log entry when SQL logging is not suppressed")
    public void shouldLogQueryWhenNotSuppressed() {
        // Given: A listener with a debug-enabled logger
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        final DefaultQueryExecutionListener listener = new DefaultQueryExecutionListener();
        listener.setLogger(logger);

        // When: A query is executed
        listener.afterQuery(new ExecutionInfo(), List.of(new QueryInfo("select 1")));

        // Then: The query log entry is written
        verify(logger).debug(anyString());
    }
}
