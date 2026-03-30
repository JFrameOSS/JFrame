package io.github.jframe.logging.scheduled;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.model.RequestId;
import io.github.jframe.logging.model.TransactionId;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import static io.github.jframe.logging.ecs.EcsFieldNames.REQUEST_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static java.util.UUID.randomUUID;

/**
 * Aspect around @{@link org.springframework.scheduling.annotation.Scheduled} annotation to allow participating in ECS tx's.
 */
@Slf4j
@Aspect
@Component
public class ScheduledAspect {

    /**
     * Create an around advise for the {@link org.springframework.scheduling.annotation.Scheduled} annotation.
     *
     * @param pjp The proceeding join point.
     * @return The original return value.
     * @throws Throwable in case of an error.
     */
    @Around("execution (@org.springframework.scheduling.annotation.Scheduled  * *.*(..))")
    public Object withTransactionId(final ProceedingJoinPoint pjp) throws Throwable {
        try {
            final UUID uuid = randomUUID();
            RequestId.set(uuid);
            EcsFields.tag(REQUEST_ID, RequestId.get());

            TransactionId.set(uuid);
            EcsFields.tag(TX_ID, TransactionId.get());

            log.trace("Started scheduled task with tx id '{}'.", TransactionId.get());
            return pjp.proceed();
        } catch (final Exception exception) {
            log.error("Caught error '{}'.", exception.getMessage(), exception);
            throw exception;
        } finally {
            RequestId.remove();
            TransactionId.remove();
            EcsFields.clear();
        }
    }
}
