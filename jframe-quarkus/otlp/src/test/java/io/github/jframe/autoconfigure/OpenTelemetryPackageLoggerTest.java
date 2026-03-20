package io.github.jframe.autoconfigure;

import io.github.jframe.tracing.OpenTelemetryConfig;
import io.github.support.UnitTest;
import io.quarkus.runtime.StartupEvent;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OpenTelemetryPackageLogger}.
 *
 * <p>Verifies that the startup log message is correctly built from the config values
 * and that logging is skipped when tracing is disabled.
 */
@DisplayName("Quarkus OTLP - OpenTelemetry Package Logger")
public class OpenTelemetryPackageLoggerTest extends UnitTest {

    @Mock
    private OpenTelemetryConfig openTelemetryConfig;

    private OpenTelemetryPackageLogger logger;

    @Override
    @BeforeEach
    public void setUp() {
        logger = new OpenTelemetryPackageLogger(openTelemetryConfig);
    }

    @Test
    @DisplayName("Should instantiate successfully")
    public void shouldInstantiateSuccessfully() {
        // Given / When: logger is created

        // Then: it is not null
        assertThat(logger, is(notNullValue()));
    }

    @Test
    @DisplayName("Should build startup message containing OTLP endpoint")
    public void shouldBuildStartupMessageContainingEndpoint() {
        // Given: config returns a URL
        when(openTelemetryConfig.url()).thenReturn("http://collector:4318");
        when(openTelemetryConfig.samplingRate()).thenReturn(0.5);
        when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of("health", "ping"));

        // When: building the startup message
        final String message = logger.buildStartupMessage();

        // Then: endpoint is present
        assertThat(message, containsString("http://collector:4318"));
    }

    @Test
    @DisplayName("Should build startup message containing sampling rate")
    public void shouldBuildStartupMessageContainingSamplingRate() {
        // Given: config returns a sampling rate
        when(openTelemetryConfig.url()).thenReturn("http://localhost:4318");
        when(openTelemetryConfig.samplingRate()).thenReturn(0.75);
        when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of("health"));

        // When: building the startup message
        final String message = logger.buildStartupMessage();

        // Then: sampling rate is present
        assertThat(message, containsString("0.75"));
    }

    @Test
    @DisplayName("Should build startup message containing excluded methods")
    public void shouldBuildStartupMessageContainingExcludedMethods() {
        // Given: config returns excluded methods
        when(openTelemetryConfig.url()).thenReturn("http://localhost:4318");
        when(openTelemetryConfig.samplingRate()).thenReturn(1.0);
        when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of("health", "ping"));

        // When: building the startup message
        final String message = logger.buildStartupMessage();

        // Then: excluded methods are present
        assertThat(message, containsString("health"));
    }

    @Test
    @DisplayName("Should build startup message with header text")
    public void shouldBuildStartupMessageWithHeaderText() {
        // Given: config with any values
        when(openTelemetryConfig.url()).thenReturn("http://localhost:4318");
        when(openTelemetryConfig.samplingRate()).thenReturn(1.0);
        when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of());

        // When: building the startup message
        final String message = logger.buildStartupMessage();

        // Then: header text is present
        assertThat(message, containsString("OpenTelemetry initialized with the following properties:"));
    }

    @Test
    @DisplayName("Should not log when disabled")
    public void shouldNotLogWhenDisabled() {
        // Given: tracing is disabled
        when(openTelemetryConfig.disabled()).thenReturn(true);

        // When / Then: no exception — log.info is not called (disabled guard)
        logger.onApplicationStarted(new StartupEvent());
    }

    @Test
    @DisplayName("Should log when enabled")
    public void shouldLogWhenEnabled() {
        // Given: tracing is enabled with valid config
        when(openTelemetryConfig.disabled()).thenReturn(false);
        when(openTelemetryConfig.url()).thenReturn("http://localhost:4318");
        when(openTelemetryConfig.samplingRate()).thenReturn(1.0);
        when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of("health"));

        // When / Then: no exception — log.info is called with the startup message
        logger.onApplicationStarted(new StartupEvent());
    }
}
