package io.github.jframe.factory;

import io.github.support.UnitTest;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.NoSuchFileException;
import java.security.KeyStore;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslStoreBundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Factory - HttpClientSSLFactory")
class HttpClientSSLFactoryTest extends UnitTest {

    private static final String TRUST_STORE_RESOURCE = "ssl/truststore.p12";
    private static final String PASSWORD = "changeit";

    private final HttpClientSSLFactory factory = new HttpClientSSLFactory();

    static Stream<TlsOptions> validOptions() throws Exception {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = loader.getResourceAsStream(TRUST_STORE_RESOURCE)) {
            trustStore.load(input, PASSWORD.toCharArray());
        }
        return Stream.of(
            TlsOptions.defaultTrustStore(),
            TlsOptions.customTrustStore(loader.getResource(TRUST_STORE_RESOURCE).toURI().getPath(), PASSWORD),
            TlsOptions.sslBundle(SslBundle.of(SslStoreBundle.of(null, null, trustStore))),
            TlsOptions.trustAll()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validOptions")
    @DisplayName("Should build request factory for every trust mode")
    void shouldBuildRequestFactoryForEveryTrustMode(final TlsOptions options) {
        // When & Then: A request factory is built
        assertThat(factory.createRequestFactory(options, 5, 10), is(notNullValue()));
    }

    @Test
    @DisplayName("Should throw IllegalStateException with cause when trust store does not exist")
    void shouldThrowIllegalStateWhenTrustStoreDoesNotExist() {
        // Given: Options pointing to a missing trust store
        final TlsOptions options = TlsOptions.customTrustStore("/does/not/exist.p12", PASSWORD);

        // When: Creating the request factory
        final IllegalStateException exception =
            assertThrows(IllegalStateException.class, () -> factory.createRequestFactory(options, 5, 10));

        // Then: The I/O failure is kept as cause
        assertThat(exception.getCause(), is(instanceOf(NoSuchFileException.class)));
    }

    @Test
    @DisplayName("Should not expose the trust-all manager publicly")
    void shouldNotExposeTrustAllManager() throws Exception {
        // When: Looking up the trust-all manager
        final Method method = HttpClientSSLFactory.class.getDeclaredMethod("getTrustAllManager");

        // Then: It is private
        assertThat(Modifier.isPrivate(method.getModifiers()), is(true));
    }

    @Test
    @DisplayName("Should reject null or blank input")
    void shouldRejectNullOrBlankInput() {
        // When & Then: Invalid input is rejected
        assertThrows(NullPointerException.class, () -> factory.createRequestFactory(null, 5, 10));
        assertThrows(NullPointerException.class, () -> TlsOptions.customTrustStore(null, PASSWORD));
        assertThrows(NullPointerException.class, () -> TlsOptions.customTrustStore("/truststore.p12", null));
        assertThrows(IllegalArgumentException.class, () -> TlsOptions.customTrustStore(" ", PASSWORD));
        assertThrows(NullPointerException.class, () -> TlsOptions.sslBundle(null));
    }
}
