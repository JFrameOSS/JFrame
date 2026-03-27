package io.github.jframe.autoconfigure.properties;

import io.github.jframe.tracing.OtlpDefaults;
import lombok.Data;

import java.util.Set;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the JFrame OpenTelemetry (OTLP) integration module.
 * <p>
 * These properties control how the framework exports tracing and telemetry data
 * to an OpenTelemetry-compatible endpoint. The configuration is typically
 * defined in your {@code application.yml} or {@code application.properties} file
 * using the prefix {@code jframe.otlp}.
 * </p>
 *
 * <p><strong>Example configuration:</strong></p>
 * <pre>{@code
 * jframe:
 *   otlp:
 *     disabled: false
 *     url: http://jaeger:4318
 *     timeout: 10s
 *     exporter: otlp
 *     sampling-rate: 0.5
 *     excluded-methods:
 *       - health
 *       - ping
 * }</pre>
 *
 * <p>This configuration allows fine-grained control over telemetry behavior —
 * such as which exporter is used, where spans are sent, and which methods are
 * excluded from tracing.</p>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jframe.otlp")
public class OpenTelemetryProperties {

    /**
     * Whether JFrame OpenTelemetry is disabled. Set to true to completely disable tracing and span creation.
     */
    private boolean disabled;

    /**
     * OpenTelemetry OTLP endpoint URL. The URL where telemetry data will be sent (e.g., http://jaeger:4318).
     */
    @NotBlank(message = "OpenTelemetry OTLP URL must not be blank")
    private String url = OtlpDefaults.DEFAULT_URL;

    /**
     * OpenTelemetry OTLP export timeout. How long to wait for telemetry export before timing out (e.g., 10s, 30s).
     */
    @NotBlank(message = "OpenTelemetry timeout must not be blank")
    @Pattern(
        regexp = "\\d+[smh]",
        message = "Timeout must be in format: number followed by s (seconds), m (minutes), or h (hours)"
    )
    private String timeout = OtlpDefaults.DEFAULT_TIMEOUT;

    /**
     * OpenTelemetry exporter type. Supported values: otlp, jaeger, zipkin
     */
    @NotBlank(message = "OpenTelemetry exporter must not be blank")
    @Pattern(
        regexp = "otlp|jaeger|zipkin",
        message = "Exporter must be one of: otlp, jaeger, zipkin"
    )
    private String exporter = OtlpDefaults.DEFAULT_EXPORTER;

    /**
     * OpenTelemetry sampling rate. Value between 0.0 (no traces) and 1.0 (all traces). Use lower values in production.
     */
    @DecimalMin(
        value = "0.0",
        message = "Sampling rate must be between 0.0 and 1.0"
    )
    @DecimalMax(
        value = "1.0",
        message = "Sampling rate must be between 0.0 and 1.0"
    )
    private double samplingRate = OtlpDefaults.DEFAULT_SAMPLING_RATE;

    /**
     * Method names to exclude from automatic tracing. These methods will not generate spans when called.
     */
    private Set<String> excludedMethods = OtlpDefaults.parseCommaSeparated(OtlpDefaults.DEFAULT_EXCLUDED_METHODS);

    /**
     * W3C trace context propagators. Comma-separated list of propagator names (e.g., tracecontext,baggage).
     */
    private String propagators = OtlpDefaults.DEFAULT_PROPAGATORS;

}
