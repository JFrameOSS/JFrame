package io.github.jframe.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves that the {@link OtlpSignalExporterEnvironmentPostProcessor} correctly wires
 * per-signal toggles to the raw {@code otel.*.exporter} properties as seen by the final
 * resolved Spring {@code Environment} — with the real {@code jframe-properties.yml}
 * loaded by {@link CoreAutoConfiguration} present so that property-source ordering is
 * fully exercised.
 *
 * <p>{@link WebApplicationContextRunner} does not invoke {@link org.springframework.boot.EnvironmentPostProcessor}s
 * automatically (those run during {@code SpringApplication.run()}). This test applies the EPP
 * explicitly via {@code withInitializer}, which executes it against the same environment the
 * context runner uses — accurately simulating the real startup order:
 * <ol>
 * <li>User properties land first (via {@code withPropertyValues}).</li>
 * <li>EPP runs (via {@code withInitializer}) and reads them.</li>
 * <li>{@link CoreAutoConfiguration} loads {@code jframe-properties.yml} during context refresh.</li>
 * <li>We assert the FINAL RESOLVED value — no competing source wins over the EPP.</li>
 * </ol>
 *
 * <p>Asserting only that a boolean field is {@code true}/{@code false} is not sufficient.
 * These tests assert the FINAL RESOLVED value of the SDK-facing property.
 */
@DisplayName("OtlpSignalExporter - resolved Environment values (context-level)")
class OtlpSignalExporterResolvedPropertyTest {

    private static final String APP_NAME = "jframe.application.name=test-service";
    private static final String APP_GROUP = "jframe.application.group=io.github.jframe";
    private static final String APP_VERSION = "jframe.application.version=0.0.1";

    private static OpenTelemetry stubbedOpenTelemetry() {
        final OpenTelemetry otel = mock(OpenTelemetry.class);
        when(otel.getTracer(anyString(), anyString())).thenReturn(mock(Tracer.class));
        return otel;
    }

    /**
     * Initializer that applies {@link OtlpSignalExporterEnvironmentPostProcessor} to the
     * context's environment — simulating what {@code SpringApplication} does during startup.
     * This makes the EPP's contribution visible to the context runner's environment before
     * {@link CoreAutoConfiguration} appends {@code jframe-properties.yml}.
     */
    private static ApplicationContextInitializer<ConfigurableApplicationContext> withEpp() {
        return ctx -> new OtlpSignalExporterEnvironmentPostProcessor()
            .postProcessEnvironment(ctx.getEnvironment(), null);
    }

