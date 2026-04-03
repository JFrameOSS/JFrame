package io.github.jframe.tracing;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link SpanNamingUtil}.
 *
 * <p>Verifies class name resolution (proxy stripping) and span name resolution.
 */
@DisplayName("Tracing - SpanNamingUtil")
class SpanNamingUtilTest extends UnitTest {

    @Nested
    @DisplayName("resolveClassName")
    class ResolveClassName {

        @Test
        @DisplayName("Should return plain name unchanged when no proxy suffix present")
        void shouldReturnPlainNameWhenNoSuffix() {
            // Given: a plain class name
            final String simpleClassName = "OrderService";

            // When: resolving
            final String result = SpanNamingUtil.resolveClassName(simpleClassName);

            // Then: name is unchanged
            assertThat(result, is("OrderService"));
        }

        @Test
        @DisplayName("Should strip Quarkus _Subclass suffix")
        void shouldStripSubclassSuffixWhenQuarkusCdiProxy() {
            // Given: a Quarkus CDI proxy class name
            final String simpleClassName = "OrderService_Subclass";

            // When: resolving
            final String result = SpanNamingUtil.resolveClassName(simpleClassName);

            // Then: suffix is removed
            assertThat(result, is("OrderService"));
        }

        @Test
        @DisplayName("Should strip Spring CGLIB $$ suffix")
        void shouldStripDollarSuffixWhenSpringCglibProxy() {
            // Given: a Spring CGLIB proxy class name
            final String simpleClassName = "OrderService$EnhancerBySpringCGLIB$abc123";

            // When: resolving
            final String result = SpanNamingUtil.resolveClassName(simpleClassName);

            // Then: everything from $ onward is removed
            assertThat(result, is("OrderService"));
        }

        @Test
        @DisplayName("Should strip both _Subclass and $ suffix when both present")
        void shouldStripBothSuffixesWhenBothPresent() {
            // Given: a name with both suffixes (edge case)
            final String simpleClassName = "OrderService_Subclass$Proxy";

            // When: resolving
            final String result = SpanNamingUtil.resolveClassName(simpleClassName);

            // Then: both suffixes removed, original name remains
            assertThat(result, is("OrderService"));
        }

        @Test
        @DisplayName("Should handle dollar sign at start by keeping full name when dollarIndex is 0")
        void shouldKeepFullNameWhenDollarIndexIsZero() {
            // Given: a name where $ is at index 0 (dollarIndex not > 0, so no stripping)
            final String simpleClassName = "$ProxyName";

            // When: resolving
            final String result = SpanNamingUtil.resolveClassName(simpleClassName);

            // Then: no stripping since dollarIndex is 0 (not > 0)
            assertThat(result, is("$ProxyName"));
        }
    }


    @Nested
    @DisplayName("resolveSpanName")
    class ResolveSpanName {

        @Test
        @DisplayName("Should return custom name when custom name is non-empty")
        void shouldReturnCustomNameWhenProvided() {
            // Given: a custom span name
            final String className = "OrderService";
            final String methodName = "process";
            final String customName = "my-custom-span";

            // When: resolving
            final String result = SpanNamingUtil.resolveSpanName(className, methodName, customName);

            // Then: custom name is returned
            assertThat(result, is("my-custom-span"));
        }

        @Test
        @DisplayName("Should return ClassName.methodName when custom name is null")
        void shouldReturnClassDotMethodWhenCustomNameIsNull() {
            // Given: null custom name
            final String className = "OrderService";
            final String methodName = "process";

            // When: resolving
            final String result = SpanNamingUtil.resolveSpanName(className, methodName, null);

            // Then: class.method format is returned
            assertThat(result, is("OrderService.process"));
        }

        @Test
        @DisplayName("Should return ClassName.methodName when custom name is empty")
        void shouldReturnClassDotMethodWhenCustomNameIsEmpty() {
            // Given: empty custom name
            final String className = "OrderService";
            final String methodName = "process";
            final String customName = "";

            // When: resolving
            final String result = SpanNamingUtil.resolveSpanName(className, methodName, customName);

            // Then: class.method format is returned
            assertThat(result, is("OrderService.process"));
        }

        @Test
        @DisplayName("Should return ClassName.methodName for normal class and method")
        void shouldReturnClassDotMethodWhenBothProvided() {
            // Given: class and method names
            final String className = "PaymentService";
            final String methodName = "executePayment";

            // When: resolving with no custom name
            final String result = SpanNamingUtil.resolveSpanName(className, methodName, null);

            // Then: properly formatted span name
            assertThat(result, is("PaymentService.executePayment"));
        }
    }
}
