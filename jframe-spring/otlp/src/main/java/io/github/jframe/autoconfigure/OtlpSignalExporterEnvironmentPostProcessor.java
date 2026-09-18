package io.github.jframe.autoconfigure;

import io.github.jframe.tracing.OtlpDefaults;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Authoritative source for {@code otel.{traces,metrics,logs}.exporter}.
 *
 * <p>Maps {@code jframe.otlp.{traces,metrics,logs}.enabled} booleans plus
 * {@code jframe.otlp.exporter} to the raw {@code otel.*.exporter} properties that the
 * OpenTelemetry Spring Boot starter reads. This EPP is the single, exclusive source for
 * all three signal exporters — no YAML property contributes them so there is no competing
 * source.
 *
 * <p><strong>Resolution rules per signal:</strong>
 * <ol>
 * <li>If the consumer has already set {@code otel.{signal}.exporter} explicitly in their
 * own property source, that value wins (we skip contribution for that signal).</li>
 * <li>If the per-signal toggle ({@code jframe.otlp.{signal}.enabled}) is {@code false},
 * contribute {@code none}.</li>
 * <li>Otherwise, contribute {@code jframe.otlp.exporter} — falling back to
 * {@link OtlpDefaults#DEFAULT_EXPORTER} when that property is not yet resolvable
 * (e.g. {@code jframe-properties.yml} has not been loaded yet).</li>
 * </ol>
 *
 * <p>The contributed {@link MapPropertySource} is added via {@code addLast()}, giving it the
 * lowest priority. Step 1 ensures explicit consumer overrides always prevail.
 */
public class OtlpSignalExporterEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String SOURCE_NAME = "jframe-otlp-signal-exporter-defaults";

    private static final String JFRAME_EXPORTER_KEY = "jframe.otlp.exporter";

    private static final String TRACES_ENABLED_KEY = "jframe.otlp.traces.enabled";
    private static final String METRICS_ENABLED_KEY = "jframe.otlp.metrics.enabled";
    private static final String LOGS_ENABLED_KEY = "jframe.otlp.logs.enabled";

    private static final String OTEL_TRACES_EXPORTER = "otel.traces.exporter";
    private static final String OTEL_METRICS_EXPORTER = "otel.metrics.exporter";
    private static final String OTEL_LOGS_EXPORTER = "otel.logs.exporter";

    private static final String NONE = "none";

    @Override
    public void postProcessEnvironment(
        final ConfigurableEnvironment environment,
        final SpringApplication application
    ) {
        final String exporter = resolveExporter(environment);
        final Map<String, Object> contribution = new LinkedHashMap<>();

        contributeSignal(environment, contribution, TRACES_ENABLED_KEY, OTEL_TRACES_EXPORTER, exporter);
        contributeSignal(environment, contribution, METRICS_ENABLED_KEY, OTEL_METRICS_EXPORTER, exporter);
        contributeSignal(environment, contribution, LOGS_ENABLED_KEY, OTEL_LOGS_EXPORTER, exporter);

        if (!contribution.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, contribution));
        }
    }

    /**
     * Adds an entry to {@code contribution} for one signal unless the consumer has already
     * set the raw {@code otel.*.exporter} property explicitly.
     */
    private void contributeSignal(
        final ConfigurableEnvironment environment,
        final Map<String, Object> contribution,
        final String enabledKey,
        final String otelExporterKey,
        final String resolvedExporter
    ) {
        if (hasExplicitUserValue(environment, otelExporterKey)) {
            return;
        }
        final boolean enabled = isEnabled(environment, enabledKey);
        contribution.put(otelExporterKey, enabled ? resolvedExporter : NONE);
    }

    /**
     * Returns {@code true} when the {@code otel.*.exporter} property is already defined in a
     * source that is not our own contributed source. This detects explicit consumer overrides
     * so we never clobber them.
     */
    private boolean hasExplicitUserValue(final ConfigurableEnvironment environment, final String key) {
        for (final PropertySource<?> source : environment.getPropertySources()) {
            if (SOURCE_NAME.equals(source.getName())) {
                continue;
            }
            if (source.containsProperty(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the configured exporter type. Falls back to {@link OtlpDefaults#DEFAULT_EXPORTER}
     * when {@code jframe.otlp.exporter} is not yet present (e.g. {@code jframe-properties.yml}
     * has not been loaded at EPP time).
     */
    private String resolveExporter(final ConfigurableEnvironment environment) {
        final String value = environment.getProperty(JFRAME_EXPORTER_KEY);
        return value != null ? value : OtlpDefaults.DEFAULT_EXPORTER;
    }

    /**
     * Returns {@code true} when the toggle property resolves to {@code true} or is absent
     * (signals default to enabled).
     */
    private boolean isEnabled(final ConfigurableEnvironment environment, final String key) {
        final String value = environment.getProperty(key);
        return value == null || Boolean.parseBoolean(value);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
