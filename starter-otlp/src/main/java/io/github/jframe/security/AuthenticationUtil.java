package io.github.jframe.security;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * General utility class to access authentication information. We currently have 2 supported authentication methods: Keycloak and API key.
 */
@Slf4j
public final class AuthenticationUtil {

    private AuthenticationUtil() {
        // No-args constructor to prevent instantiation.
    }

    /**
     * Retrieve the logged-in subject from the security context.
     *
     * @return the name of the authenticated subject or a message indicating the authentication status.
     */
    @SuppressWarnings("ReturnCount")
    public static String getAuthenticatedSubject() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isNull(authentication) || !authentication.isAuthenticated()) {
            return "ANONYMOUS - NO AUTHENTICATION";
        }

        if (isBlank(authentication.getName())) {
            log.warn("Authentication name is blank, please check your authentication configuration.");
            return "INCOMPLETE AUTHENTICATION - NO NAME";
        }
        return authentication.getName();
    }
}
