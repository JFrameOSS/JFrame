package io.github.jframe.autoconfigure;

import io.github.jframe.factory.HttpClientSSLFactory;
import io.github.jframe.logging.aspect.TimerAspect;
import io.github.jframe.security.filter.UserIdentityFilterConfiguration;
import io.github.jframe.tracing.HttpFilter;
import io.github.jframe.tracing.SpanManager;
import io.github.jframe.tracing.aspect.TracingAspect;
import io.github.jframe.tracing.enricher.TracingResponseEnricher;
import io.github.jframe.tracing.scheduled.TracingScheduledTaskEnricher;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterization / regression test for {@link OpenTelemetryAutoConfiguration} bean registration surface.
 *
 * <p>Purpose: {@link OpenTelemetryAutoConfiguration} currently carries
 * {@code @ComponentScan(basePackages = "io.github.jframe")}. We are about to DELETE that scan
 * and replace it with explicit {@code @Import} / {@code @Bean} registration. This suite proves
 * no bean silently disappears in that refactor.
 *
 * <p>Two scenarios are tested:
 * <ol>
 * <li><b>Default (otlp disabled):</b> {@code jframe.otlp.disabled=true} (the shipped default).
 * Beans guarded by {@code @ConditionalOnProperty(jframe.otlp.disabled, havingValue="false")}
 * are absent.</li>
 * <li><b>OTLP enabled:</b> {@code jframe.otlp.disabled=false}.
 * All beans including {@link SpanManager}, {@link TracingAspect},
 * {@link TracingResponseEnricher}, and {@link TracingScheduledTaskEnricher} must be present.</li>
 * </ol>
 *
 * <p>All tests MUST PASS against the unmodified code. Any failure is a finding to report.
 */
@DisplayName("Characterization Test - OpenTelemetryAutoConfiguration bean registration surface")
class OpenTelemetryAutoConfigurationBeanRegistrationTest {

    private static final String APP_NAME = "jframe.application.name=test-service";
    private static final String APP_GROUP = "jframe.application.group=io.github.jframe";
    private static final String APP_VERSION = "jframe.application.version=0.0.1";

    /**
     * A stubbed {@link OpenTelemetry} whose {@code getTracer} returns a mock {@link Tracer}.
     * This satisfies both the {@code OpenTelemetryAutoConfiguration.tracer()} bean factory method
     * (which calls {@code openTelemetry.getTracer(name, version)}) and beans that inject
     * {@code Tracer} directly — without creating a conflicting second {@code tracer} bean.
     */
    private static OpenTelemetry stubbedOpenTelemetry() {
        final OpenTelemetry otel = mock(OpenTelemetry.class);
        when(otel.getTracer(anyString(), anyString())).thenReturn(mock(Tracer.class));
        return otel;
    }

