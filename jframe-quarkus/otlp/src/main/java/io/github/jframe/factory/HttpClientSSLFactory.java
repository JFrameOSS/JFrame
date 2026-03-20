package io.github.jframe.factory;

import io.github.jframe.exception.HttpException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import jakarta.enterprise.context.ApplicationScoped;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static io.github.jframe.http.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static io.github.jframe.util.constants.Constants.Protocols.TLS;
import static java.security.KeyStore.getDefaultType;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

/**
 * CDI bean for creating {@link SSLContext} instances with TLS configuration.
 *
 * <p>Supports both secure (with truststore) and insecure (trust-all) configurations.
 * Unlike the Spring equivalent, this returns raw {@link SSLContext} instances that can
 * be used with any HTTP client (Vert.x, MicroProfile REST Client, etc.).
 */
@Slf4j
@ApplicationScoped
public class HttpClientSSLFactory {

    /**
     * Creates an SSL context with appropriate trust configuration.
     *
     * @param useSecureConnection true for secure connection with truststore, false for trust-all
     * @param trustStorePath      the path to the truststore file
     * @param trustStorePassword  the truststore password
     * @return configured SSLContext
     */
    @SuppressWarnings("ReturnCount")
    public SSLContext createSSLContext(
        final boolean useSecureConnection,
        final String trustStorePath,
        final String trustStorePassword) {
        if (useSecureConnection) {
            log.info("Creating secure SSL context with truststore");
            return createSecureSSLContext(trustStorePath, trustStorePassword);
        }
        log.warn("Creating insecure SSL context that accepts all certificates");
        return createTrustAllSSLContext();
    }

    /**
     * Creates an SSL context that trusts all certificates.
     * WARNING: Only use in development/testing environments.
     */
    public SSLContext createTrustAllSSLContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(null, getTrustAllManager(), new SecureRandom());
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
     * Configures an insecure TrustManager which will trust all client and server certificates.
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
