package io.github.jframe.exception.factory;

import io.github.jframe.exception.handler.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * This class creates proper HTTP response bodies for exceptions.
 */
@Component
public class ErrorResponseEntityBuilder {

    private final ExceptionResponseFactory exceptionResponseFactory;

    private final Set<ErrorResponseEnricher> errorResponseEnrichers = new HashSet<>();

    /** The constructor. */
    public ErrorResponseEntityBuilder(final ExceptionResponseFactory exceptionResponseFactory,
                                      final List<ErrorResponseEnricher> errorResponseEnrichers) {
        this.exceptionResponseFactory = requireNonNull(exceptionResponseFactory);
        if (nonNull(errorResponseEnrichers)) {
            this.errorResponseEnrichers.addAll(errorResponseEnrichers);
        }
    }

    /**
     * Builds a meaningful response body for the given throwable, HTTP status and request.
     *
     * <p>This method constructs an {@link ErrorResponseResource} using {@link ExceptionResponseFactory} and then applies the error response
     * enrichers returned from {@link #getResponseEnrichers()} to complete
     * the response.
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
        getResponseEnrichers().forEach(enricher -> enricher.enrich(resource, request, status));
        return (T) resource;
    }

    /**
     * Registers a {@link ErrorResponseEnricher}.
     *
     * @param errorResponseEnricher the error response enricher
     */
    public void addResponseEnricher(final ErrorResponseEnricher errorResponseEnricher) {
        errorResponseEnrichers.add(errorResponseEnricher);
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
     * Returns a collection of registered response enrichers.
     *
     * @return the response enrichers
     */
    public Collection<ErrorResponseEnricher> getResponseEnrichers() {
        return new HashSet<>(errorResponseEnrichers);
    }
}
