package io.github.jframe.logging.voter;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/** Media type voter allows configuration of allowed media types. */
@Slf4j
public class MediaTypeVoter {

    /** The configured content types. */
    private final List<MediaType> contentTypes;

    private final boolean matchIfEmpty;

    /** The constructor with content types and whether to match if there are no content types. */
    public MediaTypeVoter(final List<MediaType> contentTypes, final boolean matchIfEmpty) {
        this.contentTypes = contentTypes;
        this.matchIfEmpty = matchIfEmpty;
        log.debug("Configured content types: '{}'.", contentTypes);
    }

    /** Check if the {@code mediaType} is one of the configured content type. */
    public boolean mediaTypeMatches(final String contentType) {
        return mediaTypeMatches(parseMediaType(contentType));
    }

    /** Check if the {@code mediaType} is one of the configured content type. */
    public boolean mediaTypeMatches(final MediaType mediaType) {
        boolean matches = false;

        if (mediaType == null || contentTypes == null || contentTypes.isEmpty()) {
            matches = matchIfEmpty;
        } else {

            for (final MediaType allowedType : contentTypes) {
                final boolean includes = allowedType.includes(mediaType);
                log.trace("Type '{}' contains '{}': '{}'.", allowedType, mediaType, includes);
                if (includes) {
                    matches = true;
                }
            }

            log.debug("Media type '{}' does not match, since it is not configured.", mediaType);
        }

        return matches;
    }

    private static MediaType parseMediaType(final String contentType) {
        if (isNotBlank(contentType)) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (final InvalidMediaTypeException exception) {
                log.info("Got error parsing content type '{}'.", contentType, exception);
            }
        }

        return null;
    }
}
