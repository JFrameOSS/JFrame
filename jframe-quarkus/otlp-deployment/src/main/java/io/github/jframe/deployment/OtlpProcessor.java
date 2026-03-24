package io.github.jframe.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * Build-time processor for the jframe-quarkus-otlp Quarkus extension.
 *
 * <p>Registers the extension feature descriptor so Quarkus reports it during startup.
 */
public class OtlpProcessor {

    private static final String EXTENSION_FEATURE = "jframe-otlp";

    /**
     * Registers the jframe-otlp Quarkus extension feature at build time.
     *
     * @return the feature build item identifying this extension
     */
    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(EXTENSION_FEATURE);
    }
}
