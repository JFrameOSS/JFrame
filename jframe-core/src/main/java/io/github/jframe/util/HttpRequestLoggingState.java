package io.github.jframe.util;

/** Auto closeable state of http request logging. */
public class HttpRequestLoggingState implements AutoCloseable {

    private final boolean previouslySuppressed = HttpRequestLogging.isSuppressed();

    /** Restore the suppression state that was active when this state was created. */
    @Override
    public void close() {
        HttpRequestLogging.restore(previouslySuppressed);
    }
}
