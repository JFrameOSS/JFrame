package io.github.jframe.exception.factory;

import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Orchestrates the creation and enrichment of an {@link ErrorResponseResource}.
 *
 * <p>Uses a {@link DefaultErrorResponseFactory} to create the appropriate resource type,
 * then applies all registered {@link ErrorResponseEnricher}s in order.
 *
 * <p>Enrichers are discovered via CDI {@link Instance} so that enrichers from optional modules
 * (e.g., {@code jframe-quarkus-otlp}) are automatically included when present on the classpath.
 * When no enrichers are registered, the enrichment step is skipped gracefully.
 */
@Slf4j
@ApplicationScoped
public class ErrorResponseEntityBuilder {

    private final DefaultErrorResponseFactory factory;

    private final List<ErrorResponseEnricher> enrichers;

    /**
     * Constructs a new {@code ErrorResponseEntityBuilder} with CDI-injected dependencies.
     *
     * @param factory          the factory used to create the initial response resource
     * @param enricherInstance CDI instance providing all registered {@link ErrorResponseEnricher}s
     */
    @Inject
    public ErrorResponseEntityBuilder(
                                      final DefaultErrorResponseFactory factory,
                                      final Instance<ErrorResponseEnricher> enricherInstance) {
        this.factory = factory;
        this.enrichers = enricherInstance.stream().toList();
    }

    /**
     * Constructs a new {@code ErrorResponseEntityBuilder} directly from a list.
     *
     * <p>Package-private to support unit testing without a CDI container.
     *
     * @param factory   the factory used to create the initial response resource
     * @param enrichers the list of enrichers to apply, in order
     */
    ErrorResponseEntityBuilder(
                               final DefaultErrorResponseFactory factory,
                               final List<ErrorResponseEnricher> enrichers) {
        this.factory = factory;
        this.enrichers = enrichers;
    }

    /**
     * Builds an error response resource for the given throwable.
     *
     * @param throwable      the throwable that caused the error
     * @param requestContext the JAX-RS request context
     * @param statusCode     the HTTP status code
     * @return the enriched error response resource
     */
    public ErrorResponseResource buildErrorResponseBody(
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        final ErrorResponseResource resource = factory.create(throwable);

        for (final ErrorResponseEnricher enricher : enrichers) {
            enricher.enrich(resource, requestContext, statusCode);
        }

        return resource;
    }
}
