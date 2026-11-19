package io.github.jframe.logging.model;

/**
 * Ant-style path matcher that supports Ant-style path patterns.
 *
 * <p>Patterns may contain:
 * <ul>
 * <li>{@code ?} — matches exactly one character</li>
 * <li>{@code *} — matches one or more characters within a single path segment</li>
 * <li>{@code **} — matches any remainder of the path (including none), across segments</li>
 * </ul>
 */
public class AntStylePathMatcher {

    /** Wildcard character; {@code *} matches within a segment, {@code **} across segments. */
    private static final char WILDCARD = '*';

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

    /** Bottom-up DP: {@code dp[pi][si]} is {@code true} if {@code pat[pi..]} matches {@code str[si..]}. */
    private static boolean doMatch(final String pat, final String str) {
        final boolean[][] dp = new boolean[pat.length() + 1][str.length() + 1];
        dp[pat.length()][str.length()] = true;
        for (int pi = pat.length() - 1; pi >= 0; pi--) {
            if (pat.startsWith("**", pi)) {
                fillDoubleWildcard(pat, pi, dp, str.length());
            } else {
                fillRow(pat.charAt(pi), dp[pi], dp[pi + 1], str);
            }
        }
        return dp[0][0];
    }

    /** {@code *} matches one or more non-separator chars, {@code ?} one, any other char itself. */
    private static void fillRow(final char pc, final boolean[] row, final boolean[] next, final String str) {
        for (int si = str.length() - 1; si >= 0; si--) {
            final char sc = str.charAt(si);
            if (pc == WILDCARD) {
                row[si] = sc != PATH_SEPARATOR && (next[si + 1] || row[si + 1]);
            } else if (pc == QUESTION_MARK) {
                row[si] = sc != PATH_SEPARATOR && next[si + 1];
            } else {
                row[si] = pc == sc && next[si + 1];
            }
        }
    }

    /** {@code **} (plus an optional trailing {@code /}) matches any remainder of the path, crossing separators. */
    private static void fillDoubleWildcard(final String pat, final int pi, final boolean[][] dp, final int strLen) {
        int rest = pi + 2;
        if (rest < pat.length() && pat.charAt(rest) == PATH_SEPARATOR) {
            rest++;
        }
        final boolean matchesAll = rest >= pat.length();
        boolean anySuffix = false;
        for (int si = strLen; si >= 0; si--) {
            anySuffix = anySuffix || dp[rest][si];
            dp[pi][si] = matchesAll || anySuffix;
        }
    }
}
