package io.github.jframe.tracing.interceptor;

import java.util.concurrent.TimeUnit;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CDI interceptor that measures and logs the execution time of intercepted methods.
 *
 * <p>Activated on methods or types annotated with {@link LogExecutionTime}.
 * The elapsed time is logged even when the intercepted method throws.
 */
@Interceptor
@LogExecutionTime
public class TimerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TimerInterceptor.class);

    /**
     * Measures the execution time of the intercepted method and logs the result.
     *
     * @param context the CDI invocation context
     * @return the return value from the intercepted method
     * @throws Exception if the intercepted method throws
     */
    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        final long start = System.nanoTime();
        final String className = resolveClassName(context);
        final String methodName = context.getMethod().getName();
        try {
            return context.proceed();
        } finally {
            final long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            LOG.debug("Method '{}.{}' executed in {} ms", className, methodName, elapsedMs);
        }
    }

    /**
     * Resolves the simple class name of the invocation target.
     * Falls back to the declaring class when the CDI proxy target is {@code null}
     * (e.g. for interceptors applied to static-context calls).
     */
    private static String resolveClassName(final InvocationContext context) {
        return context.getTarget() != null
            ? context.getTarget().getClass().getSimpleName()
            : context.getMethod().getDeclaringClass().getSimpleName();
    }
}
