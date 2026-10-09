package io.github.jframe.tests.spring;

import io.github.support.ProblemJson;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The generated OpenAPI document describes error bodies as flat RFC 9457 Problem Details.
 */
@DisplayName("Spring Integration - Problem Details OpenAPI schema")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(TestSecurityConfiguration.class)
class ProblemDetailsOpenApiTest {

    private static final String REF_PREFIX = "#/components/schemas/";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @DisplayName("Should describe problem+json error schemas with RFC 9457 members and no extensions property")
    @SuppressWarnings("unchecked")
    void shouldDescribeProblemSchemasWithoutExtensionsProperty() throws Exception {
        // Given: The application's generated OpenAPI document
        final MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        final String json = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        final Map<String, Object> document = ProblemJson.parse(json);
        final Map<String, Object> schemas = (Map<String, Object>) ((Map<String, Object>) document.get("components")).get("schemas");

        // When: Collecting every schema referenced by an application/problem+json response
        final Set<String> problemSchemas = problemSchemaNames(document);

        // Then: Each has the RFC 9457 + errorCode members and no 'extensions' wrapper
        assertThat(problemSchemas, hasItem("ErrorResponseResource"));
        for (final String name : problemSchemas) {
            final Map<String, Object> properties = properties(schemas, name);
            assertThat(name, properties.keySet(), hasItems("type", "title", "status", "detail", "instance", "errorCode"));
            assertThat(name, properties.keySet(), not(hasItem("extensions")));
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> problemSchemaNames(final Map<String, Object> document) {
        final Set<String> names = new HashSet<>();
        final Map<String, Object> paths = (Map<String, Object>) document.get("paths");
        paths.values().forEach(pathItem -> ((Map<String, Object>) pathItem).values().forEach(operation -> {
            final Map<String, Object> responses = (Map<String, Object>) ((Map<String, Object>) operation).getOrDefault(
                "responses",
                Map.of()
            );
            responses.values().forEach(response -> {
                final Map<String, Object> content = (Map<String, Object>) ((Map<String, Object>) response).getOrDefault(
                    "content",
                    Map.of()
                );
                final Map<String, Object> media = (Map<String, Object>) content.get(ProblemJson.PROBLEM_JSON);
                if (media != null) {
                    final String ref = (String) ((Map<String, Object>) media.get("schema")).get("$ref");
                    names.add(ref.substring(REF_PREFIX.length()));
                }
            });
        }));
        return names;
    }

    /** Own plus {@code allOf}-inherited properties. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(final Map<String, Object> schemas, final String name) {
        final Map<String, Object> schema = (Map<String, Object>) schemas.get(name);
        final Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) schema.getOrDefault("properties", Map.of()));
        for (final Object part : (List<Object>) schema.getOrDefault("allOf", List.of())) {
            final Map<String, Object> partSchema = (Map<String, Object>) part;
            final String ref = (String) partSchema.get("$ref");
            result.putAll(
                ref == null
                    ? (Map<String, Object>) partSchema.getOrDefault("properties", Map.of())
                    : properties(schemas, ref.substring(REF_PREFIX.length()))
            );
        }
        return result;
    }
}
