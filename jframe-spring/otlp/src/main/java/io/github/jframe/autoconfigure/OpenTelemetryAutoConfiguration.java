package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.properties.ApplicationProperties;
import io.github.jframe.autoconfigure.properties.OpenTelemetryProperties;
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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Main autoconfiguration for JFrame OpenTelemetry integration.
 *
 * <p>Registers all jframe-spring/otlp beans explicitly via {@code @Import} or {@code @Bean},
 * replacing the former broad {@code @ComponentScan(basePackages = "io.github.jframe")}.
 *
 * <p>Must run after {@link CoreAutoConfiguration} because this class's {@link #tracer} bean
 * needs {@link ApplicationProperties}, which is only enabled via
 * {@code @EnableConfigurationProperties} in {@link CoreAutoConfiguration}.
 * The former duplicate {@code @PropertySource} declaration has been removed — the single
 * source in {@link CoreAutoConfiguration} is canonical.
 */
@AutoConfiguration
@AutoConfigureAfter(CoreAutoConfiguration.class)
@Import(
    {
        // Unconditional beans — always registered when otlp module is on classpath.
        OpenTelemetryPackageLogger.class,
        TimerAspect.class,
        HttpFilter.class,
        HttpClientSSLFactory.class,

        // UserIdentityFilterConfiguration carries @ConditionalOnProperty(matchIfMissing=true)
        // — @Import preserves it.
        UserIdentityFilterConfiguration.class,

        // Conditional beans — gated on @ConditionalOnProperty(jframe.otlp.disabled, havingValue="false").
        // @Import preserves class-level conditionals.
        SpanManager.class,
        TracingResponseEnricher.class,
        TracingAspect.class,
        TracingScheduledTaskEnricher.class
    }
)
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public class OpenTelemetryAutoConfiguration {

    /**
     * Creates a Tracer bean using the OpenTelemetry instance and application properties.
     *
     * @param openTelemetry         the OpenTelemetry instance
     * @param applicationProperties the application properties containing name and version
     * @return a Tracer instance
     */
    @Bean
    @ConditionalOnProperty(
        name = "jframe.otlp.disabled",
        havingValue = "false"
    )
    public Tracer tracer(final OpenTelemetry openTelemetry, final ApplicationProperties applicationProperties) {
        return openTelemetry.getTracer(applicationProperties.getName(), applicationProperties.getVersion());
    }

}
