package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.FilterConfig;
import io.github.jframe.logging.filter.JFrameFilter;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.RequestId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;

/**
 * JAX-RS filter that generates and propagates a unique request ID for each HTTP request.
 *
 * <p>On the inbound request: generates a UUID, stores it in the {@link RequestId} ThreadLocal.
 * On the outbound response: adds the request ID to the response header (if not already present).
 */
@Provider
@ApplicationScoped
@Priority(200)
@Slf4j
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter, JFrameFilter {

    private final FilterConfig filterConfig;

    /**
     * Creates a new {@code RequestIdFilter} with the given filter configuration.
     *
     * @param filterConfig the filter configuration used to determine whether the filter is enabled
     */
    public RequestIdFilter(final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (!filterConfig.requestId().enabled()) {
            return;
        }
        final UUID requestId = UUID.randomUUID();
        RequestId.set(requestId);
        KibanaLogFields.tag(REQUEST_ID, RequestId.get());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        if (!filterConfig.requestId().enabled()) {
            return;
        }
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        final String requestId = RequestId.get();
        if (requestId != null && !headers.containsKey(REQ_ID_HEADER)) {
            headers.putSingle(REQ_ID_HEADER, requestId);
        }
    }
}
