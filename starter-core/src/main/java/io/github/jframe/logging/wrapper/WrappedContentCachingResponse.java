package io.github.jframe.logging.wrapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

/**
 * An extension of {@link ContentCachingResponseWrapper} that keeps track whether the response is a redirect.
 */
@Slf4j
@Getter
public class WrappedContentCachingResponse extends ContentCachingResponseWrapper {

    /** Flag to indicate that the response is a redirect. */
    private boolean redirect;

    /** Flag to indicate that the response is a stream. */
    private boolean streaming;

    /**
     * The constructor.
     *
     * @param response The response to wrap.
     */
    public WrappedContentCachingResponse(final HttpServletResponse response) {
        super(response);
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
            log.debug("Triggered streaming for this content-caching response.");
            this.streaming = true;
        }
    }

    private static boolean isTextEventStreamHeader(final String name, final String value) {
        return CONTENT_TYPE.equals(name) && TEXT_EVENT_STREAM.equals(MediaType.valueOf(value));
    }

    @Override
    public void flushBuffer() throws IOException {
        if (streaming) {
            copyBodyToResponse(false);
            getResponse().flushBuffer();
        }
    }
}
