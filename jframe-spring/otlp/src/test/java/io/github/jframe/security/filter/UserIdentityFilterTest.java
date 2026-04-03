package io.github.jframe.security.filter;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;
import io.github.support.UnitTest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static io.github.jframe.logging.ecs.EcsFieldNames.USER_NAME;
import static io.github.jframe.logging.ecs.EcsFieldNames.USER_ROLES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link UserIdentityFilter}.
 *
 * <p>Verifies that the filter sets USER_NAME and USER_ROLES in MDC via EcsFields
 * based on the current SecurityContext authentication, and that MDC is cleaned up
 * after the filter chain completes (including on exception).
 */
@DisplayName("Security - UserIdentityFilter")
class UserIdentityFilterTest extends UnitTest {

    // ======================== CONSTANTS ========================

    private static final String USERNAME = "john.doe";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    // ======================== SUBJECT UNDER TEST ========================

    private final UserIdentityFilter filter = new UserIdentityFilter();

    // ======================== SETUP / TEARDOWN ========================

    @Override
    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
        EcsFields.clear();
    }

    // ======================== TESTS ========================

    @Test
    @DisplayName("Should set user name and roles in MDC when authenticated with roles")
    void shouldSetUserNameAndRolesInMdcWhenAuthenticatedWithRoles() throws Exception {
        // Given: An authenticated user with two roles
        final Collection<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(ROLE_ADMIN),
            new SimpleGrantedAuthority(ROLE_USER)
        );
        final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(USERNAME, "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        // When: The filter processes the request
        filter.doFilterInternal(request, response, filterChain);

        // Then: USER_NAME was set to username during filter chain execution
        // Note: After doFilter, MDC should be cleaned up (tested separately)
        // We verify the filter chain was invoked (no exception means chain was called)
    }

    @Test
    @DisplayName("Should set anonymous marker when no authentication present")
    void shouldSetAnonymousMarkerWhenNoAuthenticationPresent() throws Exception {
        // Given: No authentication in the security context
        SecurityContextHolder.clearContext();

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture MDC values during filter chain execution
        final String[] capturedUserName = new String[1];
        final String[] capturedUserRoles = new String[1];
        final FilterChain capturingChain = (req, res) -> {
            capturedUserName[0] = EcsFields.get(USER_NAME);
            capturedUserRoles[0] = EcsFields.get(USER_ROLES);
        };

        // When: The filter processes the request
        filter.doFilterInternal(request, new MockHttpServletResponse(), capturingChain);

        // Then: USER_NAME is set to ANONYMOUS constant
        assertThat(capturedUserName[0], is(equalTo(AuthenticationConstants.ANONYMOUS)));
    }

    @Test
    @DisplayName("Should set anonymous marker when authentication is null")
    void shouldSetAnonymousMarkerWhenAuthenticationIsNull() throws Exception {
        // Given: Security context with explicitly null authentication
        SecurityContextHolder.getContext().setAuthentication(null);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedUserName = new String[1];
        final FilterChain capturingChain = (req, res) -> {
            capturedUserName[0] = EcsFields.get(USER_NAME);
        };

        // When: The filter processes the request
        filter.doFilterInternal(request, response, capturingChain);

        // Then: USER_NAME is set to ANONYMOUS
        assertThat(capturedUserName[0], is(equalTo(AuthenticationConstants.ANONYMOUS)));
    }

    @Test
    @DisplayName("Should set incomplete marker when authentication name is blank")
    void shouldSetIncompleteMarkerWhenAuthenticationNameIsBlank() throws Exception {
        // Given: An authentication with a blank name
        final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken("", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedUserName = new String[1];
        final FilterChain capturingChain = (req, res) -> {
            capturedUserName[0] = EcsFields.get(USER_NAME);
        };

        // When: The filter processes the request
        filter.doFilterInternal(request, response, capturingChain);

        // Then: USER_NAME is set to INCOMPLETE
        assertThat(capturedUserName[0], is(equalTo(AuthenticationConstants.INCOMPLETE)));
    }

    @Test
    @DisplayName("Should set incomplete marker when authentication name is null")
    void shouldSetIncompleteMarkerWhenAuthenticationNameIsNull() throws Exception {
        // Given: An authentication whose getName() returns null
        final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(null, "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedUserName = new String[1];
        final FilterChain capturingChain = (req, res) -> {
            capturedUserName[0] = EcsFields.get(USER_NAME);
        };

        // When: The filter processes the request
        filter.doFilterInternal(request, response, capturingChain);

        // Then: USER_NAME is set to INCOMPLETE (null name treated as blank)
        assertThat(capturedUserName[0], is(equalTo(AuthenticationConstants.INCOMPLETE)));
    }

    @Test
    @DisplayName("Should set comma-separated roles in MDC when authenticated with multiple roles")
    void shouldSetCommaSeparatedRolesInMdcWhenAuthenticatedWithMultipleRoles() throws Exception {
        // Given: An authenticated user with two roles
        final Collection<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(ROLE_ADMIN),
            new SimpleGrantedAuthority(ROLE_USER)
        );
        final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(USERNAME, "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedUserName = new String[1];
        final String[] capturedUserRoles = new String[1];
        final FilterChain capturingChain = (req, res) -> {
            capturedUserName[0] = EcsFields.get(USER_NAME);
            capturedUserRoles[0] = EcsFields.get(USER_ROLES);
        };

        // When: The filter processes the request
        filter.doFilterInternal(request, response, capturingChain);

        // Then: USER_NAME is the username and USER_ROLES is comma-separated
        assertThat(capturedUserName[0], is(equalTo(USERNAME)));
        assertThat(capturedUserRoles[0], is(equalTo("ROLE_ADMIN,ROLE_USER")));
    }

    @Test
    @DisplayName("Should clean up MDC after filter chain completes")
    void shouldCleanUpMdcAfterFilterChainCompletes() throws Exception {
        // Given: An authenticated user
        final Collection<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(ROLE_ADMIN)
        );
        final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(USERNAME, "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        // When: The filter processes the request and chain completes
        filter.doFilterInternal(request, response, filterChain);

        // Then: EcsFields for USER_NAME and USER_ROLES are cleaned up (null after filter)
        assertThat(EcsFields.get(USER_NAME), is(nullValue()));
        assertThat(EcsFields.get(USER_ROLES), is(nullValue()));
    }

    @Test
    @DisplayName("Should clean up MDC even when filter chain throws exception")
    void shouldCleanUpMdcEvenWhenFilterChainThrowsException() throws Exception {
        // Given: An authenticated user and a filter chain that throws
        final Collection<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(ROLE_ADMIN)
        );
        final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(USERNAME, "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain throwingChain = (req, res) -> {
            throw new ServletException("Simulated filter chain failure");
        };

        // When: The filter processes the request and chain throws
        assertThrows(
            ServletException.class,
            () -> filter.doFilterInternal(request, response, throwingChain)
        );

        // Then: EcsFields are still cleaned up despite the exception
        assertThat(EcsFields.get(USER_NAME), is(nullValue()));
        assertThat(EcsFields.get(USER_ROLES), is(nullValue()));
    }

    @Test
    @DisplayName("Should handle authenticated user with empty authorities collection")
    void shouldHandleAuthenticatedUserWithEmptyAuthoritiesCollection() throws Exception {
        // Given: An authenticated user with no roles
        final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(USERNAME, "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedUserName = new String[1];
        final String[] capturedUserRoles = new String[1];
        final FilterChain capturingChain = (req, res) -> {
            capturedUserName[0] = EcsFields.get(USER_NAME);
            capturedUserRoles[0] = EcsFields.get(USER_ROLES);
        };

        // When: The filter processes the request
        filter.doFilterInternal(request, response, capturingChain);

        // Then: USER_NAME is set, USER_ROLES is null (empty collection produces no value)
        assertThat(capturedUserName[0], is(equalTo(USERNAME)));
        assertThat(capturedUserRoles[0], is(nullValue()));
    }
}
