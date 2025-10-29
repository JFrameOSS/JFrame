package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.factory.YamlPropertySourceFactory;
import io.github.jframe.autoconfigure.properties.ApplicationProperties;
import io.github.jframe.autoconfigure.properties.LoggingProperties;
import io.github.jframe.logging.filter.FilterConfiguration;
import io.github.jframe.logging.logger.*;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.jframe.logging.voter.MediaTypeVoter;
import io.github.jframe.logging.voter.RequestVoter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Main autoconfiguration for JFrame integration.
 */
@AutoConfiguration
@Import(FilterConfiguration.class)
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
@ComponentScan(basePackages = "io.github.jframe.*")
public class CoreAutoConfiguration {

    /**
     * Create a {@link PasswordMasker} bean.
     *
     * @param properties the configuration properties.
     * @return the bean.
     */
    @Bean
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
    public MediaTypeVoter mediaTypeVoter(final LoggingProperties loggingProperties) {
        return new MediaTypeVoter(loggingProperties.getAllowedContentTypes(), true);
    }

    /** Create a {@link MediaTypeVoter} for body exclusion. */
    @Bean
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
