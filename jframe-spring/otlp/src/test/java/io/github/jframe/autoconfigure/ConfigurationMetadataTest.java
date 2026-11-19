package io.github.jframe.autoconfigure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

/** Guards {@code additional-spring-configuration-metadata.json} against stale entries. */
@DisplayName("Configuration metadata - jframe-spring-otlp")
class ConfigurationMetadataTest {

    private static final String METADATA = "META-INF/additional-spring-configuration-metadata.json";

    @Test
    @DisplayName("Should only reference existing classes and jframe.otlp properties")
    void shouldOnlyReferenceExistingClassesAndOtlpProperties() throws Exception {
        // Given: the module's additional metadata
        final JsonNode root;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(METADATA)) {
            root = JsonMapper.builder().build().readTree(input);
        }

        // When / Then: every entry belongs to this module and its types exist
        for (final String section : new String[] {
            "groups",
            "properties",
            "hints"
        }) {
            for (final JsonNode entry : root.path(section)) {
                assertThat(entry.get("name").asString(), startsWith("jframe.otlp."));
                for (final String typeField : new String[] {
                    "type",
                    "sourceType"
                }) {
                    final JsonNode type = entry.get(typeField);
                    if (type != null && type.asString().startsWith("io.github.jframe")) {
                        Class.forName(type.asString());
                    }
                }
            }
        }
    }
}
