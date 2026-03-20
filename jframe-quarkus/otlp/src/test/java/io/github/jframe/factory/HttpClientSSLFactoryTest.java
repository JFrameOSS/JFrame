package io.github.jframe.factory;

import io.github.jframe.exception.HttpException;
import io.github.support.UnitTest;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link HttpClientSSLFactory}.
 *
 * <p>Verifies that:
 * <ul>
 * <li>{@code createTrustAllSSLContext} returns a valid {@link SSLContext}</li>
 * <li>{@code createSSLContext} with {@code useSecureConnection=false} returns a trust-all context</li>
 * <li>{@code createSSLContext} with {@code useSecureConnection=true} and invalid path throws {@link HttpException}</li>
 * <li>{@code getTrustAllManager} returns an array with exactly one TrustManager</li>
 * </ul>
 */
@DisplayName("Quarkus OTLP - HttpClientSSLFactory")
public class HttpClientSSLFactoryTest extends UnitTest {

    private HttpClientSSLFactory factory;

    @Override
    @BeforeEach
    public void setUp() {
        factory = new HttpClientSSLFactory();
    }

    @Test
    @DisplayName("Should create trust-all SSL context successfully")
    public void shouldCreateTrustAllSSLContextSuccessfully() {
        // Given: a new factory

        // When: creating a trust-all SSL context
        final SSLContext sslContext = factory.createTrustAllSSLContext();

        // Then: the context is not null and has the correct protocol
        assertThat(sslContext, is(notNullValue()));
        assertThat(sslContext.getProtocol(), is("TLS"));
    }

    @Test
    @DisplayName("Should return trust-all SSL context when useSecureConnection is false")
    public void shouldReturnTrustAllContextWhenSecureConnectionIsFalse() {
        // Given: useSecureConnection = false

        // When: creating an SSL context
        final SSLContext sslContext = factory.createSSLContext(false, null, null);

        // Then: a valid SSL context is returned
        assertThat(sslContext, is(notNullValue()));
        assertThat(sslContext.getProtocol(), is("TLS"));
    }

    @Test
    @DisplayName("Should throw HttpException when useSecureConnection is true and truststore path is invalid")
    public void shouldThrowHttpExceptionWhenTruststorePathInvalid() {
        // Given: an invalid truststore path
        final String invalidPath = "/non/existent/truststore.jks";
        final String password = "password";

        // When / Then: HttpException is thrown
        assertThrows(
            HttpException.class,
            () -> factory.createSSLContext(true, invalidPath, password)
        );
    }

    @Test
    @DisplayName("Should return array with exactly one TrustManager")
    public void shouldReturnArrayWithOneManager() {
        // Given / When: getting the trust-all manager array
        final TrustManager[] managers = HttpClientSSLFactory.getTrustAllManager();

        // Then: exactly one manager is returned
        assertThat(managers, is(notNullValue()));
        assertThat(managers.length, is(1));
    }

    @Test
    @DisplayName("Should return non-null TrustManager instance")
    public void shouldReturnNonNullTrustManager() {
        // Given / When: getting the trust-all manager array
        final TrustManager[] managers = HttpClientSSLFactory.getTrustAllManager();

        // Then: the manager itself is not null
        assertThat(managers[0], is(notNullValue()));
    }
}
