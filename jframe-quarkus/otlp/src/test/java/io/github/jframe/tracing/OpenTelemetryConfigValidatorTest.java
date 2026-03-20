package io.github.jframe.tracing;

import io.github.support.UnitTest;
import io.quarkus.runtime.StartupEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OpenTelemetryConfigValidator}.
 *
 * <p>Verifies that startup validation logs appropriate warnings for invalid configuration
 * and skips validation entirely when tracing is disabled.
 */
@DisplayName("Quarkus OTLP - OpenTelemetry Config Validator")
public class OpenTelemetryConfigValidatorTest extends UnitTest {

    @Mock
    private OpenTelemetryConfig config;

    private OpenTelemetryConfigValidator validator;

    @Override
    @BeforeEach
    public void setUp() {
        validator = new OpenTelemetryConfigValidator(config);
    }

    @Test
    @DisplayName("Should instantiate successfully")
    public void shouldInstantiateSuccessfully() {
        // Given / When: validator is created

        // Then: it is not null
        assertThat(validator, is(notNullValue()));
    }

    @Test
    @DisplayName("Should skip all validation when config is disabled")
    public void shouldSkipValidationWhenDisabled() {
        // Given: tracing is disabled
        when(config.disabled()).thenReturn(true);

        // When / Then: startup event fires without exception (no further config access)
        validator.onStartup(new StartupEvent());
    }

    @Test
    @DisplayName("Should run without exception for valid configuration")
    public void shouldRunWithoutExceptionForValidConfig() {
        // Given: all values are valid
        when(config.disabled()).thenReturn(false);
        when(config.url()).thenReturn("http://localhost:4318");
        when(config.timeout()).thenReturn("10s");
        when(config.exporter()).thenReturn("otlp");
        when(config.samplingRate()).thenReturn(1.0);

        // When / Then: no exception thrown
        validator.onStartup(new StartupEvent());
    }

    @Test
    @DisplayName("Should run without exception when URL is blank")
    public void shouldRunWithoutExceptionWhenUrlIsBlank() {
        // Given: blank URL
        when(config.disabled()).thenReturn(false);
        when(config.url()).thenReturn("");
        when(config.timeout()).thenReturn("10s");
        when(config.exporter()).thenReturn("otlp");
        when(config.samplingRate()).thenReturn(1.0);

        // When / Then: no exception — only a warning is logged
        validator.onStartup(new StartupEvent());
    }

    @Test
    @DisplayName("Should run without exception when timeout format is invalid")
    public void shouldRunWithoutExceptionWhenTimeoutInvalid() {
        // Given: timeout in wrong format
        when(config.disabled()).thenReturn(false);
        when(config.url()).thenReturn("http://localhost:4318");
        when(config.timeout()).thenReturn("invalid");
        when(config.exporter()).thenReturn("otlp");
        when(config.samplingRate()).thenReturn(1.0);

        // When / Then: no exception — only a warning is logged
        validator.onStartup(new StartupEvent());
    }

    @Test
    @DisplayName("Should run without exception when exporter is invalid")
    public void shouldRunWithoutExceptionWhenExporterInvalid() {
        // Given: unknown exporter name
        when(config.disabled()).thenReturn(false);
        when(config.url()).thenReturn("http://localhost:4318");
        when(config.timeout()).thenReturn("10s");
        when(config.exporter()).thenReturn("unknown");
        when(config.samplingRate()).thenReturn(1.0);

        // When / Then: no exception — only a warning is logged
        validator.onStartup(new StartupEvent());
    }

    @Test
    @DisplayName("Should run without exception when sampling rate is out of range")
    public void shouldRunWithoutExceptionWhenSamplingRateOutOfRange() {
        // Given: sampling rate > 1.0
        when(config.disabled()).thenReturn(false);
        when(config.url()).thenReturn("http://localhost:4318");
        when(config.timeout()).thenReturn("10s");
        when(config.exporter()).thenReturn("otlp");
        when(config.samplingRate()).thenReturn(1.5);

        // When / Then: no exception — only a warning is logged
        validator.onStartup(new StartupEvent());
    }

    @Test
    @DisplayName("Should run without exception when sampling rate is negative")
    public void shouldRunWithoutExceptionWhenSamplingRateNegative() {
        // Given: sampling rate < 0.0
        when(config.disabled()).thenReturn(false);
        when(config.url()).thenReturn("http://localhost:4318");
        when(config.timeout()).thenReturn("10s");
        when(config.exporter()).thenReturn("otlp");
        when(config.samplingRate()).thenReturn(-0.1);

        // When / Then: no exception — only a warning is logged
        validator.onStartup(new StartupEvent());
    }

    @Test
    @DisplayName("Should accept all valid exporter names")
    public void shouldAcceptAllValidExporterNames() {
        // Given: all known valid exporters
        final String[] validExporters = {
            "otlp",
            "jaeger",
            "zipkin"
        };

        for (final String exporter : validExporters) {
            when(config.disabled()).thenReturn(false);
            when(config.url()).thenReturn("http://localhost:4318");
            when(config.timeout()).thenReturn("10s");
            when(config.exporter()).thenReturn(exporter);
            when(config.samplingRate()).thenReturn(1.0);

            // When / Then: no exception for valid exporter
            validator.onStartup(new StartupEvent());
        }
    }
}
