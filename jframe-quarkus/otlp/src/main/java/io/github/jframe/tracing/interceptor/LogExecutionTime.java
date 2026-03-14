package io.github.jframe.tracing.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding annotation that marks methods or classes for execution-time logging.
 *
 * <p>Methods or classes annotated with {@code @LogExecutionTime} will have their execution
 * duration measured and logged by {@link TimerInterceptor}.
 */
@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
    {
        ElementType.TYPE,
        ElementType.METHOD
    }
)
public @interface LogExecutionTime {
}
