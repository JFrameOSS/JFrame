package io.github.jframe.logging.aspect;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect that measures and logs the execution time of methods annotated with
 * {@link io.github.jframe.logging.aspect.LogExecutionTime}.
 * <p>
 * This aspect uses Spring AOP and Lombok’s {@code @Slf4j} logger to record
 * the duration of a method call in milliseconds.
 * </p>
 *
 * <p><strong>How it works:</strong></p>
 * <ul>
 * <li>Intercepts methods annotated with {@code @LogExecutionTime}.</li>
 * <li>Records the start and end times of method execution.</li>
 * <li>Logs the method name and total execution time in milliseconds.</li>
 * </ul>
 *
 * <p>Useful for performance monitoring and identifying slow methods during
 * development or in production environments.</p>
 */
@Slf4j
@Aspect
@Component
public class TimerAspect {

    /**
     * Logs the execution time of the annotated method.
     *
     * @param joinPoint The join point.
     * @return The result of the method.
     * @throws Throwable If something goes wrong.
     */
    @Around("@annotation(io.github.jframe.logging.aspect.LogExecutionTime)")
    public Object logExecutionTime(final ProceedingJoinPoint joinPoint) throws Throwable {
        final long startTime = System.currentTimeMillis();
        final Object proceed = joinPoint.proceed();
        final long endTime = System.currentTimeMillis();
        log.info("[Execution Timer] Method '{}' took {}ms", joinPoint.getSignature().getName(), endTime - startTime);
        return proceed;
    }
}
