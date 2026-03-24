package io.github.jframe.tracing.interceptor;

import io.github.jframe.autoconfigure.OpenTelemetryConfig;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.lang.reflect.Method;
import java.util.Set;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.SERVICE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TracingInterceptor}.
 *
 * <p>Verifies the CDI interceptor OpenTelemetry span lifecycle including:
 * <ul>
 * <li>Span creation with correct name (ClassName.methodName or custom from @Traced)</li>
 * <li>Span attributes: service.name, service.method</li>
 * <li>Return value pass-through</li>
 * <li>Exclusion of getter/setter/well-known methods (get*, set*, is*, toString, hashCode, equals, clone)</li>
 * <li>Exclusion of methods configured in {@link OpenTelemetryConfig#excludedMethods()}</li>
 * <li>Disabled-tracing pass-through</li>
 * <li>Exception enrichment (error=true, error.type, error.message, StatusCode.ERROR) and re-throw</li>
 * <li>Span always ended in finally block</li>
 * <li>Custom span name via non-empty {@code @Traced} value</li>
 * </ul>
 */
@DisplayName("Quarkus OTLP - Tracing Interceptor")
public class TracingInterceptorTest extends UnitTest {

    // ======================== TEST HELPERS ========================

    /**
     * Simple helper class used to obtain real {@link Method} instances for mocking
     * {@link InvocationContext#getMethod()}.
     */
    @SuppressWarnings("unused")
    static class SampleService {

        public String doWork() {
            return "result";
        }

        public String getName() {
            return "name";
        }

        public void setName(final String name) {
            // setter
        }

        public boolean isActive() {
            return true;
        }

        @Override
        public String toString() {
            return "SampleService";
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(final Object obj) {
            return this == obj;
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public String getData() {
            return "data";
        }

        public String health() {
            return "UP";
        }

        public String Health() {
            return "UP";
        }
    }


    @SuppressWarnings("unused")
    static class AnnotatedService {

        public String customOperation() {
            return "custom-result";
        }
    }

    // ======================== FIXTURES ========================

    @Mock
    private Tracer tracer;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private SpanContext spanContext;

    @Mock
    private Scope scope;

    @Mock
    private OpenTelemetryConfig config;

    @InjectMocks
    private TracingInterceptor interceptor;

    @Override
    @BeforeEach
    public void setUp() {

        // Default config: tracing enabled, common excluded methods
        when(config.disabled()).thenReturn(false);
        when(config.excludedMethods()).thenReturn(Set.of("health", "ping"));

        // Default tracer/span chain
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.getSpanContext()).thenReturn(spanContext);
    }

    // ======================== FACTORY METHODS ========================

    private InvocationContext aContextFor(final Object target, final Method method) throws Exception {
        final InvocationContext ctx = mock(InvocationContext.class);
        when(ctx.getTarget()).thenReturn(target);
        when(ctx.getMethod()).thenReturn(method);
        return ctx;
    }

    private InvocationContext aContextFor(final Object target, final Method method, final Object returnValue)
        throws Exception {
        final InvocationContext ctx = aContextFor(target, method);
        when(ctx.proceed()).thenReturn(returnValue);
        return ctx;
    }

    /**
     * Creates a mock {@link InvocationContext} where the method carries a mock
     * {@link io.github.jframe.tracing.Traced} annotation with the given span value.
     * Useful for testing custom span name resolution.
     *
     * @param target      the target object returned by {@link InvocationContext#getTarget()}
     * @param methodName  the method name returned by the mock method
     * @param tracedValue the value returned by {@link io.github.jframe.tracing.Traced#value()}
     * @return a configured mock invocation context
     */
    private InvocationContext aContextWithTracedValue(
        final Object target, final String methodName, final String tracedValue) throws Exception {
        final Method methodMock = mock(Method.class);
        final io.github.jframe.tracing.Traced tracedAnnotation = mock(io.github.jframe.tracing.Traced.class);
        when(tracedAnnotation.value()).thenReturn(tracedValue);
        when(methodMock.getAnnotation(io.github.jframe.tracing.Traced.class)).thenReturn(tracedAnnotation);
        when(methodMock.getName()).thenReturn(methodName);

        final InvocationContext ctx = mock(InvocationContext.class);
        when(ctx.getTarget()).thenReturn(target);
        when(ctx.getMethod()).thenReturn(methodMock);
        when(ctx.proceed()).thenReturn("result");
        return ctx;
    }

    // ======================== AC1: SPAN CREATION ========================

    @Nested
    @DisplayName("AC1 - Span creation with correct name and attributes")
    class SpanCreation {

        @Test
        @DisplayName("Should create span named ClassName.methodName when method is intercepted")
        public void shouldCreateSpanNamedClassNameDotMethodNameWhenMethodIsIntercepted() throws Exception {
            // Given: A SampleService target and its doWork() method
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Span is created with the canonical ClassName.methodName format
            verify(tracer).spanBuilder("SampleService.doWork");
        }

        @Test
        @DisplayName("Should set service.name attribute to class simple name")
        public void shouldSetServiceNameAttributeToClassSimpleName() throws Exception {
            // Given: A SampleService target and its doWork() method
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: service.name attribute is set to the simple class name
            verify(spanBuilder).setAttribute(SERVICE_NAME, "SampleService");
        }

        @Test
        @DisplayName("Should set service.method attribute to method name")
        public void shouldSetServiceMethodAttributeToMethodName() throws Exception {
            // Given: A SampleService target and its doWork() method
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: service.method attribute is set to the method name
            verify(spanBuilder).setAttribute(SERVICE_METHOD, "doWork");
        }

        @Test
        @DisplayName("Should pass through return value from intercepted method")
        public void shouldPassThroughReturnValueFromInterceptedMethod() throws Exception {
            // Given: A SampleService method returning "result"
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            final Object returnValue = interceptor.aroundInvoke(context);

            // Then: Return value is unchanged
            assertThat(returnValue, is("result"));
        }

        @Test
        @DisplayName("Should start and end span when method completes successfully")
        public void shouldStartAndEndSpanWhenMethodCompletesSuccessfully() throws Exception {
            // Given: A SampleService target and its doWork() method
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Span was started and always ended
            verify(spanBuilder).startSpan();
            verify(span).end();
        }
    }

    // ======================== AC4: EXCLUDED PREFIXES / WELL-KNOWN METHODS ========================


    @Nested
    @DisplayName("AC4 - Excluded method prefixes and well-known method names")
    class ExcludedPrefixMethods {

        @Test
        @DisplayName("Should skip span for method with 'get' prefix (getName)")
        public void shouldSkipSpanForMethodWithGetPrefix() throws Exception {
            // Given: A getter method on SampleService
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("getName");
            final InvocationContext context = aContextFor(target, method, "name");

            // When: The interceptor processes the getter
            interceptor.aroundInvoke(context);

            // Then: No span is created — tracer is never consulted
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should skip span for method with 'set' prefix (setName)")
        public void shouldSkipSpanForMethodWithSetPrefix() throws Exception {
            // Given: A setter method on SampleService
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("setName", String.class);
            final InvocationContext context = aContextFor(target, method);
            when(context.proceed()).thenReturn(null);

            // When: The interceptor processes the setter
            interceptor.aroundInvoke(context);

            // Then: No span is created
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should skip span for method with 'is' prefix (isActive)")
        public void shouldSkipSpanForMethodWithIsPrefix() throws Exception {
            // Given: A boolean getter method on SampleService
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("isActive");
            final InvocationContext context = aContextFor(target, method, true);

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: No span is created
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should skip span for toString method")
        public void shouldSkipSpanForToStringMethod() throws Exception {
            // Given: toString method on SampleService
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("toString");
            final InvocationContext context = aContextFor(target, method, "SampleService");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: No span is created
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should skip span for hashCode method")
        public void shouldSkipSpanForHashCodeMethod() throws Exception {
            // Given: hashCode method on SampleService
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("hashCode");
            final InvocationContext context = aContextFor(target, method, 42);

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: No span is created
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should skip span for equals method")
        public void shouldSkipSpanForEqualsMethod() throws Exception {
            // Given: equals method on SampleService
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("equals", Object.class);
            final InvocationContext context = aContextFor(target, method, false);

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: No span is created
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should skip span for method with 'get' prefix even when not a simple getter (getData)")
        public void shouldSkipSpanForGetDataMethodWithGetPrefix() throws Exception {
            // Given: getData method which starts with 'get' but is not a simple accessor
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("getData");
            final InvocationContext context = aContextFor(target, method, "data");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Still excluded because of the 'get' prefix rule
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }
    }

    // ======================== AC5: CONFIG-BASED EXCLUSIONS ========================


    @Nested
    @DisplayName("AC5 - Configured excluded methods")
    class ConfiguredExcludedMethods {

        @Test
        @DisplayName("Should skip span for method name present in config.excludedMethods")
        public void shouldSkipSpanForMethodNamePresentInConfigExcludedMethods() throws Exception {
            // Given: 'health' is in the excluded methods set (lowercase match)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("health");
            final InvocationContext context = aContextFor(target, method, "UP");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: No span is created — method is excluded by config
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should skip span when method name matches exclusion case-insensitively (Health vs health)")
        public void shouldSkipSpanWhenMethodNameMatchesExclusionCaseInsensitively() throws Exception {
            // Given: Method 'Health' (uppercase H) while config has 'health' (lowercase)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("Health");
            final InvocationContext context = aContextFor(target, method, "UP");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: No span is created — case-insensitive match
            verify(tracer, never()).spanBuilder(anyString());
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should create span when config.excludedMethods is empty")
        public void shouldCreateSpanWhenConfigExcludedMethodsIsEmpty() throws Exception {
            // Given: No excluded methods in config
            when(config.excludedMethods()).thenReturn(Set.of());
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Span is created normally
            verify(tracer).spanBuilder("SampleService.doWork");
        }
    }

    // ======================== AC6: DISABLED TRACING ========================


    @Nested
    @DisplayName("AC6 - Disabled tracing")
    class DisabledTracing {

        @Test
        @DisplayName("Should skip span creation and just proceed when config.disabled() is true")
        public void shouldSkipSpanCreationAndJustProceedWhenConfigDisabled() throws Exception {
            // Given: Tracing is globally disabled
            when(config.disabled()).thenReturn(true);
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            final Object returnValue = interceptor.aroundInvoke(context);

            // Then: No span is created — tracer is never consulted
            verify(tracer, never()).spanBuilder(anyString());

            // And: Method is still invoked normally
            verify(context).proceed();
            assertThat(returnValue, is("result"));
        }

        @Test
        @DisplayName("Should not end any span when tracing is disabled")
        public void shouldNotEndAnySpanWhenTracingIsDisabled() throws Exception {
            // Given: Tracing is globally disabled
            when(config.disabled()).thenReturn(true);
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: No span end is called since no span was created
            verify(span, never()).end();
        }
    }

    // ======================== AC7: EXCEPTION HANDLING ========================


    @Nested
    @DisplayName("AC7 - Exception handling and span enrichment on error")
    class ExceptionHandling {

        @Test
        @DisplayName("Should set error=true on span when method throws RuntimeException")
        public void shouldSetErrorTrueOnSpanWhenMethodThrowsRuntimeException() throws Exception {
            // Given: A method that throws a RuntimeException
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method);
            when(context.proceed()).thenThrow(new RuntimeException("something broke"));

            // When: The interceptor processes the failing method
            try {
                interceptor.aroundInvoke(context);
            } catch (final RuntimeException ignored) {
                // expected — exception propagation is tested separately
            }

            // Then: Span is marked with error=true
            verify(span).setAttribute(ERROR, true);
        }

        @Test
        @DisplayName("Should set error.type to exception class name when method throws")
        public void shouldSetErrorTypeToExceptionClassNameWhenMethodThrows() throws Exception {
            // Given: A method that throws an IllegalStateException
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method);
            when(context.proceed()).thenThrow(new IllegalStateException("invalid state"));

            // When: The interceptor processes the failing method
            try {
                interceptor.aroundInvoke(context);
            } catch (final RuntimeException ignored) {
                // expected
            }

            // Then: error.type is the exception's simple class name
            verify(span).setAttribute(ERROR_TYPE, "IllegalStateException");
        }

        @Test
        @DisplayName("Should set error.message to exception message when method throws")
        public void shouldSetErrorMessageToExceptionMessageWhenMethodThrows() throws Exception {
            // Given: A method that throws with a specific message
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method);
            when(context.proceed()).thenThrow(new RuntimeException("something broke"));

            // When: The interceptor processes the failing method
            try {
                interceptor.aroundInvoke(context);
            } catch (final RuntimeException ignored) {
                // expected
            }

            // Then: error.message captures the exception message
            verify(span).setAttribute(ERROR_MESSAGE, "something broke");
        }

        @Test
        @DisplayName("Should set StatusCode.ERROR on span when method throws")
        public void shouldSetStatusCodeErrorOnSpanWhenMethodThrows() throws Exception {
            // Given: A method that throws
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method);
            when(context.proceed()).thenThrow(new RuntimeException("failure"));

            // When: The interceptor processes the failing method
            try {
                interceptor.aroundInvoke(context);
            } catch (final RuntimeException ignored) {
                // expected
            }

            // Then: Span status is set to ERROR
            verify(span).setStatus(StatusCode.ERROR);
        }

        @Test
        @DisplayName("Should re-throw exception unchanged when method throws")
        public void shouldRethrowExceptionUnchangedWhenMethodThrows() throws Exception {
            // Given: A method that throws a specific RuntimeException
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method);
            final RuntimeException originalException = new RuntimeException("original error");
            when(context.proceed()).thenThrow(originalException);

            // When: The interceptor processes the failing method
            RuntimeException thrownException = null;
            try {
                interceptor.aroundInvoke(context);
            } catch (final RuntimeException e) {
                thrownException = e;
            }

            // Then: The exact same exception instance is re-thrown
            assertThat(thrownException, is(notNullValue()));
            assertThat(thrownException, is(originalException));
            assertThat(thrownException.getMessage(), is("original error"));
        }

        @Test
        @DisplayName("Should end span even when method throws (finally block)")
        public void shouldEndSpanEvenWhenMethodThrows() throws Exception {
            // Given: A method that throws during execution
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method);
            when(context.proceed()).thenThrow(new RuntimeException("crash"));

            // When: The interceptor processes the failing method
            try {
                interceptor.aroundInvoke(context);
            } catch (final RuntimeException ignored) {
                // expected
            }

            // Then: Span is always ended regardless of exception
            verify(span).end();
        }

        @Test
        @DisplayName("Should handle exception with null message without NPE")
        public void shouldHandleExceptionWithNullMessageWithoutNpe() throws Exception {
            // Given: A method that throws a RuntimeException with no message (null)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method);
            when(context.proceed()).thenThrow(new RuntimeException((String) null));

            // When: The interceptor processes the failing method — should not throw NPE
            try {
                interceptor.aroundInvoke(context);
            } catch (final RuntimeException ignored) {
                // expected
            }

            // Then: Span error attributes are set (message may be null or empty, but no NPE)
            verify(span).setAttribute(ERROR, true);
            verify(span).setStatus(StatusCode.ERROR);
        }
    }

    // ======================== AC8: NESTED SPANS / PARENT CONTEXT ========================


    @Nested
    @DisplayName("AC8 - Nested spans use current span as parent context")
    class NestedSpans {

        @Test
        @DisplayName("Should make created span current so nested spans attach to it as children")
        public void shouldMakeCreatedSpanCurrentSoNestedSpansAttachToItAsChildren() throws Exception {
            // Given: A SampleService target and its doWork() method
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The span is made current (pushed onto the context stack) during method execution
            verify(span).makeCurrent();
        }

        @Test
        @DisplayName("Should close scope after method execution to restore parent context")
        public void shouldCloseScopeAfterMethodExecutionToRestoreParentContext() throws Exception {
            // Given: A SampleService target and a scope that tracks closure
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Scope is closed to restore parent context (try-with-resources or explicit close)
            verify(scope).close();
        }
    }

    // ======================== CUSTOM SPAN NAME ========================


    @Nested
    @DisplayName("Custom span name via @Traced annotation value")
    class CustomSpanName {

        @Test
        @DisplayName("Should use custom span name from @Traced annotation value when non-empty")
        public void shouldUseCustomSpanNameFromTracedAnnotationValueWhenNonEmpty() throws Exception {
            // Given: A method with @Traced("custom-span-name") annotation
            final AnnotatedService target = new AnnotatedService();
            final InvocationContext context = aContextWithTracedValue(target, "customOperation", "custom-span-name");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Span is created with the custom name, not ClassName.methodName
            verify(tracer).spanBuilder("custom-span-name");
        }

        @Test
        @DisplayName("Should use default ClassName.methodName when @Traced annotation has empty value")
        public void shouldUseDefaultSpanNameWhenTracedAnnotationHasEmptyValue() throws Exception {
            // Given: A method with @Traced("") (empty value) annotation
            final AnnotatedService target = new AnnotatedService();
            final InvocationContext context = aContextWithTracedValue(target, "customOperation", "");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Span falls back to default ClassName.methodName format
            verify(tracer).spanBuilder("AnnotatedService.customOperation");
        }

        @Test
        @DisplayName("Should use default ClassName.methodName when @Traced annotation is absent on method")
        public void shouldUseDefaultSpanNameWhenTracedAnnotationIsAbsentOnMethod() throws Exception {
            // Given: A method with no @Traced annotation (returns null from getAnnotation)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Span uses the default ClassName.methodName format
            verify(tracer).spanBuilder("SampleService.doWork");
        }
    }

    // ======================== INTEGRATION-STYLE SCENARIOS ========================


    @Nested
    @DisplayName("Method proceed integration")
    class MethodProceedIntegration {

        @Test
        @DisplayName("Should call context.proceed() exactly once during normal execution")
        public void shouldCallContextProceedExactlyOnceDuringNormalExecution() throws Exception {
            // Given: A SampleService target and its doWork() method
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The underlying method is called exactly once
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should call context.proceed() exactly once for excluded methods")
        public void shouldCallContextProceedExactlyOnceForExcludedMethods() throws Exception {
            // Given: A getter method on SampleService (excluded by prefix rule)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("getName");
            final InvocationContext context = aContextFor(target, method, "name");

            // When: The interceptor processes the excluded method
            interceptor.aroundInvoke(context);

            // Then: Method is still invoked — just without a span
            verify(context).proceed();
        }

        @Test
        @DisplayName("Should return null when intercepted method returns null")
        public void shouldReturnNullWhenInterceptedMethodReturnsNull() throws Exception {
            // Given: A SampleService method returning null
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, null);

            // When: The interceptor processes the method
            final Object returnValue = interceptor.aroundInvoke(context);

            // Then: Null is propagated without any NullPointerException
            assertThat(returnValue, is((Object) null));
        }
    }
}
