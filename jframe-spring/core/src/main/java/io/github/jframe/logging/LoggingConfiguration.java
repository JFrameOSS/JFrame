package io.github.jframe.logging;

import io.github.jframe.logging.filter.FilterConfiguration;
import io.github.jframe.logging.interceptor.LoggingClientHttpRequestInterceptor;
import io.github.jframe.logging.logger.HttpRequestResponseBodyLogger;
import io.github.jframe.logging.logger.HttpRequestResponseDebugLogger;
import io.github.jframe.logging.logger.HttpRequestResponseHeadersLogger;
import io.github.jframe.logging.scheduled.ScheduledAspect;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Aggregator configuration that registers all jframe logging beans.
 *
 * <p>Imported by {@code CoreAutoConfiguration} to keep that class's import count within
 * PMD {@code ExcessiveImports} limits. {@code FilterConfiguration} already {@code @Import}s
 * its 4 filter sub-configurations.
 */
@Configuration
@Import(
    {
        HttpRequestResponseBodyLogger.class,
        HttpRequestResponseHeadersLogger.class,
        HttpRequestResponseDebugLogger.class,
        LoggingClientHttpRequestInterceptor.class,
        ScheduledAspect.class,
        FilterConfiguration.class
    }
)
public class LoggingConfiguration {}
