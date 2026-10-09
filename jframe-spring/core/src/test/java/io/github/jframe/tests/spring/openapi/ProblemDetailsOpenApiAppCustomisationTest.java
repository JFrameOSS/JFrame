package io.github.jframe.tests.spring.openapi;

import io.github.jframe.tests.spring.TestApplication;
import io.github.jframe.tests.spring.TestSecurityConfiguration;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * jFrame's OpenAPI customiser must respect what the application documents itself.
 */
@DisplayName("Spring Integration - Problem Details OpenAPI respects application customisation")
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(
    {
        TestSecurityConfiguration.class,
        ProblemDetailsOpenApiAppCustomisationTest.AppOpenApiConfiguration.class
    }
)
class ProblemDetailsOpenApiAppCustomisationTest {

    static final String OWN_400_PATH = "/openapi-app/own-400";

    static final String DEFAULT_400_PATH = "/openapi-app/default-400";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @DisplayName("Should keep an application-registered ValidationProblemDetails schema")
    void shouldKeepAppRegisteredSchemaWhenNameClashes() throws Exception {
        // Given: The application registered its own ValidationProblemDetails
        // When: Reading the generated document
        final Map<String, Object> document = OpenApiDocs.read(webApplicationContext);

        // Then: The application's schema survives
        assertThat(
            OpenApiDocs.properties(OpenApiDocs.schema(document, "ValidationProblemDetails")).keySet(),
            hasItem("appMarker")
        );
    }

    @Test
    @DisplayName("Should leave an application-documented 400 with its own schema untouched")
    void shouldLeaveOwn400SchemaUntouched() throws Exception {
        // Given: An endpoint documenting its 400 as MyBadRequest
        // When: Reading the generated document
        final Map<String, Object> schema = OpenApiDocs.problemSchema(OpenApiDocs.read(webApplicationContext), OWN_400_PATH, "400");

        // Then: Still a plain ref to MyBadRequest, no oneOf
        assertThat(schema, hasEntry("$ref", "#/components/schemas/MyBadRequest"));
        assertThat(schema, not(hasKey("oneOf")));
    }

    @Test
    @DisplayName("Should still document jFrame 400 variants when the 400 refers to ProblemDetails")
    void shouldRewriteDefault400WhenRefIsProblemDetails() throws Exception {
        // Given: An endpoint documenting its 400 as jFrame's ProblemDetails
        // When: Reading the generated document
        final Map<String, Object> schema =
            OpenApiDocs.problemSchema(OpenApiDocs.read(webApplicationContext), DEFAULT_400_PATH, "400");

        // Then: Rewritten to the oneOf of variants
        assertThat(schema, hasKey("oneOf"));
    }

    @Test
    @DisplayName("Should run before application customisers so their ProblemDetails edits survive")
    void shouldRunBeforeAppCustomisers() throws Exception {
        // Given: An application customiser editing ProblemDetails only if present
        // When: Reading the generated document
        final Map<String, Object> properties =
            OpenApiDocs.properties(OpenApiDocs.schema(OpenApiDocs.read(webApplicationContext), "ProblemDetails"));

        // Then: jFrame's members and the application's edit are both present
        assertThat(properties.keySet(), hasItems("type", "errorCode", "appEdited"));
    }

    /** Application OpenAPI setup: own schemas, own 400 and a customiser. */
    @TestConfiguration
    static class AppOpenApiConfiguration {

        @Bean
        OpenAPI appOpenApi() {
            return new OpenAPI().components(
                new Components()
                    .addSchemas("ValidationProblemDetails", new ObjectSchema().addProperty("appMarker", new StringSchema()))
                    .addSchemas("MyBadRequest", new ObjectSchema().addProperty("reason", new StringSchema()))
            );
        }

        @Bean
        GlobalOpenApiCustomizer appProblemDetailsCustomiser() {
            return openApi -> {
                final io.swagger.v3.oas.models.media.Schema<?> problemDetails =
                    openApi.getComponents() == null || openApi.getComponents().getSchemas() == null
                        ? null
                        : openApi.getComponents().getSchemas().get("ProblemDetails");
                if (problemDetails != null) {
                    problemDetails.addProperty("appEdited", new StringSchema());
                }
            };
        }

        @Bean
        AppDocumentedController appDocumentedController() {
            return new AppDocumentedController();
        }
    }


    /** Endpoints documenting their own 400 responses. */
    @RestController
    static class AppDocumentedController {

        @GetMapping(OWN_400_PATH)
        @ApiResponse(
            responseCode = "400",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/MyBadRequest")
            )
        )
        public String own400() {
            return "ok";
        }

        @GetMapping(DEFAULT_400_PATH)
        @ApiResponse(
            responseCode = "400",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetails")
            )
        )
        public String default400() {
            return "ok";
        }
    }
}
