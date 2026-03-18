package io.github.jframe.datasource.config;

import io.github.support.UnitTest;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link DatasourceProxyProducer}.
 *
 * <p>Verifies the CDI producer that wraps a {@link DataSource} with
 * {@link ProxyDataSource} for SQL query logging, including proxy configuration,
 * datasource name, and listener wiring.
 */
@DisplayName("Quarkus JPA - DatasourceProxyProducer")
public class DatasourceProxyProducerTest extends UnitTest {

    @Mock
    private DataSource mockDataSource;

    private DatasourceProxyProducer producer;

    @Override
    @BeforeEach
    public void setUp() {
        producer = new DatasourceProxyProducer(mockDataSource);
    }

    // -------------------------------------------------------------------------
    // 1. Should return ProxyDataSource instance
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return ProxyDataSource instance from producer")
    public void shouldReturnProxyDataSourceInstanceFromProducer() {
        // Given: A producer wrapping a real DataSource

        // When: Calling the CDI producer method
        final DataSource result = producer.proxiedDataSource();

        // Then: The result should be a ProxyDataSource instance
        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(ProxyDataSource.class));
    }

    // -------------------------------------------------------------------------
    // 2. Should return ProxyDataSource that wraps the original DataSource
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should wrap original DataSource inside the ProxyDataSource")
    public void shouldWrapOriginalDataSourceInsideTheProxyDataSource() {
        // Given: A producer constructed with a known DataSource

        // When: Producing the proxied DataSource
        final DataSource result = producer.proxiedDataSource();
        final ProxyDataSource proxyDataSource = (ProxyDataSource) result;

        // Then: The inner DataSource should be the original one
        assertThat(proxyDataSource.getDataSource(), is(equalTo(mockDataSource)));
    }

    // -------------------------------------------------------------------------
    // 3. Should configure proxy with name "Datasource Query Logger"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should configure proxy with name 'Datasource Query Logger'")
    public void shouldConfigureProxyWithNameDatasourceQueryLogger() {
        // Given: A producer wrapping a DataSource

        // When: Producing the proxied DataSource
        final DataSource result = producer.proxiedDataSource();
        final ProxyDataSource proxyDataSource = (ProxyDataSource) result;

        // Then: The proxy datasource name should be "Datasource Query Logger"
        assertThat(proxyDataSource.getProxyConfig().getDataSourceName(), is(equalTo("Datasource Query Logger")));
    }

    // -------------------------------------------------------------------------
    // 4. Should configure SLF4JQueryLoggingListener on the proxy
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should configure SLF4JQueryLoggingListener on the proxy")
    public void shouldConfigureSLF4JQueryLoggingListenerOnTheProxy() {
        // Given: A producer wrapping a DataSource

        // When: Producing the proxied DataSource
        final DataSource result = producer.proxiedDataSource();
        final ProxyDataSource proxyDataSource = (ProxyDataSource) result;

        // Then: The proxy should have a ChainListener containing SLF4JQueryLoggingListener
        final ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
        assertThat(chainListener, is(notNullValue()));
        assertThat(chainListener.getListeners(), is(notNullValue()));
        final boolean hasSLF4JListener = chainListener.getListeners().stream()
            .anyMatch(listener -> listener instanceof SLF4JQueryLoggingListener);
        assertThat(hasSLF4JListener, is(true));
    }
}
