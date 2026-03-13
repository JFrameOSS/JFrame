package io.github.jframe.tracing.aspect;

import io.github.jframe.autoconfigure.properties.OpenTelemetryProperties;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static io.github.jframe.OpenTelemetryConstants.Attributes.*;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.security.AuthenticationUtil.getAuthenticatedSubject;

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
    public Object traceClass(final ProceedingJoinPoint joinPoint) throws Throwable {
        final String className = joinPoint.getTarget().getClass().getSimpleName();
        final String methodName = joinPoint.getSignature().getName();
        final String spanName = className + "." + methodName;

        if (openTelemetryProperties.getExcludedMethods().contains(methodName.toLowerCase())) {
            return joinPoint.proceed();
        }

        final Span span = tracer.spanBuilder(spanName)
            .setAttribute(SERVICE_NAME, className)
            .setAttribute(SERVICE_METHOD, methodName)
            .setAttribute(HTTP_REMOTE_USER, getAuthenticatedSubject())
            .setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID))
            .setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID))
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.trace("Entering method: {}.{} with span in current scope: {}-{}", className, methodName, span.getSpanContext(), scope);
            return joinPoint.proceed();
        } finally {
            span.end();
        }
    }
}
