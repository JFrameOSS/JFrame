package io.github.jframe.autoconfigure;

import io.github.support.UnitTest;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves that {@link OtlpSignalExporterCustomizer} wires per-signal toggles to the OTel SDK
 * config properties — not merely that the accessor returns a boolean.
 *
 * <p>The test asserts on the resolved {@code Map<String,String>} that the customizer contributes
 * to {@link AutoConfiguredOpenTelemetrySdkBuilder} via {@code addPropertiesSupplier}. This is the
 * map the SDK actually uses when configuring signal exporters — capturing it is the closest
 * unit-testable proxy for the full SDK behaviour.
 *
 * <p><strong>Limitation:</strong> This test does not boot the full Quarkus OTel stack (which
 * requires the CDI container and the Quarkus build recorder). It therefore cannot assert that the
 * actual {@code PeriodicMetricReader} is absent from the resulting {@code SdkMeterProvider}. That
 * integration-level assertion would require a Quarkus {@code @QuarkusTest} and is not present here.
 * The unit-level assertion on the SDK property map is the correct boundary for a library test.
 */
@DisplayName("OtlpSignalExporterCustomizer - SDK property wiring")
class OtlpSignalExporterCustomizerTest extends UnitTest {

    // ======================== HELPERS ========================

    private OtlpSignalExporterCustomizer buildCustomizer(
        final boolean traces,
        final boolean metrics,
        final boolean logs) {
        final OpenTelemetryConfig config = mock(OpenTelemetryConfig.class);
        when(config.tracesEnabled()).thenReturn(traces);
        when(config.metricsEnabled()).thenReturn(metrics);
        when(config.logsEnabled()).thenReturn(logs);
        return new OtlpSignalExporterCustomizer(config);
    }

