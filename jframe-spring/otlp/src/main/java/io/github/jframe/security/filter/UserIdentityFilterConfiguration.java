package io.github.jframe.security.filter;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.jframe.autoconfigure.properties.LoggingProperties.CONFIG_PREFIX;
import static io.github.jframe.logging.filter.FilterRegistrator.register;

/**
 * Configuration class for the {@link UserIdentityFilter}.
 *
 * <p>This configuration populates MDC with the authenticated user's identity and roles
 * from Spring Security's {@link org.springframework.security.core.context.SecurityContextHolder}.
 * The filter runs after authentication filters (order 100) so that the security context
 * is populated before the user identity is read.
 *
 * <h2>Configuration Properties</h2>
 * <ul>
 * <li>{@code jframe.logging.filters.user-identity.enabled} - Enable/disable the filter (default: true)</li>
 * <li>{@code jframe.logging.filters.user-identity.order} - Filter execution order (default: 100)</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>
 * jframe:
 * logging:
 * filters:
 * user-identity:
 * enabled: true
 * order: 100
 * </pre>
 *
 * @see UserIdentityFilter
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = UserIdentityFilterConfiguration.FILTER_PREFIX,
    name = "enabled",
    matchIfMissing = true
)
public class UserIdentityFilterConfiguration {

    /**
     * The configuration properties prefix for user identity filter settings.
     * Value: {@value}
     */
    public static final String FILTER_PREFIX = CONFIG_PREFIX + ".filters.user-identity";

    @Value("${" + FILTER_PREFIX + ".order:100}")
    private int filterOrder;

    /**
     * Create the {@link UserIdentityFilter} bean.
     *
     * @return the {@link UserIdentityFilter} bean
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public UserIdentityFilter userIdentityFilter() {
        log.trace("Configuration: order '{}'.", filterOrder);
        return new UserIdentityFilter();
    }

    /**
     * Register the {@link UserIdentityFilter} bean.
     *
     * @param filter the {@link UserIdentityFilter} bean.
     * @return the {@link #userIdentityFilter()} bean, wrapped in a {@link FilterRegistrationBean}
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public FilterRegistrationBean<UserIdentityFilter> userIdentityFilterRegistration(
        final UserIdentityFilter filter) {
        return register(filter, filterOrder);
    }
}
