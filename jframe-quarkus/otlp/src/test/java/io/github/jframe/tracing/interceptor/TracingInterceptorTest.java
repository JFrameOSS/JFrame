package io.github.jframe.tracing.interceptor;

import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;

import jakarta.enterprise.inject.Instance;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TracingInterceptor}.
 *
 * <p>Verifies the CDI {@code @Interceptor @Traced} functionality including:
 * <ul>
 * <li>Creating an OTEL span for annotated methods</li>
 * <li>Starting and ending the span around the method invocation</li>
 * <li>Propagating the return value from the intercepted method</li>
 * <li>Ending the span even when the intercepted method throws</li>
 * </ul>
 */
@DisplayName("Quarkus OTLP - Tracing Interceptor")
public class TracingInterceptorTest extends UnitTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Instance<Tracer> tracerInstance;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private SpanContext spanContext;

    private TracingInterceptor interceptor;

    @Override
    @BeforeEach
    public void setUp() {
        when(tracerInstance.isResolvable()).thenReturn(true);
        when(tracerInstance.get()).thenReturn(tracer);
        interceptor = new TracingInterceptor(tracerInstance);
        setupSpanBuilderMocks();
    }

    @Test
    @DisplayName("Should create a span when intercepting an annotated method")
    public void shouldCreateSpanWhenInterceptingAnnotatedMethod() throws Exception {
        // Given: An InvocationContext for a method named "processOrder"
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("processOrder");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        when(context.proceed()).thenReturn("result");

        // When: The interceptor processes the method
        interceptor.aroundInvoke(context);

        // Then: A span is created with the method name
        verify(tracer).spanBuilder(anyString());
        verify(spanBuilder).startSpan();
    }

    @Test
    @DisplayName("Should end the span after method completes successfully")
    public void shouldEndSpanAfterMethodCompletesSuccessfully() throws Exception {
        // Given: An InvocationContext for a successful method
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("processOrder");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        when(context.proceed()).thenReturn("result");

        // When: The interceptor processes the method
        interceptor.aroundInvoke(context);

        // Then: Span is ended
        verify(span).end();
    }

    @Test
    @DisplayName("Should return the value from the intercepted method")
    public void shouldReturnValueFromInterceptedMethod() throws Exception {
        // Given: An InvocationContext that returns "expectedResult"
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("processOrder");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        when(context.proceed()).thenReturn("expectedResult");

        // When: The interceptor processes the method
        final Object result = interceptor.aroundInvoke(context);

        // Then: The original return value is propagated
        assertThat(result, is(notNullValue()));
        assertThat(result, is("expectedResult"));
    }

    @Test
    @DisplayName("Should end span even when method throws an exception")
    public void shouldEndSpanEvenWhenMethodThrowsException() throws Exception {
        // Given: An InvocationContext for a method that throws
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("processOrder");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        when(context.proceed()).thenThrow(new RuntimeException("Service failed"));

        // When: The interceptor processes the failing method
        try {
            interceptor.aroundInvoke(context);
        } catch (final RuntimeException ignored) {
            // Expected
        }

        // Then: Span is ended regardless
        verify(span).end();
    }

    @Test
    @DisplayName("Should propagate exception from intercepted method")
    public void shouldPropagateExceptionFromInterceptedMethod() throws Exception {
        // Given: An InvocationContext that throws a specific exception
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("processOrder");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        final RuntimeException expectedException = new RuntimeException("Business error");
        when(context.proceed()).thenThrow(expectedException);

        // When & Then: Exception is propagated
        try {
            interceptor.aroundInvoke(context);
            throw new AssertionError("Expected exception was not thrown");
        } catch (final RuntimeException e) {
            assertThat(e.getMessage(), is("Business error"));
        }
    }

    private void setupSpanBuilderMocks() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(spanContext.getSpanId()).thenReturn(TEST_SPAN_ID);
    }

    /** Minimal service class used as interception target. */
    public static class SampleService {

        public String processOrder() {
            return "processed";
        }
    }
}
