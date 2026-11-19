package io.github.jframe.datasource.config;

import io.github.jframe.datasource.logging.DefaultQueryExecutionListener;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSource;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static net.ttddyy.dsproxy.support.ProxyDataSourceBuilder.create;

/**
 * Wraps every {@link DataSource} in a {@link ProxyDataSource} for SQL logging.
 *
 * <p>Disable with {@code jframe.datasource.proxy.enabled=false}.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(
    name = DatasourceProxyConfiguration.ENABLED_PROPERTY,
    matchIfMissing = true
)
public final class DatasourceProxyConfiguration {

    /** Property toggling the DataSource proxy. */
    public static final String ENABLED_PROPERTY = "jframe.datasource.proxy.enabled";

    private DatasourceProxyConfiguration() {
        // Only static bean methods
    }

    /**
     * Static so it does not force early initialisation of this configuration class.
     *
     * @return post-processor wrapping {@link DataSource} beans
     */
    @Bean
    public static BeanPostProcessor datasourceProxyBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(@NonNull final Object bean, @NonNull final String beanName) {
                if (!(bean instanceof ProxyDataSource) && bean instanceof DataSource dataSourceBean) {
                    log.info("Wrapping DataSource '{}' with ProxyDataSource for query logging", beanName);
                    return create(dataSourceBean)
                        .name("Datasource Query Logger")
                        .listener(new DefaultQueryExecutionListener(new PrettyQueryEntryCreator()))
                        .build();
                }
                return bean;
            }
        };
    }
}
