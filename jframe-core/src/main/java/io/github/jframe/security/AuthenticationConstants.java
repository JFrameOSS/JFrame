package io.github.jframe.security;

/**
 * Constants for authentication state representation.
 *
 * <p>Used across tracing, logging, and auditing components to represent
 * anonymous or incomplete authentication contexts in a consistent way.
 */
public final class AuthenticationConstants {

    /** Marker string used when no authentication principal is present. */
    public static final String ANONYMOUS = "ANONYMOUS - NO AUTHENTICATION";

    /** Marker string used when authentication exists but has no resolvable name. */
    public static final String INCOMPLETE = "INCOMPLETE AUTHENTICATION - NO NAME";

    private AuthenticationConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
