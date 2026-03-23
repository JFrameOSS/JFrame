package io.github.jframe.tracing.interceptor;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.security.QuarkusAuthenticationUtil;
import io.github.jframe.tracing.OpenTelemetryConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.security.identity.SecurityIdentity;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_NAME;

/**
 * CDI interceptor that creates an OpenTelemetry span around each intercepted method invocation.
 *
 * <p>Activated on methods or types annotated with {@link Traced}.
 * The span is always closed in a {@code finally} block so that it is finished even when the
 * intercepted method throws.
 *
 * <p>Common getter/setter/infrastructure methods (get*, set*, is*, toString, hashCode, equals)
 * are excluded from tracing, as are any methods listed in {@link OpenTelemetryConfig#excludedMethods()}.
 *
 * <p>When OpenTelemetry is disabled, the {@link Tracer} is unavailable and the interceptor
 * passes through to the target method without creating spans.
 */
@Slf4j
@Traced
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class TracingInterceptor {

    private final Tracer tracer;
    private final OpenTelemetryConfig openTelemetryConfig;
    private final Instance<SecurityIdentity> securityIdentityInstance;

    /**
     * Constructs a new {@code TracingInterceptor} with an optional {@link Tracer} and
     * supporting configuration.
     * When OpenTelemetry is disabled, the tracer instance is not available and
     * the interceptor becomes a pass-through.
     *
     * @param tracerInstance           the optional OpenTelemetry tracer
     * @param openTelemetryConfig      configuration providing excluded method names
     * @param securityIdentityInstance the optional CDI security identity
     */
    public TracingInterceptor(
                              final Instance<Tracer> tracerInstance,
                              final OpenTelemetryConfig openTelemetryConfig,
                              final Instance<SecurityIdentity> securityIdentityInstance) {
        this.tracer = tracerInstance.isResolvable() ? tracerInstance.get() : null;
        this.openTelemetryConfig = openTelemetryConfig;
        this.securityIdentityInstance = securityIdentityInstance;
    }

    /**
     * Wraps the target method invocation with an OpenTelemetry span.
     *
     * <p>Getter, setter, and infrastructure methods are skipped, as are methods
     * matching the configured exclusion list. When no {@link Tracer} is available,
     * proceeds without tracing.
     *
     * @param context the CDI invocation context
     * @return the return value from the intercepted method
     * @throws Exception if the intercepted method throws
     */
    @AroundInvoke
    @SuppressWarnings("ReturnCount")
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        if (tracer == null) {
            return context.proceed();
        }

        final String className = resolveClassName(context);
        final String methodName = context.getMethod().getName();

        if (isExcluded(methodName)) {
            return context.proceed();
        }

        final String spanName = className + "." + methodName;
        final SecurityIdentity securityIdentity =
            securityIdentityInstance.isResolvable() ? securityIdentityInstance.get() : null;

        final Span span = tracer.spanBuilder(spanName)
            .setAttribute(SERVICE_NAME, className)
            .setAttribute(SERVICE_METHOD, methodName)
            .setAttribute(HTTP_REMOTE_USER, QuarkusAuthenticationUtil.getSubject(securityIdentity))
            .setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID))
            .setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID))
            .startSpan();
        try (Scope scope = span.makeCurrent()) {
            log.trace(
                "Entering method: {}.{} with span in current scope: {}-{}",
                className,
                methodName,
                span.getSpanContext(),
                scope
            );
            return context.proceed();
        } finally {
            span.end();
        }
    }

    /**
     * Returns {@code true} when the given method name should be skipped for tracing.
     * Excludes standard accessor/object methods and any names in the configured exclusion set.
     */
    private boolean isExcluded(final String methodName) {
        return isAccessorOrObjectMethod(methodName)
            || openTelemetryConfig.excludedMethods().contains(methodName.toLowerCase());
    }

    /**
     * Returns {@code true} for getter, setter, and standard {@link Object} methods.
     */
    private static boolean isAccessorOrObjectMethod(final String methodName) {
        if (methodName.startsWith("get") || methodName.startsWith("set") || methodName.startsWith("is")) {
            return true;
        }
        return "toString".equals(methodName) || "hashCode".equals(methodName) || "equals".equals(methodName);
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
