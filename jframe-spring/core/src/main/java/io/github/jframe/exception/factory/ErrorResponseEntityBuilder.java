package io.github.jframe.exception.factory;

import io.github.jframe.exception.handler.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ProblemDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Builds RFC 9457 Problem Details bodies for exceptions.
 */
public class ErrorResponseEntityBuilder {

    /** Property holding the {@code type} base URI. */
    public static final String TYPE_BASE_URI_PROPERTY = "jframe.exception.type-base-uri";

    private final ExceptionResponseFactory exceptionResponseFactory;

    private final List<ErrorResponseEnricher> errorResponseEnrichers = new ArrayList<>();

    private final String typeBaseUri;

    /** The constructor; enrichers keep their injection order, which honours {@code @Order}. */
    public ErrorResponseEntityBuilder(final ExceptionResponseFactory exceptionResponseFactory,
                                      final List<ErrorResponseEnricher> errorResponseEnrichers,
                                      @Value(
                                          "${" + TYPE_BASE_URI_PROPERTY + ":" + ProblemDetails.DEFAULT_TYPE_BASE_URI + "}"
                                      ) final String typeBaseUri) {
        this.exceptionResponseFactory = requireNonNull(exceptionResponseFactory);
        this.typeBaseUri = typeBaseUri;
        if (nonNull(errorResponseEnrichers)) {
            this.errorResponseEnrichers.addAll(errorResponseEnrichers);
            sortEnrichers();
        }
    }

    /**
     * Builds the Problem Details body: creates the resource, runs enrichers in order, then derives {@code type}.
     *
     * @param throwable the exception
     * @param status    the HTTP status
     * @param request   the current request
     * @return an error response
     */
    @SuppressWarnings("unchecked")
    public <T extends ErrorResponseResource> T buildErrorResponseBody(final Throwable throwable,
        final HttpStatus status,
        final WebRequest request) {
        final ErrorResponseResource resource = exceptionResponseFactory.create(throwable);
        ProblemDetails.apply(resource, status.value(), instance(request), typeBaseUri);
        errorResponseEnrichers.forEach(enricher -> enricher.enrich(resource, request, status));
        resource.setType(ProblemDetails.type(typeBaseUri, resource.getErrorCode()));
        return (T) resource;
    }

    /**
     * Registers a {@link ErrorResponseEnricher}.
     *
     * @param errorResponseEnricher the error response enricher
     */
    public void addResponseEnricher(final ErrorResponseEnricher errorResponseEnricher) {
        errorResponseEnrichers.add(errorResponseEnricher);
        sortEnrichers();
    }

    /**
     * De-registers a {@link ErrorResponseEnricher}.
     *
     * @param errorResponseEnricher the error response enricher
     */
    public void removeResponseEnricher(final ErrorResponseEnricher errorResponseEnricher) {
        errorResponseEnrichers.remove(errorResponseEnricher);
    }

    /**
     * Returns the registered response enrichers in execution order.
     *
     * @return the response enrichers
     */
    public Collection<ErrorResponseEnricher> getResponseEnrichers() {
        return List.copyOf(errorResponseEnrichers);
    }

    /** Stable sort: keeps injection order for enrichers whose order is only declared on a {@code @Bean} method. */
    private void sortEnrichers() {
        errorResponseEnrichers.sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    private static String instance(final WebRequest request) {
        if (request instanceof final ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }
}
