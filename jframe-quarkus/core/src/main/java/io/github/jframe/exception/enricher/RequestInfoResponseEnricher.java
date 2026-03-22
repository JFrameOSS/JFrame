package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;

import java.net.URI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;

/**
 * Enricher that populates request information on the error response resource.
 *
 * <p>Extracts method, URI, query string, and content type from the JAX-RS request context.
 */
@ApplicationScoped
public class RequestInfoResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        resource.setMethod(requestContext.getMethod());

        final URI requestUri = requestContext.getUriInfo().getRequestUri();
        final String rawQuery = requestUri.getRawQuery();

        if (rawQuery != null) {
            final String fullUri = requestUri.toString();
            resource.setUri(fullUri.substring(0, fullUri.indexOf('?')));
            resource.setQuery(rawQuery);
        } else {
            resource.setUri(requestUri.toString());
            resource.setQuery(null);
        }

        final MediaType mediaType = requestContext.getMediaType();
        if (mediaType != null) {
            resource.setContentType(mediaType.getType() + "/" + mediaType.getSubtype());
        } else {
            resource.setContentType(null);
        }
    }
}