    /**
     * Context runner with CoreAutoConfiguration active so that {@code jframe-properties.yml}
     * is loaded via {@code @PropertySource} — exactly the real application scenario.
     */
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class, OpenTelemetryAutoConfiguration.class))
        .withPropertyValues(APP_NAME, APP_GROUP, APP_VERSION)
        .withBean("openTelemetry", OpenTelemetry.class, OtlpSignalExporterResolvedPropertyTest::stubbedOpenTelemetry)
        .withBean("objectMapper", ObjectMapper.class, ObjectMapper::new)
        .withInitializer(withEpp());

    // ── Default toggles — all signals enabled ─────────────────────────────

    @Nested
    @DisplayName("Default toggles (all signals enabled)")
    class DefaultToggles {

        @Test
        @DisplayName("otel.traces.exporter should resolve to otlp when toggle is absent")
        void shouldResolveTracesExporterToOtlpByDefault() {
            // Given: no per-signal override — jframe-properties.yml is the only source
            contextRunner.run(ctx -> {
                // When: reading the final resolved property from the Environment
                final String resolved = ctx.getEnvironment().getProperty("otel.traces.exporter");

                // Then: EPP contributes otlp (from jframe.otlp.exporter default)
                assertThat(resolved, is("otlp"));
            });
        }

        @Test
        @DisplayName("otel.metrics.exporter should resolve to otlp when toggle is absent")
        void shouldResolveMetricsExporterToOtlpByDefault() {
            contextRunner.run(ctx -> {
                final String resolved = ctx.getEnvironment().getProperty("otel.metrics.exporter");
                assertThat(resolved, is("otlp"));
            });
        }

        @Test
        @DisplayName("otel.logs.exporter should resolve to otlp when toggle is absent")
        void shouldResolveLogsExporterToOtlpByDefault() {
            contextRunner.run(ctx -> {
                final String resolved = ctx.getEnvironment().getProperty("otel.logs.exporter");
                assertThat(resolved, is("otlp"));
            });
        }
    }

    // ── Signal disabled — boolean reaches otel.*.exporter ─────────────────


    @Nested
    @DisplayName("Signal disabled — boolean must reach otel.*.exporter")
    class SignalDisabled {

        @Test
        @DisplayName("otel.metrics.exporter should resolve to none when jframe.otlp.metrics.enabled=false")
        void shouldResolveMetricsExporterToNoneWhenDisabled() {
            // Given: consumer disables metrics via the jframe toggle
            contextRunner
                .withPropertyValues("jframe.otlp.metrics.enabled=false")
                .withInitializer(withEpp())
                .run(ctx -> {
                    // When: reading the final resolved property from the real Environment
                    final String resolved = ctx.getEnvironment().getProperty("otel.metrics.exporter");

                    // Then: SDK-facing property is none — boolean wiring proven end-to-end
                    assertThat(resolved, is("none"));
                });
        }

        @Test
        @DisplayName("otel.traces.exporter should resolve to none when jframe.otlp.traces.enabled=false")
        void shouldResolveTracesExporterToNoneWhenDisabled() {
            contextRunner
                .withPropertyValues("jframe.otlp.traces.enabled=false")
                .withInitializer(withEpp())
                .run(ctx -> {
                    final String resolved = ctx.getEnvironment().getProperty("otel.traces.exporter");
                    assertThat(resolved, is("none"));
                });
        }

        @Test
        @DisplayName("otel.logs.exporter should resolve to none when jframe.otlp.logs.enabled=false")
        void shouldResolveLogsExporterToNoneWhenDisabled() {
            contextRunner
                .withPropertyValues("jframe.otlp.logs.enabled=false")
                .withInitializer(withEpp())
                .run(ctx -> {
                    final String resolved = ctx.getEnvironment().getProperty("otel.logs.exporter");
                    assertThat(resolved, is("none"));
                });
        }
    }

    // ── Defect 1 regression guard — jframe.otlp.exporter controls all signals ──


    @Nested
    @DisplayName("jframe.otlp.exporter controls all three signals (Defect 1 regression guard)")
    class ExporterPropagation {

        @Test
        @DisplayName("otel.traces.exporter should resolve to zipkin when jframe.otlp.exporter=zipkin")
        void shouldPropagateCustomExporterToTracesExporter() {
            // Given: consumer selects zipkin as their exporter
            contextRunner
                .withPropertyValues("jframe.otlp.exporter=zipkin")
                .withInitializer(withEpp())
                .run(ctx -> {
                    // When: reading the final resolved traces exporter
                    final String resolved = ctx.getEnvironment().getProperty("otel.traces.exporter");

                    // Then: jframe.otlp.exporter controls traces (Defect 1 regression guard)
                    assertThat(resolved, is("zipkin"));
                });
        }

        @Test
        @DisplayName("otel.metrics.exporter should resolve to zipkin when jframe.otlp.exporter=zipkin")
        void shouldPropagateCustomExporterToMetricsExporter() {
            contextRunner
                .withPropertyValues("jframe.otlp.exporter=zipkin")
                .withInitializer(withEpp())
                .run(ctx -> {
                    final String resolved = ctx.getEnvironment().getProperty("otel.metrics.exporter");
                    assertThat(resolved, is("zipkin"));
                });
        }

        @Test
        @DisplayName("otel.logs.exporter should resolve to zipkin when jframe.otlp.exporter=zipkin")
        void shouldPropagateCustomExporterToLogsExporter() {
            contextRunner
                .withPropertyValues("jframe.otlp.exporter=zipkin")
                .withInitializer(withEpp())
                .run(ctx -> {
                    final String resolved = ctx.getEnvironment().getProperty("otel.logs.exporter");
                    assertThat(resolved, is("zipkin"));
                });
        }
    }

    // ── Explicit raw otel.*.exporter must win ──────────────────────────────


    @Nested
    @DisplayName("Explicit otel.*.exporter overrides must win")
    class ExplicitOverride {

        @Test
        @DisplayName("Explicit otel.metrics.exporter survives even when signal is disabled")
        void shouldRespectExplicitRawExporterWhenSignalDisabled() {
            // Given: consumer disables the toggle AND sets raw exporter explicitly
            contextRunner
                .withPropertyValues("jframe.otlp.metrics.enabled=false", "otel.metrics.exporter=prometheus")
                .withInitializer(withEpp())
                .run(ctx -> {
                    // When: reading final resolved value
                    final String resolved = ctx.getEnvironment().getProperty("otel.metrics.exporter");

                    // Then: explicit consumer value wins — EPP does not clobber it
                    assertThat(resolved, is("prometheus"));
                });
        }
    }
}
