package io.github.jframe.datasource.logging;

/** Auto closeable state of sql statement logging. */
public class SqlLoggingState implements AutoCloseable {

    /** Stop suppressing the logging. */
    @Override
    public void close() {
        SqlStatementLogging.clear();
    }
}
