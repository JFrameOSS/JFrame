package io.github.jframe.autoconfigure;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Set;

/**
 * SmallRye {@code @ConfigMapping} for jframe OTLP/OpenTelemetry configuration.
 *
 * <p>All properties are prefixed with {@code jframe.otlp} and carry sensible defaults
 * so consumers only need to override what they need.
 */
@ConfigMapping(prefix = "jframe.otlp")
public interface OpenTelemetryConfig {

    /**
     * Whether OTLP tracing is disabled. Defaults to {@code false} (enabled by default).
     *
     * @return {@code true} if tracing is disabled; {@code false} to enable
     */
    @WithDefault("false")
    boolean disabled();

    /**
     * OTLP collector endpoint URL.
     *
     * @return the collector URL; defaults to {@code "http://localhost:4318"}
     */
    @WithDefault("http://localhost:4318")
    String url();

    /**
     * Exporter connection timeout.
     *
     * @return the timeout string (e.g. {@code "10s"}); defaults to {@code "10s"}
     */
    @WithDefault("10s")
    String timeout();

    /**
     * Exporter type to use for sending spans.
     *
     * @return the exporter name; defaults to {@code "otlp"}
     */
    @WithDefault("otlp")
    String exporter();

    /**
     * Trace sampling rate between 0.0 (no sampling) and 1.0 (full sampling).
     *
     * @return the sampling rate; defaults to {@code 1.0}
     */
    @WithDefault("1.0")
    double samplingRate();

    /**
     * Set of path segment keywords whose matching requests should be excluded from tracing.
     *
     * @return excluded path segments; defaults to common health/monitoring endpoints
     */
    @WithDefault("health,actuator,ping,status,info,metrics")
    Set<String> excludedMethods();

    /**
     * Whether automatic instrumentation of JAX-RS endpoints is active.
     *
     * @return {@code true} to enable auto-tracing; defaults to {@code true}
     */
    @WithDefault("true")
    boolean autoTrace();
}
