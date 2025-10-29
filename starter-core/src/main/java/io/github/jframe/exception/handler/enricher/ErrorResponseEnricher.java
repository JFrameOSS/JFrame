package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.handler.JFrameResponseEntityExceptionHandler;
import io.github.jframe.exception.resource.ErrorResponseResource;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * This interface defines a way to enrich the error response with values applicable to the given situation.
 *
 * <p>The situation in this case consists of the exception, the original request and the http
 * status.
 *
 * <p>A {@link JFrameResponseEntityExceptionHandler} has a list of ErrorResponseEnrichers, which
 * will make sure that all relevant information is captured in the error response.
 */
@FunctionalInterface
public interface ErrorResponseEnricher {

    /**
     * Default type that first retrieves the original throwable stored in the error response resource, and then calls
     * {@link #doEnrich(ErrorResponseResource, Throwable, WebRequest, HttpStatus)}.
     *
     * <p>Note that the http status is a given, it is assumed to be determined in the exception
     * handler.
     *
     * @param errorResponseResource the error response resource
     * @param request               the original web request
     * @param httpStatus            the http status that will be returned
     */
    default void enrich(
        final ErrorResponseResource errorResponseResource, final WebRequest request, final HttpStatus httpStatus) {
        doEnrich(errorResponseResource, errorResponseResource.getThrowable(), request, httpStatus);
    }

    /**
     * Performs the enrichment of the error response resource.
     *
     * <p>Note that the http status is a given, it is assumed to be determined in the exception
     * handler.
     *
     * @param errorResponseResource the error response resource
     * @param throwable             the exception that was raised
     * @param request               the original web request
     * @param httpStatus            the http status that will be returned
     */
    void doEnrich(
        ErrorResponseResource errorResponseResource,
        Throwable throwable,
        WebRequest request,
        HttpStatus httpStatus);
}
