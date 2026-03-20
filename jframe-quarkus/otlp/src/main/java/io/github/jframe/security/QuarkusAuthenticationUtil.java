package io.github.jframe.security;

import io.quarkus.security.identity.SecurityIdentity;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for extracting authentication information from a Quarkus {@link SecurityIdentity}.
 *
 * <p>Provides a null-safe way to retrieve the authenticated principal name (subject),
 * matching the behaviour of the Spring {@code AuthenticationUtil} counterpart.
 */
@Slf4j
public final class QuarkusAuthenticationUtil {

    private QuarkusAuthenticationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the principal name of the authenticated user.
     *
     * <ul>
     * <li>Returns {@code "ANONYMOUS - NO AUTHENTICATION"} when the identity is {@code null} or anonymous.</li>
     * <li>Returns {@code "INCOMPLETE AUTHENTICATION - NO NAME"} when the principal name is blank.</li>
     * <li>Returns the principal name otherwise.</li>
     * </ul>
     *
     * @param identity the Quarkus security identity; may be {@code null}
     * @return the principal name, or a sentinel string describing the authentication state
     */
    @SuppressWarnings("ReturnCount")
    public static String getSubject(final SecurityIdentity identity) {
        if (identity == null || identity.isAnonymous()) {
            return "ANONYMOUS - NO AUTHENTICATION";
        }
        final String name = identity.getPrincipal().getName();
        if (name == null || name.isBlank()) {
            log.warn("Authentication name is blank, please check your authentication configuration.");
            return "INCOMPLETE AUTHENTICATION - NO NAME";
        }
        return name;
    }
}
