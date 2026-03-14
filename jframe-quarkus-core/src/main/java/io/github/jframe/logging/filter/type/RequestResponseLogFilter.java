package io.github.jframe.logging.filter.type;

import java.io.IOException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS filter that logs basic request and response details.
 *
 * <p>On the inbound request: logs the HTTP method, path, and content type.
 * On the outbound response: logs the HTTP status code.
 * Handles {@code null} media types gracefully.
 */
public class RequestResponseLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseLogFilter.class);

    private static final String NO_CONTENT_TYPE = "none";

    /** Creates a new {@code RequestResponseLogFilter}. */
    public RequestResponseLogFilter() {
        // no-arg constructor
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final String method = requestContext.getMethod();
        final String path = requestContext.getUriInfo() != null
            ? requestContext.getUriInfo().getPath()
            : "unknown";
        final MediaType mediaType = requestContext.getMediaType();
        final String contentType = mediaType != null ? mediaType.toString() : NO_CONTENT_TYPE;
        LOG.debug("Incoming request: {} {} (Content-Type: {})", method, path, contentType);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        final int status = responseContext.getStatus();
        final MediaType mediaType = responseContext.getMediaType();
        final String contentType = mediaType != null ? mediaType.toString() : NO_CONTENT_TYPE;
        LOG.debug("Outgoing response: status={}, Content-Type: {}", status, contentType);
    }
}
