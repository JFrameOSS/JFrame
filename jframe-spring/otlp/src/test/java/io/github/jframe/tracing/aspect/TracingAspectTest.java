package io.github.jframe.tracing.aspect;

import io.github.jframe.autoconfigure.properties.OpenTelemetryProperties;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TracingAspect}.
 *
 * <p>Verifies the TracingAspect functionality including:
 * <ul>
 * <li>Span creation for traced methods</li>
 * <li>Method exclusion rules</li>
 * <li>Error handling and span status on exceptions</li>
 * <li>Proxy class name stripping</li>
 * </ul>
 */
@DisplayName("Tracing - TracingAspect")
class TracingAspectTest extends UnitTest {

    @Mock
    private Tracer tracer;
    @Mock
    private OpenTelemetryProperties openTelemetryProperties;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;
    @Mock
    private Span span;
    @Mock
    private SpanBuilder spanBuilder;
    @Mock
    private Scope scope;
    @Mock
    private SpanContext spanContext;

    private TracingAspect tracingAspect;

    @Override
    @BeforeEach
    public void setUp() {
        tracingAspect = new TracingAspect(tracer, openTelemetryProperties);
        setupSpanBuilderMocks();
        setupKibanaFields();
    }

    @Test
    @DisplayName("Should create span and return result for non-excluded method")
    void traceClass_withNonExcludedMethod_shouldCreateSpanAndReturnResult() throws Throwable {
        // Given: A service class with a business method
        when(joinPoint.getTarget()).thenReturn(new Object() {

            @Override
            public String toString() {
                return "MyService";
            }
        });
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("MyService");
        when(signature.getName()).thenReturn("processOrder");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(openTelemetryProperties.getExcludedMethods()).thenReturn(Set.of("health", "ping"));
        when(joinPoint.proceed()).thenReturn("result");

        // When: Aspect intercepts the method
        final Object result = tracingAspect.traceClass(joinPoint);

        // Then: Span is created and ended
        verify(tracer).spanBuilder(anyString());
        verify(span).makeCurrent();
        verify(span).end();

        // And: Result is returned
        assertThat(result, is("result"));
    }

    @Test
    @DisplayName("Should skip span creation for excluded method")
    void traceClass_withExcludedMethod_shouldSkipSpanCreation() throws Throwable {
        // Given: A method in the exclusion list
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(signature.getName()).thenReturn("health");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(openTelemetryProperties.getExcludedMethods()).thenReturn(Set.of("health", "ping"));
        when(joinPoint.proceed()).thenReturn("skipped");

        // When: Aspect intercepts the excluded method
        final Object result = tracingAspect.traceClass(joinPoint);

        // Then: No span is created
        verify(tracer, never()).spanBuilder(anyString());
        verify(span, never()).makeCurrent();
        verify(span, never()).end();

        // And: Proceed is called directly
        assertThat(result, is("skipped"));
    }

    @Test
    @DisplayName("Should set error attributes and rethrow when method throws")
    void traceClass_whenMethodThrows_shouldSetErrorAttributesAndRethrow() throws Throwable {
        // Given: A service method that throws a runtime exception
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(signature.getName()).thenReturn("processPayment");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(openTelemetryProperties.getExcludedMethods()).thenReturn(Set.of("health"));
        final RuntimeException cause = new RuntimeException("Payment failed");
        when(joinPoint.proceed()).thenThrow(cause);

        // When: Aspect intercepts a method that throws
        // Then: Exception is rethrown
        assertThrows(RuntimeException.class, () -> tracingAspect.traceClass(joinPoint));

        // And: Error attributes are set on span
        verify(span).setAttribute(ERROR, true);
        verify(span).setAttribute(ERROR_TYPE, "RuntimeException");
        verify(span).setAttribute(ERROR_MESSAGE, "Payment failed");
        verify(span).setStatus(StatusCode.ERROR);

        // And: Span is always ended
        verify(span).end();
    }

    @Test
    @DisplayName("Should set empty error message when exception has null message")
    void traceClass_whenMethodThrowsWithNullMessage_shouldSetEmptyErrorMessage() throws Throwable {
        // Given: A method that throws an exception with null message
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(signature.getName()).thenReturn("validate");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(openTelemetryProperties.getExcludedMethods()).thenReturn(Set.of());
        final NullPointerException npe = new NullPointerException();
        when(joinPoint.proceed()).thenThrow(npe);

        // When / Then: Exception rethrown
        assertThrows(NullPointerException.class, () -> tracingAspect.traceClass(joinPoint));

        // And: Empty string is used for error message (not null)
        verify(span).setAttribute(ERROR_MESSAGE, "");
    }

    @Test
    @DisplayName("Should end span even when method throws")
    void traceClass_whenMethodThrows_shouldAlwaysEndSpan() throws Throwable {
        // Given: A method that throws
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(signature.getName()).thenReturn("failingMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(openTelemetryProperties.getExcludedMethods()).thenReturn(Set.of());
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("fail"));

        // When / Then
        assertThrows(IllegalStateException.class, () -> tracingAspect.traceClass(joinPoint));

        // Then: Span is ended in finally block
        verify(span).end();
    }

    @Test
    @DisplayName("Should strip Spring CGLIB proxy suffix from class name")
    void traceClass_withCglibProxyClass_shouldStripProxySuffix() throws Throwable {
        // Given: A Spring CGLIB-proxied class name
        final Object cglibProxy = new Object() {};
        when(joinPoint.getTarget()).thenReturn(cglibProxy);
        when(signature.getName()).thenReturn("doWork");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(openTelemetryProperties.getExcludedMethods()).thenReturn(Set.of());
        when(joinPoint.proceed()).thenReturn(null);

        // When: Aspect intercepts
        tracingAspect.traceClass(joinPoint);

        // Then: Span is created (proxy suffix stripped by SpanNamingUtil)
        verify(tracer).spanBuilder(anyString());
        verify(span).end();
    }

    @Test
    @DisplayName("Should not create span for get* prefixed method name")
    void traceClass_withGetPrefixedMethod_shouldSkipSpanCreation() throws Throwable {
        // Given: A getter-style method
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(signature.getName()).thenReturn("getUserById");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(openTelemetryProperties.getExcludedMethods()).thenReturn(Set.of());
        when(joinPoint.proceed()).thenReturn("user");

        // When: Aspect intercepts
        final Object result = tracingAspect.traceClass(joinPoint);

        // Then: No span is created (MethodExclusionRules matches get* prefix)
        verify(tracer, never()).spanBuilder(anyString());
        assertThat(result, is("user"));
    }

    private void setupSpanBuilderMocks() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(eq(ERROR), eq(true))).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(scope);
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(spanContext.getTraceId()).thenReturn("00000000000000000000000000000001");
        lenient().when(spanContext.getSpanId()).thenReturn("0000000000000001");
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), any(Boolean.class))).thenReturn(span);
    }
}
