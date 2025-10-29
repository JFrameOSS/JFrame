package io.github.jframe.logging.filter.config;

import io.github.jframe.logging.filter.type.TracingResponseFilter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.jframe.autoconfigure.properties.LoggingProperties.CONFIG_PREFIX;
import static io.github.jframe.logging.filter.FilterRegistrator.register;
import static io.github.jframe.util.constants.Constants.Headers.SPAN_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;


/**
 * Configuration class for the {@link TracingResponseFilter}.
 *
 * <p>This configuration enables OpenTelemetry trace ID propagation in HTTP responses.
 * The filter extracts the current OpenTelemetry trace ID from the active span context
 * and adds it to the HTTP response headers, allowing clients to correlate their requests
 * with backend traces for end-to-end observability.
 *
 * <p>Trace ID response headers are valuable for:
 * <ul>
 * <li>End-to-end distributed tracing across frontend and backend systems</li>
 * <li>Correlating client-side errors with backend traces in APM tools</li>
 * <li>Debugging issues by linking user-reported errors to specific traces</li>
 * <li>Providing trace context to external monitoring and logging systems</li>
 * <li>Supporting trace continuity in microservices architectures</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>The filter can be configured using the following properties:
 * <ul>
 * <li>{@code jframe.logging.filters.tracing-id.enabled} - Enable/disable the filter (default: true)</li>
 * <li>{@code jframe.logging.filters.tracing-id.order} - Filter execution order (default: -1000)</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>
 * jframe:
 * logging:
 * filters:
 * tracing-id:
 * enabled: true
 * order: -1000
 * </pre>
 *
 * <p>The filter writes the OpenTelemetry trace ID to the HTTP response header defined by
 * {@link io.github.jframe.util.constants.Constants.Headers#TRACE_ID_HEADER}.
 *
 * <p><strong>Note:</strong> This filter requires OpenTelemetry instrumentation to be active.
 * If no active span is available, the header will not be added to the response.
 *
 * @see TracingResponseFilter
 * @see io.github.jframe.util.constants.Constants.Headers#TRACE_ID_HEADER
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = TracingIdResponseFilterConfiguration.FILTER_PREFIX,
    name = "enabled",
    matchIfMissing = true
)
public class TracingIdResponseFilterConfiguration {

    /**
     * The configuration properties prefix for tracing ID filter settings.
     * Value: {@value}
     */
    public static final String FILTER_PREFIX = CONFIG_PREFIX + ".filters.tracing-id";

    @Value("${" + FILTER_PREFIX + ".order:-1000}")
    private int filterOrder;

    /**
     * Create the {@link TracingResponseFilter} bean.
     *
     * @return the {@link TracingResponseFilter} bean
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public TracingResponseFilter traceIdResponseFilter() {
        log.trace("Configuration: headers '{}, {}', order '{}'.", TRACE_ID_HEADER, SPAN_ID_HEADER, filterOrder);
        return new TracingResponseFilter(TRACE_ID_HEADER, SPAN_ID_HEADER);
    }

    /**
     * Create the {@link FilterRegistrationBean} for the {@link TracingResponseFilter}.
     *
     * @param filter the {@link TracingResponseFilter} to register
     * @return the {@link FilterRegistrationBean} for the {@link TracingResponseFilter}
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public FilterRegistrationBean<TracingResponseFilter> openTelemetryResponseFilterRegistration(
        final TracingResponseFilter filter) {
        return register(filter, filterOrder);
    }
}
