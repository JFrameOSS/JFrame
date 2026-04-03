package io.github.jframe.logging.voter;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

/** Media type voter allows configuration of allowed media types for JAX-RS filter decisions. */
@Slf4j
public class MediaTypeVoter {

    private static final String WILDCARD = "*";

    private static final String WILDCARD_ALL = "*/*";

    /** The configured allowed content types. */
    private final List<String> allowedContentTypes;

    /** The fallback value when content type is absent or allowed list is empty. */
    private final boolean matchIfEmpty;

    /**
     * Constructs a new {@code MediaTypeVoter}.
     *
     * @param allowedContentTypes the list of allowed content type strings (e.g. "application/json")
     * @param matchIfEmpty        the value returned when content type or allowed list is absent
     */
    public MediaTypeVoter(final List<String> allowedContentTypes, final boolean matchIfEmpty) {
        this.allowedContentTypes = allowedContentTypes;
        this.matchIfEmpty = matchIfEmpty;
        log.debug("Configured allowed content types: '{}'.", allowedContentTypes);
    }

    /**
     * Returns {@code true} if the given content type matches one of the allowed types.
     *
     * @param contentType the raw Content-Type header value (may be null or blank)
     * @return match result; falls back to {@code matchIfEmpty} when input or list is absent
     */
    public boolean matches(final String contentType) {
        final boolean emptyInput = contentType == null || contentType.isBlank();
        final boolean emptyList = allowedContentTypes == null || allowedContentTypes.isEmpty();

        boolean result = matchIfEmpty;

        if (!emptyInput && !emptyList) {
            result = evaluateAllowedTypes(contentType);
        }

        return result;
    }

    private boolean evaluateAllowedTypes(final String contentType) {
        final String baseType = parseBaseType(contentType);
        boolean found = false;

        if (baseType != null) {
            for (final String allowed : allowedContentTypes) {
                if (typeMatches(allowed, baseType)) {
                    log.trace("Content type '{}' matched allowed type '{}'.", baseType, allowed);
                    found = true;
                }
            }
        }

        if (!found) {
            log.debug("Content type '{}' did not match any allowed type.", contentType);
        }

        return found;
    }

    /**
     * Parses the base media type from a raw Content-Type string, stripping parameters.
     * Returns {@code null} if the string is malformed (no '/' separator).
     */
    private static String parseBaseType(final String contentType) {
        final int semicolonIndex = contentType.indexOf(';');
        final String withoutParams = semicolonIndex >= 0
            ? contentType.substring(0, semicolonIndex).trim()
            : contentType.trim();

        String result = null;
        if (withoutParams.contains("/")) {
            result = withoutParams;
        }

        return result;
    }

    /**
     * Checks whether the {@code allowed} type pattern matches the {@code actual} base type.
     * Supports wildcards: {@code *}{@code /*} and {@code type/*}.
     */
    private static boolean typeMatches(final String allowed, final String actual) {
        final String normalizedAllowed = allowed.trim().toLowerCase(Locale.ROOT);
        final String normalizedActual = actual.trim().toLowerCase(Locale.ROOT);

        boolean result = WILDCARD_ALL.equals(normalizedAllowed);

        if (!result) {
            result = subtypeMatches(normalizedAllowed, normalizedActual);
        }

        return result;
    }

    private static boolean subtypeMatches(final String normalizedAllowed, final String normalizedActual) {
        final int slashAllowed = normalizedAllowed.indexOf('/');
        final int slashActual = normalizedActual.indexOf('/');

        boolean result = false;

        if (slashAllowed >= 0 && slashActual >= 0) {
            final String allowedType = normalizedAllowed.substring(0, slashAllowed);
            final String actualType = normalizedActual.substring(0, slashActual);

            if (allowedType.equals(actualType)) {
                final String allowedSubtype = normalizedAllowed.substring(slashAllowed + 1);
                final String actualSubtype = normalizedActual.substring(slashActual + 1);
                result = WILDCARD.equals(allowedSubtype) || allowedSubtype.equals(actualSubtype);
            }
        }

        return result;
    }
}
