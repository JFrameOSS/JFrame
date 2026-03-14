package io.github.jframe.logging.voter;

import io.github.jframe.autoconfigure.properties.LoggingProperties;
import io.github.jframe.logging.model.PathDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * Request voter allows configuration of excluded URL request based on paths patterns and HTTP methods.
 */
@Slf4j
@Component
public class RequestVoter {

    /** The excluded paths. */
    private final List<PathDefinition> exclusions;

    /**
     * The constructor.
     *
     * @param properties The properties.
     */
    public RequestVoter(final LoggingProperties properties) {
        exclusions = properties.getExcludePaths();
        log.info("Excluded paths '{}'.", exclusions);
    }

    /**
     * Return {@code true} if the {@code request} is allowed.
     *
     * @param request The request to check.
     * @return {@code true} if the {@code request} is allowed.
     */
    public boolean allowed(final HttpServletRequest request) {
        if (!isEmpty()) {
            final String method = request.getMethod();
            final String path = request.getServletPath();
            for (final PathDefinition exclusion : exclusions) {
                final boolean excluded = exclusion.matches(method, path);
                log.trace("Request'{} {}' matches '{}': '{}'.", method, path, exclusion, excluded);
                if (excluded) {
                    log.trace(
                        "Request'{} {}' excluded because of match with '{}'.",
                        method,
                        path,
                        exclusion
                    );
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isEmpty() {
        return exclusions == null || exclusions.isEmpty();
    }
}
