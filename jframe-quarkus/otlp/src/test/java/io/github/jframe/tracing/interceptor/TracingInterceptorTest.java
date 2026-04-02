package io.github.jframe.tracing.interceptor;

import io.github.jframe.autoconfigure.OpenTelemetryConfig;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;
import io.github.jframe.security.QuarkusAuthenticationUtil;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
 * <li>Exclusion of getter/setter/well-known methods (toString, hashCode, equals, clone)</li>
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

    @Mock
    private QuarkusAuthenticationUtil authenticationUtil;

    @InjectMocks
    private TracingInterceptor interceptor;

    @Override
    @BeforeEach
    public void setUp() {

        // Default config: tracing enabled, common excluded methods
        when(config.disabled()).thenReturn(false);
        when(config.excludedMethods()).thenReturn(Set.of("health", "ping"));

        // Default Tracer → SpanBuilder → Span chain
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.getSpanContext()).thenReturn(spanContext);

        // Default auth: anonymous (no user authenticated)
        EcsFields.tag(USER_NAME, AuthenticationConstants.ANONYMOUS);
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
            verify(spanBuilder).setAttribute(SPAN_SERVICE_NAME.getKey(), "SampleService");
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
            verify(spanBuilder).setAttribute(SPAN_SERVICE_METHOD.getKey(), "doWork");
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

    // ======================== AC4: EXCLUDED WELL-KNOWN METHODS ========================


    @Nested
    @DisplayName("AC4 - Excluded well-known method names")
    class ExcludedWellKnownMethods {

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
        @DisplayName("Should create span for method starting with get (prefix exclusion removed)")
        public void shouldCreateSpanForMethodWithGetPrefix() throws Exception {
            // Given: No excluded methods in config — getName starts with 'get' but prefix exclusion is gone
            when(config.excludedMethods()).thenReturn(Set.of());
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("getName");
            final InvocationContext context = aContextFor(target, method, "name");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: Span IS created — prefix-based exclusion no longer exists
            verify(tracer).spanBuilder("SampleService.getName");
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
        @DisplayName("Should record exception and set error on span when method throws")
        public void shouldRecordExceptionAndSetErrorOnSpanWhenMethodThrows() throws Exception {
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

            // Then: Span records the exception via OTEL API
            verify(span).recordException(any(RuntimeException.class));
        }

        @Test
        @DisplayName("Should record exception on span when method throws IllegalStateException")
        public void shouldRecordExceptionWhenMethodThrowsIllegalState() throws Exception {
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

            // Then: Span records the exception via OTEL API
            verify(span).recordException(any(IllegalStateException.class));
        }

        @Test
        @DisplayName("Should record exception on span when method throws RuntimeException")
        public void shouldRecordExceptionWhenMethodThrowsRuntimeException() throws Exception {
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

            // Then: Span records the exception via OTEL API
            verify(span).recordException(any(RuntimeException.class));
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
        @DisplayName("Should record exception with null message without NPE")
        public void shouldRecordExceptionWithNullMessageWithoutNpe() throws Exception {
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

            // Then: Span records the exception and sets ERROR status even with null message
            verify(span).recordException(any(RuntimeException.class));
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
            // Given: A well-known method on SampleService (excluded by EXCLUDED_NAMES)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("toString");
            final InvocationContext context = aContextFor(target, method, "SampleService");

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

    // ======================== AC_ENRICHMENT: SPAN ENRICHMENT ========================


    @Nested
    @DisplayName("AC_ENRICHMENT - Span enrichment with authenticated user and correlation IDs")
    class SpanEnrichment {

        @AfterEach
        public void clearMdc() {
            // Clean MDC entries written by the test to avoid cross-test pollution
            EcsFields.clear();
        }

        @Test
        @DisplayName("Should set http.remote_user attribute on span when authenticated user is present")
        public void shouldSetHttpRemoteUserAttributeOnSpanWhenAuthenticatedUserIsPresent() throws Exception {
            // Given: The authentication utility reports an authenticated user
            EcsFields.tag(USER_NAME, "john.doe");
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The span receives the authenticated principal name as http.remote_user
            verify(span).setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), "john.doe");
        }

        @Test
        @DisplayName("Should set http.remote_user to anonymous message when no authentication exists")
        public void shouldSetHttpRemoteUserToAnonymousMessageWhenNoAuthenticationExists() throws Exception {
            // Given: The authentication utility reports an anonymous caller (default setUp stub)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The span receives the anonymous fallback message as http.remote_user
            verify(span).setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), "ANONYMOUS - NO AUTHENTICATION");
        }

        @Test
        @DisplayName("Should set http.transaction_id attribute when transaction ID is present in MDC")
        public void shouldSetHttpTransactionIdAttributeWhenTransactionIdIsPresentInMdc() throws Exception {
            // Given: A transaction ID is stored in the MDC by a filter earlier in the call chain
            EcsFields.tag(TX_ID, "tx-123");
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The span receives the transaction ID as http.transaction_id
            verify(span).setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), "tx-123");
        }

        @Test
        @DisplayName("Should set http.request_id attribute when request ID is present in MDC")
        public void shouldSetHttpRequestIdAttributeWhenRequestIdIsPresentInMdc() throws Exception {
            // Given: A request ID is stored in the MDC by a filter earlier in the call chain
            EcsFields.tag(REQUEST_ID, "req-456");
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The span receives the request ID as http.request_id
            verify(span).setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), "req-456");
        }

        @Test
        @DisplayName("Should not set http.transaction_id when MDC value is null (e.g. scheduled tasks)")
        public void shouldNotSetHttpTransactionIdWhenMdcValueIsNull() throws Exception {
            // Given: No transaction ID has been set in the MDC (e.g. a background scheduled task)
            // (MDC is clean — no EcsFields.tag call for TX_ID)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The span does not receive an http.transaction_id attribute
            verify(span, never()).setAttribute(eq(SPAN_HTTP_TRANSACTION_ID.getKey()), anyString());
        }

        @Test
        @DisplayName("Should not set http.request_id when MDC value is null")
        public void shouldNotSetHttpRequestIdWhenMdcValueIsNull() throws Exception {
            // Given: No request ID has been set in the MDC
            // (MDC is clean — no EcsFields.tag call for REQUEST_ID)
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: The span does not receive an http.request_id attribute
            verify(span, never()).setAttribute(eq(SPAN_HTTP_REQUEST_ID.getKey()), anyString());
        }

        @Test
        @DisplayName("Should set all enrichment attributes when both MDC values and auth are present")
        public void shouldSetAllEnrichmentAttributesWhenBothMdcValuesAndAuthArePresent() throws Exception {
            // Given: A fully identified request — authenticated user and both correlation IDs in MDC
            EcsFields.tag(USER_NAME, "jane.smith");
            EcsFields.tag(TX_ID, "tx-999");
            EcsFields.tag(REQUEST_ID, "req-888");
            final SampleService target = new SampleService();
            final Method method = SampleService.class.getMethod("doWork");
            final InvocationContext context = aContextFor(target, method, "result");

            // When: The interceptor processes the method
            interceptor.aroundInvoke(context);

            // Then: All three enrichment attributes are set on the span
            verify(span).setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), "jane.smith");
            verify(span).setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), "tx-999");
            verify(span).setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), "req-888");
        }
    }
}
