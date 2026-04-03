package io.github.jframe.tracing.aspect;

import io.github.jframe.autoconfigure.properties.OpenTelemetryProperties;
import io.github.jframe.logging.ecs.EcsField;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;
import io.github.jframe.tracing.MethodExclusionRules;
import io.github.jframe.tracing.SpanNamingUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static io.github.jframe.logging.ecs.EcsFieldNames.USER_NAME;

/**
 * Aspect for tracing method execution using OpenTelemetry. Creates a span for each method in traced classes.
 *
 * <p>{@code @Scheduled} methods are traced by {@link io.github.jframe.tracing.scheduled.TracingScheduledTaskEnricher}
 * via the {@link io.github.jframe.logging.scheduled.ScheduledAspect} enricher strategy pattern.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "otel.sdk.disabled",
    havingValue = "false"
)
public class TracingAspect {

    private final Tracer tracer;

    private final OpenTelemetryProperties openTelemetryProperties;

    /**
     * Intercepts method calls in classes annotated with @Traced and creates a span for the method execution.
     *
     * @param joinPoint The join point representing the method call.
     * @return The result of the method execution.
     * @throws Throwable If an error occurs during method execution.
     */
    @Around(
        "@within(org.springframework.stereotype.Service) || "
            + "@within(org.springframework.stereotype.Controller) || "
            + "@within(org.springframework.web.bind.annotation.RestController) || "
            + "@within(io.github.jframe.tracing.aspect.Traced) && "
            + "!execution(* get*()) && "
            + "!execution(* set*()) && "
            + "!execution(* is*()) && "
            + "!execution(* toString()) && "
            + "!execution(* hashCode()) && "
            + "!execution(* equals(..))"
    )
    @SuppressWarnings(
        {
            "try",
            "PMD.UnusedLocalVariable"
        }
    )
    public Object traceClass(final ProceedingJoinPoint joinPoint) throws Throwable {
        final String className = SpanNamingUtil.resolveClassName(joinPoint.getTarget().getClass().getSimpleName());
        final String methodName = joinPoint.getSignature().getName();

        if (MethodExclusionRules.isExcluded(methodName, openTelemetryProperties.getExcludedMethods())) {
            return joinPoint.proceed();
        }

        final String spanName = SpanNamingUtil.resolveSpanName(className, methodName, null);
        final Span span = tracer.spanBuilder(spanName)
            .setAttribute(SPAN_SERVICE_NAME.getKey(), className)
            .setAttribute(SPAN_SERVICE_METHOD.getKey(), methodName)
            .setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), EcsFields.getOrDefault(USER_NAME, AuthenticationConstants.ANONYMOUS))
            .setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), EcsFields.get(TX_ID))
            .setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), EcsFields.get(REQUEST_ID))
            .startSpan();

        final String previousTraceId = EcsFields.get(TRACE_ID);
        final String previousSpanId = EcsFields.get(SPAN_ID);

        final long startTime = System.nanoTime();
        try (Scope scope = span.makeCurrent()) {
            EcsFields.tag(TRACE_ID, span.getSpanContext().getTraceId());
            EcsFields.tag(SPAN_ID, span.getSpanContext().getSpanId());
            log.trace(
                "[OPENTELEMETRY] Entering {} | user={} traceId={} spanId={}",
                spanName,
                EcsFields.getOrDefault(USER_NAME, AuthenticationConstants.ANONYMOUS),
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId()
            );
            final Object result = joinPoint.proceed();
            final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.trace("[OPENTELEMETRY] Completed {} in {}ms", spanName, durationMs);
            return result;
        } catch (final Throwable throwable) {
            final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR);
            log.error(
                "[OPENTELEMETRY] Failed {} in {}ms | error={} message={}",
                spanName,
                durationMs,
                throwable.getClass().getSimpleName(),
                throwable.getMessage() != null ? throwable.getMessage() : ""
            );
            throw throwable;
        } finally {
            span.end();
            restoreMdcField(TRACE_ID, previousTraceId);
            restoreMdcField(SPAN_ID, previousSpanId);
        }
    }

    private void restoreMdcField(final EcsField field, final String previousValue) {
        if (previousValue != null) {
            EcsFields.tag(field, previousValue);
        } else {
            EcsFields.clear(field);
        }
    }
}
