package io.github.jframe.logging.filter.client;

import io.github.jframe.logging.LoggingConfig;
import io.github.jframe.logging.filter.FilterConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS client filter that logs outbound HTTP requests and their corresponding responses.
 *
 * <p>Logging is skipped when {@link LoggingConfig#disabled()} is {@code true} or when the
 * request URI path matches one of the configured {@link LoggingConfig#excludePaths()} patterns.
 * A boolean property is stored on the request context so that the response-side filter knows
 * whether the request was actually logged.
 */
@Provider
@ApplicationScoped
@Priority(300)
@Slf4j
public class OutboundLoggingFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final String PROPERTY_KEY =
        "io.github.jframe.logging.filter.client.OutboundLoggingFilter.LOGGING_ENABLED";

    private final LoggingConfig loggingConfig;
    private final FilterConfig filterConfig;

    /**
     * Creates a new {@code OutboundLoggingFilter} with the given logging and filter configuration.
     *
     * @param loggingConfig the logging configuration used to determine whether logging is enabled
     * @param filterConfig  the filter configuration used to determine whether the filter is enabled
     */
    public OutboundLoggingFilter(final LoggingConfig loggingConfig, final FilterConfig filterConfig) {
        this.loggingConfig = loggingConfig;
        this.filterConfig = filterConfig;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        if (!filterConfig.outboundLogging().enabled()) {
            return;
        }
        final URI uri = requestContext.getUri();
        final String method = requestContext.getMethod();
        final MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        final boolean enabled = isLoggingEnabled(uri);
        requestContext.setProperty(PROPERTY_KEY, enabled);
        if (enabled) {
            log.debug("[EXTERNAL] Outbound request is: {} {} {}", method, uri, headers);
        }
    }

    @Override
    public void filter(final ClientRequestContext requestContext,
        final ClientResponseContext responseContext) throws IOException {
        if (!filterConfig.outboundLogging().enabled()) {
            return;
        }
        if (isLoggingConfiguredAsEnabled() && Boolean.TRUE.equals(requestContext.getProperty(PROPERTY_KEY))) {
            log.debug(
                "[EXTERNAL] Incoming response is: {} {}",
                responseContext.getStatus(),
                responseContext.getHeaders()
            );
        }
    }

    private boolean isLoggingEnabled(final URI uri) {
        final boolean disabled = loggingConfig.disabled();
        final boolean excluded = isExcludedPath(uri.getPath());
        return !disabled && !excluded;
    }

    private boolean isLoggingConfiguredAsEnabled() {
        return !loggingConfig.disabled() && loggingConfig.excludePaths() != null;
    }

    private boolean isExcludedPath(final String path) {
        return loggingConfig.excludePaths().stream()
            .anyMatch(pattern -> matchesPattern(path, pattern));
    }

    private boolean matchesPattern(final String path, final String pattern) {
        if (pattern.endsWith("/*")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 2));
        }
        return path.equals(pattern);
    }
}
