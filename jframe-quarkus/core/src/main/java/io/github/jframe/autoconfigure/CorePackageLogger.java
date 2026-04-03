package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.properties.ApplicationConfig;
import io.github.jframe.logging.filter.JFrameFilter;
import io.quarkus.runtime.StartupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.TimeZone;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

import static io.github.jframe.util.constants.Constants.Characters.NEW_LINE;

/**
 * CDI bean that logs application metadata at startup.
 *
 * <p>Observes {@link StartupEvent} and emits a formatted INFO log message containing
 * application properties (name, group, environment, version, timezone) and the list of
 * registered {@link JFrameFilter} implementations.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CorePackageLogger {

    private final ApplicationConfig applicationConfig;
    private final Instance<JFrameFilter> filters;

    /**
     * Handles the Quarkus {@link StartupEvent} by building and logging the startup message.
     *
     * @param event the CDI startup event (unused, present for CDI observer resolution)
     */
    public void onApplicationStarted(@Observes final StartupEvent event) {
        log.info(buildStartupMessage());
    }

    /**
     * Builds the formatted startup log message containing application configuration
     * and registered filters.
     *
     * @return the fully assembled startup message string
     */
    String buildStartupMessage() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Application started with the following properties:\n");
        builder.append("  - Application Name: ").append(applicationConfig.name()).append(NEW_LINE);
        builder.append("  - Application Group: ").append(applicationConfig.group()).append(NEW_LINE);
        builder.append("  - Application Environment: ").append(applicationConfig.environment()).append(NEW_LINE);
        builder.append("  - Application Version: ").append(applicationConfig.version()).append(NEW_LINE);
        builder.append("  - Default Timezone: ").append(TimeZone.getDefault().getID()).append(NEW_LINE);
        builder.append("  - Registered Filters:").append(NEW_LINE);
        for (final JFrameFilter filter : filters) {
            builder.append("      * ").append(filter.getClass().getSimpleName()).append(NEW_LINE);
        }
        return builder.toString();
    }
}
