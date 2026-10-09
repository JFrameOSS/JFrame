package io.github.jframe.openapi;

import io.github.support.UnitTest;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.MediaType;
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
            final MediaType mediaType = response.getContent().getMediaType(PROBLEM_JSON);
            assertThat(code, mediaType.getSchema().getRef(), startsWith("#/components/schemas/"));
        }
        assertThat(
            operation.getResponses().getAPIResponse("401").getContent().getMediaType(PROBLEM_JSON).getSchema().getRef(),
            endsWith("/ErrorResponseResource")
        );
        assertThat(
            operation.getResponses().getAPIResponse("429").getContent().getMediaType(PROBLEM_JSON).getSchema().getRef(),
            endsWith("RateLimitErrorResponseResource")
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
}
