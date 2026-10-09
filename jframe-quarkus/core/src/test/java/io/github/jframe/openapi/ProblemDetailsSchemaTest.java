package io.github.jframe.openapi;

import io.github.jframe.exception.page.InvalidPageErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchErrorResponseResource;
import io.github.jframe.exception.sort.InvalidSortErrorResponseResource;
import io.smallrye.openapi.api.SmallRyeOpenAPI;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Unit Test - Quarkus Problem Details OpenAPI schema")
class ProblemDetailsSchemaTest {

    private static final String REF_PREFIX = "#/components/schemas/";

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = {
            "ProblemDetails | type",
            "ValidationProblemDetails | type",
            "RateLimitProblemDetails | type",
            "InvalidSortProblemDetails | rejectedField,sortableFields",
            "InvalidSearchProblemDetails | rejectedField,rejectedValue,searchableFields",
            "InvalidPageProblemDetails | rejectedParameter,rejectedValue"
        }
    )
    @DisplayName("Should describe error schemas with RFC 9457 and extension members, no extensions property")
    void shouldDescribeErrorSchema(final String schemaName, final String members) throws IOException {
        // Given: A resource documenting every Problem Details error body
        // When: Scanning it with SmallRye OpenAPI
        final Map<String, org.eclipse.microprofile.openapi.models.media.Schema> schemas = scan().getComponents().getSchemas();

        // Then: RFC 9457 + errorCode + extension members present, no 'extensions' wrapper
        final Set<String> properties = properties(schemas, schemaName).keySet();
        assertThat(properties, hasItems("type", "title", "status", "detail", "instance", "errorCode"));
        assertThat(properties, hasItems(members.split(",")));
        assertThat(properties, not(hasItem("extensions")));
    }

    @Test
    @DisplayName("Should publish no *ResponseResource schema names")
    void shouldNotPublishResourceClassNames() throws IOException {
        // Given: A resource documenting every Problem Details error body
        // When: Scanning it
        final Set<String> names = scan().getComponents().getSchemas().keySet();

        // Then: Only Problem Details names
        assertThat(names, everyItem(not(endsWith("ResponseResource"))));
    }

    private static OpenAPI scan() throws IOException {
        final Index index = Index.of(
            OrdersResource.class,
            ErrorResponseResource.class,
            ValidationErrorResponseResource.class,
            RateLimitErrorResponseResource.class,
            InvalidSortErrorResponseResource.class,
            InvalidSearchErrorResponseResource.class,
            InvalidPageErrorResponseResource.class
        );
        return SmallRyeOpenAPI.builder()
            .withConfig(ConfigProvider.getConfig())
            .withIndex(index)
            .enableModelReader(false)
            .enableStandardStaticFiles(false)
            .build()
            .model();
    }

    /** Own plus {@code allOf}-inherited properties. */
    private static Map<String, org.eclipse.microprofile.openapi.models.media.Schema> properties(
        final Map<String, org.eclipse.microprofile.openapi.models.media.Schema> schemas, final String name) {
        final org.eclipse.microprofile.openapi.models.media.Schema schema = schemas.get(name);
        assertThat(name, schema, is(notNullValue()));
        final Map<String, org.eclipse.microprofile.openapi.models.media.Schema> result = new LinkedHashMap<>();
        if (schema.getProperties() != null) {
            result.putAll(schema.getProperties());
        }
        if (schema.getAllOf() != null) {
            for (final org.eclipse.microprofile.openapi.models.media.Schema part : schema.getAllOf()) {
                if (part.getRef() != null) {
                    result.putAll(properties(schemas, part.getRef().substring(REF_PREFIX.length())));
                } else if (part.getProperties() != null) {
                    result.putAll(part.getProperties());
                }
            }
        }
        return result;
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
            responseCode = "422",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ValidationErrorResponseResource.class)
            )
        )
        @APIResponse(
            responseCode = "429",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = RateLimitErrorResponseResource.class)
            )
        )
        @APIResponse(
            responseCode = "460",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = InvalidSortErrorResponseResource.class)
            )
        )
        @APIResponse(
            responseCode = "461",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = InvalidSearchErrorResponseResource.class)
            )
        )
        @APIResponse(
            responseCode = "462",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = InvalidPageErrorResponseResource.class)
            )
        )
        public String list() {
            return "";
        }
    }
}
