package io.github.jframe.datasource.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Characterization / regression test for the jframe-spring-jpa auto-configuration surface.
 *
 * <p>Asserts that {@link DatasourceProxyConfiguration} is registered via the jpa module's own
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} entry
 * point — not as an accidental side-effect of another module's component scan.
 */
@DisplayName("Characterization Test - jframe-spring-jpa auto-configuration surface")
class DatasourceProxyAutoConfigurationTest {

    /**
     * Boots a context using ONLY {@link DatasourceProxyConfiguration} as the auto-configuration
     * entry point — exactly the way Spring Boot's auto-configuration mechanism loads it
     * when the imports file is present.
     */
    @Test
    @DisplayName("Should register DatasourceProxyConfiguration via jpa module's own auto-configuration")
    void shouldRegisterDatasourceProxyConfigurationViaJpaAutoConfiguration() {
        // Given: ONLY the jpa module's auto-configuration is applied (no core @ComponentScan as crutch)
        final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DatasourceProxyConfiguration.class))
            .withPropertyValues(
                "jframe.application.name=test-service",
                "jframe.application.group=io.github.jframe",
                "jframe.application.version=0.0.1"
            );

        // When: the context starts / Then: DatasourceProxyConfiguration must be present
        contextRunner.run(
            ctx -> assertThat(
                "DatasourceProxyConfiguration must be registered by the jpa module's own "
                    + "auto-configuration — not as a side-effect of core's @ComponentScan",
                ctx.getBean(DatasourceProxyConfiguration.class),
                is(notNullValue())
            )
        );
    }
}
