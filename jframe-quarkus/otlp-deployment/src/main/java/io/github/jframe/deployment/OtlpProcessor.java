package io.github.jframe.deployment;

import io.github.jframe.tracing.Traced;
import io.github.jframe.tracing.interceptor.TracingInterceptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

/**
 * Build-time processor for the jframe-quarkus-otlp Quarkus extension.
 *
 * <p>Registers the extension feature descriptor, the {@link TracingInterceptor} as an
 * unremovable CDI bean, and automatically adds the {@link Traced} binding to all
 * {@code @ApplicationScoped} beans and Vaadin {@code @Route} views.
 */
public class OtlpProcessor {

    private static final String EXTENSION_FEATURE = "jframe-otlp";
    private static final DotName APPLICATION_SCOPED =
        DotName.createSimple("jakarta.enterprise.context.ApplicationScoped");
    private static final DotName VAADIN_ROUTE = DotName.createSimple("com.vaadin.flow.router.Route");

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
     * Automatically adds the {@link Traced} interceptor binding to all
     * {@code @ApplicationScoped} beans and Vaadin {@code @Route} views at build time,
     * so they are traced without requiring explicit annotation.
     *
     * @return the annotations transformer build item
     */
    @BuildStep
    AnnotationsTransformerBuildItem autoTraceTransformer() {
        return new AnnotationsTransformerBuildItem(
            AnnotationTransformation.forClasses()
                .whenAnyMatch(APPLICATION_SCOPED, VAADIN_ROUTE)
                .transform(ctx -> ctx.add(Traced.class))
        );
    }
}
