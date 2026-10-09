package io.github.jframe.openapi;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.smallrye.openapi.api.SmallRyeOpenAPI;

import java.io.IOException;
import java.util.Map;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Unit Test - Quarkus Problem Details OpenAPI schema")
class ProblemDetailsSchemaTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
            "ErrorResponseResource",
            "RateLimitErrorResponseResource"
        }
    )
    @DisplayName("Should describe error schemas with RFC 9457 members and no extensions property")
    void shouldDescribeErrorSchemaWithoutExtensionsProperty(final String schemaName) throws IOException {
        // Given: A resource documenting Problem Details error responses
        final Index index = Index.of(
            OrdersResource.class,
            ErrorResponseResource.class,
            RateLimitErrorResponseResource.class
        );

        // When: Scanning it with SmallRye OpenAPI
        final OpenAPI openApi = SmallRyeOpenAPI.builder()
            .withConfig(ConfigProvider.getConfig())
            .withIndex(index)
            .enableModelReader(false)
            .enableStandardStaticFiles(false)
            .build()
            .model();
        final Map<String, org.eclipse.microprofile.openapi.models.media.Schema> properties =
            openApi.getComponents().getSchemas().get(schemaName).getProperties();

        // Then: RFC 9457 + errorCode members present, no 'extensions' wrapper
        assertThat(properties.keySet(), hasItems("type", "title", "status", "detail", "instance", "errorCode"));
        assertThat(properties.keySet(), not(hasItem("extensions")));
    }

    /** Minimal JAX-RS resource so the scanner runs. */
    @Path("/orders")
    public static class OrdersResource {

        @GET
        @APIResponse(
            responseCode = "400",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ErrorResponseResource.class)
            )
        )
        @APIResponse(
            responseCode = "429",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = RateLimitErrorResponseResource.class)
            )
        )
        public String list() {
            return "";
        }
    }
}
