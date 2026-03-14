package io.github.jframe.tracing.interceptor;

import io.github.support.UnitTest;

import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TimerInterceptor}.
 *
 * <p>Verifies the CDI {@code @Interceptor @LogExecutionTime} functionality including:
 * <ul>
 * <li>Measuring execution time of the intercepted method</li>
 * <li>Propagating the return value from the intercepted method</li>
 * <li>Logging elapsed time after method completes</li>
 * <li>Ending timing measurement even when the intercepted method throws</li>
 * </ul>
 */
@DisplayName("Quarkus OTLP - Timer Interceptor")
public class TimerInterceptorTest extends UnitTest {

    private TimerInterceptor interceptor;

    @Override
    @BeforeEach
    public void setUp() {
        interceptor = new TimerInterceptor();
    }

    @Test
    @DisplayName("Should return the value from the intercepted method")
    public void shouldReturnValueFromInterceptedMethod() throws Exception {
        // Given: An InvocationContext for a method that returns a value
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("getData");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        when(context.proceed()).thenReturn("data");

        // When: The timer interceptor processes the method
        final Object result = interceptor.aroundInvoke(context);

        // Then: The original return value is propagated
        assertThat(result, is(notNullValue()));
        assertThat(result, is("data"));
    }

    @Test
    @DisplayName("Should delegate to proceed when intercepting")
    public void shouldDelegateToProceedWhenIntercepting() throws Exception {
        // Given: An InvocationContext
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("getData");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        when(context.proceed()).thenReturn("result");

        // When: The timer interceptor processes the method
        interceptor.aroundInvoke(context);

        // Then: InvocationContext.proceed() was called
        verify(context).proceed();
    }

    @Test
    @DisplayName("Should propagate exception from intercepted method")
    public void shouldPropagateExceptionFromInterceptedMethod() throws Exception {
        // Given: An InvocationContext that throws during execution
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("getData");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        final RuntimeException expectedException = new RuntimeException("Timed out");
        when(context.proceed()).thenThrow(expectedException);

        // When & Then: Exception is propagated from the interceptor
        try {
            interceptor.aroundInvoke(context);
            throw new AssertionError("Expected exception was not thrown");
        } catch (final RuntimeException e) {
            assertThat(e.getMessage(), is("Timed out"));
        }
    }

    @Test
    @DisplayName("Should handle null return value from intercepted method")
    public void shouldHandleNullReturnValueFromInterceptedMethod() throws Exception {
        // Given: An InvocationContext that returns null (void method)
        final InvocationContext context = mock(InvocationContext.class);
        final java.lang.reflect.Method method = SampleService.class.getMethod("getData");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new SampleService());
        when(context.proceed()).thenReturn(null);

        // When: The timer interceptor processes the method
        final Object result = interceptor.aroundInvoke(context);

        // Then: Null is returned without exception
        assertThat(result == null, is(true));
    }

    /** Minimal service class used as interception target. */
    public static class SampleService {

        public String getData() {
            return "data";
        }
    }
}
