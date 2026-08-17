package io.github.jframe.logging.wrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpUpgradeHandler;

/**
 * HttpServletRequestWrapper that allows resetting of the input stream.
 *
 * <p>The wrapper reads the full request body at construction time so the downstream
 * application can read it repeatedly via {@link #getInputStream()} / {@link #reset()}.
 * When constructed via the cap-aware constructor, an internal capped copy is kept for
 * logging purposes; the full body is always available to the application.
 */
public class ResettableHttpServletRequest extends HttpServletRequestWrapper {

    /** The original request. */
    private final HttpServletRequest request;

    /** The (wrapped) response. */
    private final HttpServletResponse response;

    /** The input stream we can reset. */
    private ResettableServletInputStream servletStream;

    /**
     * Full body bytes — always the complete request body regardless of any cap.
     * Initialised lazily on first {@link #getInputStream()} call (no-cap constructor)
     * or eagerly at construction time (cap-aware constructor).
     */
    private byte[] rawData;

    /**
     * Capped copy for logging. {@code null} when the no-cap constructor is used.
     * Contains at most {@code byteCap} bytes, or the full body if the body is
     * smaller than the cap or the cap is {@code -1} (unlimited).
     */
    private byte[] cachedBodyForLogging;

    /**
     * The constructor.
     *
     * @param request  The original request.
     * @param response The (wrapped) response.
     */
    public ResettableHttpServletRequest(final HttpServletRequest request, final HttpServletResponse response) {
        super(request);
        this.request = request;
        this.response = response;
    }

    /**
     * Cap-aware constructor. Reads the full body eagerly and stores a capped slice for
     * logging while keeping the complete body for the downstream application.
     *
     * @param request  The original request.
     * @param response The (wrapped) response.
     * @param byteCap  Maximum bytes to retain in the logging copy.
     *                 Use {@code -1} for unlimited; {@code 0} produces an empty logging copy.
     * @throws IOException if the request body cannot be read.
     */
    public ResettableHttpServletRequest(final HttpServletRequest request, final HttpServletResponse response, final int byteCap)
        throws IOException {
        super(request);
        this.request = request;
        this.response = response;
        this.rawData = request.getInputStream().readAllBytes();
        this.servletStream = new ResettableServletInputStream(rawData);
        this.cachedBodyForLogging = applyByteCap(rawData, byteCap);
    }

    /**
     * Reset the input stream, so we can read it again.
     *
     * @throws IOException in case reset fails.
     */
    public void reset() throws IOException {
        if (this.servletStream != null) {
            this.servletStream.reset();
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (servletStream == null) {
            rawData = copyRawData();
            servletStream = new ResettableServletInputStream(rawData);
        }
        return servletStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(
            new InputStreamReader(getInputStream(), request.getCharacterEncoding())
        );
    }

    /**
     * Returns the capped logging copy of the request body as a byte array.
     *
     * <p>This method is package-private intentionally — it is accessible to unit tests
     * in the same package without being part of the public API surface.
     *
     * @return capped body bytes for logging; {@code null} if the no-cap constructor was used.
     */
    byte[] getCachedBodyForLogging() {
        if (cachedBodyForLogging == null) {
            return new byte[0];
        }
        return Arrays.copyOf(cachedBodyForLogging, cachedBodyForLogging.length);
    }

    /**
     * Returns the full request body as an {@link InputStream}.
     * Used internally to expose the complete raw data for logging at the logger layer.
     *
     * @return an {@link InputStream} over the raw (un-capped) body bytes.
     */
    InputStream getRawInputStream() {
        if (rawData == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return new ByteArrayInputStream(rawData);
    }

    private byte[] copyRawData() throws IOException {
        return request.getInputStream().readAllBytes();
    }

    private static byte[] applyByteCap(final byte[] data, final int byteCap) {
        if (data == null || data.length == 0 || byteCap == 0) {
            return new byte[0];
        }
        final int len = byteCap == -1 ? data.length : Math.min(data.length, byteCap);
        return Arrays.copyOf(data, len);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(final Class<T> httpUpgradeHandlerClass) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);
        return request.upgrade(httpUpgradeHandlerClass);
    }
}
