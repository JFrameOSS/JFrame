package io.github.jframe.logging.filter.config;

import io.github.jframe.logging.filter.type.RequestDurationFilter;
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
 * Configuration class for the {@link RequestDurationFilter}.
 *
 * <p>This configuration enables automatic measurement and logging of HTTP request processing durations.
 * The filter captures the start and end time of each request and calculates the total processing time,
 * making it available via SLF4J MDC (Mapped Diagnostic Context) for inclusion in log entries.
 *
 * <p>Request duration tracking is essential for:
 * <ul>
 * <li>Performance monitoring and optimization</li>
 * <li>Identifying slow endpoints and bottlenecks</li>
 * <li>Service Level Agreement (SLA) compliance verification</li>
 * <li>Alerting on response time degradation</li>
 * <li>Production troubleshooting and performance analysis</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>The filter can be configured using the following properties:
 * <ul>
 * <li>{@code jframe.logging.filters.request-duration.enabled} - Enable/disable the filter (default: true)</li>
 * <li>{@code jframe.logging.filters.request-duration.order} - Filter execution order (default: -17500)</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>
 * jframe:
 * logging:
 * filters:
 * request-duration:
 * enabled: true
 * order: -17500
 * </pre>
 *
 * <p><strong>Note:</strong> The filter uses a {@link FilterVoter} to determine whether duration tracking
 * should be applied to a specific request. This allows for selective filtering based on paths,
 * content types, or other request characteristics.
 *
 * @see RequestDurationFilter
 * @see FilterVoter
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = RequestDurationFilterConfiguration.FILTER_PREFIX,
    name = "enabled",
    matchIfMissing = true
)
public class RequestDurationFilterConfiguration {

    /**
     * The configuration properties prefix for request duration filter settings.
     * Value: {@value}
     */
    public static final String FILTER_PREFIX = CONFIG_PREFIX + ".filters.request-duration";

    @Value("${" + FILTER_PREFIX + ".order:-17500}")
    private int filterOrder;

    /**
     * Create the {@link RequestDurationFilter} bean.
     *
     * @param voter The filter voter.
     * @return the {@link RequestDurationFilter} bean
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public RequestDurationFilter requestDurationFilter(final FilterVoter voter) {
        log.trace("Configuration: order '{}'.", filterOrder);
        return new RequestDurationFilter(voter);
    }

    /**
     * Register the {@link #requestDurationFilter(FilterVoter)} bean.
     *
     * @param filter the request duration filter
     * @return the {@link #requestDurationFilter(FilterVoter)} bean, wrapped in a {@link FilterRegistrationBean}
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public FilterRegistrationBean<RequestDurationFilter> requestDurationFilterRegistration(final RequestDurationFilter filter) {
        return register(filter, filterOrder);
    }
}
