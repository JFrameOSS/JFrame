package io.github.jframe.tracing.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding annotation that marks methods or classes for OpenTelemetry tracing.
 *
 * <p>Methods or classes annotated with {@code @Traced} will have a new OTEL span created and
 * closed around each invocation by {@link TracingInterceptor}.
 */
@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
    {
        ElementType.TYPE,
        ElementType.METHOD
    }
)
public @interface Traced {
}
