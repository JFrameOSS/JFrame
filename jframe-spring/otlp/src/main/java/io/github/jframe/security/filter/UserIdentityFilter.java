package io.github.jframe.security.filter;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;

import java.io.IOException;
import java.util.stream.Collectors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import static io.github.jframe.logging.ecs.EcsFieldNames.USER_NAME;
import static io.github.jframe.logging.ecs.EcsFieldNames.USER_ROLES;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Servlet filter that populates MDC with the authenticated user's identity and roles.
 *
 * <p>Reads the current {@link Authentication} from {@link SecurityContextHolder} and sets
 * {@code USER_NAME} and {@code USER_ROLES} ECS fields in MDC for structured logging.
 * MDC values are cleaned up in a {@code finally} block to prevent leakage to subsequent requests.
 *
 * <p>Authentication states:
 * <ul>
 * <li>No authentication → {@link AuthenticationConstants#ANONYMOUS}</li>
 * <li>Authentication with blank/null name → {@link AuthenticationConstants#INCOMPLETE}</li>
 * <li>Valid authentication → {@code authentication.getName()}</li>
 * </ul>
 */
public class UserIdentityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
        final FilterChain filterChain) throws ServletException, IOException {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            EcsFields.tag(USER_NAME, AuthenticationConstants.ANONYMOUS);
        } else if (authentication.getName() == null || isBlank(authentication.getName())) {
            EcsFields.tag(USER_NAME, AuthenticationConstants.INCOMPLETE);
        } else {
            EcsFields.tag(USER_NAME, authentication.getName());
        }

        if (authentication != null && authentication.getAuthorities() != null
            && !authentication.getAuthorities().isEmpty()) {
            final String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
            EcsFields.tag(USER_ROLES, roles);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            EcsFields.clear(USER_NAME);
            EcsFields.clear(USER_ROLES);
        }
    }
}
