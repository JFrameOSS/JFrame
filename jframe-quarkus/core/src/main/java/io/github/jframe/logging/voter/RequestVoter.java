package io.github.jframe.logging.voter;

import io.github.jframe.logging.model.PathDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Request voter allows configuration of excluded URL requests based on path patterns and HTTP methods.
 *
 * <p>Returns {@code true} (allowed) when no exclusion matches. Returns {@code false} (excluded) as
 * soon as any configured {@link PathDefinition} matches the given method and path.
 */
@Slf4j
public class RequestVoter {

    /** The configured exclusion path definitions. */
    private final List<PathDefinition> excludePaths;

    /**
     * Constructs a new {@code RequestVoter}.
     *
     * @param excludePaths the list of path/method exclusion definitions (may be null or empty)
     */
    public RequestVoter(final List<PathDefinition> excludePaths) {
        this.excludePaths = excludePaths;
        log.debug("Configured excluded paths: '{}'.", excludePaths);
    }

    /**
     * Returns {@code true} if the request is allowed (not excluded).
     *
     * @param method the HTTP method (e.g. "GET", "POST")
     * @param path   the request path (e.g. "/api/users")
     * @return {@code true} if no exclusion matches; {@code false} if any exclusion matches
     */
    public boolean allowed(final String method, final String path) {
        boolean allowed = true;

        if (excludePaths != null && !excludePaths.isEmpty()) {
            allowed = isNotExcluded(method, path);
        }

        return allowed;
    }

    private boolean isNotExcluded(final String method, final String path) {
        boolean notExcluded = true;

        for (final PathDefinition exclusion : excludePaths) {
            final boolean excluded = exclusion.matches(method, path);
            log.trace("Request '{} {}' matches exclusion '{}': '{}'.", method, path, exclusion, excluded);
            if (excluded) {
                log.debug("Request '{} {}' excluded by pattern '{}'.", method, path, exclusion);
                notExcluded = false;
            }
        }

        return notExcluded;
    }
}
