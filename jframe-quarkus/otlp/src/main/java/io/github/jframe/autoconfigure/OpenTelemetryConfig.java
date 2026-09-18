package io.github.jframe.autoconfigure;

import io.github.jframe.tracing.OtlpDefaults;
import io.smallrye.config.SmallRyeConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * CDI bean that reads jframe OTLP/OpenTelemetry configuration from the MicroProfile Config API.
 *
 * <p>All properties are prefixed with {@code jframe.otlp} and carry sensible defaults
 * so consumers only need to override what they need.
 *
 * <p>Configuration is read lazily on first access to avoid issues with CDI static-init
 * phase ordering. The {@link org.eclipse.microprofile.config.Config} instance is only
 * obtained when a property accessor is first called, ensuring all Quarkus runtime
 * services (such as TLS configuration) are fully initialized.
 */
@Slf4j
@ApplicationScoped
public class OpenTelemetryConfig {

    private static final String PREFIX = OtlpDefaults.PREFIX;

    private final ReentrantLock initLock = new ReentrantLock();
    private volatile ConfigValues values;

    /**
     * CDI proxy constructor — no config reading happens here.
     */
    OpenTelemetryConfig() {
        // Intentionally empty — config is read lazily on first access
    }

    private void ensureInitialized() {
        if (values == null) {
            initLock.lock();
            try {
                if (values == null) {
                    final SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                    final boolean disabled = config.getOptionalValue(PREFIX + "disabled", Boolean.class)
                        .orElse(OtlpDefaults.DEFAULT_DISABLED);
                    final String url = config.getOptionalValue(PREFIX + "url", String.class)
                        .orElse(OtlpDefaults.DEFAULT_URL);
                    final String timeout = config.getOptionalValue(PREFIX + "timeout", String.class)
                        .orElse(OtlpDefaults.DEFAULT_TIMEOUT);
                    final String exporter = config.getOptionalValue(PREFIX + "exporter", String.class)
                        .orElse(OtlpDefaults.DEFAULT_EXPORTER);
                    final double samplingRate = config.getOptionalValue(PREFIX + "sampling-rate", Double.class)
                        .orElse(OtlpDefaults.DEFAULT_SAMPLING_RATE);
                    final String propagators = config.getOptionalValue(PREFIX + "propagators", String.class)
                        .orElse(OtlpDefaults.DEFAULT_PROPAGATORS);
                    final String excludedStr = config.getOptionalValue(PREFIX + "excluded-methods", String.class)
                        .orElse(OtlpDefaults.DEFAULT_EXCLUDED_METHODS);
                    final Set<String> excludedMethods = OtlpDefaults.parseCommaSeparated(excludedStr);
                    final String samplerType = config.getOptionalValue(PREFIX + "sampler-type", String.class)
                        .orElse(OtlpDefaults.DEFAULT_SAMPLER_TYPE);
                    // Use getConfigValue to preserve explicit empty-string overrides:
                    // getOptionalValue treats "" as absent and returns Optional.empty(),
                    // but we must yield an empty Set when the consumer explicitly clears the list.
                    final io.smallrye.config.ConfigValue excludedResourceConfigValue =
                        config.getConfigValue(PREFIX + "excluded-resource-attributes");
                    final String excludedResourceStr = excludedResourceConfigValue.getRawValue() != null
                        ? excludedResourceConfigValue.getRawValue()
                        : OtlpDefaults.DEFAULT_EXCLUDED_RESOURCE_ATTRIBUTES;
                    final Set<String> excludedResourceAttributes = OtlpDefaults.parseCommaSeparated(excludedResourceStr);
                    final boolean tracesEnabled = config.getOptionalValue(PREFIX + "traces.enabled", Boolean.class)
                        .orElse(OtlpDefaults.DEFAULT_TRACES_ENABLED);
                    final boolean metricsEnabled = config.getOptionalValue(PREFIX + "metrics.enabled", Boolean.class)
                        .orElse(OtlpDefaults.DEFAULT_METRICS_ENABLED);
                    final boolean logsEnabled = config.getOptionalValue(PREFIX + "logs.enabled", Boolean.class)
                        .orElse(OtlpDefaults.DEFAULT_LOGS_ENABLED);
                    values = new ConfigValues(
                        disabled,
                        url,
                        timeout,
                        exporter,
                        samplingRate,
                        excludedMethods,
                        propagators,
                        samplerType,
                        excludedResourceAttributes,
                        tracesEnabled,
                        metricsEnabled,
                        logsEnabled
                    );
                }
            } finally {
                initLock.unlock();
            }
        }
    }

