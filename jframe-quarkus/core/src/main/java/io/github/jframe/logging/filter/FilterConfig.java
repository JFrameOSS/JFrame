package io.github.jframe.logging.filter;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus SmallRye Config mapping for JFrame HTTP filter configuration.
 *
 * <p>Binds all {@code jframe.logging.filters.*} configuration properties into a single typed
 * interface. Most filters are <em>enabled by default</em>; opt-out by setting
 * {@code jframe.logging.filters.<filter-name>.enabled=false}.
 *
 * <p>Filters ON by default: {@code request-duration}, {@code request-response},
 * {@code user-identity}.
 *
 * <p>Filters OFF by default: {@code transaction-id}, {@code request-id},
 * {@code outbound-correlation}, {@code outbound-logging}.
 *
 * <p>Example {@code application.properties} overrides:
 *
 * <pre>{@code
 * jframe.logging.filters.transaction-id.enabled=true
 * jframe.logging.filters.request-id.enabled=true
 * jframe.logging.filters.request-duration.enabled=false
 * jframe.logging.filters.request-response.enabled=false
 * jframe.logging.filters.outbound-correlation.enabled=true
 * jframe.logging.filters.outbound-logging.enabled=true
 * jframe.logging.filters.user-identity.enabled=false
 * }</pre>
 */
@ConfigMapping(prefix = "jframe.logging.filters")
public interface FilterConfig {

    /**
     * Configuration for the transaction ID filter.
     *
     * @return the transaction ID filter config
     */
    TransactionIdConfig transactionId();

    /**
     * Configuration for the request ID filter.
     *
     * @return the request ID filter config
     */
    RequestIdConfig requestId();

    /**
     * Configuration for the request duration filter.
     *
     * @return the request duration filter config
     */
    RequestDurationConfig requestDuration();

    /**
     * Configuration for the request/response log filter.
     *
     * @return the request/response log filter config
     */
    RequestResponseConfig requestResponse();

    /**
     * Configuration for the outbound correlation filter.
     *
     * @return the outbound correlation filter config
     */
    OutboundCorrelationConfig outboundCorrelation();

    /**
     * Configuration for the outbound logging filter.
     *
     * @return the outbound logging filter config
     */
    OutboundLoggingConfig outboundLogging();

    /**
     * Configuration for the user identity filter.
     *
     * @return the user identity filter config
     */
    UserIdentityConfig userIdentity();

    /** Configuration for the {@code TransactionIdFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface TransactionIdConfig {

        /**
         * Whether the transaction ID filter is enabled.
         *
         * @return {@code false} (default) — opt in to enable the filter
         */
        @WithDefault("false")
        boolean enabled();
    }


    /** Configuration for the {@code RequestIdFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface RequestIdConfig {

        /**
         * Whether the request ID filter is enabled.
         *
         * @return {@code false} (default) — opt in to enable the filter
         */
        @WithDefault("false")
        boolean enabled();
    }


    /** Configuration for the {@code RequestDurationFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface RequestDurationConfig {

        /**
         * Whether the request duration filter is enabled.
         *
         * @return {@code true} (default) — opt out to disable the filter
         */
        @WithDefault("true")
        boolean enabled();
    }


    /** Configuration for the {@code RequestResponseLogFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface RequestResponseConfig {

        /**
         * Whether the request/response log filter is enabled.
         *
         * @return {@code true} (default) — opt out to disable the filter
         */
        @WithDefault("true")
        boolean enabled();
    }


    /** Configuration for the {@code OutboundCorrelationFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface OutboundCorrelationConfig {

        /**
         * Whether the outbound correlation filter is enabled.
         *
         * @return {@code false} (default) — opt in to enable the filter
         */
        @WithDefault("false")
        boolean enabled();
    }


    /** Configuration for the {@code OutboundLoggingFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface OutboundLoggingConfig {

        /**
         * Whether the outbound logging filter is enabled.
         *
         * @return {@code false} (default) — opt in to enable the filter
         */
        @WithDefault("false")
        boolean enabled();
    }


    /** Configuration for the {@code UserIdentityFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface UserIdentityConfig {

        /**
         * Whether the user identity filter is enabled.
         *
         * @return {@code true} (default) — opt out to disable the filter
         */
        @WithDefault("true")
        boolean enabled();
    }
}