    /**
     * Invokes the customizer against a mocked builder, captures the supplier passed to
     * {@code addPropertiesSupplier}, and returns the resolved map.
     *
     * <p>If the customizer did not call {@code addPropertiesSupplier} at all (all signals enabled),
     * returns an empty map to allow the "no override" assertions.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> captureOverrides(final OtlpSignalExporterCustomizer customizer) {
        final AutoConfiguredOpenTelemetrySdkBuilder builder = mock(AutoConfiguredOpenTelemetrySdkBuilder.class);
        when(builder.addPropertiesSupplier(org.mockito.ArgumentMatchers.any())).thenReturn(builder);

        customizer.customize(builder);

        final ArgumentCaptor<Supplier<Map<String, String>>> captor =
            ArgumentCaptor.forClass(Supplier.class);

        try {
            verify(builder).addPropertiesSupplier(captor.capture());
            return captor.getValue().get();
        } catch (final org.mockito.exceptions.verification.WantedButNotInvoked ignored) {
            // No override injected — all signals are enabled
            return Map.of();
        }
    }

    // ======================== ALL ENABLED (DEFAULT) ========================

    @Nested
    @DisplayName("All signals enabled (default)")
    class AllSignalsEnabled {

        @Test
        @DisplayName("Should not inject any exporter override when all signals are enabled")
        void shouldNotInjectAnyOverrideWhenAllEnabled() {
            // Given: all three signals enabled
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(true, true, true);
            final AutoConfiguredOpenTelemetrySdkBuilder builder = mock(AutoConfiguredOpenTelemetrySdkBuilder.class);

            // When: customizer runs
            customizer.customize(builder);

            // Then: addPropertiesSupplier is never called — Quarkus cdi defaults are preserved intact
            verify(builder, never()).addPropertiesSupplier(org.mockito.ArgumentMatchers.any());
        }
    }

    // ======================== METRICS DISABLED ========================


    @Nested
    @DisplayName("Metrics signal disabled")
    class MetricsDisabled {

        @Test
        @DisplayName("Should set otel.metrics.exporter=none when metricsEnabled=false")
        void shouldSetMetricsExporterToNoneWhenDisabled() {
            // Given: metrics disabled, other signals on
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(true, false, true);

            // When: capturing the overrides the customizer contributes to the SDK builder
            final Map<String, String> overrides = captureOverrides(customizer);

            // Then: SDK-facing property is none — this is what stops the PeriodicMetricReader
            assertThat(
                "otel.metrics.exporter must be none when metrics signal is disabled",
                overrides,
                hasEntry("otel.metrics.exporter", "none")
            );
        }

        @Test
        @DisplayName("Should NOT set otel.traces.exporter when only metrics is disabled")
        void shouldNotOverrideTracesWhenOnlyMetricsDisabled() {
            // Given: only metrics disabled
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(true, false, true);

            // When: capturing overrides
            final Map<String, String> overrides = captureOverrides(customizer);

            // Then: traces exporter is untouched — Quarkus cdi default applies
            assertThat(overrides, not(hasKey("otel.traces.exporter")));
        }

        @Test
        @DisplayName("Should NOT set otel.logs.exporter when only metrics is disabled")
        void shouldNotOverrideLogsWhenOnlyMetricsDisabled() {
            // Given: only metrics disabled
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(true, false, true);

            // When: capturing overrides
            final Map<String, String> overrides = captureOverrides(customizer);

            // Then: logs exporter is untouched
            assertThat(overrides, not(hasKey("otel.logs.exporter")));
        }
    }

    // ======================== TRACES DISABLED ========================


    @Nested
    @DisplayName("Traces signal disabled")
    class TracesDisabled {

        @Test
        @DisplayName("Should set otel.traces.exporter=none when tracesEnabled=false")
        void shouldSetTracesExporterToNoneWhenDisabled() {
            // Given: traces disabled
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(false, true, true);

            // When / Then
            final Map<String, String> overrides = captureOverrides(customizer);
            assertThat(overrides, hasEntry("otel.traces.exporter", "none"));
        }
    }

    // ======================== LOGS DISABLED ========================


    @Nested
    @DisplayName("Logs signal disabled")
    class LogsDisabled {

        @Test
        @DisplayName("Should set otel.logs.exporter=none when logsEnabled=false")
        void shouldSetLogsExporterToNoneWhenDisabled() {
            // Given: logs disabled
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(true, true, false);

            // When / Then
            final Map<String, String> overrides = captureOverrides(customizer);
            assertThat(overrides, hasEntry("otel.logs.exporter", "none"));
        }
    }

    // ======================== ALL DISABLED ========================


    @Nested
    @DisplayName("All signals disabled")
    class AllSignalsDisabled {

        @Test
        @DisplayName("Should set all three exporter keys to none when all signals are disabled")
        void shouldSetAllExportersToNoneWhenAllDisabled() {
            // Given: all three signals disabled
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(false, false, false);

            // When: capturing overrides
            final Map<String, String> overrides = captureOverrides(customizer);

            // Then: all three SDK-facing keys are none
            assertThat(overrides, hasEntry("otel.traces.exporter", "none"));
            assertThat(overrides, hasEntry("otel.metrics.exporter", "none"));
            assertThat(overrides, hasEntry("otel.logs.exporter", "none"));
        }

        @Test
        @DisplayName("Should inject exactly three override entries when all signals are disabled")
        void shouldInjectExactlyThreeEntriesWhenAllDisabled() {
            // Given: all three signals disabled
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(false, false, false);

            // When: capturing overrides
            final Map<String, String> overrides = captureOverrides(customizer);

            // Then: no extra entries — exactly three
            assertThat(overrides.size(), is(3));
        }
    }

    // ======================== PARTIAL: two disabled ========================


    @Nested
    @DisplayName("Two of three signals disabled")
    class TwoSignalsDisabled {

        @Test
        @DisplayName("Should set metrics and logs exporter to none when both are disabled")
        void shouldSetMetricsAndLogsToNoneWhenBothDisabled() {
            // Given: traces on, metrics + logs off
            final OtlpSignalExporterCustomizer customizer = buildCustomizer(true, false, false);

            // When: capturing overrides
            final Map<String, String> overrides = captureOverrides(customizer);

            // Then: only the disabled signals appear in the map
            assertThat(overrides, hasEntry("otel.metrics.exporter", "none"));
            assertThat(overrides, hasEntry("otel.logs.exporter", "none"));
            assertThat(overrides, not(hasKey("otel.traces.exporter")));
            assertThat(overrides.size(), is(2));
        }
    }
}
