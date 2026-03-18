package io.github.jframe.autoconfigure.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus {@code @ConfigMapping} interface for JFrame application configuration.
 *
 * <p>Binds SmallRye Config properties under the {@code jframe.application} prefix.
 * Provides application metadata such as name, group, version, and environment.
 */
@ConfigMapping(prefix = "jframe.application")
public interface ApplicationConfig {

    /**
     * The application name.
     *
     * @return the application name (required, no default)
     */
    String name();

    /**
     * The application group (e.g. Maven groupId or team name).
     *
     * @return the application group (required, no default)
     */
    String group();

    /**
     * The application version.
     *
     * @return the application version (required, no default)
     */
    String version();

    /**
     * The deployment environment (e.g. dev, staging, prod).
     *
     * @return the environment; defaults to {@code "dev"}
     */
    @WithDefault("dev")
    String environment();
}
