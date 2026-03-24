package io.github.jframe.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding for OpenTelemetry span creation.
 *
 * <p>When applied to a method or class, the
 * {@link io.github.jframe.tracing.interceptor.TracingInterceptor} wraps each invocation
 * in an OpenTelemetry span. An optional {@link #value()} overrides the default
 * {@code ClassName.methodName} span name.
 *
 * <p>Usage examples:
 * <pre>{@code
 * // Default span name: MyService.processOrder
 * @Traced
 * public Order processOrder(OrderRequest request) { ... }
 *
 * // Custom span name
 * @Traced("order-processing")
 * public Order processOrder(OrderRequest request) { ... }
 * }</pre>
 *
 * <p>The annotation is {@link Inherited}, meaning subclasses automatically inherit
 * the interceptor binding from annotated parent classes.
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

    /**
     * Optional custom span name. When non-empty, used instead of the default
     * {@code ClassName.methodName} format.
     *
     * @return custom span name, or empty string for default naming
     */
    @Nonbinding
    String value() default "";

    /**
     * Whether to record method parameters as span attributes.
     *
     * @return {@code true} to record parameters; defaults to {@code false}
     */
    @Nonbinding
    boolean recordParameters() default false;
}
