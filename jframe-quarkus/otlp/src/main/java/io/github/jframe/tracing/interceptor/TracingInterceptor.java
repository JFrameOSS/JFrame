package io.github.jframe.tracing.interceptor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * CDI interceptor that creates an OpenTelemetry span around each intercepted method invocation.
 *
 * <p>Activated on methods or types annotated with {@link Traced}.
 * The span is always closed in a {@code finally} block so that it is finished even when the
 * intercepted method throws.
 *
 * <p>Only activated when {@code quarkus.otel.enabled=true}; otherwise the interceptor is not
 * registered, avoiding an unsatisfied {@link Tracer} dependency when OTel is disabled.
 */
@Interceptor
@Traced
@IfBuildProperty(
    name = "quarkus.otel.enabled",
    stringValue = "true"
)
public class TracingInterceptor {

    private final Tracer tracer;

    /**
     * Constructs a new {@code TracingInterceptor} with the given {@link Tracer}.
     *
     * @param tracer the OpenTelemetry tracer used to create spans
     */
    public TracingInterceptor(final Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Wraps the target method invocation with an OpenTelemetry span.
     *
     * @param context the CDI invocation context
     * @return the return value from the intercepted method
     * @throws Exception if the intercepted method throws
     */
    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        final String className = resolveClassName(context);
        final String methodName = context.getMethod().getName();
        final String spanName = className + "." + methodName;

        final Span span = tracer.spanBuilder(spanName).startSpan();
        try {
            return context.proceed();
        } finally {
            span.end();
        }
    }

    /**
     * Resolves the simple class name of the invocation target.
     * Falls back to the declaring class when the CDI proxy target is {@code null}.
     */
    private static String resolveClassName(final InvocationContext context) {
        return context.getTarget() != null
            ? context.getTarget().getClass().getSimpleName()
            : context.getMethod().getDeclaringClass().getSimpleName();
    }
}
