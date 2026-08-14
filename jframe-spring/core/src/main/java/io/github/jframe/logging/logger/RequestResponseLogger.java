package io.github.jframe.logging.logger;

import io.github.jframe.logging.wrapper.BufferedClientHttpResponse;
import io.github.jframe.logging.wrapper.ResettableHttpServletRequest;
import io.github.jframe.logging.wrapper.WrappedContentCachingResponse;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;

/** Responsible for logging Http requests and responses. */
public interface RequestResponseLogger {

    /**
     * Returns {@code true} when DEBUG logging is enabled.
     *
     * <p>The default implementation returns {@code true} so that existing custom implementations
     * continue to compile and keep their current (always-capture) behaviour.
     *
     * @return {@code true} if DEBUG logging is enabled
     */
    default boolean isDebugEnabled() {
        return true;
    }

    /**
     * Log the request.
     *
     * @param request The request.
     * @param body    The body.
     */
    void logRequest(HttpRequest request, byte[] body);

    /**
     * Log the request.
     *
     * @param wrappedRequest The request.
     * @throws IOException in case of an error.
     */
    void logRequest(ResettableHttpServletRequest wrappedRequest) throws IOException;

    /**
     * Log the response.
     *
     * @param response The response to log.
     * @throws IOException in case of an error.
     */
    void logResponse(BufferedClientHttpResponse response) throws IOException;

    /**
     * Log the response.
     *
     * @param servletRequest  The request.
     * @param wrappedResponse The response.
     * @throws IOException in case of an error.
     */
    void logResponse(HttpServletRequest servletRequest, WrappedContentCachingResponse wrappedResponse)
        throws IOException;
}
