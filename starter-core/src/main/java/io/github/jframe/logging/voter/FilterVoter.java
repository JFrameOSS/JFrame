package io.github.jframe.logging.voter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Voter for various logging filters. Allows configuration of excluded paths and included content types.
 */
@Slf4j
@RequiredArgsConstructor
public class FilterVoter {

    /** The media type voter. */
    private final MediaTypeVoter mediaTypeVoter;

    /** The path voter. */
    private final RequestVoter requestVoter;

    /**
     * Returns {@code true} if the filter should be enabled.
     *
     * @param request The request.
     * @return {@code true} if the filter should be enabled.
     */
    public boolean enabled(final HttpServletRequest request) {
        Boolean isEnabled = (Boolean) request.getAttribute(getAttributeName());
        log.trace("Got '{}' from attribute.", isEnabled);
        if (isEnabled == null) {
            final boolean mediaTypeAllowed = mediaTypeVoter.mediaTypeMatches(request.getContentType());
            final boolean requestAllowed = requestVoter.allowed(request);
            isEnabled = mediaTypeAllowed && requestAllowed;
            request.setAttribute(getAttributeName(), isEnabled);
        }
        log.trace("Is enabled: '{}'.", isEnabled);

        return isEnabled;
    }

    private String getAttributeName() {
        return this.getClass().getPackageName() + ".FILTER_VOTER";
    }
}
