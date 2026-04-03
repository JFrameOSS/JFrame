package io.github.jframe.security;

import io.github.support.UnitTest;
import io.quarkus.security.identity.SecurityIdentity;

import java.security.Principal;
import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuarkusAuthenticationUtil}.
 *
 * <p>Verifies the CDI bean resolves the authenticated subject from Quarkus
 * {@link SecurityIdentity}, covering all identity states:
 * <ul>
 * <li>Authenticated principal with a non-blank name</li>
 * <li>Unsatisfied CDI Instance (no SecurityIdentity available in context)</li>
 * <li>Anonymous SecurityIdentity (unauthenticated request)</li>
 * <li>SecurityIdentity whose principal has a blank or null name</li>
 * </ul>
 */
@DisplayName("Quarkus OTLP - QuarkusAuthenticationUtil")
public class QuarkusAuthenticationUtilTest extends UnitTest {

    // ======================== CONSTANTS ========================

    private static final String ANONYMOUS_MESSAGE = "ANONYMOUS - NO AUTHENTICATION";
    private static final String INCOMPLETE_MESSAGE = "INCOMPLETE AUTHENTICATION - NO NAME";

    // ======================== FIXTURES ========================

    @Mock
    private Instance<SecurityIdentity> securityIdentityInstance;

    @Mock
    private SecurityIdentity securityIdentity;

    @Mock
    private Principal principal;

    @InjectMocks
    private QuarkusAuthenticationUtil authenticationUtil;

    // ======================== SETUP ========================

    @Override
    @BeforeEach
    public void setUp() {
        // Default: Instance is satisfied and SecurityIdentity is non-anonymous
        when(securityIdentityInstance.isUnsatisfied()).thenReturn(false);
        when(securityIdentityInstance.get()).thenReturn(securityIdentity);
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
    }

    // ======================== FACTORY METHODS ========================

    private QuarkusAuthenticationUtil anAuthenticationUtil() {
        return authenticationUtil;
    }

    // ======================== AC1: AUTHENTICATED USER ========================

    @Nested
    @DisplayName("Authenticated user with principal name")
    class AuthenticatedUser {

        @Test
        @DisplayName("Should return principal name when SecurityIdentity is present, non-anonymous, and name is non-blank")
        public void shouldReturnPrincipalNameWhenSecurityIdentityIsPresentAndNonAnonymous() {
            // Given: A fully authenticated SecurityIdentity with a known principal name
            when(principal.getName()).thenReturn("john.doe");

            // When: Resolving the authenticated subject
            final String subject = anAuthenticationUtil().getAuthenticatedSubject();

            // Then: The principal name is returned as-is
            assertThat(subject, is(equalTo("john.doe")));
        }
    }

    // ======================== AC2: UNSATISFIED INSTANCE ========================


    @Nested
    @DisplayName("Unsatisfied CDI Instance — no SecurityIdentity in context")
    class UnsatisfiedInstance {

        @Test
        @DisplayName("Should return anonymous message when Instance is unsatisfied")
        public void shouldReturnAnonymousMessageWhenInstanceIsUnsatisfied() {
            // Given: No SecurityIdentity bean is available in the CDI context
            when(securityIdentityInstance.isUnsatisfied()).thenReturn(true);

            // When: Resolving the authenticated subject
            final String subject = anAuthenticationUtil().getAuthenticatedSubject();

            // Then: The anonymous fallback message is returned
            assertThat(subject, is(equalTo(ANONYMOUS_MESSAGE)));
        }
    }

    // ======================== AC3: ANONYMOUS IDENTITY ========================


    @Nested
    @DisplayName("Anonymous SecurityIdentity — unauthenticated request")
    class AnonymousIdentity {

        @Test
        @DisplayName("Should return anonymous message when SecurityIdentity.isAnonymous() is true")
        public void shouldReturnAnonymousMessageWhenSecurityIdentityIsAnonymous() {
            // Given: A SecurityIdentity that represents an unauthenticated (anonymous) request
            when(securityIdentity.isAnonymous()).thenReturn(true);

            // When: Resolving the authenticated subject
            final String subject = anAuthenticationUtil().getAuthenticatedSubject();

            // Then: The anonymous fallback message is returned
            assertThat(subject, is(equalTo(ANONYMOUS_MESSAGE)));
        }
    }

    // ======================== AC4 & AC5: INCOMPLETE IDENTITY ========================


    @Nested
    @DisplayName("SecurityIdentity with blank or null principal name")
    class IncompleteIdentity {

        @Test
        @DisplayName("Should return incomplete message when principal name is blank")
        public void shouldReturnIncompleteMessageWhenPrincipalNameIsBlank() {
            // Given: SecurityIdentity is present and non-anonymous, but the principal name is empty
            when(principal.getName()).thenReturn("");

            // When: Resolving the authenticated subject
            final String subject = anAuthenticationUtil().getAuthenticatedSubject();

            // Then: The incomplete-authentication message is returned
            assertThat(subject, is(equalTo(INCOMPLETE_MESSAGE)));
        }

        @Test
        @DisplayName("Should return incomplete message when getPrincipal() returns null")
        public void shouldReturnIncompleteMessageWhenPrincipalIsNull() {
            // Given: SecurityIdentity is present and non-anonymous, but getPrincipal() returns null
            when(securityIdentity.getPrincipal()).thenReturn(null);

            // When: Resolving the authenticated subject
            final String subject = anAuthenticationUtil().getAuthenticatedSubject();

            // Then: The incomplete-authentication message is returned without NullPointerException
            assertThat(subject, is(equalTo(INCOMPLETE_MESSAGE)));
        }
    }
}
