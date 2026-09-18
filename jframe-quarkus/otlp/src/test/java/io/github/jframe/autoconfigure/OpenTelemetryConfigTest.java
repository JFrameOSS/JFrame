package io.github.jframe.autoconfigure;

import io.github.jframe.tracing.OtlpDefaults;
import io.github.support.UnitTest;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link OpenTelemetryConfig}.
 *
 * <p>Verifies configuration reading via MicroProfile Config API including:
 * <ul>
 * <li>Default values when no system properties are set</li>
 * <li>Custom value overrides via system properties (highest-priority MicroProfile Config source)</li>
 * <li>Correct parsing of comma-separated {@code Set<String>} properties</li>
 * </ul>
 */
@DisplayName("Config - OpenTelemetryConfig")
class OpenTelemetryConfigTest extends UnitTest {

    // ======================== HELPERS ========================

    private OpenTelemetryConfig buildDefaultConfig() {
        return new OpenTelemetryConfig();
    }

    private OpenTelemetryConfig buildConfigWith(final String key, final String value) {
        System.setProperty(key, value);
        return new OpenTelemetryConfig();
    }

    @AfterEach
    void clearSystemProperties() {
        System.getProperties().stringPropertyNames().stream()
            .filter(name -> name.startsWith("jframe.otlp."))
            .forEach(System::clearProperty);
    }

    // ======================== DEFAULT VALUES ========================

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("Should return true for disabled by default — telemetry requires explicit opt-in")
        public void shouldReturnTrueForDisabledByDefault() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the disabled flag
            final boolean disabled = otlpConfig.disabled();

