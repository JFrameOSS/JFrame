package io.github.jframe.tracing.interceptor;

import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * CDI portable extension that programmatically applies {@link Traced} to all
 * {@link ApplicationScoped} beans at container boot time.
 *
 * <p>This replicates the behaviour of Spring's {@code TracingAspect}, which auto-intercepts
 * every {@code @Service}, {@code @Controller}, and {@code @RestController} bean without
 * requiring explicit {@code @Traced} annotations on each class.
 *
 * <p>The extension fires during CDI type discovery via the
 * {@link ProcessAnnotatedType} lifecycle event and adds the {@link Traced} interceptor
 * binding to every {@code @ApplicationScoped} type that does not already carry it.
 *
 * <p>Auto-tracing can be disabled entirely by setting the MicroProfile Config property:
 *
 * <pre>{@code
 * jframe.otlp.auto-trace=false
 * }</pre>
 *
 * <p>Note: CDI portable extensions must <em>not</em> be {@code @ApplicationScoped} beans.
 * They are registered via the Java {@link java.util.ServiceLoader} mechanism through
 * {@code META-INF/services/jakarta.enterprise.inject.spi.Extension}.
 */
@Slf4j
public class AutoTracingExtension implements Extension {

    /** MicroProfile Config property that controls whether auto-tracing is active. */
    private static final String AUTO_TRACE_PROPERTY = "jframe.otlp.auto-trace";

    /** Fully-qualified class name of the tracing interceptor itself — excluded to avoid recursion. */
    private static final String TRACING_INTERCEPTOR_CLASS =
        "io.github.jframe.tracing.interceptor.TracingInterceptor";

    /**
     * Observes every {@link ApplicationScoped} bean type discovered during CDI initialisation and,
     * when auto-tracing is enabled, adds the {@link Traced} interceptor binding if it is not
     * already present.
     *
     * <p>Skips:
     * <ul>
     * <li>Beans that already carry {@code @Traced} — no double-wrapping.</li>
     * <li>{@link TracingInterceptor} itself — prevents infinite interception recursion.</li>
     * </ul>
     *
     * @param event the CDI {@link ProcessAnnotatedType} event for the discovered type
     * @param <T>   the Java type being processed
     */
    public <T> void addTracedToApplicationScopedBeans(
        @Observes
        @WithAnnotations(ApplicationScoped.class) final ProcessAnnotatedType<T> event) {

        final String className = event.getAnnotatedType().getJavaClass().getName();

        if (shouldSkip(className, event)) {
            log.debug("AutoTracingExtension: skipping {} (auto-trace disabled, already traced, or interceptor)", className);
        } else {
            log.debug("AutoTracingExtension: adding @Traced to {}", className);
            event.configureAnnotatedType().add(Traced.Literal.INSTANCE);
        }
    }

    /**
     * Returns {@code true} when the given type should be skipped by the auto-tracing extension.
     * Skips when auto-tracing is disabled, the class is the tracing interceptor itself,
     * or the class already carries {@code @Traced}.
     *
     * @param className the fully-qualified class name of the discovered type
     * @param event     the CDI {@link ProcessAnnotatedType} event for the discovered type
     * @param <T>       the Java type being processed
     * @return {@code true} if the type should not have {@code @Traced} added
     */
    private static <T> boolean shouldSkip(
        final String className, final ProcessAnnotatedType<T> event) {
        return !isAutoTraceEnabled()
            || TRACING_INTERCEPTOR_CLASS.equals(className)
            || event.getAnnotatedType().isAnnotationPresent(Traced.class);
    }

    /**
     * Reads the {@value #AUTO_TRACE_PROPERTY} MicroProfile Config property.
     * Uses {@link ConfigProvider} directly because CDI beans are not yet available
     * when portable extension lifecycle events fire.
     *
     * @return {@code true} when auto-tracing is enabled (the default); {@code false} otherwise
     */
    private static boolean isAutoTraceEnabled() {
        return ConfigProvider.getConfig()
            .getOptionalValue(AUTO_TRACE_PROPERTY, Boolean.class)
            .orElse(true);
    }
}
