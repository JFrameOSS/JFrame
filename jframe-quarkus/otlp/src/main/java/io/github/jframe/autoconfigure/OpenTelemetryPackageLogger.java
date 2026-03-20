package io.github.jframe.autoconfigure;

import io.github.jframe.tracing.OpenTelemetryConfig;
import io.quarkus.runtime.StartupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import static io.github.jframe.tracing.OpenTelemetryConstants.Logging.LINE_BREAK;

/**
 * CDI bean that logs OpenTelemetry configuration at startup.
 *
 * <p>Observes {@link StartupEvent} and emits a formatted INFO log message containing
 * the active OTLP configuration properties.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OpenTelemetryPackageLogger {

    /** The active OpenTelemetry configuration properties. */
    private final OpenTelemetryConfig openTelemetryConfig;

    /**
     * Handles the Quarkus {@link StartupEvent} by logging the OpenTelemetry startup message.
     *
     * @param event the CDI startup event (unused, present for CDI observer resolution)
     */
    public void onApplicationStarted(@Observes final StartupEvent event) {
        if (!openTelemetryConfig.disabled()) {
            log.info(buildStartupMessage());
        }
    }

    /**
     * Builds the formatted startup log message containing OpenTelemetry configuration.
     *
     * @return the fully assembled startup message string
     */
    String buildStartupMessage() {
        final StringBuilder builder = new StringBuilder();
        builder.append("OpenTelemetry initialized with the following properties:\n");
        builder.append("  - OTLP Endpoint: ").append(openTelemetryConfig.url()).append(LINE_BREAK);
        builder.append("  - OTLP Sampling Rate: ").append(openTelemetryConfig.samplingRate()).append(LINE_BREAK);
        builder.append("  - Excluded Methods: ").append(openTelemetryConfig.excludedMethods()).append(LINE_BREAK);
        builder.append("  - Auto-Trace: ").append(openTelemetryConfig.autoTrace()).append(LINE_BREAK);
        return builder.toString();
    }
}
