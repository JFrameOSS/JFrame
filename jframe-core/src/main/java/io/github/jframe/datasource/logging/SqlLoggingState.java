package io.github.jframe.datasource.logging;

/** Auto closeable state of sql statement logging. */
public class SqlLoggingState implements AutoCloseable {

    private final boolean previouslySuppressed = SqlStatementLogging.isSuppressed();

    /** Restore the suppression state that was active when this state was created. */
    @Override
    public void close() {
        SqlStatementLogging.restore(previouslySuppressed);
    }
}
