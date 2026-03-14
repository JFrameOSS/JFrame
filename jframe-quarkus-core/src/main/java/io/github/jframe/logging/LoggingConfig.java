package io.github.jframe.logging;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;

/**
 * Quarkus {@code @ConfigMapping} interface for JFrame HTTP logging configuration.
 *
 * <p>Binds SmallRye Config properties under the {@code jframe.logging} prefix.
 * Provides defaults equivalent to the Spring Boot {@code LoggingProperties} class.
 */
@ConfigMapping(prefix = "jframe.logging")
public interface LoggingConfig {

    /**
     * Whether HTTP request/response logging is disabled.
     *
     * @return {@code true} to disable logging; defaults to {@code false}
     */
    @WithDefault("false")
    boolean disabled();

    /**
     * Maximum number of characters to include from the response body.
     *
     * <p>A value of {@code -1} means unlimited.
     *
     * @return maximum response body length; defaults to {@code -1}
     */
    @WithDefault("-1")
    int responseLength();

    /**
     * Content types whose request body should be excluded from logging.
     *
     * @return list of excluded content-type strings; defaults to {@code ["multipart/form-data"]}
     */
    @WithDefault("multipart/form-data")
    List<String> bodyExcludedContentTypes();

    /**
     * Request paths that should be excluded from logging.
     *
     * @return list of path patterns to exclude; defaults to {@code ["/actuator/*"]}
     */
    @WithDefault("/actuator/*")
    List<String> excludePaths();

    /**
     * Request/response body field names whose values should be masked in logs.
     *
     * @return list of sensitive field names; defaults to {@code ["password","keyPassphrase","client_secret","secret"]}
     */
    @WithDefault("password,keyPassphrase,client_secret,secret")
    List<String> fieldsToMask();

    /**
     * Content types for which the request/response body is eligible to be logged.
     *
     * @return list of allowed content-type strings; defaults to the standard set of loggable media types
     */
    @WithDefault(
        "application/json,application/graphql+json,application/hal+json,"
            + "application/problem+json,application/vnd.spring-boot.actuator.v1+json,"
            + "application/vnd.spring-boot.actuator.v3+json,"
            + "application/vnd.spring-cloud.config-server.v2+json,"
            + "application/x-www-form-urlencoded,application/xml,multipart/form-data,"
            + "text/plain,text/xml"
    )
    List<String> allowedContentTypes();
}
