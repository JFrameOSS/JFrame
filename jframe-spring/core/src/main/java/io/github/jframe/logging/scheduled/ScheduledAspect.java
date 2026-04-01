package io.github.jframe.logging.scheduled;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.model.RequestId;
import io.github.jframe.logging.model.TransactionId;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static io.github.jframe.logging.ecs.EcsFieldNames.REQUEST_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static java.util.UUID.randomUUID;

/**
 * Aspect around @{@link org.springframework.scheduling.annotation.Scheduled} annotation to allow participating in ECS tx's.
 *
 * <p>Sets TX_ID and REQUEST_ID for the scheduled task, then delegates to registered
 * {@link ScheduledTaskEnricher} instances for additional context (e.g. tracing spans).
 * Enricher resources are closed in reverse order after task completion.
 */
@Slf4j
@Aspect
@Component
public class ScheduledAspect {

    private final List<ScheduledTaskEnricher> enrichers;

    /**
     * Constructor.
     *
     * @param enrichers optional list of enrichers; defaults to empty if null
     */
    @Autowired
    public ScheduledAspect(@Autowired(required = false) final List<ScheduledTaskEnricher> enrichers) {
        this.enrichers = enrichers != null ? enrichers : Collections.emptyList();
    }

    /**
     * Create an around advise for the {@link org.springframework.scheduling.annotation.Scheduled} annotation.
     *
     * @param pjp The proceeding join point.
     * @return The original return value.
     * @throws Throwable in case of an error.
     */
    @Around("execution (@org.springframework.scheduling.annotation.Scheduled  * *.*(..))")
    @SuppressWarnings("PMD.UseTryWithResources")
    public Object withTransactionId(final ProceedingJoinPoint pjp) throws Throwable {
        final List<AutoCloseable> closeables = new ArrayList<>();
        try {
            final UUID uuid = randomUUID();
            RequestId.set(uuid);
            EcsFields.tag(REQUEST_ID, RequestId.get());

            TransactionId.set(uuid);
            EcsFields.tag(TX_ID, TransactionId.get());

            log.trace("Started scheduled task with tx id '{}'.", TransactionId.get());

            for (final ScheduledTaskEnricher enricher : enrichers) {
                try {
                    final AutoCloseable closeable = enricher.enrich(pjp);
                    if (closeable != null) {
                        closeables.add(closeable);
                    }
                } catch (final Exception exception) {
                    log.warn("Enricher '{}' failed during enrich(): {}", enricher.getClass().getSimpleName(), exception.getMessage());
                }
            }

            return pjp.proceed();
        } catch (final Exception exception) {
            log.error("Caught error '{}'.", exception.getMessage(), exception);
            throw exception;
        } finally {
            final List<AutoCloseable> reversed = new ArrayList<>(closeables);
            Collections.reverse(reversed);
            for (final AutoCloseable closeable : reversed) {
                try {
                    closeable.close();
                } catch (final Exception exception) {
                    log.warn("Failed to close enricher resource: {}", exception.getMessage());
                }
            }

            RequestId.remove();
            TransactionId.remove();
            EcsFields.clear();
        }
    }
}
