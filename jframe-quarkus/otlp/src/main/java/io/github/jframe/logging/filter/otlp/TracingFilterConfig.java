package io.github.jframe.logging.filter.otlp;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus SmallRye Config mapping for JFrame tracing filter configuration.
 *
 * <p>Binds {@code jframe.logging.filters.tracing-response.*} and
 * {@code jframe.logging.filters.outbound-tracing.*} configuration properties, providing sensible
 * defaults so that both tracing filters are enabled out-of-the-box.
 *
 * <p>Example {@code application.properties} overrides:
 *
 * <pre>{@code
 * jframe.logging.filters.tracing-response.enabled=false
 * jframe.logging.filters.outbound-tracing.enabled=false
 * }</pre>
 */
@ConfigMapping(prefix = "jframe.logging.filters")
public interface TracingFilterConfig {

    /**
     * Configuration for the tracing response filter.
     *
     * @return the tracing response filter config
     */
    TracingResponseConfig tracingResponse();

    /**
     * Configuration for the outbound tracing filter.
     *
     * @return the outbound tracing filter config
     */
    OutboundTracingConfig outboundTracing();

    /** Configuration for the {@code TracingResponseFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface TracingResponseConfig {

        /**
         * Whether the tracing response filter is enabled.
         *
         * @return {@code true} (default) to enable the filter
         */
        @WithDefault("true")
        boolean enabled();
    }


    /** Configuration for the {@code OutboundTracingFilter}. */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    interface OutboundTracingConfig {

        /**
         * Whether the outbound tracing filter is enabled.
         *
         * @return {@code true} (default) to enable the filter
         */
        @WithDefault("true")
        boolean enabled();
    }
}
