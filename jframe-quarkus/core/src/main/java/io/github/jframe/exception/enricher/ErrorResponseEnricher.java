package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Strategy interface for enriching a {@link ErrorResponseResource} with additional context.
 *
 * <p>Implementations enricher specific fields on the resource based on the request context,
 * throwable, or status code.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ErrorResponseEnricher {

    /**
     * Enriches the given resource using the throwable stored on the resource.
     *
     * @param resource       the resource to enrich
     * @param requestContext the JAX-RS request context
     * @param statusCode     the HTTP status code
     */
    default void enrich(
        final ErrorResponseResource resource,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        doEnrich(resource, resource.getThrowable(), requestContext, statusCode);
    }

    /**
     * Performs the actual enrichment logic.
     *
     * @param resource       the resource to enrich
     * @param throwable      the throwable that caused this error
     * @param requestContext the JAX-RS request context
     * @param statusCode     the HTTP status code
     */
    void doEnrich(
        ErrorResponseResource resource,
        Throwable throwable,
        ContainerRequestContext requestContext,
        int statusCode);
}
