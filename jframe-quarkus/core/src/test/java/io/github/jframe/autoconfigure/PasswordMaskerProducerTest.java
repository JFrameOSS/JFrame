package io.github.jframe.autoconfigure;

import io.github.jframe.logging.LoggingConfig;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PasswordMaskerProducer}.
 *
 * <p>Verifies that the CDI producer creates a {@link PasswordMasker} configured
 * with the fields from {@link LoggingConfig}.
 */
@DisplayName("Autoconfigure - PasswordMaskerProducer")
public class PasswordMaskerProducerTest extends UnitTest {

    @Mock
    private LoggingConfig loggingConfig;

    @Test
    @DisplayName("Should produce a non-null PasswordMasker from LoggingConfig fields")
    public void shouldProducePasswordMaskerWhenLoggingConfigProvided() {
        // Given: A LoggingConfig returning a list of fields to mask
        when(loggingConfig.fieldsToMask()).thenReturn(List.of("password", "secret"));
        final PasswordMaskerProducer producer = new PasswordMaskerProducer(loggingConfig);

        // When: Producing the PasswordMasker
        final PasswordMasker passwordMasker = producer.passwordMasker();

        // Then: A non-null PasswordMasker is produced
        assertThat(passwordMasker, is(notNullValue()));
    }

    @Test
    @DisplayName("Should mask configured fields when masking input")
    public void shouldMaskConfiguredFieldsWhenMaskingInput() {
        // Given: A LoggingConfig returning "password" and "secret" as fields to mask
        when(loggingConfig.fieldsToMask()).thenReturn(List.of("password", "secret"));
        final PasswordMaskerProducer producer = new PasswordMaskerProducer(loggingConfig);
        final PasswordMasker passwordMasker = producer.passwordMasker();

        // When: Masking a JSON string containing those fields
        final String masked = passwordMasker.maskPasswordsIn("{\"password\":\"s3cr3t\",\"username\":\"admin\"}");

        // Then: The password field is masked and non-sensitive fields are untouched
        assertThat(masked, is("{\"password\":\"***\",\"username\":\"admin\"}"));
    }

    @Test
    @DisplayName("Should mask all configured fields when multiple sensitive fields present")
    public void shouldMaskAllConfiguredFieldsWhenMultipleSensitiveFieldsPresent() {
        // Given: A LoggingConfig returning both "password" and "secret" as fields to mask
        when(loggingConfig.fieldsToMask()).thenReturn(List.of("password", "secret"));
        final PasswordMaskerProducer producer = new PasswordMaskerProducer(loggingConfig);
        final PasswordMasker passwordMasker = producer.passwordMasker();

        // When: Masking a JSON string containing both sensitive fields
        final String masked = passwordMasker.maskPasswordsIn("{\"password\":\"abc123\",\"secret\":\"topsecret\"}");

        // Then: Both fields are masked
        assertThat(masked, is("{\"password\":\"***\",\"secret\":\"***\"}"));
    }

    @Test
    @DisplayName("Should not mask non-configured fields when masking input")
    public void shouldNotMaskNonConfiguredFieldsWhenMaskingInput() {
        // Given: A LoggingConfig returning only "password" as a field to mask
        when(loggingConfig.fieldsToMask()).thenReturn(List.of("password"));
        final PasswordMaskerProducer producer = new PasswordMaskerProducer(loggingConfig);
        final PasswordMasker passwordMasker = producer.passwordMasker();

        // When: Masking a JSON string with an unmasked field
        final String masked = passwordMasker.maskPasswordsIn("{\"username\":\"admin\",\"email\":\"admin@example.com\"}");

        // Then: No masking is applied to non-configured fields
        assertThat(masked, is("{\"username\":\"admin\",\"email\":\"admin@example.com\"}"));
    }
}
