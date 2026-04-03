package io.github.jframe.logging.scheduled;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Strategy interface for enriching scheduled task execution.
 *
 * <p>Implementations can add tracing, MDC context, or other cross-cutting concerns
 * to {@code @Scheduled} methods via the {@link ScheduledAspect}.
 *
 * <p>The returned {@link AutoCloseable} is closed after the scheduled task completes
 * (in reverse enricher order), allowing cleanup of resources like spans or MDC fields.
 */
@FunctionalInterface
public interface ScheduledTaskEnricher {

    /**
     * Enrich the scheduled task execution context.
     *
     * @param pjp the proceeding join point for the scheduled method
     * @return an {@link AutoCloseable} to clean up after task completion, or {@code null} for no-op
     */
    AutoCloseable enrich(ProceedingJoinPoint pjp);
}
