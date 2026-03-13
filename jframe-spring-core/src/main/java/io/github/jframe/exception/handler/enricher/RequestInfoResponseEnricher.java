package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * This enricher copies information from the original web request onto the error response resource.
 *
 * <p>It only applies to a {@link ServletWebRequest}.</p>
 */
@Component
public class RequestInfoResponseEnricher implements ErrorResponseEnricher {

    /**
     * {@inheritDoc}
     *
     * <p><strong>NOTE:</strong> This enricher only applies if the request is a {@link ServletWebRequest}.
     */
    @Override
    public void doEnrich(
        final ErrorResponseResource errorResponseResource,
        final Throwable throwable,
        final WebRequest request,
        final HttpStatus httpStatus) {
        if (request instanceof final ServletWebRequest servletWebRequest) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletWebRequest.getNativeRequest();
            errorResponseResource.setUri(httpServletRequest.getRequestURI());
            errorResponseResource.setQuery(httpServletRequest.getQueryString());
            errorResponseResource.setMethod(httpServletRequest.getMethod());
            errorResponseResource.setContentType(httpServletRequest.getContentType());
        }
    }
}
