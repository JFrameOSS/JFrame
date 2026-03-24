package io.github.jframe.autoconfigure;

import io.github.support.UnitTest;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link OpenTelemetryConfig}.
 *
 * <p>Verifies the SmallRye {@code @ConfigMapping} contract including:
 * <ul>
 * <li>Default values defined via {@code @WithDefault}</li>
 * <li>Custom value overrides via programmatic config builder</li>
 * <li>Correct parsing of comma-separated {@code Set<String>} properties</li>
 * </ul>
 */
@DisplayName("Config Mapping - OpenTelemetryConfig")
class OpenTelemetryConfigTest extends UnitTest {

    // ======================== HELPERS ========================

    private OpenTelemetryConfig buildDefaultConfig() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
            .withMapping(OpenTelemetryConfig.class)
            .build();
        return config.getConfigMapping(OpenTelemetryConfig.class);
    }

    private OpenTelemetryConfig buildConfigWith(final String key, final String value) {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
            .withMapping(OpenTelemetryConfig.class)
            .withDefaultValue(key, value)
            .build();
        return config.getConfigMapping(OpenTelemetryConfig.class);
    }

    // ======================== DEFAULT VALUES ========================

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("Should return false for disabled by default (enabled by default)")
        public void shouldReturnFalseForDisabledByDefault() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the disabled flag
            final boolean disabled = otlpConfig.disabled();

            // Then: Default is false — OTLP is enabled by default
            assertThat(disabled, is(false));
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
        @DisplayName("Should return true for autoTrace by default")
        public void shouldReturnTrueForAutoTraceByDefault() {
            // Given: Config built with no overrides
            final OpenTelemetryConfig otlpConfig = buildDefaultConfig();

            // When: Accessing the autoTrace flag
            final boolean autoTrace = otlpConfig.autoTrace();

            // Then: Default autoTrace is true — automatic instrumentation is active
            assertThat(autoTrace, is(true));
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
        @DisplayName("Should reflect overridden autoTrace=false when provided")
        public void shouldReflectOverriddenAutoTraceFalseWhenProvided() {
            // Given: Config overriding autoTrace to false (manual tracing only)
            final OpenTelemetryConfig otlpConfig = buildConfigWith("jframe.otlp.auto-trace", "false");

            // When: Accessing the autoTrace flag
            final boolean autoTrace = otlpConfig.autoTrace();

            // Then: Automatic tracing is disabled
            assertThat(autoTrace, is(false));
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
        @DisplayName("Should allow disabled=true with autoTrace=false simultaneously")
        public void shouldAllowDisabledTrueWithAutoTraceFalseSimultaneously() {
            // Given: Config with both disabled=true and autoTrace=false
            final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(OpenTelemetryConfig.class)
                .withDefaultValue("jframe.otlp.disabled", "true")
                .withDefaultValue("jframe.otlp.auto-trace", "false")
                .build();
            final OpenTelemetryConfig otlpConfig = config.getConfigMapping(OpenTelemetryConfig.class);

            // When: Accessing both flags
            final boolean disabled = otlpConfig.disabled();
            final boolean autoTrace = otlpConfig.autoTrace();

            // Then: Both flags reflect their configured values independently
            assertThat(disabled, is(true));
            assertThat(autoTrace, is(false));
        }
    }
}
