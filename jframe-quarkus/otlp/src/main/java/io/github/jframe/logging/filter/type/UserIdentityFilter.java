package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;
import io.quarkus.security.identity.SecurityIdentity;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.Principal;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import static io.github.jframe.logging.ecs.EcsFieldNames.USER_NAME;
import static io.github.jframe.logging.ecs.EcsFieldNames.USER_ROLES;

/**
 * JAX-RS filter that populates MDC with user identity information from the Quarkus
 * {@link SecurityIdentity}.
 *
 * <p>On the inbound request: resolves the authenticated principal name and roles from the
 * security context and stores them in the MDC via {@link EcsFields}.
 * On the outbound response: removes the user identity fields from the MDC.
 *
 * <p>Handles all identity states: authenticated principal with roles, anonymous identity,
 * unsatisfied CDI instance, and identities with blank or null principal names.
 */
@Provider
@ApplicationScoped
@Priority(250)
@Slf4j
public class UserIdentityFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final Instance<SecurityIdentity> securityIdentityInstance;

    /**
     * Creates a new {@code UserIdentityFilter} with the given CDI security identity instance.
     *
     * @param securityIdentityInstance the CDI instance used to resolve the security identity
     */
    public UserIdentityFilter(final Instance<SecurityIdentity> securityIdentityInstance) {
        this.securityIdentityInstance = securityIdentityInstance;
    }

    @Override
    @SuppressWarnings("ReturnCount")
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (securityIdentityInstance.isUnsatisfied()) {
            EcsFields.tag(USER_NAME, AuthenticationConstants.ANONYMOUS);
            return;
        }

        final SecurityIdentity identity = securityIdentityInstance.get();
        if (identity.isAnonymous()) {
            EcsFields.tag(USER_NAME, AuthenticationConstants.ANONYMOUS);
            return;
        }

        final Principal principal = identity.getPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            EcsFields.tag(USER_NAME, AuthenticationConstants.INCOMPLETE);
        } else {
            EcsFields.tag(USER_NAME, principal.getName());
        }

        if (!identity.getRoles().isEmpty()) {
            EcsFields.tag(USER_ROLES, String.join(", ", identity.getRoles()));
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        EcsFields.clear(USER_NAME);
        EcsFields.clear(USER_ROLES);
    }
}
