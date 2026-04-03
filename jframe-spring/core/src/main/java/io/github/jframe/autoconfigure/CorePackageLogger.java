package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.properties.ApplicationProperties;
import io.github.jframe.logging.filter.AbstractGenericFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.TimeZone;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static io.github.jframe.util.constants.Constants.Characters.NEW_LINE;

/**
 * Logs application properties when the application context is started. This class listens for the {@link ApplicationStartedEvent} and logs
 * relevant application information such as name, group, environment, version, and default timezone.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorePackageLogger {

    /**
     * The application properties that are used to log application configuration.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * The registered filters in the application.
     */
    private final List<AbstractGenericFilter> registeredFilters;

    /**
     * Logs application properties when the application context is started.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Application started with the following properties:\n");
        builder.append("  - Application Name: ").append(applicationProperties.getName()).append(NEW_LINE);
        builder.append("  - Application Group: ").append(applicationProperties.getGroup()).append(NEW_LINE);
        builder.append("  - Application Environment: ").append(applicationProperties.getEnvironment()).append(NEW_LINE);
        builder.append("  - Application Version: ").append(applicationProperties.getVersion()).append(NEW_LINE);
        builder.append("  - Default Timezone: ").append(TimeZone.getDefault().getID()).append(NEW_LINE);
        builder.append("  - Registered Filters:").append(NEW_LINE);
        for (final AbstractGenericFilter filter : registeredFilters) {
            builder.append("      * ").append(filter.getClass().getSimpleName()).append(NEW_LINE);
        }
        builder.setLength(builder.length());
        log.info(builder.toString());
    }


}
