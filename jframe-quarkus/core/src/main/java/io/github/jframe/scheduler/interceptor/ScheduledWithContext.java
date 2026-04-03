package io.github.jframe.scheduler.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding annotation that marks scheduled methods for context propagation.
 *
 * <p>Methods or classes annotated with {@code @ScheduledWithContext} will have a correlation
 * context (RequestId, TransactionId, MDC fields) established and cleaned up around each
 * invocation by {@link ScheduledContextInterceptor}.
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
public @interface ScheduledWithContext {
}
