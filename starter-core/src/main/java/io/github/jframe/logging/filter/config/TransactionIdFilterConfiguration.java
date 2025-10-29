package io.github.jframe.logging.filter.config;

import io.github.jframe.logging.filter.type.TransactionIdFilter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.jframe.autoconfigure.properties.LoggingProperties.CONFIG_PREFIX;
import static io.github.jframe.logging.filter.FilterRegistrator.register;
import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;

/**
 * Configuration class for the {@link TransactionIdFilter}.
 *
 * <p>This configuration enables tracking of business transactions across multiple HTTP requests
 * through a unique transaction identifier. The filter extracts or generates a transaction ID
 * from the HTTP request header and makes it available throughout the request lifecycle via
 * SLF4J MDC (Mapped Diagnostic Context).
 *
 * <p>The transaction ID differs from the request ID in that it spans multiple requests that
 * belong to the same logical business transaction. This is particularly useful for:
 * <ul>
 * <li>Tracking multi-step business processes across service boundaries</li>
 * <li>Correlating requests in asynchronous workflows</li>
 * <li>End-to-end tracing in distributed systems</li>
 * <li>Analyzing business flows in production environments</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>The filter can be configured using the following properties:
 * <ul>
 * <li>{@code jframe.logging.filters.transaction-id.enabled} - Enable/disable the filter (default: true)</li>
 * <li>{@code jframe.logging.filters.transaction-id.order} - Filter execution order (default: -500)</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>
 * jframe:
 * logging:
 * filters:
 * transaction-id:
 * enabled: true
 * order: -500
 * </pre>
 *
 * <p>The filter reads the transaction ID from the HTTP header defined by
 * {@link io.github.jframe.util.constants.Constants.Headers#TX_ID_HEADER}.
 * If no transaction ID is present in the incoming request, a new UUID will be generated.
 *
 * @see TransactionIdFilter
 * @see io.github.jframe.util.constants.Constants.Headers#TX_ID_HEADER
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = TransactionIdFilterConfiguration.FILTER_PREFIX,
    name = "enabled",
    matchIfMissing = true
)
public class TransactionIdFilterConfiguration {

    /**
     * The configuration properties prefix for transaction ID filter settings.
     * Value: {@value}
     */
    public static final String FILTER_PREFIX = CONFIG_PREFIX + ".filters.transaction-id";

    @Value("${" + FILTER_PREFIX + ".order:-500}")
    private int filterOrder;

    /**
     * Create the {@link TransactionIdFilter} bean.
     *
     * @return the {@link TransactionIdFilter} bean
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public TransactionIdFilter transactionIdFilter() {
        log.trace("Configuration: header '{}', order '{}'.", TX_ID_HEADER, filterOrder);
        return new TransactionIdFilter(TX_ID_HEADER);
    }

    /**
     * Register the {@link #transactionIdFilter()} bean.
     *
     * @param filter the transaction id filter
     * @return the {@link #transactionIdFilter()} bean, wrapped in a {@link FilterRegistrationBean}
     */
    @Bean
    @ConditionalOnProperty(
        prefix = FILTER_PREFIX,
        name = "enabled",
        matchIfMissing = true
    )
    public FilterRegistrationBean<TransactionIdFilter> transactionIdFilterRegistration(final TransactionIdFilter filter) {
        return register(filter, filterOrder);
    }
}
