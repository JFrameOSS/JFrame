package io.github.jframe.tracing;

import io.quarkus.runtime.StartupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Validates {@link OpenTelemetryConfig} properties at application startup.
 * Logs warnings for invalid configuration values rather than failing startup,
 * since this is a library and should not prevent consumer applications from starting.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OpenTelemetryConfigValidator {

    private final OpenTelemetryConfig config;

    /**
     * Observes the Quarkus {@link StartupEvent} and validates all OTLP configuration properties.
     * Skips validation entirely when {@link OpenTelemetryConfig#disabled()} returns {@code true}.
     *
     * @param event the CDI startup event (unused, present for CDI observer resolution)
     */
    void onStartup(@Observes final StartupEvent event) {
        if (config.disabled()) {
            return;
        }
        validateUrl();
        validateTimeout();
        validateExporter();
        validateSamplingRate();
    }

    private void validateUrl() {
        if (config.url() == null || config.url().isBlank()) {
            log.warn("jframe.otlp.url must not be blank, using default");
        }
    }

    private void validateTimeout() {
        if (config.timeout() == null || !config.timeout().matches("\\d+[smh]")) {
            log.warn("jframe.otlp.timeout must be in format: number followed by s/m/h (e.g. 10s), got: {}", config.timeout());
        }
    }

    private void validateExporter() {
        if (config.exporter() == null || !config.exporter().matches("otlp|jaeger|zipkin")) {
            log.warn("jframe.otlp.exporter must be one of: otlp, jaeger, zipkin, got: {}", config.exporter());
        }
    }

    private void validateSamplingRate() {
        if (config.samplingRate() < 0.0 || config.samplingRate() > 1.0) {
            log.warn("jframe.otlp.sampling-rate must be between 0.0 and 1.0, got: {}", config.samplingRate());
        }
    }
}
