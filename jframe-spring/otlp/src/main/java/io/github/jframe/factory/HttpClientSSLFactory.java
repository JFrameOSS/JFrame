package io.github.jframe.factory;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import static io.github.jframe.util.constants.Constants.Protocols.TLS;
import static java.security.KeyStore.getDefaultType;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

/** Creates HTTP request factories with TLS configured according to {@link TlsOptions}. */
@Slf4j
@Component
public class HttpClientSSLFactory {

    /**
     * Creates an HTTP request factory.
     *
     * @param options               TLS trust configuration
     * @param connectTimeoutSeconds connect and connection-request timeout in seconds
     * @param readTimeoutSeconds    read timeout in seconds
     * @return configured request factory
     */
    public HttpComponentsClientHttpRequestFactory createRequestFactory(
        final TlsOptions options,
        final int connectTimeoutSeconds,
        final int readTimeoutSeconds) {

        Objects.requireNonNull(options, "options");
        final Duration connectTimeout = Duration.ofSeconds(connectTimeoutSeconds);
        final PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setTlsSocketStrategy(createTlsStrategy(options))
            .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.of(connectTimeout)).build())
            .build();

        final HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(HttpClients.custom().setConnectionManager(connectionManager).build());
        requestFactory.setConnectionRequestTimeout(connectTimeout);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return requestFactory;
    }

    private static DefaultClientTlsStrategy createTlsStrategy(final TlsOptions options) {
        try {
            return switch (options.mode()) {
                case DEFAULT -> new DefaultClientTlsStrategy(SSLContext.getDefault());
                case CUSTOM -> new DefaultClientTlsStrategy(
                    createTrustStoreContext(options.trustStorePath(), options.trustStorePassword())
                );
                case SSL_BUNDLE -> new DefaultClientTlsStrategy(options.sslBundle().createSslContext());
                case TRUST_ALL -> {
                    log.warn("Creating insecure HTTP client that accepts all SSL certificates");
                    yield new DefaultClientTlsStrategy(createContext(getTrustAllManager()), (hostname, session) -> true);
                }
            };
        } catch (final Exception exception) {
            throw new HttpException(JFrameErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private static SSLContext createTrustStoreContext(final String path, final String password) throws Exception {
        log.info("Initializing SSL context with truststore: {}", path);
        final KeyStore trustStore = KeyStore.getInstance(getDefaultType());
        try (InputStream input = Files.newInputStream(Paths.get(path))) {
            trustStore.load(input, password.toCharArray());
        }
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return createContext(trustManagerFactory.getTrustManagers());
    }

    private static SSLContext createContext(final TrustManager... trustManagers) throws Exception {
        final SSLContext sslContext = SSLContext.getInstance(TLS);
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext;
    }

    /**
     * Configures an insecure {@link TrustManager} which will trust all client and server certificates.
     *
     * @return The list with the configured trust manager
     */
    public static TrustManager[] getTrustAllManager() {
        return new TrustManager[] {
            new X509TrustManager() {

                @Override
                public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                    // Trust all client certificates
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
                    // Trust all server certificates
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            },
        };
    }
}
