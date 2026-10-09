package io.github.jframe.tests.spring.openapi;

import io.github.support.ProblemJson;

import java.util.Map;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Reads the generated {@code /v3/api-docs} document. */
final class OpenApiDocs {

    private OpenApiDocs() {
    }

    static Map<String, Object> read(final WebApplicationContext context) throws Exception {
        final MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        return ProblemJson.parse(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString());
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> schema(final Map<String, Object> document, final String name) {
        final Map<String, Object> schemas =
            (Map<String, Object>) ((Map<String, Object>) document.get("components")).get("schemas");
        return (Map<String, Object>) schemas.get(name);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> properties(final Map<String, Object> schema) {
        return (Map<String, Object>) schema.getOrDefault("properties", Map.of());
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> problemSchema(final Map<String, Object> document, final String path, final String code) {
        final Map<String, Object> operation =
            (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) document.get("paths")).get(path)).get("get");
        final Map<String, Object> response = (Map<String, Object>) ((Map<String, Object>) operation.get("responses")).get(code);
        final Map<String, Object> media =
            (Map<String, Object>) ((Map<String, Object>) response.get("content")).get(ProblemJson.PROBLEM_JSON);
        return (Map<String, Object>) media.get("schema");
    }
}
