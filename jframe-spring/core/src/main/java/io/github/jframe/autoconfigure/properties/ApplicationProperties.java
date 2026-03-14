package io.github.jframe.autoconfigure.properties;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for JFrame applications. These properties are common across all applications regardless of which starters are
 * used.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jframe.application")
public class ApplicationProperties {

    /**
     * Name of the application. This will be used as the service name in OpenTelemetry traces.
     */
    @NotBlank(message = "Application name must not be blank")
    private String name;

    /**
     * Group/namespace of the application. Used for organizing services in monitoring systems.
     */
    @NotBlank(message = "group/namespace must not be blank")
    private String group;

    /**
     * Version of the application. Helps track which version generated specific traces.
     */
    @NotBlank(message = "version must not be blank")
    private String version;

    /**
     * Environment where the application is running. Examples: dev, test, staging, prod
     */
    @NotBlank(message = "Application environment must not be blank")
    private String environment = "dev";

    /**
     * Base URL of the application. Used for health checks and service discovery.
     */
    private String url;
}
