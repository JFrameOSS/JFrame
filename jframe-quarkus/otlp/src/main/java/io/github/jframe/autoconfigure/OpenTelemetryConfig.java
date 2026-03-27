package io.github.jframe.autoconfigure;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
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

    private static final String PREFIX = "jframe.otlp.";

    private final ReentrantLock initLock = new ReentrantLock();
    private volatile boolean initialized;
    private boolean disabledValue;
    private String urlValue;
    private String timeoutValue;
    private String exporterValue;
    private double samplingRateValue;
    private Set<String> excludedMethodsValue;
    private String propagatorsValue;

    /**
     * CDI proxy constructor — no config reading happens here.
     */
    OpenTelemetryConfig() {
        // Intentionally empty — config is read lazily on first access
    }

    private void ensureInitialized() {
        if (!initialized) {
            initLock.lock();
            try {
                if (!initialized) {
                    final org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();
                    disabledValue = config.getOptionalValue(PREFIX + "disabled", Boolean.class).orElse(false);
                    urlValue = config.getOptionalValue(PREFIX + "url", String.class)
                        .orElse("http://localhost:4318");
                    timeoutValue = config.getOptionalValue(PREFIX + "timeout", String.class).orElse("10s");
                    exporterValue = config.getOptionalValue(PREFIX + "exporter", String.class).orElse("otlp");
                    samplingRateValue = config.getOptionalValue(PREFIX + "sampling-rate", Double.class).orElse(1.0);
                    propagatorsValue = config.getOptionalValue(PREFIX + "propagators", String.class)
                        .orElse("tracecontext,baggage");

                    final String excludedStr = config.getOptionalValue(PREFIX + "excluded-methods", String.class)
                        .orElse("health,actuator,ping,status,info,metrics");
                    excludedMethodsValue = Arrays.stream(excludedStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
                    initialized = true;
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
        return disabledValue;
    }

    /**
     * OTLP collector endpoint URL.
     *
     * @return the collector URL; defaults to {@code "http://localhost:4318"}
     */
    public String url() {
        ensureInitialized();
        return urlValue;
    }

    /**
     * Exporter connection timeout.
     *
     * @return the timeout string (e.g. {@code "10s"}); defaults to {@code "10s"}
     */
    public String timeout() {
        ensureInitialized();
        return timeoutValue;
    }

    /**
     * Exporter type to use for sending spans.
     *
     * @return the exporter name; defaults to {@code "otlp"}
     */
    public String exporter() {
        ensureInitialized();
        return exporterValue;
    }

    /**
     * Trace sampling rate between 0.0 (no sampling) and 1.0 (full sampling).
     *
     * @return the sampling rate; defaults to {@code 1.0}
     */
    public double samplingRate() {
        ensureInitialized();
        return samplingRateValue;
    }

    /**
     * Set of path segment keywords whose matching requests should be excluded from tracing.
     *
     * @return excluded path segments; defaults to common health/monitoring endpoints
     */
    public Set<String> excludedMethods() {
        ensureInitialized();
        return excludedMethodsValue;
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
        return propagatorsValue;
    }
}
