package io.github.jframe.exception.handler;

import io.github.jframe.exception.page.InvalidPageErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchErrorResponseResource;
import io.github.jframe.exception.sort.InvalidSortErrorResponseResource;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.List;
import java.util.Objects;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

/**
 * Documents every 400 Problem Details variant as {@code oneOf} on each 400 response.
 */
public class ProblemDetailsOpenApiCustomizer implements GlobalOpenApiCustomizer {

    private static final String BAD_REQUEST = "400";

    private static final List<Class<? extends ErrorResponseResource>> BAD_REQUEST_VARIANTS = List.of(
        ErrorResponseResource.class,
        ValidationErrorResponseResource.class,
        InvalidSortErrorResponseResource.class,
        InvalidSearchErrorResponseResource.class,
        InvalidPageErrorResponseResource.class
    );

    @Override
    public void customise(final OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        final ModelConverters converters = ModelConverters.getInstance(openApi.getSpecVersion() == SpecVersion.V31);
        final Schema<Object> oneOf = new Schema<>();
        for (final Class<? extends ErrorResponseResource> variant : BAD_REQUEST_VARIANTS) {
            converters.readAll(variant).forEach(openApi.getComponents()::addSchemas);
            final String name = converters.readAllAsResolvedSchema(variant).schema.getName();
            oneOf.addOneOfItem(new Schema<>().$ref(Components.COMPONENTS_SCHEMAS_REF + name));
        }
        openApi.getPaths().values().stream()
            .flatMap(path -> path.readOperations().stream())
            .map(operation -> operation.getResponses() == null ? null : operation.getResponses().get(BAD_REQUEST))
            .filter(Objects::nonNull)
            .forEach(response -> document(response, oneOf));
    }

    private static void document(final ApiResponse response, final Schema<Object> oneOf) {
        if (response.getContent() == null) {
            response.setContent(new Content());
        }
        final MediaType mediaType = response.getContent().get(ProblemDetails.MEDIA_TYPE);
        if (mediaType == null) {
            response.getContent().addMediaType(ProblemDetails.MEDIA_TYPE, new MediaType().schema(oneOf));
        } else {
            mediaType.setSchema(oneOf);
        }
    }
}
