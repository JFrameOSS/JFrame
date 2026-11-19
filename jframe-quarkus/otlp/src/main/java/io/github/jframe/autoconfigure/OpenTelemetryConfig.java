package io.github.jframe.autoconfigure;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;
import java.util.Set;

/**
 * SmallRye {@code @ConfigMapping} for jframe OTLP/OpenTelemetry configuration.
 *
 * <p>Bound and validated by Quarkus at startup, so malformed values fail fast.
 * Defaults mirror {@link io.github.jframe.tracing.OtlpDefaults}.
 */
@ConfigMapping(prefix = "jframe.otlp")
public interface OpenTelemetryConfig {

    /**
     * Whether OTLP tracing is disabled.
     *
     * @return {@code true} if tracing is disabled; defaults to {@code true} (explicit opt-in)
     */
    @WithDefault("true")
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
     * Method-name keywords excluded from tracing.
     *
     * @return excluded keywords; defaults to common health/monitoring names
     */
    @WithDefault("health,actuator,ping,status,info,metrics")
    Set<String> excludedMethods();

    /**
     * Comma-separated list of OpenTelemetry context propagators.
     *
     * @return the propagator names; defaults to {@code "tracecontext,baggage"}
     */
    @WithDefault("tracecontext,baggage")
    String propagators();

    /**
     * The sampler type for traces.
     *
     * @return the sampler type; defaults to {@code "parentbased_traceidratio"}
     */
    @WithDefault("parentbased_traceidratio")
    String samplerType();

    /**
     * Resource attribute keys excluded from exported telemetry (may leak JVM command-line secrets).
     *
     * @return excluded keys; defaults to the process command-line keys; empty when set to {@code ""}
     */
    default Set<String> excludedResourceAttributes() {
        return excludedResourceAttributesValue().orElse(Set.of());
    }

    /**
     * Raw binding for {@link #excludedResourceAttributes()}; {@link Optional} so {@code ""} means none.
     *
     * @return the configured keys, or empty when explicitly cleared
     */
    @WithName("excluded-resource-attributes")
    @WithDefault("process.command_args,process.command_line,process.executable.path")
    Optional<Set<String>> excludedResourceAttributesValue();

    /**
     * Whether the traces signal is exported.
     *
     * @return {@code true} if traces are enabled; defaults to {@code true}
     */
    @WithName("traces.enabled")
    @WithDefault("true")
    boolean tracesEnabled();

    /**
     * Whether the metrics signal is exported.
     *
     * @return {@code true} if metrics are enabled; defaults to {@code true}
     */
    @WithName("metrics.enabled")
    @WithDefault("true")
    boolean metricsEnabled();

    /**
     * Whether the logs signal is exported.
     *
     * @return {@code true} if logs are enabled; defaults to {@code true}
     */
    @WithName("logs.enabled")
    @WithDefault("true")
    boolean logsEnabled();
}
