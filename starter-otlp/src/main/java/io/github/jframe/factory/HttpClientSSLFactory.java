package io.github.jframe.factory;

import io.github.jframe.exception.HttpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import static io.github.jframe.util.constants.Constants.Protocols.TLS;
import static java.security.KeyStore.getDefaultType;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Factory for creating HTTP request factories with SSL/TLS configuration. Supports both secure (with truststore) and insecure (trust-all)
 * configurations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClientSSLFactory {

    /**
     * Creates an HTTP request factory with appropriate SSL configuration.
     *
     * @param useSecureConnection   true for secure connection with truststore, false for trust-all
     * @param trustStorePath        the path to the truststore file containing the certificates
     * @param trustStorePassword    the associated password for the truststore
     * @param connectTimeoutSeconds connection timeout in seconds
     * @param readTimeoutSeconds    read timeout in seconds
     * @return configured HttpComponentsClientHttpRequestFactory
     */
    public HttpComponentsClientHttpRequestFactory createRequestFactory(
        final boolean useSecureConnection,
        final String trustStorePath,
        final String trustStorePassword,
        final int connectTimeoutSeconds,
        final int readTimeoutSeconds) {

        final HttpClientBuilder builder = HttpClients.custom();
        try {
            final PoolingHttpClientConnectionManager connectionManager;

            if (useSecureConnection) {
                log.info("Creating secure HTTP client with truststore");
                connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(
                        new DefaultClientTlsStrategy(
                            createSecureSSLContext(trustStorePath, trustStorePassword)
                        )
                    )
                    .build();
            } else {
                log.warn("Creating insecure HTTP client that accepts all SSL certificates");
                connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(
                        new DefaultClientTlsStrategy(
                            createTrustAllSSLContext(),
                            (hostname, session) -> true
                        )
                    ) // Accept all hostnames
                    .build();
            }

            builder.setConnectionManager(connectionManager);

            final HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(builder.build());
            requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
            requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

            return requestFactory;
        } catch (final Exception exception) {
            throw new HttpException("Could not create HTTP client with TLS strategy", exception, INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates an SSL context that trusts all certificates. WARNING: Only use in development/testing environments.
     */
    private SSLContext createTrustAllSSLContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(
                null,
                getTrustAllManager(),
                new SecureRandom()
            );

            return sslContext;
        } catch (final Exception exception) {
            throw new HttpException("Could not create trust-all SSL context", exception, INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Creates a secure SSL context using the provided truststore.
     */
    private SSLContext createSecureSSLContext(final String trustStorePath, final String trustStorePassword) {
        try (InputStream trustStoreFile = Files.newInputStream(Paths.get(trustStorePath))) {
            log.info("Initializing secure SSL Context with truststore: {}", trustStorePath);
            final KeyStore trustStore = KeyStore.getInstance(getDefaultType());
            trustStore.load(trustStoreFile, trustStorePassword.toCharArray());

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            final SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (final Exception exception) {
            throw new HttpException("Could not create SSL context", exception, INTERNAL_SERVER_ERROR);
        }
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
