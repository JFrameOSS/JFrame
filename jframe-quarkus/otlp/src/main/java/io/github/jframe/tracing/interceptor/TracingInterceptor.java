package io.github.jframe.tracing.interceptor;

import io.github.jframe.autoconfigure.OpenTelemetryConfig;
import io.github.jframe.logging.kibana.KibanaLogField;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.security.QuarkusAuthenticationUtil;
import io.github.jframe.tracing.MethodExclusionRules;
import io.github.jframe.tracing.SpanNamingUtil;
import io.github.jframe.tracing.Traced;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.SPAN_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TRACE_ID;
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
@SuppressWarnings("PMD.ExcessiveImports")
public class TracingInterceptor {

    /** Lazily initialised; Mockito {@code @InjectMocks} injects a test double via reflection. */
    private Tracer tracer;

    @Inject
    private OpenTelemetryConfig config;

    @Inject
    private QuarkusAuthenticationUtil authenticationUtil;

    private Tracer resolveTracer() {
        if (tracer == null) {
            tracer = GlobalOpenTelemetry.getTracer("jframe-otlp");
        }
        return tracer;
    }

    /**
     * Wraps the target method invocation in an OpenTelemetry span.
     *
     * @param context the CDI invocation context
     * @return the return value from the intercepted method
     * @throws Exception if the intercepted method throws
     */
    @AroundInvoke
    @SuppressWarnings(
        {
            "try",
            "PMD.UnusedLocalVariable"
        }
    )
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        final String className = resolveClassName(context.getTarget().getClass().getSimpleName());
        final String methodName = context.getMethod().getName();

        if (config.disabled() || isExcluded(methodName)) {
            return context.proceed();
        }

        final String spanName = resolveSpanName(context, className, methodName);
        final Span span = resolveTracer()
            .spanBuilder(spanName)
            .setAttribute(SERVICE_NAME, className)
            .setAttribute(SERVICE_METHOD, methodName)
            .startSpan();

        // Ensure traceId/spanId are in MDC so log lines include them
        final String previousTraceId = KibanaLogFields.get(TRACE_ID);
        final String previousSpanId = KibanaLogFields.get(SPAN_ID);
        KibanaLogFields.tag(TRACE_ID, span.getSpanContext().getTraceId());
        KibanaLogFields.tag(SPAN_ID, span.getSpanContext().getSpanId());

        enrichSpanAndLog(span, spanName);

        final long startTime = System.nanoTime();
        try (Scope scope = span.makeCurrent()) {
            final Object result = context.proceed();
            final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("[jframe-otlp] Completed {} in {}ms", spanName, durationMs);
            return result;
        } catch (final Exception exception) {
            handleSpanError(span, spanName, startTime, exception);
            throw exception;
        } finally {
            span.end();
            restoreMdcField(TRACE_ID, previousTraceId);
            restoreMdcField(SPAN_ID, previousSpanId);
        }
    }

    private void enrichSpanAndLog(final Span span, final String spanName) {
        final String user = authenticationUtil.getAuthenticatedSubject();
        final String txId = KibanaLogFields.get(TX_ID);
        final String requestId = KibanaLogFields.get(REQUEST_ID);

        span.setAttribute(HTTP_REMOTE_USER, user);
        setAttributeIfPresent(span, HTTP_TRANSACTION_ID, txId);
        setAttributeIfPresent(span, HTTP_REQUEST_ID, requestId);

        log.debug(
            "[jframe-otlp] Entering {} | user={} traceId={} spanId={}",
            spanName,
            user,
            span.getSpanContext().getTraceId(),
            span.getSpanContext().getSpanId()
        );
    }

    private void handleSpanError(
        final Span span,
        final String spanName,
        final long startTime,
        final Exception exception) {

        final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        final String errorMessage = exception.getMessage() != null ? exception.getMessage() : "";
        span.setAttribute(ERROR, true);
        span.setAttribute(ERROR_TYPE, exception.getClass().getSimpleName());
        span.setAttribute(ERROR_MESSAGE, errorMessage);
        span.setStatus(StatusCode.ERROR);
        log.error(
            "[jframe-otlp] Failed {} in {}ms | error={} message={}",
            spanName,
            durationMs,
            exception.getClass().getSimpleName(),
            errorMessage
        );
    }

    private boolean isExcluded(final String methodName) {
        return MethodExclusionRules.isExcluded(methodName, config.excludedMethods());
    }

    private String resolveClassName(final String simpleClassName) {
        return SpanNamingUtil.resolveClassName(simpleClassName);
    }

    private String resolveSpanName(final InvocationContext context, final String className, final String methodName) {
        final Traced traced = context.getMethod().getAnnotation(Traced.class);
        final String customName = traced != null ? traced.value() : null;
        return SpanNamingUtil.resolveSpanName(className, methodName, customName);
    }

    private void setAttributeIfPresent(final Span span, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            span.setAttribute(key, value);
        }
    }

    private void restoreMdcField(final KibanaLogField field, final String previousValue) {
        if (previousValue != null) {
            KibanaLogFields.tag(field, previousValue);
        } else {
            KibanaLogFields.clear(field);
        }
    }
}
