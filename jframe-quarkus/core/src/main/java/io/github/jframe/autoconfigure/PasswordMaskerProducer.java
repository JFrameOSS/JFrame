package io.github.jframe.autoconfigure;

import io.github.jframe.logging.LoggingConfig;
import io.github.jframe.logging.masker.type.PasswordMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI producer that creates a {@link PasswordMasker} bean configured with the
 * fields-to-mask list from {@link LoggingConfig}.
 *
 * <p>Quarkus equivalent of Spring's {@code CoreAutoConfiguration.passwordMaskerUtil()}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PasswordMaskerProducer {

    private final LoggingConfig loggingConfig;

    /**
     * Produces a {@link PasswordMasker} configured with the fields from
     * {@link LoggingConfig#fieldsToMask()}.
     *
     * @return the configured {@link PasswordMasker}
     */
    @Produces
    @ApplicationScoped
    public PasswordMasker passwordMasker() {
        return new PasswordMasker(loggingConfig.fieldsToMask());
    }
}
