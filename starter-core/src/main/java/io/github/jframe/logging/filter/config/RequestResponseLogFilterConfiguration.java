package io.github.jframe.logging.filter.config;

import io.github.jframe.logging.filter.type.RequestResponseLogFilter;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.voter.FilterVoter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.jframe.autoconfigure.properties.LoggingProperties.CONFIG_PREFIX;
import static io.github.jframe.logging.filter.FilterRegistrator.register;

/**
 * Configuration class for the {@link RequestResponseLogFilter}.
 *
 * <p>This configuration enables comprehensive logging of HTTP request and response details including
 * headers, bodies, status codes, and other metadata. The filter provides detailed visibility into
 * API interactions for debugging, auditing, and compliance purposes.
 *
 * <p>Request and response logging is valuable for:
 * <ul>
 * <li>Debugging integration issues and API problems</li>
 * <li>Security auditing and compliance requirements</li>
 * <li>API usage analysis and monitoring</li>
 * <li>Troubleshooting production incidents</li>
 * <li>Understanding data flow in distributed systems</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>The filter can be configured using the following properties:
 * <ul>
 * <li>{@code jframe.logging.filters.request-response.enabled} - Enable/disable the filter (default: true)</li>
 * <li>{@code jframe.logging.filters.request-response.order} - Filter execution order (default: -950)</li>
 * <li>{@code jframe.logging.response-length} - Maximum response body length to log (default: -1 for unlimited)</li>
 * <li>{@code jframe.logging.allowed-content-types} - Content types to include in logging</li>
 * <li>{@code jframe.logging.exclude-paths} - Request paths to exclude from logging</li>
 * <li>{@code jframe.logging.fields-to-mask} - Sensitive field names to mask in logs</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>
 * jframe:
 * logging:
 * response-length: 10000
 * allowed-content-types:
 * - application/json
 * - application/xml
 * exclude-paths:
 * - /health
 * - /actuator/**
 * fields-to-mask:
 * - password
 * - token
 * - apiKey
 * filters:
 * request-response:
 * enabled: true
 * order: -950
 * </pre>
 *
 * <p><strong>Security Note:</strong> This filter automatically masks sensitive fields (like passwords)
 * in request and response bodies. Configure {@code fields-to-mask} to add additional sensitive field names.
 *
 * <p><strong>Performance Note:</strong> The filter uses a {@link FilterVoter} to selectively log requests
 * based on content type and path patterns, minimizing performance impact on high-volume endpoints.
 *
 * @see RequestResponseLogFilter
 * @see RequestResponseLogger
 * @see FilterVoter
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = RequestResponseLogFilterConfiguration.FILTER_PREFIX,
    name = "enabled",
    matchIfMissing = true
)
public class RequestResponseLogFilterConfiguration {

    /**
     * The configuration properties prefix for request/response log filter settings.
     * Value: {@value}
     */
    public static final String FILTER_PREFIX = CONFIG_PREFIX + ".filters.request-response";

    @Value("${" + FILTER_PREFIX + ".order:-950}")
    private int filterOrder;

    /**
     * Create the request/response logging filter bean.
     *
     * @param voter  The filter voter.
     * @param logger The logger.
     * @return the {@link RequestResponseLogFilter} bean
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public RequestResponseLogFilter requestResponseLogFilter(final FilterVoter voter, final RequestResponseLogger logger) {
        log.trace("Configuration: order '{}'.", filterOrder);
        return new RequestResponseLogFilter(logger, voter);
    }

    /**
     * Create and register the {@link RequestResponseLogFilter} bean.
     *
     * @param filter The filter to register.
     * @return the requestResponseLogFilter bean, wrapped in a {@link FilterRegistrationBean}
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public FilterRegistrationBean<RequestResponseLogFilter> requestResponseLogFilterRegistration(final RequestResponseLogFilter filter) {
        return register(filter, filterOrder);
    }
}
