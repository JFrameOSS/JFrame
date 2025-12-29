package io.github.jframe.autoconfigure;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration for Jackson JSON serialization and deserialization.
 */
@Configuration
public class JacksonConfig {

    /** Customizes the Jackson ObjectMapper settings. */
    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
            .changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.ALWAYS))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }
}
