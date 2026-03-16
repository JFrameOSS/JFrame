package io.github.jframe.logging.voter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;

/**
 * Voter for JAX-RS logging filters. Combines {@link MediaTypeVoter} and {@link RequestVoter}
 * decisions to determine whether a filter should be active for the current request.
 *
 * <p>Results are cached in the {@link ContainerRequestContext} properties under a
 * package-qualified key so that multiple filters sharing a voter instance pay the evaluation
 * cost only once per request.
 */
@RequiredArgsConstructor
@Slf4j
public class FilterVoter {

    /** Property key suffix stored in the request context for caching the decision. */
    private static final String CACHE_KEY_SUFFIX = ".FILTER_VOTER";

    /** The media type voter. */
    private final MediaTypeVoter mediaTypeVoter;

    /** The request (path/method) voter. */
    private final RequestVoter requestVoter;

    /**
     * Returns {@code true} if the filter should be enabled for the given request context.
     *
     * <p>The result is cached in the request context under a package-qualified property key so
     * that repeated calls within the same request lifecycle return the cached value without
     * re-evaluating the voters.
     *
     * @param requestContext the JAX-RS container request context
     * @return {@code true} if both voters allow the request; {@code false} otherwise
     */
    public boolean enabled(final ContainerRequestContext requestContext) {
        final String key = this.getClass().getPackageName() + CACHE_KEY_SUFFIX;
        final Boolean cached = (Boolean) requestContext.getProperty(key);

        log.trace("Got cached value '{}' from property '{}'.", cached, key);

        if (cached != null) {
            return cached;
        }

        final MediaType mediaType = requestContext.getMediaType();
        final String contentType = mediaType != null ? mediaType.toString() : null;
        final String method = requestContext.getMethod();
        final String path = requestContext.getUriInfo().getPath();

        final boolean mediaTypeAllowed = mediaTypeVoter.matches(contentType);
        final boolean requestAllowed = requestVoter.allowed(method, path);
        final boolean isEnabled = mediaTypeAllowed && requestAllowed;

        log.trace(
            "Filter decision: mediaType='{}' allowed='{}', request='{} {}' allowed='{}' \u2192 enabled='{}'.",
            contentType,
            mediaTypeAllowed,
            method,
            path,
            requestAllowed,
            isEnabled
        );

        requestContext.setProperty(key, isEnabled);
        return isEnabled;
    }
}
