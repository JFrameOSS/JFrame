package io.github.jframe.logging.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the execution time of the annotated method should be logged.
 * <p>
 * When a method annotated with {@code @LogExecutionTime} is executed, the
 * {@link io.github.jframe.logging.aspect.TimerAspect} intercepts the call
 * and logs the total execution time in milliseconds.
 * </p>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * @LogExecutionTime
 * public void performTask() {
 * // some time-consuming logic
 * }
 * }</pre>
 *
 * <p>The execution time will be logged automatically by the
 * {@code TimerAspect}.</p>
 *
 * @see io.github.jframe.logging.aspect.TimerAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {

}