            // Then: Default is true — OTLP is disabled until the consumer opts in
            assertThat(disabled, is(true));
        }

        @Test
        @DisplayName("Should return localhost OTLP endpoint as default url")
        public void shouldReturnLocalhostUrlByDefault() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the url property
            final String url = otlpConfig.url();

            // Then: Default url points to localhost OTLP HTTP endpoint
            assertThat(url, is("http://localhost:4318"));
        }

        @Test
        @DisplayName("Should return 10s as default timeout")
        public void shouldReturn10sAsDefaultTimeout() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the timeout property
            final String timeout = otlpConfig.timeout();

            // Then: Default timeout is 10 seconds
            assertThat(timeout, is("10s"));
        }

        @Test
        @DisplayName("Should return otlp as default exporter")
        public void shouldReturnOtlpAsDefaultExporter() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the exporter property
            final String exporter = otlpConfig.exporter();

            // Then: Default exporter is otlp
            assertThat(exporter, is("otlp"));
        }

        @Test
        @DisplayName("Should return 1.0 as default samplingRate (full sampling)")
        public void shouldReturn1_0AsDefaultSamplingRate() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the samplingRate property
            final double samplingRate = otlpConfig.samplingRate();

            // Then: Default sampling rate is 1.0 — all traces are captured
            assertThat(samplingRate, is(1.0));
        }

        @Test
        @DisplayName("Should return common health endpoints as default excludedMethods")
        public void shouldReturnCommonHealthEndpointsAsDefaultExcludedMethods() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the excludedMethods set
            final Set<String> excludedMethods = otlpConfig.excludedMethods();

            // Then: Default set contains all 6 common health/monitoring endpoints
            assertThat(excludedMethods, hasSize(6));
            assertThat(excludedMethods, containsInAnyOrder("health", "actuator", "ping", "status", "info", "metrics"));
        }

        @Test
        @DisplayName("Should return tracecontext,baggage as default propagators")
        public void shouldReturnW3cAsDefaultPropagators() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the propagators property
            final String propagators = otlpConfig.propagators();

            // Then: Default propagators use W3C standard (no extra dependencies needed)
            assertThat(propagators, is("tracecontext,baggage"));
        }

        @Test
        @DisplayName("Should return parentbased_traceidratio as default sampler type")
        public void shouldReturnParentBasedTraceIdRatioAsDefaultSamplerType() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the samplerType property
            final String samplerType = otlpConfig.samplerType();

            // Then: Default sampler type is parentbased_traceidratio — respects parent sampling decision
            assertThat(samplerType, is(OtlpDefaults.DEFAULT_SAMPLER_TYPE));
        }

        @Test
        @DisplayName("Should return three process.* keys as default excludedResourceAttributes")
        public void shouldReturnProcessKeysAsDefaultExcludedResourceAttributes() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the excludedResourceAttributes set
            final Set<String> excluded = otlpConfig.excludedResourceAttributes();

            // Then: Default excludes three OTel spec Opt-In process keys that can leak secrets
            assertThat(excluded, hasSize(3));
            assertThat(
                excluded,
                containsInAnyOrder(
                    "process.command_args",
                    "process.command_line",
                    "process.executable.path"
                )
            );
        }

        @Test
        @DisplayName("Should return true for traces enabled by default")
        public void shouldReturnTrueForTracesEnabledByDefault() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the tracesEnabled flag
            final boolean tracesEnabled = otlpConfig.tracesEnabled();

            // Then: Traces are enabled by default
            assertThat(tracesEnabled, is(true));
        }

        @Test
        @DisplayName("Should return true for metrics enabled by default")
        public void shouldReturnTrueForMetricsEnabledByDefault() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the metricsEnabled flag
            final boolean metricsEnabled = otlpConfig.metricsEnabled();

            // Then: Metrics are enabled by default
            assertThat(metricsEnabled, is(true));
        }

        @Test
        @DisplayName("Should return true for logs enabled by default")
        public void shouldReturnTrueForLogsEnabledByDefault() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the logsEnabled flag
            final boolean logsEnabled = otlpConfig.logsEnabled();

            // Then: Logs are enabled by default
            assertThat(logsEnabled, is(true));
        }
    }

    // ======================== OVERRIDE VALUES ========================


    @Nested
    @DisplayName("Custom value overrides")
    class CustomValueOverrides {

        @Test
        @DisplayName("Should reflect overridden disabled=false when provided")
        public void shouldReflectOverriddenDisabledFalseWhenProvided() {
            // Given: Config overriding disabled to false (consumer opts in)
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.disabled", "false");

            // When: Accessing the disabled flag
            final boolean disabled = otlpConfig.disabled();

            // Then: Tracing is enabled
            assertThat(disabled, is(false));
        }

        @Test
        @DisplayName("Should reflect overridden url when provided")
        public void shouldReflectOverriddenUrlWhenProvided() {
            // Given: Config overriding url to a custom collector
            final String customUrl = "http://otel-collector:4318";
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.url", customUrl);

            // When: Accessing the url property
            final String url = otlpConfig.url();

            // Then: URL points to the custom collector
            assertThat(url, is(customUrl));
        }

        @Test
        @DisplayName("Should reflect overridden timeout when provided")
        public void shouldReflectOverriddenTimeoutWhenProvided() {
            // Given: Config overriding timeout to 30 seconds
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.timeout", "30s");

            // When: Accessing the timeout property
            final String timeout = otlpConfig.timeout();

            // Then: Timeout is 30 seconds
            assertThat(timeout, is("30s"));
        }

        @Test
        @DisplayName("Should reflect overridden exporter when provided")
        public void shouldReflectOverriddenExporterWhenProvided() {
            // Given: Config overriding exporter to zipkin
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.exporter", "zipkin");

            // When: Accessing the exporter property
            final String exporter = otlpConfig.exporter();

            // Then: Exporter is zipkin
            assertThat(exporter, is("zipkin"));
        }

        @Test
        @DisplayName("Should reflect overridden samplingRate when provided")
        public void shouldReflectOverriddenSamplingRateWhenProvided() {
            // Given: Config overriding samplingRate to 0.5 (50% sampling)
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.sampling-rate", "0.5");

            // When: Accessing the samplingRate property
            final double samplingRate = otlpConfig.samplingRate();

            // Then: Sampling rate is 0.5
            assertThat(samplingRate, is(0.5));
        }

        @Test
        @DisplayName("Should parse overridden excludedMethods from comma-separated string")
        public void shouldParseOverriddenExcludedMethodsFromCommaSeparatedString() {
            // Given: Config overriding excludedMethods with a custom comma-separated list
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.excluded-methods", "debug,trace,live");

            // When: Accessing the excludedMethods set
            final Set<String> excludedMethods = otlpConfig.excludedMethods();

            // Then: Set contains the 3 custom entries parsed from the comma-separated value
            assertThat(excludedMethods, hasSize(3));
            assertThat(excludedMethods, containsInAnyOrder("debug", "trace", "live"));
        }

        @Test
        @DisplayName("Should reflect overridden propagators when provided")
        public void shouldReflectOverriddenPropagatorsWhenProvided() {
            // Given: Config overriding propagators to include B3
            final OpenTelemetryConfig otlpConfig = buildConfigWith(
                "jframe.otlp.propagators",
                "tracecontext,baggage,b3"
            );

            // When: Accessing the propagators property
            final String propagators = otlpConfig.propagators();

            // Then: Propagators include B3
            assertThat(propagators, is("tracecontext,baggage,b3"));
        }

        @Test
        @DisplayName("Should reflect overridden sampler type when provided")
        public void shouldReflectOverriddenSamplerTypeWhenProvided() {
            // Given: Config overriding sampler type to a fixed rate sampler
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.sampler-type", "traceidratio");

            // When: Accessing the samplerType property
            final String samplerType = otlpConfig.samplerType();

            // Then: Sampler type is the custom value
            assertThat(samplerType, is("traceidratio"));
        }

        @Test
        @DisplayName("Should parse overridden excludedResourceAttributes from comma-separated string")
        public void shouldParseOverriddenExcludedResourceAttributesFromCommaSeparatedString() {
            // Given: Config overriding excludedResourceAttributes with custom keys
            final OpenTelemetryConfig otlpConfig = buildConfigWith(
                "jframe.otlp.excluded-resource-attributes",
                "process.command_args,host.name"
            );

            // When: Accessing the excludedResourceAttributes set
            final Set<String> excluded = otlpConfig.excludedResourceAttributes();

            // Then: Set contains exactly the two overridden keys
            assertThat(excluded, hasSize(2));
            assertThat(excluded, containsInAnyOrder("process.command_args", "host.name"));
        }

        @Test
        @DisplayName("Should reflect overridden tracesEnabled=false when provided")
        public void shouldReflectOverriddenTracesEnabledFalseWhenProvided() {
            // Given: Config disabling traces signal
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.traces.enabled", "false");

            // When: Accessing the tracesEnabled flag
            final boolean tracesEnabled = otlpConfig.tracesEnabled();

            // Then: Traces are disabled
            assertThat(tracesEnabled, is(false));
        }

        @Test
        @DisplayName("Should reflect overridden metricsEnabled=false when provided")
        public void shouldReflectOverriddenMetricsEnabledFalseWhenProvided() {
            // Given: Config disabling metrics signal
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.metrics.enabled", "false");

            // When: Accessing the metricsEnabled flag
            final boolean metricsEnabled = otlpConfig.metricsEnabled();

            // Then: Metrics are disabled
            assertThat(metricsEnabled, is(false));
        }

        @Test
        @DisplayName("Should reflect overridden logsEnabled=false when provided")
        public void shouldReflectOverriddenLogsEnabledFalseWhenProvided() {
            // Given: Config disabling logs signal
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.logs.enabled", "false");

            // When: Accessing the logsEnabled flag
            final boolean logsEnabled = otlpConfig.logsEnabled();

            // Then: Logs are disabled
            assertThat(logsEnabled, is(false));
        }
    }

    // ======================== EDGE CASES ========================


    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should return single-element set when excludedMethods has one entry")
        public void shouldReturnSingleElementSetWhenExcludedMethodsHasOneEntry() {
            // Given: Config with a single excluded method
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.excluded-methods", "health");

            // When: Accessing the excludedMethods set
            final Set<String> excludedMethods = otlpConfig.excludedMethods();

            // Then: Set has exactly one entry
            assertThat(excludedMethods, hasSize(1));
            assertThat(excludedMethods, containsInAnyOrder("health"));
        }

        @Test
        @DisplayName("Should return zero samplingRate when explicitly set to 0.0")
        public void shouldReturnZeroSamplingRateWhenExplicitlySetToZero() {
            // Given: Config with samplingRate set to 0.0 (no tracing)
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.sampling-rate", "0.0");

            // When: Accessing the samplingRate property
            final double samplingRate = otlpConfig.samplingRate();

            // Then: Sampling rate is 0.0 — no traces are captured
            assertThat(samplingRate, is(0.0));
        }

        @Test
        @DisplayName("Should return empty set when excludedResourceAttributes is overridden to empty string")
        public void shouldReturnEmptySetWhenExcludedResourceAttributesIsEmpty() {
            // Given: Config explicitly clearing excluded resource attributes (consumer wants none excluded)
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.excluded-resource-attributes", "");

            // When: Accessing the excludedResourceAttributes set
            final Set<String> excluded = otlpConfig.excludedResourceAttributes();

            // Then: Result is empty — not a single blank entry
            assertThat(excluded, is(empty()));
        }
    }
}
