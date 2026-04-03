package io.github.jframe.datasource.config;

import io.github.jframe.datasource.logging.DefaultQueryExecutionListener;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSource;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import static net.ttddyy.dsproxy.support.ProxyDataSourceBuilder.create;

/**
 * Builds a ProxyDataSource with a SLF4JQueryLoggingListener.
 */
@Slf4j
@Configuration
public class DatasourceProxyConfiguration implements BeanPostProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public Object postProcessAfterInitialization(@NonNull final Object bean, @NonNull final String beanName) throws BeansException {
        if (!(bean instanceof ProxyDataSource) && bean instanceof DataSource dataSourceBean) {
            log.info("Wrapping DataSource '{}' with ProxyDataSource for query logging", beanName);
            return create(dataSourceBean)
                .name("Datasource Query Logger")
                .listener(new DefaultQueryExecutionListener(new PrettyQueryEntryCreator()))
                .build();
        }
        return bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object postProcessBeforeInitialization(@NonNull final Object bean, @NonNull final String beanName) throws BeansException {
        return bean;
    }
}
