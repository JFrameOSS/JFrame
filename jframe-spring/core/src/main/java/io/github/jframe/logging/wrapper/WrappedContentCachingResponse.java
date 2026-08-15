package io.github.jframe.logging.wrapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

/**
 * An extension of {@link ContentCachingResponseWrapper} that tracks redirects, streams,
 * and applies an optional logging cap.
 *
 * <p>All write paths ({@code getWriter()} and {@code getOutputStream()}) are handled by the
 * parent class, which buffers the complete response body. The client always receives the full
 * body via {@link #copyBodyToResponse()}.
 *
 * <p>To obtain a possibly-truncated copy for logging only, call {@link #getContentForLogging()}.
 * Do <em>not</em> override {@link #getContentAsByteArray()}; its inherited contract is
 * "all content written" and filters such as {@code ShallowEtagHeaderFilter} depend on it.
 */
@Slf4j
public class WrappedContentCachingResponse extends ContentCachingResponseWrapper {

    /** Flag to indicate that the response is a redirect. */
    @Getter
    private boolean redirect;

    /** Flag to indicate that the response is a stream. */
    @Getter
    private boolean streaming;

    /**
     * Maximum bytes to retain in the logging copy returned by {@link #getContentForLogging()}.
     * {@code -1} means unlimited (no truncation).
     */
    private final int contentCacheLimit;

    /**
     * No-cap constructor. Caches the full response body for client delivery.
     * {@link #getContentForLogging()} will return the complete body (no truncation).
     *
     * @param response The response to wrap.
     */
    public WrappedContentCachingResponse(final HttpServletResponse response) {
        super(response);
        this.contentCacheLimit = -1;
    }

    /**
     * Cap-aware constructor. The parent class still buffers the complete response body so
     * the client always receives it intact via {@link #copyBodyToResponse()}.
     * {@link #getContentForLogging()} returns at most {@code contentCacheLimit} bytes.
     *
     * <p>A value of {@code -1} means unlimited. A value of {@code 0} yields an empty
     * logging copy while still delivering the full body to the client.
     *
     * @param response          The response to wrap.
     * @param contentCacheLimit Maximum bytes to include in the logging copy.
     *                          Use {@code -1} for unlimited.
     */
    public WrappedContentCachingResponse(final HttpServletResponse response, final int contentCacheLimit) {
        super(response);
        this.contentCacheLimit = contentCacheLimit;
    }

    /**
     * Returns the response body bytes intended for logging only, possibly truncated to
     * {@code contentCacheLimit} bytes.
     *
     * <p>The client always receives the complete, unmodified body via
     * {@link #copyBodyToResponse()} — this method only affects the logging copy.
     * A cap of {@code 0} yields an empty array; {@code -1} yields the full body.
     *
     * @return a possibly-truncated copy of the buffered body, never {@code null}
     */
    public byte[] getContentForLogging() {
        final byte[] full = super.getContentAsByteArray();
        return (contentCacheLimit < 0 || full.length <= contentCacheLimit)
            ? full : Arrays.copyOf(full, contentCacheLimit);
    }

    @Override
    public void sendError(final int statusCode) throws IOException {
        redirect = true;
        super.sendError(statusCode);
    }

    @Override
    public void sendError(final int statusCode, final String message) throws IOException {
        redirect = true;
        super.sendError(statusCode, message);
    }

    @Override
    public void sendRedirect(final String location) throws IOException {
        redirect = true;
        super.sendRedirect(location);
    }

    @Override
    public void addHeader(final String name, final String value) {
        super.addHeader(name, value);
        if (isTextEventStreamHeader(name, value)) {
            log.debug("Triggered streaming for this content-cache response.");
            this.streaming = true;
        }
    }

    private static boolean isTextEventStreamHeader(final String name, final String value) {
        return CONTENT_TYPE.equals(name) && TEXT_EVENT_STREAM.equals(MediaType.valueOf(value));
    }

    @Override
    public void flushBuffer() throws IOException {
        if (streaming) {
            super.copyBodyToResponse(false);
            getResponse().flushBuffer();
        }
    }
}
