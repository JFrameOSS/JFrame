package io.github.jframe.openapi;

import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import static io.quarkus.smallrye.openapi.OpenApiFilter.RunStage.RUNTIME_PER_REQUEST;

/**
 * OASFilter that automatically adds standard error response documentation to all operations.
 *
 * <p>Adds 400, 429, and 500 error responses to every operation that does not already define them,
 * ensuring consistent error documentation across all endpoints without requiring per-endpoint {@code @APIResponse} annotations.
 */
@Slf4j
@ApplicationScoped
@OpenApiFilter(stages = RUNTIME_PER_REQUEST)
public class JFrameErrorResponseFilter implements OASFilter {

    private static final String STATUS_400 = "400";
    private static final String STATUS_429 = "429";
    private static final String STATUS_500 = "500";

    private static final String MEDIA_TYPE_JSON = "application/json";

    @Override
    public void filterOpenAPI(final OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }

        for (final Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().getPathItems().entrySet()) {
            final PathItem pathItem = pathEntry.getValue();
            if (pathItem.getOperations() == null) {
                continue;
            }

            for (final Map.Entry<PathItem.HttpMethod, Operation> operationEntry : pathItem.getOperations().entrySet()) {
                addStandardErrorResponses(operationEntry.getValue());
            }
        }
    }

    private static void addStandardErrorResponses(final Operation operation) {
        final APIResponses responses = ensureResponses(operation);
        addIfAbsent(responses, STATUS_400, "Bad Request", ApiErrorResponseResource.class.getSimpleName());
        addIfAbsent(responses, STATUS_429, "Too Many Requests", RateLimitErrorResponseResource.class.getSimpleName());
        addIfAbsent(responses, STATUS_500, "Internal Server Error", ErrorResponseResource.class.getSimpleName());
    }

    private static APIResponses ensureResponses(final Operation operation) {
        if (operation.getResponses() == null) {
            final APIResponses responses = OASFactory.createObject(APIResponses.class);
            operation.setResponses(responses);
            return responses;
        }
        return operation.getResponses();
    }

    private static void addIfAbsent(final APIResponses responses, final String statusCode,
        final String description, final String schemaRef) {
        if (responses.hasAPIResponse(statusCode)) {
            return;
        }
        responses.addAPIResponse(statusCode, buildErrorResponse(description, schemaRef));
    }

    private static APIResponse buildErrorResponse(final String description, final String schemaRef) {
        final Schema schema = OASFactory.createObject(Schema.class)
            .ref("#/components/schemas/" + schemaRef);

        final MediaType mediaType = OASFactory.createObject(MediaType.class)
            .schema(schema);

        final Content content = OASFactory.createObject(Content.class)
            .addMediaType(MEDIA_TYPE_JSON, mediaType);

        return OASFactory.createObject(APIResponse.class)
            .description(description)
            .content(content);
    }
}
