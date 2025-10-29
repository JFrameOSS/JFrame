package io.github.jframe.logging.logger;

import io.github.jframe.logging.wrapper.ResettableHttpServletRequest;
import io.github.jframe.logging.wrapper.WrappedContentCachingResponse;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/** Responsible for logging Http requests and responses. */
public interface RequestResponseLogger {

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
    void logResponse(ClientHttpResponse response) throws IOException;

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
