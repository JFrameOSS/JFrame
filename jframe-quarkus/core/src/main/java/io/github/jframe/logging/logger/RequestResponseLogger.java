package io.github.jframe.logging.logger;

import io.github.jframe.logging.wrapper.CachingRequestContext;
import io.github.jframe.logging.wrapper.CachingResponseContext;

import jakarta.ws.rs.container.ContainerRequestContext;

/** Contract for logging incoming HTTP requests and outgoing HTTP responses. */
public interface RequestResponseLogger {

    /**
     * Returns {@code true} when DEBUG logging is enabled.
     *
     * <p>The default implementation returns {@code true} so that existing custom
     * implementations keep compiling and retain their current (always-log) behaviour.
     * Override in concrete implementations to gate on the actual logger level.
     *
     * @return {@code true} if DEBUG logging is enabled
     */
    default boolean isDebugEnabled() {
        return true;
    }

    /**
     * Logs the incoming HTTP request.
     *
     * @param request the caching request context
     */
    void logRequest(CachingRequestContext request);

    /**
     * Logs the outgoing HTTP response.
     *
     * @param request  the original container request context
     * @param response the caching response context
     */
    void logResponse(ContainerRequestContext request, CachingResponseContext response);
}
