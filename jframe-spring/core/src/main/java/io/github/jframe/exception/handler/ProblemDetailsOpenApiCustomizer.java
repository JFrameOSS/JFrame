package io.github.jframe.exception.handler;

import io.github.jframe.exception.page.InvalidPageErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchErrorResponseResource;
import io.github.jframe.exception.sort.InvalidSortErrorResponseResource;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.springdoc.core.customizers.OpenApiLocaleCustomizer;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Documents every 400 Problem Details variant as {@code oneOf} on each 400 response.
 *
 * <p>Never overwrites application schemas or application-specific 400 schemas. Implemented as
 * {@link OpenApiLocaleCustomizer} because springdoc runs those before all {@code OpenApiCustomizer}s
 * ({@code @Order} is ignored there), so application customisers see jFrame's schemas.
 */
public class ProblemDetailsOpenApiCustomizer implements OpenApiLocaleCustomizer {

    private static final String BAD_REQUEST = "400";

    private static final List<Class<? extends ErrorResponseResource>> BAD_REQUEST_VARIANTS = List.of(
        ErrorResponseResource.class,
        ValidationErrorResponseResource.class,
        InvalidSortErrorResponseResource.class,
        InvalidSearchErrorResponseResource.class,
        InvalidPageErrorResponseResource.class
    );

    private final ObjectProvider<OpenAPI> applicationOpenApi;

    /** The constructor; {@code applicationOpenApi} supplies schemas the application registered itself. */
    public ProblemDetailsOpenApiCustomizer(final ObjectProvider<OpenAPI> applicationOpenApi) {
        this.applicationOpenApi = applicationOpenApi;
    }

    @Override
    public void customise(final OpenAPI openApi, final Locale locale) {
        if (openApi.getPaths() == null) {
            return;
        }
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        restoreApplicationSchemas(openApi.getComponents());
        final ModelConverters converters = ModelConverters.getInstance(openApi.getSpecVersion() == SpecVersion.V31);
        final Schema<Object> oneOf = new Schema<>();
        for (final Class<? extends ErrorResponseResource> variant : BAD_REQUEST_VARIANTS) {
            converters.readAll(variant).forEach(addIfAbsent(openApi.getComponents()));
            final String name = converters.readAllAsResolvedSchema(variant).schema.getName();
            oneOf.addOneOfItem(new Schema<>().$ref(Components.COMPONENTS_SCHEMAS_REF + name));
        }
        final String problemDetailsRef =
            Components.COMPONENTS_SCHEMAS_REF + converters.readAllAsResolvedSchema(ErrorResponseResource.class).schema.getName();
        openApi.getPaths().values().stream()
            .flatMap(path -> path.readOperations().stream())
            .map(operation -> operation.getResponses() == null ? null : operation.getResponses().get(BAD_REQUEST))
            .filter(Objects::nonNull)
            .forEach(response -> document(response, oneOf, problemDetailsRef));
    }

    /** Springdoc may overwrite application schemas while scanning jFrame's handler; put the originals back. */
    @SuppressWarnings("rawtypes")
    private void restoreApplicationSchemas(final Components components) {
        final OpenAPI application = applicationOpenApi.getIfUnique();
        final Map<String, Schema> schemas =
            application == null || application.getComponents() == null ? null : application.getComponents().getSchemas();
        if (schemas != null) {
            schemas.forEach((name, schema) -> components.addSchemas(name, Json.mapper().convertValue(schema, Schema.class)));
        }
    }

    @SuppressWarnings("rawtypes")
    private static BiConsumer<String, Schema> addIfAbsent(final Components components) {
        return (name, schema) -> {
            if (components.getSchemas() == null || !components.getSchemas().containsKey(name)) {
                components.addSchemas(name, schema);
            }
        };
    }

    private static void document(final ApiResponse response, final Schema<Object> oneOf, final String problemDetailsRef) {
        if (response.getContent() == null) {
            response.setContent(new Content());
        }
        final MediaType mediaType = response.getContent().get(ProblemDetails.MEDIA_TYPE);
        if (mediaType == null) {
            response.getContent().addMediaType(ProblemDetails.MEDIA_TYPE, new MediaType().schema(oneOf));
        } else if (mediaType.getSchema() == null || problemDetailsRef.equals(mediaType.getSchema().get$ref())) {
            mediaType.setSchema(oneOf);
        }
    }
}
