package io.github.jframe.autoconfigure;

import io.github.support.UnitTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static io.github.jframe.tracing.OtlpDefaults.DEFAULT_EXPORTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link OtlpSignalExporterEnvironmentPostProcessor}.
 *
 * <p>Verifies that per-signal boolean toggles ({@code jframe.otlp.*.enabled}) are correctly
 * translated into the raw {@code otel.*.exporter} properties that the OpenTelemetry starter
 * reads. Asserting only that the boolean field is {@code true}/{@code false} is NOT sufficient
 * — these tests prove the wire-up actually reaches the SDK-facing property.
 */
@DisplayName("OtlpSignalExporterEnvironmentPostProcessor")
class OtlpSignalExporterEnvironmentPostProcessorTest extends UnitTest {

    private OtlpSignalExporterEnvironmentPostProcessor processor;
    private SpringApplication application;

    @Override
    @BeforeEach
    public void setUp() {
        processor = new OtlpSignalExporterEnvironmentPostProcessor();
        application = new SpringApplication();
    }

    // ── All signals enabled (default) ──────────────────────────────────────

    @Nested
    @DisplayName("When all signals are enabled (default)")
    class AllSignalsEnabled {

        @Test
        @DisplayName("Should contribute DEFAULT_EXPORTER to otel.traces.exporter when traces are enabled")
        void shouldContributeDefaultExporterForTracesWhenEnabled() {
            // Given: traces are enabled (default) — no jframe.otlp.exporter set, falls back to DEFAULT_EXPORTER
            final MockEnvironment environment = new MockEnvironment();

            // When: post-processing the environment
            processor.postProcessEnvironment(environment, application);

            // Then: EPP is authoritative — contributes the configured exporter (not none)
            assertThat(environment.getProperty("otel.traces.exporter"), is(DEFAULT_EXPORTER));
        }

        @Test
        @DisplayName("Should contribute DEFAULT_EXPORTER to otel.metrics.exporter when metrics are enabled")
        void shouldContributeDefaultExporterForMetricsWhenEnabled() {
            // Given: metrics are enabled (default)
            final MockEnvironment environment = new MockEnvironment();

            // When: post-processing the environment
            processor.postProcessEnvironment(environment, application);

            // Then: EPP contributes the configured exporter
            assertThat(environment.getProperty("otel.metrics.exporter"), is(DEFAULT_EXPORTER));
        }

        @Test
        @DisplayName("Should contribute DEFAULT_EXPORTER to otel.logs.exporter when logs are enabled")
        void shouldContributeDefaultExporterForLogsWhenEnabled() {
            // Given: logs are enabled (default)
            final MockEnvironment environment = new MockEnvironment();

            // When: post-processing the environment
            processor.postProcessEnvironment(environment, application);

            // Then: EPP contributes the configured exporter
            assertThat(environment.getProperty("otel.logs.exporter"), is(DEFAULT_EXPORTER));
        }
    }

    // ── Per-signal disabled — boolean reaches raw otel.*.exporter ──────────


    @Nested
    @DisplayName("When a signal is disabled")
    class SignalDisabled {

        @Test
        @DisplayName("Should set otel.traces.exporter=none when jframe.otlp.traces.enabled=false")
        void shouldSetTracesExporterToNoneWhenTracesDisabled() {
            // Given: consumer disables traces
            final MockEnvironment environment = new MockEnvironment();
            environment.setProperty("jframe.otlp.traces.enabled", "false");

            // When: post-processing
            processor.postProcessEnvironment(environment, application);

            // Then: the SDK-facing property is set to none — the boolean reached otel.traces.exporter
            assertThat(environment.getProperty("otel.traces.exporter"), is("none"));
        }

        @Test
        @DisplayName("Should set otel.metrics.exporter=none when jframe.otlp.metrics.enabled=false")
        void shouldSetMetricsExporterToNoneWhenMetricsDisabled() {
            // Given: consumer disables metrics
            final MockEnvironment environment = new MockEnvironment();
            environment.setProperty("jframe.otlp.metrics.enabled", "false");

            // When: post-processing
            processor.postProcessEnvironment(environment, application);

            // Then: the SDK-facing property is set to none — the boolean reached otel.metrics.exporter
            assertThat(environment.getProperty("otel.metrics.exporter"), is("none"));
        }

        @Test
        @DisplayName("Should set otel.logs.exporter=none when jframe.otlp.logs.enabled=false")
        void shouldSetLogsExporterToNoneWhenLogsDisabled() {
            // Given: consumer disables logs
            final MockEnvironment environment = new MockEnvironment();
            environment.setProperty("jframe.otlp.logs.enabled", "false");

            // When: post-processing
            processor.postProcessEnvironment(environment, application);

            // Then: the SDK-facing property is set to none — the boolean reached otel.logs.exporter
            assertThat(environment.getProperty("otel.logs.exporter"), is("none"));
        }

        @Test
        @DisplayName("Should set traces exporter to none and other signals to DEFAULT_EXPORTER when only traces are disabled")
        void shouldOnlyAffectTracesWhenOnlyTracesDisabled() {
            // Given: only traces disabled
            final MockEnvironment environment = new MockEnvironment();
            environment.setProperty("jframe.otlp.traces.enabled", "false");

            // When: post-processing
            processor.postProcessEnvironment(environment, application);

            // Then: traces exporter is none; metrics and logs get the configured exporter (EPP is authoritative for all three)
            assertThat(environment.getProperty("otel.traces.exporter"), is("none"));
            assertThat(environment.getProperty("otel.metrics.exporter"), is(DEFAULT_EXPORTER));
            assertThat(environment.getProperty("otel.logs.exporter"), is(DEFAULT_EXPORTER));
        }
    }

    // ── Explicit user config must win ──────────────────────────────────────


    @Nested
    @DisplayName("When user explicitly sets otel.*.exporter")
    class ExplicitUserConfig {

        @Test
        @DisplayName("Should not clobber explicit otel.traces.exporter when signal is disabled")
        void shouldNotClobberExplicitTracesExporterWhenTracesDisabled() {
            // Given: consumer disables traces AND explicitly sets a custom exporter
            final ConfigurableEnvironment environment = new StandardEnvironment();
            final Map<String, Object> userProps = new HashMap<>();
            userProps.put("jframe.otlp.traces.enabled", "false");
            userProps.put("otel.traces.exporter", "zipkin");
            // User config is added with high priority (first)
            environment.getPropertySources().addFirst(new MapPropertySource("test-user-config", userProps));

            // When: post-processing
            processor.postProcessEnvironment(environment, application);

            // Then: user's explicit value wins over the processor's none
            assertThat(environment.getProperty("otel.traces.exporter"), is("zipkin"));
        }

        @Test
        @DisplayName("Should not clobber explicit otel.metrics.exporter when signal is disabled")
        void shouldNotClobberExplicitMetricsExporterWhenMetricsDisabled() {
            // Given: consumer disables metrics AND sets a custom exporter
            final ConfigurableEnvironment environment = new StandardEnvironment();
            final Map<String, Object> userProps = new HashMap<>();
            userProps.put("jframe.otlp.metrics.enabled", "false");
            userProps.put("otel.metrics.exporter", "prometheus");
            environment.getPropertySources().addFirst(new MapPropertySource("test-user-config", userProps));

            // When: post-processing
            processor.postProcessEnvironment(environment, application);

            // Then: user's explicit value wins
            assertThat(environment.getProperty("otel.metrics.exporter"), is("prometheus"));
        }
    }
}
