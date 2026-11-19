package io.github.jframe.datasource.config;

import net.ttddyy.dsproxy.support.ProxyDataSource;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

/** Context-runner tests for {@link DatasourceProxyConfiguration}. */
@DisplayName("Datasource Proxy - Spring auto-configuration")
class DatasourceProxyAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DatasourceProxyConfiguration.class))
        .withBean("dataSource", DataSource.class, () -> mock(DataSource.class));

    @Test
    @DisplayName("Should wrap DataSource with ProxyDataSource by default")
    void shouldWrapDataSourceByDefault() {
        // When / Then: the DataSource is proxied
        contextRunner.run(ctx -> assertThat(ctx.getBean(DataSource.class), is(instanceOf(ProxyDataSource.class))));
    }

    @Test
    @DisplayName("Should not wrap DataSource when jframe.datasource.proxy.enabled=false")
    void shouldNotWrapDataSourceWhenDisabled() {
        // When / Then: the DataSource is left untouched
        contextRunner
            .withPropertyValues("jframe.datasource.proxy.enabled=false")
            .run(ctx -> assertThat(ctx.getBean(DataSource.class), is(not(instanceOf(ProxyDataSource.class)))));
    }
}
