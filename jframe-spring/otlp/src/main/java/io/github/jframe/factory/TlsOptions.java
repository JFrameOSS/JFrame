package io.github.jframe.factory;

import java.util.Objects;

import org.springframework.boot.ssl.SslBundle;

/**
 * TLS trust configuration for {@link HttpClientSSLFactory}. Create via the static factories.
 *
 * @param mode               the trust mode
 * @param trustStorePath     trust store path, only for {@link Mode#CUSTOM}
 * @param trustStorePassword trust store password, only for {@link Mode#CUSTOM}
 * @param sslBundle          SSL bundle, only for {@link Mode#SSL_BUNDLE}
 */
public record TlsOptions(Mode mode, String trustStorePath, String trustStorePassword, SslBundle sslBundle) {

    /** Validates the fields required by the given mode. */
    public TlsOptions {
        Objects.requireNonNull(mode, "mode");
        switch (mode) {
            case CUSTOM -> {
                Objects.requireNonNull(trustStorePath, "path");
                Objects.requireNonNull(trustStorePassword, "password");
                if (trustStorePath.isBlank()) {
                    throw new IllegalArgumentException("Trust store path must not be blank");
                }
            }
            case SSL_BUNDLE -> Objects.requireNonNull(sslBundle, "bundle");
            default -> {
                // No additional fields required
            }
        }
    }

    /** Supported trust modes. */
    public enum Mode {
        DEFAULT,
        CUSTOM,
        SSL_BUNDLE,
        TRUST_ALL
    }

    /** Verified TLS using the JVM default trust store (cacerts). */
    public static TlsOptions defaultTrustStore() {
        return new TlsOptions(Mode.DEFAULT, null, null, null);
    }

    /** Verified TLS using a trust store file. */
    public static TlsOptions customTrustStore(final String path, final String password) {
        return new TlsOptions(Mode.CUSTOM, path, password, null);
    }

    /** Verified TLS using a Spring Boot {@link SslBundle}. */
    public static TlsOptions sslBundle(final SslBundle bundle) {
        return new TlsOptions(Mode.SSL_BUNDLE, null, null, bundle);
    }

    /** Trusts all certificates and hostnames. Development/testing only. */
    public static TlsOptions trustAll() {
        return new TlsOptions(Mode.TRUST_ALL, null, null, null);
    }

    @Override
    public String toString() {
        return "TlsOptions[mode=" + mode + ", trustStorePath=" + trustStorePath + "]";
    }
}
