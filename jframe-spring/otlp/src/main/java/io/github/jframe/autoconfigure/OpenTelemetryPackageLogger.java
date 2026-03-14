package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.properties.OpenTelemetryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static io.github.jframe.OpenTelemetryConstants.Logging.LINE_BREAK;

/**
 * Logs application properties when the application context is started. This class listens for the {@link ApplicationStartedEvent} and logs
 * relevant application information such as name, group, environment, version, and default timezone.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenTelemetryPackageLogger {

    /**
     * The OpenTelemetry properties that are used to log OpenTelemetry configuration.
     */
    private final OpenTelemetryProperties openTelemetryProperties;

    /**
     * Logs application properties when the application context is started.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        if (!openTelemetryProperties.isDisabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("OpenTelemetry initialized with the following properties:\n");
            builder.append("  - OTLP Endpoint: ").append(openTelemetryProperties.getUrl()).append(LINE_BREAK);
            builder.append("  - OTLP Sampling Rate: ").append(openTelemetryProperties.getSamplingRate()).append(LINE_BREAK);
            builder.append("  - Excluded Methods: ").append(openTelemetryProperties.getExcludedMethods()).append(LINE_BREAK);
            builder.setLength(builder.length());
            log.info(builder.toString());
        }
    }
}
