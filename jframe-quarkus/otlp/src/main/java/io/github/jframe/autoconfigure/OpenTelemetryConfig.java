package io.github.jframe.autoconfigure;

import io.github.jframe.tracing.OtlpDefaults;
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
                    final org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();
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
                    values = new ConfigValues(disabled, url, timeout, exporter, samplingRate, excludedMethods, propagators);
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

    private static final class ConfigValues {

        private final boolean disabled;
        private final String url;
        private final String timeout;
        private final String exporter;
        private final double samplingRate;
        private final Set<String> excludedMethods;
        private final String propagators;

        ConfigValues(
                     final boolean disabled,
                     final String url,
                     final String timeout,
                     final String exporter,
                     final double samplingRate,
                     final Set<String> excludedMethods,
                     final String propagators) {

            this.disabled = disabled;
            this.url = url;
            this.timeout = timeout;
            this.exporter = exporter;
            this.samplingRate = samplingRate;
            this.excludedMethods = excludedMethods;
            this.propagators = propagators;
        }
    }
}