    /**
     * Context runner with BOTH auto-configurations active (core + otlp), mirroring how a real
     * consumer would pull in both on the classpath. Mock infrastructure beans are registered to
     * satisfy dependencies without wiring the full OTel SDK or Jackson auto-configuration.
     */
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class, OpenTelemetryAutoConfiguration.class))
        .withPropertyValues(APP_NAME, APP_GROUP, APP_VERSION)
        .withBean("openTelemetry", OpenTelemetry.class, OpenTelemetryAutoConfigurationBeanRegistrationTest::stubbedOpenTelemetry)
        .withBean("objectMapper", ObjectMapper.class, ObjectMapper::new);

    // =========================================================================
    // Scenario 1: default context — jframe.otlp.disabled=true (shipped default)
    // =========================================================================

    @Nested
    @DisplayName("Scenario 1: default context (jframe.otlp.disabled=true — shipped default)")
    class OtlpDisabledByDefault {

        /**
         * Beans NOT gated on {@code jframe.otlp.disabled} must always be present, regardless of
         * whether OTLP tracing is disabled.
         */
        @Test
        @DisplayName("Should register unconditional OTLP beans when auto-configuration is active")
        void shouldRegisterUnconditionalOtlpBeans() {
            // Given: default context — jframe.otlp.disabled=true (shipped via jframe-properties.yml)
            contextRunner.run(ctx -> {
                // When / Then: unconditional beans must be present
                assertThat("OpenTelemetryPackageLogger", ctx.getBean(OpenTelemetryPackageLogger.class), is(notNullValue()));
                assertThat("TimerAspect", ctx.getBean(TimerAspect.class), is(notNullValue()));
                assertThat("HttpFilter", ctx.getBean(HttpFilter.class), is(notNullValue()));
                assertThat("HttpClientSSLFactory", ctx.getBean(HttpClientSSLFactory.class), is(notNullValue()));
                assertThat(
                    "UserIdentityFilterConfiguration (matchIfMissing=true)",
                    ctx.getBean(UserIdentityFilterConfiguration.class),
                    is(notNullValue())
                );
            });
        }

        /**
         * {@link SpanManager} is gated on {@code @ConditionalOnProperty(name="jframe.otlp.disabled", havingValue="false")}.
         * The shipped {@code jframe-properties.yml} sets {@code jframe.otlp.disabled: true}, so
         * {@link SpanManager} must be ABSENT in a default context.
         */
        @Test
        @DisplayName("Should NOT register SpanManager when jframe.otlp.disabled=true (default)")
        void shouldNotRegisterSpanManagerByDefault() {
            // Given: default context — jframe.otlp.disabled=true
            contextRunner.run(ctx ->
            // Then: SpanManager must be absent
            assertThat(
                "SpanManager must be absent when jframe.otlp.disabled=true (the default)",
                ctx.containsBean("spanManager"),
                is(false)
            )
            );
        }

        /**
         * {@link TracingResponseEnricher}, {@link TracingAspect}, and {@link TracingScheduledTaskEnricher}
         * share the same {@code @ConditionalOnProperty(jframe.otlp.disabled, havingValue="false")} guard.
         * They must be ABSENT by default for the same reason as {@link SpanManager}.
         */
        @Test
        @DisplayName("Should NOT register otlp-disabled-gated tracing beans by default")
        void shouldNotRegisterOtlpGatedTracingBeansByDefault() {
            // Given: default context — jframe.otlp.disabled=true
            contextRunner.run(ctx -> {
                assertThat(
                    "TracingResponseEnricher must be absent when jframe.otlp.disabled=true",
                    ctx.containsBean("tracingResponseEnricher"),
                    is(false)
                );
                assertThat(
                    "TracingAspect must be absent when jframe.otlp.disabled=true",
                    ctx.containsBean("tracingAspect"),
                    is(false)
                );
                assertThat(
                    "TracingScheduledTaskEnricher must be absent when jframe.otlp.disabled=true",
                    ctx.containsBean("tracingScheduledTaskEnricher"),
                    is(false)
                );
            });
        }
    }

    // =========================================================================
    // Scenario 2: OTLP explicitly enabled — jframe.otlp.disabled=false
    // =========================================================================


    @Nested
    @DisplayName("Scenario 2: OTLP enabled (jframe.otlp.disabled=false)")
    class OtlpEnabled {

        /**
         * Context runner with {@code jframe.otlp.disabled=false}. No separate {@code Tracer}
         * mock is needed — the {@code OpenTelemetryAutoConfiguration.tracer()} {@code @Bean}
         * method becomes active and will call {@code stubbedOpenTelemetry().getTracer()} to
         * produce it, keeping the context consistent.
         */
        private final WebApplicationContextRunner otlpEnabledRunner = contextRunner
            .withPropertyValues("jframe.otlp.disabled=false");

        @Test
        @DisplayName("Should register SpanManager when jframe.otlp.disabled=false")
        void shouldRegisterSpanManagerWhenOtlpEnabled() {
            // Given: jframe.otlp.disabled=false
            otlpEnabledRunner.run(ctx ->
            // When / Then: SpanManager must be present
            assertThat(
                "SpanManager must be present when jframe.otlp.disabled=false",
                ctx.getBean(SpanManager.class),
                is(notNullValue())
            )
            );
        }

        @Test
        @DisplayName("Should register all tracing beans when jframe.otlp.disabled=false")
        void shouldRegisterAllTracingBeansWhenOtlpEnabled() {
            // Given: jframe.otlp.disabled=false
            otlpEnabledRunner.run(ctx -> {
                // When / Then: all tracing beans gated on jframe.otlp.disabled=false must be present
                assertThat("SpanManager", ctx.getBean(SpanManager.class), is(notNullValue()));
                assertThat("TracingResponseEnricher", ctx.getBean(TracingResponseEnricher.class), is(notNullValue()));
                assertThat("TracingAspect", ctx.getBean(TracingAspect.class), is(notNullValue()));
                assertThat("TracingScheduledTaskEnricher", ctx.getBean(TracingScheduledTaskEnricher.class), is(notNullValue()));
            });
        }

        @Test
        @DisplayName("Should still register unconditional OTLP beans when jframe.otlp.disabled=false")
        void shouldRegisterUnconditionalBeansAlsoWhenOtlpEnabled() {
            // Given: jframe.otlp.disabled=false
            otlpEnabledRunner.run(ctx -> {
                assertThat("OpenTelemetryPackageLogger", ctx.getBean(OpenTelemetryPackageLogger.class), is(notNullValue()));
                assertThat("TimerAspect", ctx.getBean(TimerAspect.class), is(notNullValue()));
                assertThat("HttpFilter", ctx.getBean(HttpFilter.class), is(notNullValue()));
                assertThat("HttpClientSSLFactory", ctx.getBean(HttpClientSSLFactory.class), is(notNullValue()));
                assertThat("UserIdentityFilterConfiguration", ctx.getBean(UserIdentityFilterConfiguration.class), is(notNullValue()));
            });
        }
    }
}
