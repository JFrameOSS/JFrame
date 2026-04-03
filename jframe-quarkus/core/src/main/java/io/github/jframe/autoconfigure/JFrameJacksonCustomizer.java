package io.github.jframe.autoconfigure;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration for Jackson JSON serialization and deserialization.
 *
 * <p>CDI producer that creates a configured {@link ObjectMapper} equivalent to the
 * Spring Boot {@code JacksonConfig}.
 */
@ApplicationScoped
public class JFrameJacksonCustomizer {

    /**
     * Produces a configured {@link ObjectMapper} bean.
     *
     * @return the configured {@link ObjectMapper}
     */
    @Produces
    @ApplicationScoped
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.ALWAYS))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build();
    }
}
