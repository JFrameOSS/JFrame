package io.github.jframe.autoconfigure.properties;

import io.github.jframe.logging.model.PathDefinition;
import lombok.Data;

import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;

import static io.github.jframe.autoconfigure.properties.LoggingProperties.CONFIG_PREFIX;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.parseMediaType;

/**
 * Configuration properties for the JFrame core logging module.
 * <p>
 * These properties define the global logging behavior for HTTP requests and responses
 * across the framework. They control aspects such as allowed content types, excluded paths,
 * and sensitive data masking.
 * </p>
 *
 * <p>
 * Configuration is typically provided in your {@code application.yml} or {@code application.properties}
 * using the prefix {@code jframe.logging}.
 * </p>
 *
 * <p><strong>Example configuration:</strong></p>
 * <pre>{@code
 * jframe:
 *   logging:
 *     disabled: false
 *     response-length: 2000
 *     body-excluded-content-types:
 *       - multipart/form-data
 *     exclude-paths:
 *       - pattern: /actuator/*
 *     fields-to-mask:
 *       - password
 *       - client_secret
 *     allowed-content-types:
 *       - application/json
 *       - text/plain
 * }</pre>
 *
 * <p>This configuration ensures safe and readable log output for HTTP requests and responses,
 * while avoiding sensitive data exposure and excessive payload sizes.</p>
 */
@Data
@Validated
@ConfigurationProperties(CONFIG_PREFIX)
public class LoggingProperties {

    /** Configuration properties prefix. */
    public static final String CONFIG_PREFIX = "jframe.logging";

    /**
     * Whether JFrame logging is disabled. Set to true to disable HTTP request/response logging.
     */
    private boolean disabled;

    /**
     * Maximum response body length to log in characters. Set to -1 for unlimited length. Large responses will be truncated.
     */
    @Min(
        value = -1,
        message = "Response length must be -1 (unlimited) or a positive number"
    )
    private int responseLength = -1;

    /**
     * Content types for which the body should be excluded from logging.
     */
    private List<MediaType> bodyExcludedContentTypes = List.of(MULTIPART_FORM_DATA);

    /**
     * Paths to exclude for logging. Uses the same syntax as Spring's {@code PathPattern}.
     */
    private List<PathDefinition> excludePaths = List.of(new PathDefinition("/actuator/*"));

    /**
     * Fields to mask in request and response bodies.
     */
    private List<String> fieldsToMask = List.of("password", "keyPassphrase", "client_secret", "secret");

    /**
     * All allowed-content types will be fully logged, this means the request / response headers and the body. The body can be suppressed by
     * configuring the content-type in the `body-excluded-content-types` property.
     */
    @NotEmpty(message = "Allowed content types list cannot be empty")
    private List<MediaType> allowedContentTypes = List.of(
        parseMediaType("application/json"),
        parseMediaType("application/graphql+json"),
        parseMediaType("application/hal+json"),
        parseMediaType("application/problem+json"),
        parseMediaType("application/vnd.spring-boot.actuator.v1+json"),
        parseMediaType("application/vnd.spring-boot.actuator.v3+json"),
        parseMediaType("application/vnd.spring-cloud.config-server.v2+json"),
        parseMediaType("application/x-www-form-urlencoded"),
        parseMediaType("application/xml"),
        parseMediaType("multipart/form-data"),
        parseMediaType("text/plain"),
        parseMediaType("text/xml")
    );
}
