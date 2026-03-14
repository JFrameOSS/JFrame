package io.github.jframe.tracing.interceptor;

import io.github.support.UnitTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.interceptor.InterceptorBinding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link LogExecutionTime} interceptor binding annotation.
 *
 * <p>Verifies that the annotation has the correct retention, targets, and
 * is marked as a CDI interceptor binding.
 */
@DisplayName("Quarkus OTLP - LogExecutionTime Annotation")
public class LogExecutionTimeAnnotationTest extends UnitTest {

    @Test
    @DisplayName("Should have RUNTIME retention")
    public void shouldHaveRuntimeRetention() {
        // Given: The LogExecutionTime annotation class

        // When: Inspecting its Retention meta-annotation
        final Retention retention = LogExecutionTime.class.getAnnotation(Retention.class);

        // Then: Retention is RUNTIME
        assertThat(retention, is(notNullValue()));
        assertThat(retention.value(), is(RetentionPolicy.RUNTIME));
    }

    @Test
    @DisplayName("Should be applicable to TYPE and METHOD targets")
    public void shouldBeApplicableToTypeAndMethodTargets() {
        // Given: The LogExecutionTime annotation class

        // When: Inspecting its Target meta-annotation
        final Target target = LogExecutionTime.class.getAnnotation(Target.class);

        // Then: Target includes TYPE and METHOD
        assertThat(target, is(notNullValue()));
        boolean hasType = false;
        boolean hasMethod = false;
        for (final ElementType elementType : target.value()) {
            if (elementType == ElementType.TYPE)
                hasType = true;
            if (elementType == ElementType.METHOD)
                hasMethod = true;
        }
        assertThat(hasType, is(true));
        assertThat(hasMethod, is(true));
    }

    @Test
    @DisplayName("Should be annotated with @InterceptorBinding")
    public void shouldBeAnnotatedWithInterceptorBinding() {
        // Given: The LogExecutionTime annotation class

        // When: Checking for @InterceptorBinding meta-annotation
        final InterceptorBinding interceptorBinding = LogExecutionTime.class.getAnnotation(InterceptorBinding.class);

        // Then: @InterceptorBinding is present
        assertThat(interceptorBinding, is(notNullValue()));
    }
}
