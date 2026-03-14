package io.github.jframe.security;

import io.github.support.UnitTest;
import io.quarkus.security.identity.SecurityIdentity;

import java.security.Principal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link QuarkusAuthenticationUtil}.
 *
 * <p>Verifies retrieval of the authenticated subject from Quarkus {@link SecurityIdentity}
 * including anonymous, authenticated, and null identity scenarios.
 */
@DisplayName("Quarkus OTLP - Authentication Util")
public class QuarkusAuthenticationUtilTest extends UnitTest {

    @Mock
    private SecurityIdentity securityIdentity;

    @Test
    @DisplayName("Should return principal name for authenticated user")
    public void shouldReturnPrincipalNameForAuthenticatedUser() {
        // Given: A SecurityIdentity with an authenticated principal
        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("john.doe");
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);

        // When: Getting the authenticated subject
        final String subject = QuarkusAuthenticationUtil.getSubject(securityIdentity);

        // Then: The principal name is returned
        assertThat(subject, is(notNullValue()));
        assertThat(subject, is(equalTo("john.doe")));
    }

    @Test
    @DisplayName("Should return null for anonymous identity")
    public void shouldReturnNullForAnonymousIdentity() {
        // Given: An anonymous SecurityIdentity
        when(securityIdentity.isAnonymous()).thenReturn(true);

        // When: Getting the authenticated subject
        final String subject = QuarkusAuthenticationUtil.getSubject(securityIdentity);

        // Then: Null is returned for anonymous users
        assertThat(subject, is(nullValue()));
    }

    @Test
    @DisplayName("Should return null when SecurityIdentity is null")
    public void shouldReturnNullWhenSecurityIdentityIsNull() {
        // Given: A null SecurityIdentity

        // When: Getting the authenticated subject
        final String subject = QuarkusAuthenticationUtil.getSubject(null);

        // Then: Null is returned
        assertThat(subject, is(nullValue()));
    }

    @Test
    @DisplayName("Should return empty string when principal name is empty")
    public void shouldReturnEmptyStringWhenPrincipalNameIsEmpty() {
        // Given: A SecurityIdentity with an empty principal name
        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("");
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);

        // When: Getting the authenticated subject
        final String subject = QuarkusAuthenticationUtil.getSubject(securityIdentity);

        // Then: Empty string is returned (not null)
        assertThat(subject, is(notNullValue()));
        assertThat(subject, is(equalTo("")));
    }

    @Test
    @DisplayName("Should return principal name with special characters")
    public void shouldReturnPrincipalNameWithSpecialCharacters() {
        // Given: A SecurityIdentity whose principal has a service-account-style name
        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("service-account@tenant.example.com");
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);

        // When: Getting the authenticated subject
        final String subject = QuarkusAuthenticationUtil.getSubject(securityIdentity);

        // Then: The full principal name is returned
        assertThat(subject, is(equalTo("service-account@tenant.example.com")));
    }
}
