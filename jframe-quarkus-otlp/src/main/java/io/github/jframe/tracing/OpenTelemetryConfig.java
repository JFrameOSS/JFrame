package io.github.jframe.tracing;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Set;

/**
 * Quarkus SmallRye Config mapping for JFrame OpenTelemetry properties.
 *
 * <p>Binds all {@code jframe.otlp.*} configuration properties into a single typed interface,
 * providing sensible defaults for every setting so that applications work out-of-the-box
 * without any explicit configuration.
 *
 * <p>Example {@code application.properties} overrides:
 *
 * <pre>{@code
 * jframe.otlp.disabled=false
 * jframe.otlp.url=http://collector:4318
 * jframe.otlp.timeout=5s
 * jframe.otlp.exporter=otlp
 * jframe.otlp.sampling-rate=0.5
 * jframe.otlp.excluded-methods=health,ping
 * }</pre>
 */
@ConfigMapping(prefix = "jframe.otlp")
public interface OpenTelemetryConfig {

    /**
     * Whether OpenTelemetry tracing is disabled.
     *
     * @return {@code true} to suppress all tracing; {@code false} (default) to enable it
     */
    @WithDefault("false")
    boolean disabled();

    /**
     * Base URL of the OTLP collector endpoint.
     *
     * @return the OTLP exporter URL; defaults to {@code http://localhost:4318}
     */
    @WithDefault("http://localhost:4318")
    String url();

    /**
     * Export timeout as a duration string (e.g. {@code 10s}, {@code 500ms}).
     *
     * @return the export timeout; defaults to {@code 10s}
     */
    @WithDefault("10s")
    String timeout();

    /**
     * Name of the span exporter to use (e.g. {@code otlp}, {@code zipkin}, {@code logging}).
     *
     * @return the exporter name; defaults to {@code otlp}
     */
    @WithDefault("otlp")
    String exporter();

    /**
     * Fraction of traces to sample, between {@code 0.0} (none) and {@code 1.0} (all).
     *
     * @return the sampling rate; defaults to {@code 1.0}
     */
    @WithDefault("1.0")
    double samplingRate();

    /**
     * HTTP path segments whose requests should be excluded from tracing.
     *
     * <p>The default set covers common infrastructure endpoints that generate noise.
     *
     * @return an immutable set of excluded path segments; defaults to
     *         {@code health,actuator,ping,status}
     */
    @WithDefault("health,actuator,ping,status")
    Set<String> excludedMethods();
}
