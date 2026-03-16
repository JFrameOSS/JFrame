package io.github.jframe.exception.factory;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;

import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ErrorResponseEntityBuilder}.
 *
 * <p>Verifies the builder correctly orchestrates factory creation and enricher invocation including:
 * <ul>
 * <li>Factory is called to create the initial resource</li>
 * <li>All enrichers are invoked with correct arguments</li>
 * <li>Resource type is preserved after enrichment</li>
 * <li>Empty enricher set works without error</li>
 * </ul>
 */
@DisplayName("Unit Test - Error Response Entity Builder")
public class ErrorResponseEntityBuilderTest extends UnitTest {

    @Mock
    private DefaultErrorResponseFactory factory;

    @Test
    @DisplayName("Should call factory and invoke all enrichers on build")
    public void shouldCallFactoryAndInvokeAllEnrichersOnBuild() {
        // Given: A throwable, request context, and a set of mock enrichers
        final Throwable throwable = new RuntimeException("Test error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final int statusCode = 500;
        final ErrorResponseResource expectedResource = new ErrorResponseResource(throwable);

        when(factory.create(throwable)).thenReturn(expectedResource);

        final io.github.jframe.exception.enricher.ErrorResponseEnricher enricher1 = mock(
            io.github.jframe.exception.enricher.ErrorResponseEnricher.class
        );
        final io.github.jframe.exception.enricher.ErrorResponseEnricher enricher2 = mock(
            io.github.jframe.exception.enricher.ErrorResponseEnricher.class
        );

        final ErrorResponseEntityBuilder builder = new ErrorResponseEntityBuilder(factory, List.of(enricher1, enricher2));

        // When: Building the error response body
        final ErrorResponseResource result = builder.buildErrorResponseBody(throwable, requestContext, statusCode);

        // Then: Factory was called and both enrichers were invoked
        verify(factory).create(throwable);
        verify(enricher1).enrich(expectedResource, requestContext, statusCode);
        verify(enricher2).enrich(expectedResource, requestContext, statusCode);
        assertThat(result, is(notNullValue()));
    }

    @Test
    @DisplayName("Should return resource produced by factory")
    public void shouldReturnResourceProducedByFactory() {
        // Given: A factory that returns a known resource instance
        final Throwable throwable = new RuntimeException("Test error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ErrorResponseResource expectedResource = new ErrorResponseResource(throwable);

        when(factory.create(throwable)).thenReturn(expectedResource);

        final ErrorResponseEntityBuilder builder = new ErrorResponseEntityBuilder(factory, List.of());

        // When: Building the error response body
        final ErrorResponseResource result = builder.buildErrorResponseBody(throwable, requestContext, 400);

        // Then: The exact resource from factory is returned
        assertThat(result, is(expectedResource));
    }

    @Test
    @DisplayName("Should work with empty enricher collection")
    public void shouldWorkWithEmptyEnricherCollection() {
        // Given: An empty enricher collection
        final Throwable throwable = new RuntimeException("Test error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final ErrorResponseResource expectedResource = new ErrorResponseResource(throwable);

        when(factory.create(throwable)).thenReturn(expectedResource);

        final ErrorResponseEntityBuilder builder = new ErrorResponseEntityBuilder(factory, List.of());

        // When: Building the error response body
        final ErrorResponseResource result = builder.buildErrorResponseBody(throwable, requestContext, 500);

        // Then: No exception thrown and resource is returned
        assertThat(result, is(notNullValue()));
        assertThat(result, is(expectedResource));
    }

    @Test
    @DisplayName("Should invoke enrichers in order with correct arguments")
    public void shouldInvokeEnrichersInOrderWithCorrectArguments() {
        // Given: Multiple enrichers and a specific status code
        final Throwable throwable = new RuntimeException("Test error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final int statusCode = 404;
        final ErrorResponseResource resource = new ErrorResponseResource(throwable);

        when(factory.create(throwable)).thenReturn(resource);

        final io.github.jframe.exception.enricher.ErrorResponseEnricher enricher = mock(
            io.github.jframe.exception.enricher.ErrorResponseEnricher.class
        );

        final ErrorResponseEntityBuilder builder = new ErrorResponseEntityBuilder(factory, List.of(enricher));

        // When: Building the error response body
        builder.buildErrorResponseBody(throwable, requestContext, statusCode);

        // Then: Enricher invoked exactly once with the correct resource, context, and status code
        verify(enricher, times(1)).enrich(resource, requestContext, statusCode);
    }

    @Test
    @DisplayName("Should preserve resource type through enrichment")
    public void shouldPreserveResourceTypeThroughEnrichment() {
        // Given: A factory that returns an ApiErrorResponseResource subtype
        final Throwable throwable = new RuntimeException("Test error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final io.github.jframe.exception.resource.ApiErrorResponseResource apiResource =
            new io.github.jframe.exception.resource.ApiErrorResponseResource();

        when(factory.create(throwable)).thenReturn(apiResource);

        final ErrorResponseEntityBuilder builder = new ErrorResponseEntityBuilder(factory, List.of());

        // When: Building the error response body
        final ErrorResponseResource result = builder.buildErrorResponseBody(throwable, requestContext, 400);

        // Then: Returned resource is still the ApiErrorResponseResource
        assertThat(result, is(instanceOf(io.github.jframe.exception.resource.ApiErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should invoke single enricher with all three arguments")
    public void shouldInvokeSingleEnricherWithAllThreeArguments() {
        // Given: A throwable, context, status code, and single enricher
        final Throwable throwable = new RuntimeException("error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final int statusCode = 429;
        final ErrorResponseResource resource = new ErrorResponseResource(throwable);

        when(factory.create(throwable)).thenReturn(resource);

        final io.github.jframe.exception.enricher.ErrorResponseEnricher enricher = mock(
            io.github.jframe.exception.enricher.ErrorResponseEnricher.class
        );
        final ErrorResponseEntityBuilder builder = new ErrorResponseEntityBuilder(factory, List.of(enricher));

        // When: Building the error response body
        builder.buildErrorResponseBody(throwable, requestContext, statusCode);

        // Then: Enricher receives resource, requestContext, and statusCode
        verify(enricher).enrich(resource, requestContext, statusCode);
    }
}
