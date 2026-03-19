package io.github.jframe.datasource.config;

import io.agroal.api.AgroalDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;

import static net.ttddyy.dsproxy.support.ProxyDataSourceBuilder.create;

/**
 * CDI producer that wraps the Agroal {@link DataSource} with a proxy for SQL query logging.
 *
 * <p>Uses {@link Alternative} with {@link Priority} to replace the default Agroal-managed
 * {@link DataSource} bean, preventing ambiguous resolution.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DatasourceProxyProducer {

    @NonNull
    private final AgroalDataSource dataSource;

    /**
     * Produces a proxied {@link DataSource} that logs all SQL queries via SLF4J.
     *
     * @return a {@link DataSource} proxy configured with {@link SLF4JQueryLoggingListener}
     */
    @Produces
    @Alternative
    @Priority(1)
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
