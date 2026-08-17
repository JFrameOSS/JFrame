package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.factory.YamlPropertySourceFactory;
import io.github.jframe.autoconfigure.properties.ApplicationProperties;
import io.github.jframe.autoconfigure.properties.LoggingProperties;
import io.github.jframe.exception.ExceptionConfiguration;
import io.github.jframe.logging.LoggingConfiguration;
import io.github.jframe.logging.logger.DefaultRequestResponseLogger;
import io.github.jframe.logging.logger.HttpRequestResponseBodyLogger;
import io.github.jframe.logging.logger.HttpRequestResponseDebugLogger;
import io.github.jframe.logging.logger.HttpRequestResponseHeadersLogger;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.jframe.logging.voter.MediaTypeVoter;
import io.github.jframe.logging.voter.RequestVoter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Main autoconfiguration for JFrame integration.
 *
 * <p>Registers all jframe-spring/core beans explicitly via {@code @Import} or {@code @Bean},
 * replacing the former broad {@code @ComponentScan(basePackages = "io.github.jframe")}.
 * The narrow {@code @ComponentScan("io.github.jframe.util.mapper")} is intentional: MapStruct
 * generates {@code *Impl} classes (e.g. {@code DateTimeMapperImpl}) into that package at
 * compile time, making hand-written {@code @Bean} methods fragile. This narrow scan is cheap
 * (one package, a handful of generated files) and correct.
 */
@AutoConfiguration
@Import(
    {
        JacksonConfig.class,
        CorePackageLogger.class,
        ExceptionConfiguration.class,
        LoggingConfiguration.class
    }
)
@EnableConfigurationProperties(
    {
        ApplicationProperties.class,
        LoggingProperties.class
    }
)
@PropertySource(
    value = "classpath:jframe-properties.yml",
    factory = YamlPropertySourceFactory.class
)
// Narrow scan for MapStruct-generated mapper implementations (e.g. DateTimeMapperImpl, UuidMapperImpl,
// AmountMapperImpl). MapStruct generates *Impl classes into this package at compile time; hand-writing
// @Bean methods for generated classes is fragile. This one-package scan is cheap and correct.
// The former broad @ComponentScan(basePackages = "io.github.jframe") has been removed.
@ComponentScan("io.github.jframe.util.mapper")
public class CoreAutoConfiguration {

    /**
     * Create a {@link PasswordMasker} bean.
     *
     * @param properties the configuration properties.
     * @return the bean.
     */
    @Bean
    @ConditionalOnMissingBean(PasswordMasker.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public PasswordMasker passwordMaskerUtil(final LoggingProperties properties) {
        return new PasswordMasker(properties.getFieldsToMask());
    }

    /**
     * Create a Media Type voter.
     *
     * @param loggingProperties The configuration properties.
     * @return The bean.
     */
    @Bean
    @ConditionalOnMissingBean(name = "mediaTypeVoter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public MediaTypeVoter mediaTypeVoter(final LoggingProperties loggingProperties) {
        return new MediaTypeVoter(loggingProperties.getAllowedContentTypes(), true);
    }

    /**
     * Create a {@link MediaTypeVoter} for body exclusion.
     *
     * @param loggingProperties The configuration properties.
     * @return The bean.
     */
    @Bean
    @ConditionalOnMissingBean(name = "bodyExcludedMediaTypeVoter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public MediaTypeVoter bodyExcludedMediaTypeVoter(final LoggingProperties loggingProperties) {
        return new MediaTypeVoter(loggingProperties.getBodyExcludedContentTypes(), false);
    }

    /**
     * Create a request voter.
     *
     * @param loggingProperties The configuration properties.
     * @return The bean.
     */
    @Bean
    @ConditionalOnMissingBean(RequestVoter.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestVoter requestVoter(final LoggingProperties loggingProperties) {
        return new RequestVoter(loggingProperties);
    }

    /**
     * Create a filter voter parameter.
     *
     * @param mediaTypeVoter The media type voter.
     * @param requestVoter   The request voter.
     * @return The bean.
     */
    @Bean
    @ConditionalOnMissingBean(FilterVoter.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterVoter filterVoter(final MediaTypeVoter mediaTypeVoter, final RequestVoter requestVoter) {
        return new FilterVoter(mediaTypeVoter, requestVoter);
    }

    /**
     * Create a {@link RequestResponseLogger} bean.
     *
     * @param headersLogger              The headers logger.
     * @param bodyLogger                 The body logger.
     * @param debugLogger                The debug logger.
     * @param mediaTypeVoter             The media type voter.
     * @param bodyExcludedMediaTypeVoter The body excluded media type voter.
     * @return the bean.
     */
    @Bean
    @ConditionalOnMissingBean(RequestResponseLogger.class)
    public RequestResponseLogger requestResponseLogger(
        final HttpRequestResponseHeadersLogger headersLogger,
        final HttpRequestResponseBodyLogger bodyLogger,
        final HttpRequestResponseDebugLogger debugLogger,
        final MediaTypeVoter mediaTypeVoter,
        @Qualifier("bodyExcludedMediaTypeVoter") final MediaTypeVoter bodyExcludedMediaTypeVoter) {
        return new DefaultRequestResponseLogger(
            headersLogger,
            bodyLogger,
            debugLogger,
            mediaTypeVoter,
            bodyExcludedMediaTypeVoter
        );
    }
}
