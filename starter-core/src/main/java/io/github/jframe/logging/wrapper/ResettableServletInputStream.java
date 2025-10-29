package io.github.jframe.logging.wrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Input stream that can be 'reset', that is, the stream can be reset by supplying the (original) data again.
 */
public class ResettableServletInputStream extends ServletInputStream {

    /** The input stream to use. */
    private final InputStream stream;

    /** Flag to indicate that the stream is finished. */
    private boolean finished;

    /**
     * The constructor.
     *
     * @param rawData A copy of another servlet input stream.
     */
    public ResettableServletInputStream(final byte[] rawData) {
        super();
        stream = new ByteArrayInputStream(rawData);
    }

    @Override
    public int read() throws IOException {
        final int read = stream.read();
        if (read == -1) {
            finished = true;
        }
        return read;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(final ReadListener listener) {
        // ignored
    }

    /**
     * Set the input to use for the stream. This relies on the stream being a byte array input stream (or a stream that supports reset...)
     *
     * @throws IOException in case the stream cannot be reset.
     */
    @Override
    public void reset() throws IOException {
        stream.reset();
    }
}
