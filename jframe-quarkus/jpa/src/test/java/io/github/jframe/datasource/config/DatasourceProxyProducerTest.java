package io.github.jframe.datasource.config;

import io.agroal.api.AgroalDataSource;
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
 * <p>Verifies the CDI producer that wraps an {@link AgroalDataSource} with
 * {@link ProxyDataSource} for SQL query logging, including proxy configuration,
 * datasource name, and listener wiring.
 */
@DisplayName("Quarkus JPA - DatasourceProxyProducer")
public class DatasourceProxyProducerTest extends UnitTest {

    @Mock
    private AgroalDataSource mockAgroalDataSource;

    private DatasourceProxyProducer producer;

    @Override
    @BeforeEach
    public void setUp() {
        producer = new DatasourceProxyProducer(mockAgroalDataSource);
    }

    // -------------------------------------------------------------------------
    // 1. Should return non-null ProxyDataSource instance
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return non-null ProxyDataSource instance from producer")
    public void shouldReturnNonNullProxyDataSourceInstanceFromProducer() {
        // Given: A producer wrapping a valid AgroalDataSource

        // When: Calling the CDI producer method
        final DataSource result = producer.proxiedDataSource();

        // Then: The result should be a non-null ProxyDataSource instance
        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(ProxyDataSource.class));
    }

    // -------------------------------------------------------------------------
    // 2. Should wrap original AgroalDataSource inside the ProxyDataSource
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should wrap original AgroalDataSource inside the ProxyDataSource")
    public void shouldWrapOriginalAgroalDataSourceInsideTheProxyDataSource() {
        // Given: A producer constructed with a known AgroalDataSource

        // When: Producing the proxied DataSource
        final DataSource result = producer.proxiedDataSource();
        final ProxyDataSource proxyDataSource = (ProxyDataSource) result;

        // Then: The inner DataSource should be the original AgroalDataSource
        assertThat(proxyDataSource.getDataSource(), is(equalTo(mockAgroalDataSource)));
    }

    // -------------------------------------------------------------------------
    // 3. Should expose a non-null proxy config
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should expose a non-null proxy config")
    public void shouldExposeNonNullProxyConfig() {
        // Given: A producer wrapping an AgroalDataSource

        // When: Producing the proxied DataSource
        final DataSource result = producer.proxiedDataSource();
        final ProxyDataSource proxyDataSource = (ProxyDataSource) result;

        // Then: The proxy config should be accessible
        assertThat(proxyDataSource.getProxyConfig(), is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // 4. Should configure SLF4JQueryLoggingListener on the proxy
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should configure SLF4JQueryLoggingListener on the proxy")
    public void shouldConfigureSLF4JQueryLoggingListenerOnTheProxy() {
        // Given: A producer wrapping an AgroalDataSource

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
