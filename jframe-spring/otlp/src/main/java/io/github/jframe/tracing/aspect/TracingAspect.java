package io.github.jframe.tracing.aspect;

import io.github.jframe.autoconfigure.properties.OpenTelemetryProperties;
import io.github.jframe.logging.kibana.KibanaLogFields;
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

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.security.AuthenticationUtil.getAuthenticatedSubject;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_NAME;

/**
 * Aspect for tracing method execution using Micrometer Tracing. This aspect will create a span for each method annotated with @Traced.
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
            .setAttribute(SERVICE_NAME, className)
            .setAttribute(SERVICE_METHOD, methodName)
            .setAttribute(HTTP_REMOTE_USER, getAuthenticatedSubject())
            .setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID))
            .setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID))
            .startSpan();

        final long startTime = System.nanoTime();
        try (Scope scope = span.makeCurrent()) {
            log.debug(
                "[jframe-otlp] Entering {} | user={} traceId={} spanId={}",
                spanName,
                getAuthenticatedSubject(),
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId()
            );
            final Object result = joinPoint.proceed();
            final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("[jframe-otlp] Completed {} in {}ms", spanName, durationMs);
            return result;
        } catch (final Throwable throwable) {
            final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            span.setAttribute(ERROR, true);
            span.setAttribute(ERROR_TYPE, throwable.getClass().getSimpleName());
            span.setAttribute(ERROR_MESSAGE, throwable.getMessage() != null ? throwable.getMessage() : "");
            span.setStatus(StatusCode.ERROR);
            log.error(
                "[jframe-otlp] Failed {} in {}ms | error={} message={}",
                spanName,
                durationMs,
                throwable.getClass().getSimpleName(),
                throwable.getMessage() != null ? throwable.getMessage() : ""
            );
            throw throwable;
        } finally {
            span.end();
        }
    }
}
