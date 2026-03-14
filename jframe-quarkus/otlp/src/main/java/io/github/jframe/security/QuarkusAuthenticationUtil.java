package io.github.jframe.security;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Utility class for extracting authentication information from a Quarkus {@link SecurityIdentity}.
 *
 * <p>Provides a null-safe way to retrieve the authenticated principal name (subject),
 * returning {@code null} for anonymous or absent identities.
 */
public final class QuarkusAuthenticationUtil {

    private QuarkusAuthenticationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the principal name of the authenticated user, or {@code null} if the identity
     * is {@code null} or anonymous.
     *
     * @param identity the Quarkus security identity; may be {@code null}
     * @return the principal name, or {@code null}
     */
    public static String getSubject(final SecurityIdentity identity) {
        if (identity == null || identity.isAnonymous()) {
            return null;
        }
        return identity.getPrincipal().getName();
    }
}
