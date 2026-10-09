package io.github.jframe.exception.handler;

import io.github.jframe.exception.resource.MethodArgumentNotValidResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.support.UnitTest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * OpenAPI contract of {@link JFrameResponseEntityExceptionHandler}.
 *
 * <p>Runtime behaviour is covered end-to-end by {@code ExceptionHandlingContractTest}.
 */
@DisplayName("Exception Handler - OpenAPI contract")
public class JFrameResponseEntityExceptionHandlerTest extends UnitTest {

    private static List<ApiResponse> apiResponses() {
        return Arrays.stream(JFrameResponseEntityExceptionHandler.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(ApiResponse.class))
            .map(method -> method.getAnnotation(ApiResponse.class))
            .toList();
    }

    @Test
    @DisplayName("Should have unique @ApiResponse response codes across all handler methods")
    public void shouldHaveUniqueApiResponseCodesAcrossAllHandlerMethods() {
        // Given: all @ApiResponse annotations of the handler
        // When: collecting all responseCode values
        final List<String> allCodes = apiResponses().stream().map(ApiResponse::responseCode).toList();

        // Then: every code appears exactly once — duplicates make springdoc nondeterministic
        assertThat(
            "Duplicate @ApiResponse responseCodes: " + allCodes,
            allCodes.stream().distinct().count(),
            is(equalTo((long) allCodes.size()))
        );
    }

    @Test
    @DisplayName("Should document every error response as application/problem+json with a schema")
    public void shouldDocumentErrorResponsesAsProblemJson() {
        // Given: all @ApiResponse annotations of the handler
        // When: collecting their content declarations
        final List<Content> contents = apiResponses().stream().flatMap(response -> Arrays.stream(response.content())).toList();

        // Then: every content is problem+json with an implementation schema
        assertThat(contents, is(not(empty())));
        contents.forEach(content -> {
            assertThat(content.mediaType(), is(equalTo("application/problem+json")));
            assertThat(content.schema().implementation(), is(not(equalTo(Void.class))));
        });
    }

    @Test
    @DisplayName("Should have MethodArgumentNotValidResponseResource assignable to ValidationErrorResponseResource")
    public void shouldHaveMethodArgumentNotValidResponseResourceAssignableToValidationErrorResponseResource() {
        // Given: the two 400 validation response resource types
        // When: checking the type hierarchy
        final boolean isAssignable =
            ValidationErrorResponseResource.class.isAssignableFrom(MethodArgumentNotValidResponseResource.class);

        // Then: both validation 400 bodies share one OpenAPI schema shape
        assertThat(isAssignable, is(true));
    }
}
