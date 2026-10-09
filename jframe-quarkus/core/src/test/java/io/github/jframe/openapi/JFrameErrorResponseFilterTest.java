package io.github.jframe.openapi;

import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Unit Test - JFrame OpenAPI error response filter")
class JFrameErrorResponseFilterTest extends UnitTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String[] STANDARD_CODES = {
        "400",
        "401",
        "403",
        "404",
        "429",
        "500"
    };

    private static OpenAPI anOpenApiWithOperation(final Operation operation) {
        return OASFactory.createOpenAPI()
            .paths(OASFactory.createPaths().addPathItem("/orders", OASFactory.createPathItem().GET(operation)));
    }

    @Test
    @DisplayName("Should document standard error responses as application/problem+json")
    void shouldDocumentErrorResponsesAsProblemJson() {
        // Given: An operation without error responses
        final Operation operation = OASFactory.createOperation();

        // When: Filtering the document
        new JFrameErrorResponseFilter().filterOpenAPI(anOpenApiWithOperation(operation));

        // Then: 400/401/403/404/429/500 (Spring parity) use problem+json only, with Problem Details schemas
        assertThat(operation.getResponses().getAPIResponses().keySet(), containsInAnyOrder(STANDARD_CODES));
        for (final String code : STANDARD_CODES) {
            final APIResponse response = operation.getResponses().getAPIResponse(code);
            assertThat(code, response.getContent().getMediaTypes().keySet(), contains(PROBLEM_JSON));
            assertThat(code, refs(response.getContent().getMediaType(PROBLEM_JSON).getSchema()), is(not(empty())));
        }
        assertThat(
            operation.getResponses().getAPIResponse("401").getContent().getMediaType(PROBLEM_JSON).getSchema().getRef(),
            endsWith("/ProblemDetails")
        );
        assertThat(
            operation.getResponses().getAPIResponse("429").getContent().getMediaType(PROBLEM_JSON).getSchema().getRef(),
            endsWith("/RateLimitProblemDetails")
        );
    }

    @Test
    @DisplayName("Should keep application-defined responses and stay deterministic when applied twice")
    void shouldKeepExistingResponsesAndStayDeterministic() {
        // Given: An operation that already documents 400
        final APIResponse own = OASFactory.createAPIResponse().description("Own bad request");
        final Operation operation = OASFactory.createOperation()
            .responses(OASFactory.createAPIResponses().addAPIResponse("400", own));
        final OpenAPI openApi = anOpenApiWithOperation(operation);
        final JFrameErrorResponseFilter filter = new JFrameErrorResponseFilter();

        // When: Filtering twice
        filter.filterOpenAPI(openApi);
        filter.filterOpenAPI(openApi);

        // Then: Own 400 untouched, each code exactly once
        final APIResponses responses = operation.getResponses();
        assertThat(responses.getAPIResponse("400"), is(sameInstance(own)));
        assertThat(responses.getAPIResponses().keySet(), containsInAnyOrder(STANDARD_CODES));
    }

    @Test
    @DisplayName("Should reference every 400 variant via oneOf and no *ResponseResource schema")
    void shouldReferenceAll400Variants() {
        // Given: An operation without error responses
        final Operation operation = OASFactory.createOperation();

        // When: Filtering the document
        new JFrameErrorResponseFilter().filterOpenAPI(anOpenApiWithOperation(operation));

        // Then: 400 references all Problem Details variants by their published names
        final List<String> refs = new ArrayList<>();
        operation.getResponses().getAPIResponses().values()
            .forEach(response -> refs.addAll(refs(response.getContent().getMediaType(PROBLEM_JSON).getSchema())));
        assertThat(
            refs(operation.getResponses().getAPIResponse("400").getContent().getMediaType(PROBLEM_JSON).getSchema()),
            hasItems(
                endsWith("/ProblemDetails"),
                endsWith("/ValidationProblemDetails"),
                endsWith("/InvalidSortProblemDetails"),
                endsWith("/InvalidSearchProblemDetails"),
                endsWith("/InvalidPageProblemDetails")
            )
        );
        assertThat(refs, everyItem(not(endsWith("ResponseResource"))));
    }

    @Test
    @DisplayName("Should register full Problem Details schemas when components are missing")
    void shouldRegisterFullSchemasWhenComponentsMissing() {
        // Given: A document without components
        final OpenAPI openApi = anOpenApiWithOperation(OASFactory.createOperation());

        // When: Filtering the document
        new JFrameErrorResponseFilter().filterOpenAPI(openApi);

        // Then: Each schema carries base and subtype-specific properties
        final String[] base = {
            "type",
            "title",
            "status",
            "detail",
            "instance",
            "errorCode",
            "txId",
            "traceId",
            "spanId"
        };
        assertThat(props(openApi, "ProblemDetails"), hasItems(base));
        assertThat(props(openApi, "ValidationProblemDetails"), hasItems(base));
        assertThat(props(openApi, "ValidationProblemDetails"), hasItem("errors"));
        assertThat(props(openApi, "RateLimitProblemDetails"), hasItems("status", "limit", "remaining", "resetDate"));
        assertThat(props(openApi, "InvalidSortProblemDetails"), hasItems("status", "rejectedField", "sortableFields"));
        assertThat(
            props(openApi, "InvalidSearchProblemDetails"),
            hasItems("status", "rejectedField", "rejectedValue", "searchableFields")
        );
        assertThat(props(openApi, "InvalidPageProblemDetails"), hasItems("status", "rejectedParameter", "rejectedValue"));
        final Schema problem = openApi.getComponents().getSchemas().get("ProblemDetails");
        assertThat(problem.getProperties().get("type").getFormat(), is("uri-reference"));
        assertThat(problem.getProperties().get("status").getType(), contains(Schema.SchemaType.INTEGER));
        final Schema errors = openApi.getComponents().getSchemas().get("ValidationProblemDetails").getProperties().get("errors");
        assertThat(errors.getType(), contains(Schema.SchemaType.ARRAY));
        assertThat(errors.getItems().getProperties().keySet(), containsInAnyOrder("field", "code"));
        assertThat(
            openApi.getComponents().getSchemas().get("RateLimitProblemDetails").getProperties().get("resetDate").getFormat(),
            is("date-time")
        );
    }

    @Test
    @DisplayName("Should keep schemas already scanned")
    void shouldKeepScannedSchemas() {
        // Given: A document with a scanned ProblemDetails schema
        final Schema scanned = OASFactory.createSchema().description("scanned");
        final OpenAPI openApi = anOpenApiWithOperation(OASFactory.createOperation())
            .components(OASFactory.createComponents().addSchema("ProblemDetails", scanned));

        // When: Filtering the document
        new JFrameErrorResponseFilter().filterOpenAPI(openApi);

        // Then: Scanned schema untouched
        assertThat(openApi.getComponents().getSchemas().get("ProblemDetails"), is(sameInstance(scanned)));
        assertThat(scanned.getProperties(), is(nullValue()));
    }

    private static java.util.Set<String> props(final OpenAPI openApi, final String name) {
        return openApi.getComponents().getSchemas().get(name).getProperties().keySet();
    }

    /** Refs of the schema itself or its {@code oneOf} members. */
    private static List<String> refs(final Schema schema) {
        final List<String> refs = new ArrayList<>();
        if (schema.getRef() != null) {
            refs.add(schema.getRef());
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(part -> refs.addAll(refs(part)));
        }
        return refs;
    }
}
