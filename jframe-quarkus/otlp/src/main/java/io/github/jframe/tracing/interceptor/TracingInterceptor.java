package io.github.jframe.tracing.interceptor;

import io.github.jframe.autoconfigure.OpenTelemetryConfig;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.security.QuarkusAuthenticationUtil;
import io.github.jframe.tracing.Traced;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_NAME;

/**
 * CDI interceptor that wraps intercepted method invocations in OpenTelemetry spans.
 *
 * <p>Activated on methods or types annotated with {@link Traced}. The interceptor:
 * <ul>
 * <li>Skips tracing when {@link OpenTelemetryConfig#disabled()} is {@code true}</li>
 * <li>Skips tracing for methods with prefixes: {@code get}, {@code set}, {@code is}</li>
 * <li>Skips tracing for well-known methods: {@code toString}, {@code hashCode},
 * {@code equals}, {@code clone}</li>
 * <li>Skips tracing for methods in {@link OpenTelemetryConfig#excludedMethods()} (case-insensitive)</li>
 * <li>Creates a span named {@code ClassName.methodName}, or uses a custom name
 * from {@link Traced#value()} when non-empty</li>
 * <li>Sets {@code service.name} and {@code service.method} span attributes</li>
 * <li>Enriches every span with {@code http.remote_user} (always), {@code http.transaction_id}
 * and {@code http.request_id} (only when non-blank in MDC)</li>
 * <li>On exception: sets {@code error=true}, {@code error.type}, {@code error.message},
 * {@code StatusCode.ERROR}, then re-throws</li>
 * <li>Always ends the span and closes the scope in a {@code finally} block</li>
 * </ul>
 */
@Slf4j
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
@Traced
public class TracingInterceptor {

    private static final Set<String> EXCLUDED_PREFIXES = Set.of("get", "set", "is");
    private static final Set<String> EXCLUDED_NAMES = Set.of("toString", "hashCode", "equals", "clone");

    @Inject
    private Tracer tracer;

    @Inject
    private OpenTelemetryConfig config;

    @Inject
    private QuarkusAuthenticationUtil authenticationUtil;

    /**
     * Wraps the target method invocation in an OpenTelemetry span.
     *
     * @param context the CDI invocation context
     * @return the return value from the intercepted method
     * @throws Exception if the intercepted method throws
     */
    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        final String className = resolveClassName(context.getTarget().getClass().getSimpleName());
        final String methodName = context.getMethod().getName();

        if (config.disabled() || isExcluded(methodName)) {
            return context.proceed();
        }

        final String spanName = resolveSpanName(context, className, methodName);
        final Span span = tracer.spanBuilder(spanName)
            .setAttribute(SERVICE_NAME, className)
            .setAttribute(SERVICE_METHOD, methodName)
            .startSpan();

        span.setAttribute(HTTP_REMOTE_USER, authenticationUtil.getAuthenticatedSubject());
        setAttributeIfPresent(span, HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID));
        setAttributeIfPresent(span, HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID));

        try (Scope scope = span.makeCurrent()) {
            log.trace("Entering method: {}.{} span: {} scope: {}", className, methodName, span.getSpanContext(), scope);
            return context.proceed();
        } catch (final Exception exception) {
            span.setAttribute(ERROR, true);
            span.setAttribute(ERROR_TYPE, exception.getClass().getSimpleName());
            span.setAttribute(ERROR_MESSAGE, exception.getMessage() != null ? exception.getMessage() : "");
            span.setStatus(StatusCode.ERROR);
            throw exception;
        } finally {
            span.end();
        }
    }

    private boolean isExcluded(final String methodName) {
        if (EXCLUDED_NAMES.contains(methodName)) {
            return true;
        }
        final boolean hasPrefixMatch = EXCLUDED_PREFIXES.stream().anyMatch(methodName::startsWith);
        return hasPrefixMatch || config.excludedMethods().contains(methodName.toLowerCase());
    }

    private String resolveClassName(final String simpleClassName) {
        // CDI proxy classes may include suffixes like "$Proxy$..." — strip them
        final int dollarIndex = simpleClassName.indexOf('$');
        if (dollarIndex > 0) {
            return simpleClassName.substring(0, dollarIndex);
        }
        return simpleClassName;
    }

    private String resolveSpanName(final InvocationContext context, final String className, final String methodName) {
        final Traced traced = context.getMethod().getAnnotation(Traced.class);
        if (traced != null && !traced.value().isEmpty()) {
            return traced.value();
        }
        return className + "." + methodName;
    }

    private void setAttributeIfPresent(final Span span, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            span.setAttribute(key, value);
        }
    }
}
