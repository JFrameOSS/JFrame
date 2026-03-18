package io.github.jframe.datasource.config;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;

import static net.ttddyy.dsproxy.support.ProxyDataSourceBuilder.create;

/**
 * CDI producer that wraps a {@link DataSource} with a proxy for SQL query logging.
 */
@Slf4j
@ApplicationScoped
public class DatasourceProxyProducer {

    private final DataSource dataSource;

    /**
     * Constructs a new {@code DatasourceProxyProducer} with the given {@link DataSource}.
     *
     * @param dataSource the original data source to wrap
     */
    public DatasourceProxyProducer(@NonNull final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Produces a proxied {@link DataSource} that logs all SQL queries via SLF4J.
     *
     * @return a {@link DataSource} proxy configured with {@link SLF4JQueryLoggingListener}
     */
    @Produces
    @ApplicationScoped
    public DataSource proxiedDataSource() {
        log.info("Wrapping DataSource with ProxyDataSource for query logging");
        return create(dataSource)
            .name("Datasource Query Logger")
            .listener(buildLoggingListener())
            .build();
    }

    private SLF4JQueryLoggingListener buildLoggingListener() {
        final SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        final PrettyQueryEntryCreator prettyQueryEntryCreator = new PrettyQueryEntryCreator();
        prettyQueryEntryCreator.setMultiline(true);
        loggingListener.setQueryLogEntryCreator(prettyQueryEntryCreator);
        return loggingListener;
    }
}
