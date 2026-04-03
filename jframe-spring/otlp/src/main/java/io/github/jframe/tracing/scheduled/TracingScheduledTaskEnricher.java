package io.github.jframe.tracing.scheduled;

import io.github.jframe.logging.ecs.EcsField;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.scheduled.ScheduledTaskEnricher;
import io.github.jframe.tracing.SpanNamingUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static io.github.jframe.security.AuthenticationUtil.getAuthenticatedSubject;

/**
 * Enricher that creates an OpenTelemetry span for {@code @Scheduled} method executions.
 *
 * <p>Activated only when OpenTelemetry is enabled ({@code otel.sdk.disabled=false}).
 * Sets span attributes for service name, method, transaction/request IDs, and remote user.
 * Tags MDC with the span's trace ID and span ID, restoring previous values on close.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "otel.sdk.disabled",
    havingValue = "false"
)
public class TracingScheduledTaskEnricher implements ScheduledTaskEnricher {

    private final Tracer tracer;

    @Override
    public AutoCloseable enrich(final ProceedingJoinPoint pjp) {
        try {
            final String className = SpanNamingUtil.resolveClassName(pjp.getTarget().getClass().getSimpleName());
            final String methodName = pjp.getSignature().getName();
            final String spanName = SpanNamingUtil.resolveSpanName(className, methodName, null);

            final SpanBuilder builder = tracer.spanBuilder(spanName)
                .setAttribute(SPAN_SERVICE_NAME.getKey(), className)
                .setAttribute(SPAN_SERVICE_METHOD.getKey(), methodName)
                .setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), getAuthenticatedSubject());

            final String txId = EcsFields.get(TX_ID);
            if (txId != null) {
                builder.setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), txId);
            }
            final String requestId = EcsFields.get(REQUEST_ID);
            if (requestId != null) {
                builder.setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), requestId);
            }

            final Span span = builder.startSpan();

            final Scope scope = span.makeCurrent();

            final String previousTraceId = EcsFields.get(TRACE_ID);
            final String previousSpanId = EcsFields.get(SPAN_ID);

            EcsFields.tag(TRACE_ID, span.getSpanContext().getTraceId());
            EcsFields.tag(SPAN_ID, span.getSpanContext().getSpanId());

            return () -> {
                span.end();
                scope.close();
                restoreMdcField(TRACE_ID, previousTraceId);
                restoreMdcField(SPAN_ID, previousSpanId);
            };
        } catch (final Exception exception) {
            log.warn("Failed to create tracing span for scheduled task: {}", exception.getMessage());
            return () -> {};
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
