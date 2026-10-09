package io.github.jframe.tests.spring.openapi;

import io.github.jframe.tests.spring.TestApplication;
import io.github.jframe.tests.spring.TestSecurityConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * An application schema named {@code ProblemDetails} must not be overwritten by jFrame.
 */
@DisplayName("Spring Integration - Problem Details OpenAPI keeps application ProblemDetails schema")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(
    {
        TestSecurityConfiguration.class,
        ProblemDetailsOpenApiAppSchemaTest.AppSchemaConfiguration.class
    }
)
class ProblemDetailsOpenApiAppSchemaTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @DisplayName("Should keep an application-registered ProblemDetails schema")
    void shouldKeepAppProblemDetailsSchema() throws Exception {
        // Given: The application registered its own ProblemDetails
        // When: Reading the generated document
        final Map<String, Object> schema = OpenApiDocs.schema(OpenApiDocs.read(webApplicationContext), "ProblemDetails");

        // Then: The application's distinctive property survives
        assertThat(OpenApiDocs.properties(schema).keySet(), hasItem("appMarker"));
    }

    /** Application OpenAPI with its own ProblemDetails schema. */
    @TestConfiguration
    static class AppSchemaConfiguration {

        @Bean
        OpenAPI appOpenApi() {
            return new OpenAPI().components(
                new Components().addSchemas("ProblemDetails", new ObjectSchema().addProperty("appMarker", new StringSchema()))
            );
        }
    }
}
