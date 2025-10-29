package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.factory.YamlPropertySourceFactory;
import io.github.jframe.autoconfigure.properties.ApplicationProperties;
import io.github.jframe.autoconfigure.properties.OpenTelemetryProperties;
import io.github.jframe.logging.filter.config.TracingIdResponseFilterConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Main autoconfiguration for JFrame OpenTelemetry integration.
 */
@PropertySource(
    value = "classpath:jframe-properties.yml",
    factory = YamlPropertySourceFactory.class
)
@AutoConfiguration
@Import(TracingIdResponseFilterConfiguration.class)
@ComponentScan(basePackages = "io.github.jframe.*")
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