    /**
     * Whether OTLP tracing is disabled. Defaults to {@code false} (enabled by default).
     *
     * @return {@code true} if tracing is disabled; {@code false} to enable
     */
    public boolean disabled() {
        ensureInitialized();
        return values.disabled;
    }

    /**
     * OTLP collector endpoint URL.
     *
     * @return the collector URL; defaults to {@code "http://localhost:4318"}
     */
    public String url() {
        ensureInitialized();
        return values.url;
    }

    /**
     * Exporter connection timeout.
     *
     * @return the timeout string (e.g. {@code "10s"}); defaults to {@code "10s"}
     */
    public String timeout() {
        ensureInitialized();
        return values.timeout;
    }

    /**
     * Exporter type to use for sending spans.
     *
     * @return the exporter name; defaults to {@code "otlp"}
     */
    public String exporter() {
        ensureInitialized();
        return values.exporter;
    }

    /**
     * Trace sampling rate between 0.0 (no sampling) and 1.0 (full sampling).
     *
     * @return the sampling rate; defaults to {@code 1.0}
     */
    public double samplingRate() {
        ensureInitialized();
        return values.samplingRate;
    }

    /**
     * Set of path segment keywords whose matching requests should be excluded from tracing.
     *
     * @return excluded path segments; defaults to common health/monitoring endpoints
     */
    public Set<String> excludedMethods() {
        ensureInitialized();
        return values.excludedMethods;
    }

    /**
     * Comma-separated list of OpenTelemetry context propagators.
     *
     * <p>Defaults to W3C {@code tracecontext,baggage} which requires no additional
     * dependencies. To use B3 (Zipkin) or Jaeger propagation, add the corresponding
     * propagator artifact to the classpath and include it here (e.g.
     * {@code "tracecontext,baggage,b3"}).
     *
     * @return the propagator names; defaults to {@code "tracecontext,baggage"}
     */
    public String propagators() {
        ensureInitialized();
        return values.propagators;
    }

    /**
     * The sampler type for traces.
     *
     * @return the sampler type; defaults to {@code "parentbased_traceidratio"}
     */
    public String samplerType() {
        ensureInitialized();
        return values.samplerType;
    }

    /**
     * Set of resource attribute keys to exclude from exported telemetry.
     *
     * <p>These attributes are excluded because they may expose secrets passed as JVM
     * command-line flags (e.g. {@code -Dspring.datasource.password=...}).
     *
     * @return excluded resource attribute keys; defaults to process command-line keys
     */
    public Set<String> excludedResourceAttributes() {
        ensureInitialized();
        return values.excludedResourceAttributes;
    }

    /**
     * Whether traces signal is enabled.
     *
     * @return {@code true} if traces are enabled; defaults to {@code true}
     */
    public boolean tracesEnabled() {
        ensureInitialized();
        return values.tracesEnabled;
    }

    /**
     * Whether metrics signal is enabled.
     *
     * @return {@code true} if metrics are enabled; defaults to {@code true}
     */
    public boolean metricsEnabled() {
        ensureInitialized();
        return values.metricsEnabled;
    }

    /**
     * Whether logs signal is enabled.
     *
     * @return {@code true} if logs are enabled; defaults to {@code true}
     */
    public boolean logsEnabled() {
        ensureInitialized();
        return values.logsEnabled;
    }

    private static final class ConfigValues {

        private final boolean disabled;
        private final String url;
        private final String timeout;
        private final String exporter;
        private final double samplingRate;
        private final Set<String> excludedMethods;
        private final String propagators;
        private final String samplerType;
        private final Set<String> excludedResourceAttributes;
        private final boolean tracesEnabled;
        private final boolean metricsEnabled;
        private final boolean logsEnabled;

        @SuppressWarnings(
            {
                "checkstyle:ParameterNumber",
                "PMD.ExcessiveParameterList"
            }
        )
        ConfigValues(
                     final boolean disabled,
                     final String url,
                     final String timeout,
                     final String exporter,
                     final double samplingRate,
                     final Set<String> excludedMethods,
                     final String propagators,
                     final String samplerType,
                     final Set<String> excludedResourceAttributes,
                     final boolean tracesEnabled,
                     final boolean metricsEnabled,
                     final boolean logsEnabled) {

            this.disabled = disabled;
            this.url = url;
            this.timeout = timeout;
            this.exporter = exporter;
            this.samplingRate = samplingRate;
            this.excludedMethods = excludedMethods;
            this.propagators = propagators;
            this.samplerType = samplerType;
            this.excludedResourceAttributes = excludedResourceAttributes;
            this.tracesEnabled = tracesEnabled;
            this.metricsEnabled = metricsEnabled;
            this.logsEnabled = logsEnabled;
        }
    }
}
