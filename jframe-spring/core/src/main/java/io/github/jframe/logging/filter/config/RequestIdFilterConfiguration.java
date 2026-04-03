package io.github.jframe.logging.filter.config;

import io.github.jframe.logging.filter.type.RequestIdFilter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.jframe.autoconfigure.properties.LoggingProperties.CONFIG_PREFIX;
import static io.github.jframe.logging.filter.FilterRegistrator.register;
import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;

/**
 * Configuration class for the {@link RequestIdFilter}.
 *
 * <p>This configuration enables tracking of HTTP requests through a unique request identifier.
 * The filter extracts or generates a request ID from the HTTP request header and makes it
 * available throughout the request lifecycle via SLF4J MDC (Mapped Diagnostic Context).
 *
 * <p>The request ID is useful for:
 * <ul>
 * <li>Correlating log entries for a single request across multiple components</li>
 * <li>Distributed tracing across microservices</li>
 * <li>Debugging and troubleshooting in production environments</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>The filter can be configured using the following properties:
 * <ul>
 * <li>{@code jframe.logging.filters.request-id.enabled} - Enable/disable the filter (default: true)</li>
 * <li>{@code jframe.logging.filters.request-id.order} - Filter execution order (default: -400)</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>
 * jframe:
 * logging:
 * filters:
 * request-id:
 * enabled: true
 * order: -400
 * </pre>
 *
 * <p>The filter reads the request ID from the HTTP header defined by
 * {@link io.github.jframe.util.constants.Constants.Headers#REQ_ID_HEADER}.
 * If no request ID is present in the incoming request, a new UUID will be generated.
 *
 * @see RequestIdFilter
 * @see io.github.jframe.util.constants.Constants.Headers#REQ_ID_HEADER
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = RequestIdFilterConfiguration.FILTER_PREFIX,
    name = "enabled",
    matchIfMissing = true
)
public class RequestIdFilterConfiguration {

    /**
     * The configuration properties prefix for request ID filter settings.
     * Value: {@value}
     */
    public static final String FILTER_PREFIX = CONFIG_PREFIX + ".filters.request-id";

    @Value("${" + FILTER_PREFIX + ".order:-400}")
    private int filterOrder;

    /**
     * Create the {@link RequestIdFilter} bean.
     *
     * @return the {@link RequestIdFilter} bean
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public RequestIdFilter requestIdFilter() {
        log.trace("Configuration: header '{}', order '{}'.", REQ_ID_HEADER, filterOrder);
        return new RequestIdFilter(REQ_ID_HEADER);
    }

    /**
     * Register the {@link RequestIdFilter} bean.
     *
     * @param filter the {@link RequestIdFilter} bean.
     * @return the {@link #requestIdFilter()} bean, wrapped in a {@link FilterRegistrationBean}
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(final RequestIdFilter filter) {
        return register(filter, filterOrder);
    }
}
