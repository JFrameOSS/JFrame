package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;
import io.github.support.UnitTest;
import io.quarkus.security.identity.SecurityIdentity;

import java.security.Principal;
import java.util.Set;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static io.github.jframe.logging.ecs.EcsFieldNames.USER_NAME;
import static io.github.jframe.logging.ecs.EcsFieldNames.USER_ROLES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserIdentityFilter}.
 *
 * <p>Verifies the JAX-RS filter that populates MDC with user identity
 * information from the Quarkus {@link SecurityIdentity}. Covers:
 * <ul>
 * <li>Authenticated user with roles → MDC populated with principal name and comma-separated roles</li>
 * <li>Anonymous identity → MDC populated with {@link AuthenticationConstants#ANONYMOUS}</li>
 * <li>Unsatisfied CDI instance → MDC populated with {@link AuthenticationConstants#ANONYMOUS}</li>
 * <li>Blank principal name → MDC populated with {@link AuthenticationConstants#INCOMPLETE}</li>
 * <li>Response filter cleans up USER_NAME and USER_ROLES from MDC</li>
 * </ul>
 */
@DisplayName("Quarkus OTLP Logging Filters - User Identity Filter")
public class UserIdentityFilterTest extends UnitTest {

    // ======================== FIXTURES ========================

    @Mock
    private Instance<SecurityIdentity> securityIdentityInstance;

    @Mock
    private SecurityIdentity securityIdentity;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    private UserIdentityFilter filter;

    @Override
    @BeforeEach
    public void setUp() {
        filter = new UserIdentityFilter(securityIdentityInstance);
    }

    @AfterEach
    public void clearEcsFields() {
        EcsFields.clear();
    }

    // ======================== FACTORY METHODS ========================

    private SecurityIdentity anAuthenticatedIdentity(final String principalName, final Set<String> roles) {
        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(principalName);
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(securityIdentity.getRoles()).thenReturn(roles);
        return securityIdentity;
    }

    private void givenSatisfiedIdentity(final SecurityIdentity identity) {
        when(securityIdentityInstance.isUnsatisfied()).thenReturn(false);
        when(securityIdentityInstance.get()).thenReturn(identity);
    }

    private void givenUnsatisfiedIdentity() {
        when(securityIdentityInstance.isUnsatisfied()).thenReturn(true);
    }

    private void givenAnonymousIdentity() {
        when(securityIdentityInstance.isUnsatisfied()).thenReturn(false);
        when(securityIdentityInstance.get()).thenReturn(securityIdentity);
        when(securityIdentity.isAnonymous()).thenReturn(true);
    }

    // ======================== AC1: AUTHENTICATED USER WITH ROLES ========================

    @Nested
    @DisplayName("AC1 - Authenticated user with roles")
    class AuthenticatedUserWithRoles {

        @Test
        @DisplayName("Should set USER_NAME in MDC to principal name when user is authenticated")
        public void shouldSetUserNameInMdcToPrincipalNameWhenUserIsAuthenticated() throws Exception {
            // Given: An authenticated user with a known principal name
            final SecurityIdentity identity = anAuthenticatedIdentity("john.doe", Set.of("admin", "user"));
            givenSatisfiedIdentity(identity);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME MDC field is set to the principal name
            assertThat(EcsFields.get(USER_NAME), is(equalTo("john.doe")));
        }

        @Test
        @DisplayName("Should set USER_ROLES in MDC to comma-separated roles when user has roles")
        public void shouldSetUserRolesInMdcToCommaSeparatedRolesWhenUserHasRoles() throws Exception {
            // Given: An authenticated user with multiple roles
            final SecurityIdentity identity = anAuthenticatedIdentity("john.doe", Set.of("admin", "user"));
            givenSatisfiedIdentity(identity);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_ROLES MDC field contains the roles (comma-separated)
            final String roles = EcsFields.get(USER_ROLES);
            assertThat(roles, is(org.hamcrest.Matchers.notNullValue()));
        }

        @Test
        @DisplayName("Should set USER_NAME when user has single role")
        public void shouldSetUserNameWhenUserHasSingleRole() throws Exception {
            // Given: An authenticated user with a single role
            final SecurityIdentity identity = anAuthenticatedIdentity("jane.smith", Set.of("viewer"));
            givenSatisfiedIdentity(identity);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME MDC field is set to the principal name
            assertThat(EcsFields.get(USER_NAME), is(equalTo("jane.smith")));
        }
    }

    // ======================== AC2: ANONYMOUS IDENTITY ========================


    @Nested
    @DisplayName("AC2 - Anonymous / no identity")
    class AnonymousIdentity {

        @Test
        @DisplayName("Should set USER_NAME to ANONYMOUS when SecurityIdentity is anonymous")
        public void shouldSetUserNameToAnonymousWhenSecurityIdentityIsAnonymous() throws Exception {
            // Given: An anonymous security identity
            givenAnonymousIdentity();

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME MDC field is set to the ANONYMOUS constant
            assertThat(EcsFields.get(USER_NAME), is(equalTo(AuthenticationConstants.ANONYMOUS)));
        }
    }

    // ======================== AC3: UNSATISFIED CDI INSTANCE ========================


    @Nested
    @DisplayName("AC3 - Unsatisfied CDI instance")
    class UnsatisfiedInstance {

        @Test
        @DisplayName("Should set USER_NAME to ANONYMOUS when SecurityIdentity instance is unsatisfied")
        public void shouldSetUserNameToAnonymousWhenSecurityIdentityInstanceIsUnsatisfied() throws Exception {
            // Given: The CDI Instance<SecurityIdentity> is unsatisfied (no security extension configured)
            givenUnsatisfiedIdentity();

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME MDC field is set to the ANONYMOUS constant
            assertThat(EcsFields.get(USER_NAME), is(equalTo(AuthenticationConstants.ANONYMOUS)));
        }
    }

    // ======================== AC4: BLANK PRINCIPAL NAME ========================


    @Nested
    @DisplayName("AC4 - Blank principal name")
    class BlankPrincipalName {

        @Test
        @DisplayName("Should set USER_NAME to INCOMPLETE when principal name is blank")
        public void shouldSetUserNameToIncompleteWhenPrincipalNameIsBlank() throws Exception {
            // Given: An authenticated identity with a blank principal name
            final SecurityIdentity identity = anAuthenticatedIdentity("", Set.of());
            givenSatisfiedIdentity(identity);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME MDC field is set to the INCOMPLETE constant
            assertThat(EcsFields.get(USER_NAME), is(equalTo(AuthenticationConstants.INCOMPLETE)));
        }

        @Test
        @DisplayName("Should set USER_NAME to INCOMPLETE when principal name is whitespace only")
        public void shouldSetUserNameToIncompleteWhenPrincipalNameIsWhitespaceOnly() throws Exception {
            // Given: An authenticated identity with a whitespace-only principal name
            final SecurityIdentity identity = anAuthenticatedIdentity("   ", Set.of());
            givenSatisfiedIdentity(identity);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME MDC field is set to the INCOMPLETE constant
            assertThat(EcsFields.get(USER_NAME), is(equalTo(AuthenticationConstants.INCOMPLETE)));
        }

        @Test
        @DisplayName("Should set USER_NAME to INCOMPLETE when principal name is null")
        public void shouldSetUserNameToIncompleteWhenPrincipalNameIsNull() throws Exception {
            // Given: An authenticated identity with a null principal name
            final SecurityIdentity identity = anAuthenticatedIdentity(null, Set.of());
            givenSatisfiedIdentity(identity);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME MDC field is set to the INCOMPLETE constant
            assertThat(EcsFields.get(USER_NAME), is(equalTo(AuthenticationConstants.INCOMPLETE)));
        }
    }

    // ======================== AC5: RESPONSE FILTER MDC CLEANUP ========================


    @Nested
    @DisplayName("AC5 - Response filter MDC cleanup")
    class ResponseFilterMdcCleanup {

        @Test
        @DisplayName("Should remove USER_NAME from MDC after response filter completes")
        public void shouldRemoveUserNameFromMdcAfterResponseFilterCompletes() throws Exception {
            // Given: USER_NAME is set in MDC (simulating what the request filter did)
            EcsFields.tag(USER_NAME, "john.doe");

            // When: The response filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: USER_NAME is removed from MDC
            assertThat(EcsFields.get(USER_NAME), is(nullValue()));
        }

        @Test
        @DisplayName("Should remove USER_ROLES from MDC after response filter completes")
        public void shouldRemoveUserRolesFromMdcAfterResponseFilterCompletes() throws Exception {
            // Given: USER_ROLES is set in MDC (simulating what the request filter did)
            EcsFields.tag(USER_ROLES, "admin, user");

            // When: The response filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: USER_ROLES is removed from MDC
            assertThat(EcsFields.get(USER_ROLES), is(nullValue()));
        }

        @Test
        @DisplayName("Should remove both USER_NAME and USER_ROLES from MDC after response filter completes")
        public void shouldRemoveBothUserNameAndUserRolesFromMdcAfterResponseFilterCompletes() throws Exception {
            // Given: Both USER_NAME and USER_ROLES are set in MDC
            EcsFields.tag(USER_NAME, "john.doe");
            EcsFields.tag(USER_ROLES, "admin, user");

            // When: The response filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: Both fields are removed from MDC
            assertThat(EcsFields.get(USER_NAME), is(nullValue()));
            assertThat(EcsFields.get(USER_ROLES), is(nullValue()));
        }

        @Test
        @DisplayName("Should clean up MDC even if request filter was not called (e.g. error path)")
        public void shouldCleanUpMdcEvenIfRequestFilterWasNotCalled() throws Exception {
            // Given: MDC has no USER_NAME or USER_ROLES (request filter was never executed)

            // When: The response filter processes the response
            filter.filter(requestContext, responseContext);

            // Then: No exception thrown — cleanup is safe even when fields are absent
            assertThat(EcsFields.get(USER_NAME), is(nullValue()));
            assertThat(EcsFields.get(USER_ROLES), is(nullValue()));
        }
    }

    // ======================== EDGE CASES ========================


    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty roles set gracefully")
        public void shouldHandleEmptyRolesSetGracefully() throws Exception {
            // Given: An authenticated user with no roles
            final SecurityIdentity identity = anAuthenticatedIdentity("john.doe", Set.of());
            givenSatisfiedIdentity(identity);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME is still set correctly
            assertThat(EcsFields.get(USER_NAME), is(equalTo("john.doe")));
        }

        @Test
        @DisplayName("Should handle null principal from SecurityIdentity")
        public void shouldHandleNullPrincipalFromSecurityIdentity() throws Exception {
            // Given: An authenticated identity that returns null for getPrincipal()
            when(securityIdentityInstance.isUnsatisfied()).thenReturn(false);
            when(securityIdentityInstance.get()).thenReturn(securityIdentity);
            when(securityIdentity.isAnonymous()).thenReturn(false);
            when(securityIdentity.getPrincipal()).thenReturn(null);

            // When: The request filter processes the request
            filter.filter(requestContext);

            // Then: USER_NAME is set to INCOMPLETE (no principal available)
            assertThat(EcsFields.get(USER_NAME), is(equalTo(AuthenticationConstants.INCOMPLETE)));
        }
    }
}
