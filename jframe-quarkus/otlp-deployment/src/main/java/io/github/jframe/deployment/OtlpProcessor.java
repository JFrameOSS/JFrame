package io.github.jframe.deployment;

import io.github.jframe.tracing.Traced;
import io.github.jframe.tracing.interceptor.TracingInterceptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * Build-time processor for the jframe-quarkus-otlp Quarkus extension.
 *
 * <p>Registers the extension feature descriptor, the {@link TracingInterceptor} as an
 * unremovable CDI bean, and automatically adds the {@link Traced} binding to public
 * methods of {@code @ApplicationScoped} application beans (excluding framework internals).
 */
public class OtlpProcessor {

    private static final String EXTENSION_FEATURE = "jframe-otlp";
    private static final DotName APPLICATION_SCOPED =
        DotName.createSimple("jakarta.enterprise.context.ApplicationScoped");
    private static final DotName VAADIN_ROUTE = DotName.createSimple("com.vaadin.flow.router.Route");
    private static final DotName TRACED = DotName.createSimple(Traced.class.getName());

    /** Framework package prefixes excluded from auto-tracing. */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
        "io.quarkus.",
        "io.smallrye.",
        "io.vertx.",
        "io.netty.",
        "io.opentelemetry.",
        "org.jboss.",
        "org.hibernate.",
        "org.eclipse.microprofile.",
        "jakarta.",
        "com.fasterxml.",
        "io.github.jframe."
    );

    /**
     * Registers the jframe-otlp Quarkus extension feature at build time.
     *
     * @return the feature build item identifying this extension
     */
    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(EXTENSION_FEATURE);
    }

    /**
     * Registers {@link TracingInterceptor} as an unremovable CDI bean so Quarkus does not
     * remove it during bean removal optimization.
     *
     * @return the additional bean build item for the tracing interceptor
     */
    @BuildStep
    AdditionalBeanBuildItem registerTracingInterceptor() {
        return AdditionalBeanBuildItem.unremovableOf(TracingInterceptor.class);
    }

    /**
     * Automatically adds the {@link Traced} interceptor binding to public methods of
     * {@code @ApplicationScoped} beans and Vaadin {@code @Route} views at build time.
     *
     * <p>Only application classes are transformed — framework-internal packages
     * (Quarkus, SmallRye, Vert.x, Hibernate, Jakarta, etc.) are excluded to prevent
     * CDI phase-ordering issues during static initialization.
     *
     * @param index the combined Jandex index of all classes on the classpath
     * @return the annotations transformer build item
     */
    @BuildStep
    AnnotationsTransformerBuildItem autoTraceTransformer(final CombinedIndexBuildItem index) {
        final Set<DotName> eligibleClasses = index.getIndex()
            .getAnnotations(APPLICATION_SCOPED)
            .stream()
            .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
            .map(ai -> ai.target().asClass().name())
            .filter(name -> isApplicationClass(name.toString()))
            .collect(Collectors.toSet());

        // Also include Vaadin @Route classes if present
        index.getIndex()
            .getAnnotations(VAADIN_ROUTE)
            .stream()
            .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
            .map(ai -> ai.target().asClass().name())
            .filter(name -> isApplicationClass(name.toString()))
            .forEach(eligibleClasses::add);

        return new AnnotationsTransformerBuildItem(
            AnnotationTransformation.forMethods()
                .whenMethod(method -> shouldTraceMethod(method, eligibleClasses))
                .transform(ctx -> ctx.add(Traced.class))
        );
    }

    private static boolean isApplicationClass(final String className) {
        for (final String prefix : EXCLUDED_PREFIXES) {
            if (className.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static boolean shouldTraceMethod(final MethodInfo method, final Set<DotName> eligibleClasses) {
        final boolean isEligibleBean = eligibleClasses.contains(method.declaringClass().name())
            && !method.hasAnnotation(TRACED)
            && !method.name().startsWith("<");
        return isEligibleBean && Modifier.isPublic(method.flags()) && !Modifier.isStatic(method.flags());
    }
}
