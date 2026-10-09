package io.github.jframe.openapi;

import io.github.jframe.exception.page.InvalidPageErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchErrorResponseResource;
import io.github.jframe.exception.sort.InvalidSortErrorResponseResource;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
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
 * <p>Adds 400, 401, 403, 404, 429, and 500 error responses to every operation that does not already define them,
 * ensuring consistent error documentation across all endpoints without requiring per-endpoint {@code @APIResponse} annotations.
 */
@Slf4j
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@ApplicationScoped
@OpenApiFilter(stages = RUNTIME_PER_REQUEST)
public class JFrameErrorResponseFilter implements OASFilter {

    private static final String STATUS_400 = "400";
    private static final String STATUS_401 = "401";
    private static final String STATUS_403 = "403";
    private static final String STATUS_404 = "404";
    private static final String STATUS_429 = "429";
    private static final String STATUS_500 = "500";

    private static final String MEDIA_TYPE_PROBLEM_JSON = ProblemDetails.MEDIA_TYPE;
    private static final String REJECTED_FIELD = "rejectedField";
    private static final String REJECTED_VALUE = "rejectedValue";
    private static final String REF_PREFIX = "#/components/schemas/";

    private static final String PROBLEM = schemaName(ErrorResponseResource.class);
    private static final String RATE_LIMIT = schemaName(RateLimitErrorResponseResource.class);
    private static final List<String> BAD_REQUEST_VARIANTS = List.of(
        PROBLEM,
        schemaName(ValidationErrorResponseResource.class),
        schemaName(InvalidSortErrorResponseResource.class),
        schemaName(InvalidSearchErrorResponseResource.class),
        schemaName(InvalidPageErrorResponseResource.class)
    );

    @Override
    public void filterOpenAPI(final OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }
        registerSchemas(openAPI);

        for (final PathItem pathItem : openAPI.getPaths().getPathItems().values()) {
            if (pathItem.getOperations() != null) {
                pathItem.getOperations().values().forEach(JFrameErrorResponseFilter::addStandardErrorResponses);
            }
        }
    }

    private static void addStandardErrorResponses(final Operation operation) {
        final APIResponses responses = ensureResponses(operation);
        addIfAbsent(responses, STATUS_400, "Bad Request", oneOf(BAD_REQUEST_VARIANTS));
        addIfAbsent(responses, STATUS_401, "Unauthorized", ref(PROBLEM));
        addIfAbsent(responses, STATUS_403, "Forbidden", ref(PROBLEM));
        addIfAbsent(responses, STATUS_404, "Not Found", ref(PROBLEM));
        addIfAbsent(responses, STATUS_429, "Too Many Requests", ref(RATE_LIMIT));
        addIfAbsent(responses, STATUS_500, "Internal Server Error", ref(PROBLEM));
    }

    /** Registers full Problem Details schemas missing from the document (SmallRye scans only referenced types). */
    private static void registerSchemas(final OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        final Components components = openAPI.getComponents();
        final Map<Class<?>, Map<String, Schema>> extras = new LinkedHashMap<>();
        extras.put(ErrorResponseResource.class, Map.of());
        extras.put(
            RateLimitErrorResponseResource.class,
            Map.of(
                "limit",
                prop(Schema.SchemaType.INTEGER, null),
                "remaining",
                prop(Schema.SchemaType.INTEGER, null),
                "resetDate",
                prop(Schema.SchemaType.STRING, "date-time")
            )
        );
        extras.put(
            ValidationErrorResponseResource.class,
            Map.of(
                "errors",
                prop(Schema.SchemaType.ARRAY, null).items(
                    prop(Schema.SchemaType.OBJECT, null).properties(
                        new LinkedHashMap<>(
                            Map.of("field", prop(Schema.SchemaType.STRING, null), "code", prop(Schema.SchemaType.STRING, null))
                        )
                    )
                )
            )
        );
        extras.put(
            InvalidSortErrorResponseResource.class,
            Map.of(
                REJECTED_FIELD,
                prop(Schema.SchemaType.STRING, null),
                "sortableFields",
                prop(Schema.SchemaType.ARRAY, null).items(prop(Schema.SchemaType.STRING, null))
            )
        );
        extras.put(
            InvalidSearchErrorResponseResource.class,
            Map.of(
                REJECTED_FIELD,
                prop(Schema.SchemaType.STRING, null),
                REJECTED_VALUE,
                prop(Schema.SchemaType.STRING, null),
                "searchableFields",
                prop(Schema.SchemaType.ARRAY, null).items(prop(Schema.SchemaType.STRING, null))
            )
        );
        extras.put(
            InvalidPageErrorResponseResource.class,
            Map.of(
                "rejectedParameter",
                prop(Schema.SchemaType.STRING, null),
                REJECTED_VALUE,
                prop(Schema.SchemaType.INTEGER, null)
            )
        );
        extras.forEach((type, own) -> {
            final String name = schemaName(type);
            if (components.getSchemas() == null || !components.getSchemas().containsKey(name)) {
                final Schema schema = prop(Schema.SchemaType.OBJECT, null).properties(baseProperties());
                own.forEach(schema::addProperty);
                components.addSchema(name, schema);
            }
        });
    }

    private static Map<String, Schema> baseProperties() {
        final Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("type", prop(Schema.SchemaType.STRING, "uri-reference"));
        properties.put("title", prop(Schema.SchemaType.STRING, null));
        properties.put("status", prop(Schema.SchemaType.INTEGER, null));
        for (final String name : List.of("detail", "instance", "errorCode", "txId", "traceId", "spanId")) {
            properties.put(name, prop(Schema.SchemaType.STRING, null));
        }
        return properties;
    }

    private static Schema prop(final Schema.SchemaType type, final String format) {
        return OASFactory.createObject(Schema.class).type(List.of(type)).format(format);
    }

    private static String schemaName(final Class<?> type) {
        final org.eclipse.microprofile.openapi.annotations.media.Schema annotation =
            type.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
        return annotation != null && !annotation.name().isEmpty() ? annotation.name() : type.getSimpleName();
    }

    private static Schema ref(final String name) {
        return OASFactory.createObject(Schema.class).ref(REF_PREFIX + name);
    }

    private static Schema oneOf(final List<String> names) {
        return OASFactory.createObject(Schema.class).oneOf(names.stream().map(JFrameErrorResponseFilter::ref).toList());
    }

    private static APIResponses ensureResponses(final Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(OASFactory.createObject(APIResponses.class));
        }
        return operation.getResponses();
    }

    private static void addIfAbsent(final APIResponses responses, final String statusCode,
        final String description, final Schema schema) {
        if (responses.hasAPIResponse(statusCode)) {
            return;
        }
        responses.addAPIResponse(statusCode, buildErrorResponse(description, schema));
    }

    private static APIResponse buildErrorResponse(final String description, final Schema schema) {
        final MediaType mediaType = OASFactory.createObject(MediaType.class).schema(schema);
        final Content content = OASFactory.createObject(Content.class).addMediaType(MEDIA_TYPE_PROBLEM_JSON, mediaType);

        return OASFactory.createObject(APIResponse.class)
            .description(description)
            .content(content);
    }
}
