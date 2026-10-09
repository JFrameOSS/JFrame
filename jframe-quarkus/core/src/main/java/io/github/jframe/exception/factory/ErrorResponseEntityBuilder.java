package io.github.jframe.exception.factory;

import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;
import io.quarkus.arc.properties.IfBuildProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Builds RFC 9457 Problem Details bodies: creates the resource, applies standard members, runs enrichers
 * ordered by {@link Priority} (unannotated last), then derives {@code type} from the final error code.
 */
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@ApplicationScoped
public class ErrorResponseEntityBuilder {

    /** Property holding the {@code type} base URI. */
    public static final String TYPE_BASE_URI_PROPERTY = "jframe.exception.type-base-uri";

    private static final Comparator<ErrorResponseEnricher> BY_PRIORITY =
        Comparator.comparingInt(ErrorResponseEntityBuilder::priorityOf);

    private final DefaultErrorResponseFactory factory;

    private final List<ErrorResponseEnricher> enrichers;

    private final String typeBaseUri;

    /**
     * CDI constructor.
     *
     * @param factory          the resource factory
     * @param enricherInstance all registered enrichers
     */
    @Inject
    public ErrorResponseEntityBuilder(
                                      final DefaultErrorResponseFactory factory,
                                      final Instance<ErrorResponseEnricher> enricherInstance) {
        this(
            factory,
            enricherInstance.stream().toList(),
            ConfigProvider.getConfig().getOptionalValue(TYPE_BASE_URI_PROPERTY, String.class)
                .orElse(ProblemDetails.DEFAULT_TYPE_BASE_URI)
        );
    }

    /**
     * Constructor for use without CDI; uses the default {@code type} base URI.
     *
     * @param factory   the resource factory
     * @param enrichers the enrichers to apply
     */
    ErrorResponseEntityBuilder(final DefaultErrorResponseFactory factory, final List<ErrorResponseEnricher> enrichers) {
        this(factory, enrichers, ProblemDetails.DEFAULT_TYPE_BASE_URI);
    }

    private ErrorResponseEntityBuilder(
                                       final DefaultErrorResponseFactory factory,
                                       final List<ErrorResponseEnricher> enrichers,
                                       final String typeBaseUri) {
        this.factory = factory;
        final List<ErrorResponseEnricher> sorted = new ArrayList<>(enrichers);
        sorted.sort(BY_PRIORITY);
        this.enrichers = List.copyOf(sorted);
        this.typeBaseUri = typeBaseUri;
    }

    /**
     * Builds the Problem Details body for the given throwable.
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
        ProblemDetails.apply(resource, statusCode, instance(requestContext), typeBaseUri);
        for (final ErrorResponseEnricher enricher : enrichers) {
            enricher.enrich(resource, requestContext, statusCode);
        }
        resource.setType(ProblemDetails.type(typeBaseUri, resource.getErrorCode()));
        return resource;
    }

    private static String instance(final ContainerRequestContext requestContext) {
        final UriInfo uriInfo = requestContext == null ? null : requestContext.getUriInfo();
        final URI requestUri = uriInfo == null ? null : uriInfo.getRequestUri();
        return requestUri == null ? null : requestUri.getRawPath();
    }

    /** Priority from the class hierarchy (handles CDI client proxies); unannotated sorts last. */
    private static int priorityOf(final ErrorResponseEnricher enricher) {
        Class<?> type = enricher.getClass();
        while (type != null) {
            final Priority priority = type.getAnnotation(Priority.class);
            if (priority != null) {
                return priority.value();
            }
            type = type.getSuperclass();
        }
        return Integer.MAX_VALUE;
    }
}
