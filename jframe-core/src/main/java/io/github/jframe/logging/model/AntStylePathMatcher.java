package io.github.jframe.logging.model;

/**
 * Ant-style path matcher that supports Ant-style path patterns.
 *
 * <p>Patterns may contain:
 * <ul>
 * <li>{@code ?} — matches exactly one character</li>
 * <li>{@code *} — matches zero or more characters within a single path segment</li>
 * <li>{@code **} — matches zero or more path segments</li>
 * </ul>
 */
public class AntStylePathMatcher {

    /** Wildcard character matching any single character except path separators. */
    private static final char SINGLE_WILDCARD = '*';

    /** Wildcard character matching exactly one non-separator character. */
    private static final char QUESTION_MARK = '?';

    /** Path separator character. */
    private static final char PATH_SEPARATOR = '/';

    private final String pattern;

    /**
     * Constructs a new {@code AntStylePathMatcher} with no fixed pattern.
     * Use {@link #match(String, String)} to perform ad-hoc matching.
     */
    public AntStylePathMatcher() {
        this.pattern = null;
    }

    /**
     * Constructs a new {@code AntStylePathMatcher} with the given pattern.
     *
     * @param pattern the Ant-style path pattern (may be {@code null} — will not match any path)
     */
    public AntStylePathMatcher(final String pattern) {
        this.pattern = pattern;
    }

    /**
     * Returns {@code true} if the given path matches this matcher's pattern.
     *
     * @param path the path to test (may be {@code null})
     * @return {@code true} if the path matches
     */
    public boolean matches(final String path) {
        if (pattern == null || path == null) {
            return false;
        }
        return doMatch(pattern, path);
    }

    /**
     * Returns {@code true} if the given path matches the given pattern.
     *
     * @param patternToMatch the Ant-style path pattern (may be {@code null})
     * @param path           the path to test (may be {@code null})
     * @return {@code true} if the path matches
     */
    public boolean match(final String patternToMatch, final String path) {
        if (patternToMatch == null || path == null) {
            return false;
        }
        return doMatch(patternToMatch, path);
    }

    private boolean doMatch(final String pat, final String str) {
        return matchPattern(pat, 0, str, 0);
    }

    @SuppressWarnings(
        {
            "CyclomaticComplexity",
            "checkstyle:ReturnCount",
            "checkstyle:NPathComplexity"
        }
    )
    private boolean matchPattern(final String pat, final int pi, final String str, final int si) {
        final int patLen = pat.length();
        final int strLen = str.length();

        // Base case: both exhausted
        if (pi == patLen && si == strLen) {
            return true;
        }

        // Pattern exhausted but string not
        if (pi == patLen) {
            return false;
        }

        // Check for ** wildcard
        if (pi + 1 < patLen && pat.charAt(pi) == SINGLE_WILDCARD && pat.charAt(pi + 1) == SINGLE_WILDCARD) {
            return matchDoubleWildcard(pat, pi, str, si, patLen, strLen);
        }

        // Check for single * wildcard
        if (pat.charAt(pi) == SINGLE_WILDCARD) {
            return matchSingleWildcard(pat, pi, str, si, strLen);
        }

        // Check for ? wildcard — matches exactly one non-slash character
        if (pat.charAt(pi) == QUESTION_MARK) {
            if (si >= strLen || str.charAt(si) == PATH_SEPARATOR) {
                return false;
            }
            return matchPattern(pat, pi + 1, str, si + 1);
        }

        // Literal character match
        if (si < strLen && pat.charAt(pi) == str.charAt(si)) {
            return matchPattern(pat, pi + 1, str, si + 1);
        }

        return false;
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    private boolean matchDoubleWildcard(
        final String pat, final int pi, final String str, final int si,
        final int patLen, final int strLen) {
        // Move past the **
        final int nextPi = pi + 2;

        // Skip any trailing slash after **
        final int adjustedPi;
        if (nextPi < patLen && pat.charAt(nextPi) == PATH_SEPARATOR) {
            adjustedPi = nextPi + 1;
        } else {
            adjustedPi = nextPi;
        }

        // ** at end of pattern matches everything
        if (adjustedPi >= patLen) {
            return true;
        }

        // Try matching the rest of the pattern against every suffix of str
        for (int i = si; i <= strLen; i++) {
            if (matchPattern(pat, adjustedPi, str, i)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchSingleWildcard(
        final String pat, final int pi, final String str, final int si, final int strLen) {
        final int nextPi = pi + 1;
        // * matches one or more chars within a single segment (no slash crossing, no empty match)
        for (int i = si + 1; i <= strLen; i++) {
            // Don't let * match across path separator
            if (str.charAt(i - 1) == PATH_SEPARATOR) {
                break;
            }
            if (matchPattern(pat, nextPi, str, i)) {
                return true;
            }
        }
        return false;
    }
}
