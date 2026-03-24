package io.github.jframe.security;

import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * General utility CDI bean to access authentication information in Quarkus applications.
 *
 * <p>Resolves the authenticated subject from the Quarkus {@link SecurityIdentity}, covering all
 * identity states: authenticated principal, unsatisfied CDI instance, anonymous identity, and
 * identities with blank or null principal names.
 *
 * <p>Uses optional CDI injection ({@link Instance}) so it is safe in contexts where Quarkus
 * Security is not configured.
 */
@ApplicationScoped
public class QuarkusAuthenticationUtil {

    private static final String ANONYMOUS = "ANONYMOUS - NO AUTHENTICATION";
    private static final String INCOMPLETE = "INCOMPLETE AUTHENTICATION - NO NAME";

    @Inject
    private Instance<SecurityIdentity> securityIdentity;

    /**
     * Retrieve the logged-in subject from the Quarkus security context.
     *
     * @return the name of the authenticated subject or a message indicating the authentication status.
     */
    @SuppressWarnings("ReturnCount")
    public String getAuthenticatedSubject() {
        if (securityIdentity.isUnsatisfied()) {
            return ANONYMOUS;
        }

        final SecurityIdentity identity = securityIdentity.get();
        if (identity.isAnonymous()) {
            return ANONYMOUS;
        }

        if (identity.getPrincipal() == null) {
            return INCOMPLETE;
        }

        if (identity.getPrincipal().getName().isBlank()) {
            return INCOMPLETE;
        }

        return identity.getPrincipal().getName();
    }
}
