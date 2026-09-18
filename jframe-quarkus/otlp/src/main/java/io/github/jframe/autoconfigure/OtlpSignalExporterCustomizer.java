package io.github.jframe.autoconfigure;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Wires the {@code jframe.otlp.{traces,metrics,logs}.enabled} toggles to the OTel SDK so that
 * disabling a signal actually stops it from being exported.
 *
 * <p>When a signal is disabled, this customizer adds {@code otel.{signal}.exporter=none} to the
 * properties seen by the autoconfigured SDK. Because {@link AutoConfiguredOpenTelemetrySdkBuilder}
 * merges additional property suppliers in order — last-added wins — this value overwrites the
 * Quarkus-contributed {@code otel.{signal}.exporter=cdi} that originates from
 * {@code quarkus.otel.{signal}.exporter=cdi} in {@code microprofile-config.properties}.
 *
 * <p>When a signal is enabled, no entry is added and the Quarkus default ({@code cdi}) is
 * preserved intact — preserving the CDI exporter bridge that Quarkus uses to wire its native
 * OTLP pipeline.
 *
 * <p>This is the correct mechanism to use instead of the build-time properties
 * {@code quarkus.otel.{signal}.enabled}, which are baked at build time and cannot be driven
 * by a runtime {@code jframe.otlp.*} property.
 */
@Slf4j
@ApplicationScoped
public class OtlpSignalExporterCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

    private static final String EXPORTER_NONE = "none";

    private final OpenTelemetryConfig otlpConfig;

    /**
     * CDI injection constructor.
     *
     * @param otlpConfig the jframe OTLP configuration bean
     */
    @Inject
    OtlpSignalExporterCustomizer(final OpenTelemetryConfig otlpConfig) {
        this.otlpConfig = otlpConfig;
    }

    /**
     * CDI proxy constructor — do not use directly.
     */
    OtlpSignalExporterCustomizer() {
        this(null);
    }

    @Override
    public void customize(final AutoConfiguredOpenTelemetrySdkBuilder builder) {
        final Map<String, String> overrides = buildExporterOverrides();
        if (!overrides.isEmpty()) {
            log.debug("Disabling OTel signal exporters: {}", overrides.keySet());
            builder.addPropertiesSupplier(() -> overrides);
        }
    }

    private Map<String, String> buildExporterOverrides() {
        final Map<String, String> overrides = new HashMap<>();
        if (!otlpConfig.tracesEnabled()) {
            overrides.put("otel.traces.exporter", EXPORTER_NONE);
        }
        if (!otlpConfig.metricsEnabled()) {
            overrides.put("otel.metrics.exporter", EXPORTER_NONE);
        }
        if (!otlpConfig.logsEnabled()) {
            overrides.put("otel.logs.exporter", EXPORTER_NONE);
        }
        return overrides;
    }
}
